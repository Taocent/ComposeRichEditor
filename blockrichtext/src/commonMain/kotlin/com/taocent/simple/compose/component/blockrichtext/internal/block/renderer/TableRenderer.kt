package com.taocent.simple.compose.component.blockrichtext.internal.block.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.MoreHorizontal
import com.composables.icons.feather.MoreVertical
import com.taocent.simple.compose.component.blockrichtext.ExperimentalBlockRichTextApi
import com.taocent.simple.compose.component.blockrichtext.RichTextState
import com.taocent.simple.compose.component.blockrichtext.BlockState
import com.taocent.simple.compose.component.blockrichtext.TableBlock
import com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DragState(
    val mode: DragMode,
    val dragOffset: Float = 0f,
    val targetIndex: Int = -1,
    val initialScrollValue: Int = 0,
    val wasSelected: Boolean = false
)

private sealed class DragMode {
    data class Row(val fromIndex: Int, val rowHeightPx: Float) : DragMode()
    data class Column(val fromIndex: Int, val columnWidthPx: Float) : DragMode()
}

@Composable
@OptIn(ExperimentalBlockRichTextApi::class)
internal fun TableRenderer(
    block: TableBlock,
    blockState: BlockState,
    modifier: Modifier = Modifier
) {
    val config = LocalRichTextConfig.current
    val tableConfig = config.table
    val density = LocalDensity.current
    val cellMinHeightPx = with(density) { tableConfig.cellMinHeight.toPx() }
    var tableWidthPx by remember { mutableIntStateOf(0) }
    val tableWidthDp = with(density) { tableWidthPx.toDp() }
    val needScroll = block.columnCount > tableConfig.maxColumnsBeforeScroll
    val scrollState = rememberScrollState()
    val isSelected = blockState.selectedTableBlockId == block.id
    val isNavSelected = blockState.navSelectedTableId == block.id
    val menuButtonSize = tableConfig.menuButtonSize

    LaunchedEffect(isSelected) {
        if (isSelected) {
            kotlinx.coroutines.delay(tableConfig.selectionAutoClearDelayMs)
            blockState.clearTableSelection()
            blockState.navRestoreIfPending()
        }
    }

    val contentWidthDp = tableWidthDp - menuButtonSize * 2
    val columnWidthDp = if (needScroll) {
        val baseWidth = contentWidthDp / tableConfig.maxColumnsBeforeScroll
        baseWidth - tableConfig.columnScrollPadding
    } else {
        if (block.columnCount > 0) contentWidthDp / block.columnCount else contentWidthDp
    }
    val columnWidthPx = with(density) { columnWidthDp.toPx() }

    var focusedRowIndex by remember { mutableStateOf<Int?>(null) }
    var focusedColumnIndex by remember { mutableStateOf<Int?>(null) }
    var focusedRowId by remember { mutableStateOf<String?>(null) }
    var focusedCellId by remember { mutableStateOf<String?>(null) }
    var pendingFocusCellId by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedRowIndex by remember { mutableStateOf<Int?>(null) }
    var selectedColumnIndex by remember { mutableStateOf<Int?>(null) }
    var showRowMenu by remember { mutableStateOf(false) }
    var showColumnMenu by remember { mutableStateOf(false) }

    var dragState by remember { mutableStateOf<DragState?>(null) }
    // 使用 SnapshotStateMap 隔离行高状态变化，避免整表重组
    val rowHeightsPx = remember { mutableStateMapOf<Int, Float>() }
    val columnAnimStates = remember { mutableMapOf<String, androidx.compose.animation.core.Animatable<Float, *>>() }
    val rowAnimStates = remember { mutableMapOf<String, androidx.compose.animation.core.Animatable<Float, *>>() }

    val finishingAnimatable = remember { androidx.compose.animation.core.Animatable(0f) }
    var finishingTargetIndex by remember { mutableIntStateOf(-1) }
    var finishingIsRow by remember { mutableStateOf(false) }
    var finishingStartOffset by remember { mutableFloatStateOf(0f) }

    val cellStates = remember(block.id) {
        mutableMapOf<String, RichTextState>()
    }
    val cellSyncVersions = remember(block.id) {
        mutableMapOf<String, Int>()
    }
    // 单元格焦点请求器映射（用于统一处理焦点恢复）
    val cellFocusRequesters = remember { mutableMapOf<Pair<String, String>, FocusRequester>() }
    LaunchedEffect(pendingFocusCellId) {
        val pending = pendingFocusCellId ?: return@LaunchedEffect
        cellFocusRequesters[pending]?.requestFocus()
        pendingFocusCellId = null
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val edgeThresholdPx = with(density) { tableConfig.dragEdgeThreshold.toPx() }

    LaunchedEffect(dragState) {
        if (dragState == null) {
            columnAnimStates.values.forEach { it.snapTo(0f) }
            rowAnimStates.values.forEach { it.snapTo(0f) }
        }
    }

    LaunchedEffect(finishingTargetIndex) {
        if (finishingTargetIndex >= 0) {
            finishingAnimatable.snapTo(finishingStartOffset)
            finishingAnimatable.animateTo(0f, tween(tableConfig.dragFinishAnimationMs))
            finishingTargetIndex = -1
        }
    }

    LaunchedEffect(dragState) {
        val ds = dragState
        if (ds != null && ds.mode is DragMode.Column) {
            val colMode = ds.mode
            val containerWidthPx = tableWidthPx.toFloat() - with(density) { menuButtonSize.toPx() }
            while (true) {
                val colLeftEdge = colMode.fromIndex * colMode.columnWidthPx + ds.dragOffset
                val colRightEdge = colLeftEdge + colMode.columnWidthPx
                val visibleStart = scrollState.value.toFloat()
                val visibleEnd = visibleStart + containerWidthPx
                when {
                    colRightEdge > visibleEnd -> {
                        scrollState.scrollTo((scrollState.value + tableConfig.dragAutoScrollStepPx).coerceAtMost(scrollState.maxValue))
                    }
                    colLeftEdge < visibleStart -> {
                        scrollState.scrollTo((scrollState.value - tableConfig.dragAutoScrollStepPx).coerceAtLeast(0))
                    }
                }
                kotlinx.coroutines.delay(tableConfig.dragAutoScrollIntervalMs)
            }
        }
    }

    fun rowVisualOffsetAt(index: Int): Float {
        val row = block.rows.getOrNull(index) ?: return 0f
        val ds = dragState
        val rowMode = ds?.mode as? DragMode.Row
        return when {
            rowMode != null && rowMode.fromIndex == index -> ds.dragOffset
            rowMode != null && rowMode.fromIndex < ds.targetIndex && index in (rowMode.fromIndex + 1)..ds.targetIndex ->
                rowAnimStates[row.rowId]?.value ?: -rowMode.rowHeightPx
            rowMode != null && rowMode.fromIndex > ds.targetIndex && index in ds.targetIndex until rowMode.fromIndex ->
                rowAnimStates[row.rowId]?.value ?: rowMode.rowHeightPx
            finishingIsRow && finishingTargetIndex == index && finishingTargetIndex >= 0 -> finishingAnimatable.value
            else -> 0f
        }
    }

    fun columnVisualOffsetAt(index: Int): Float {
        val firstRowCellId = block.rows.firstOrNull()?.cells?.getOrNull(index)?.cellId
        val ds = dragState
        val colMode = ds?.mode as? DragMode.Column
        val scrollDelta = ds
            ?.takeIf { colMode != null && colMode.fromIndex == index }
            ?.let { (scrollState.value - it.initialScrollValue).toFloat() }
            ?: 0f
        return when {
            colMode != null && colMode.fromIndex == index -> ds.dragOffset + scrollDelta
            colMode != null && colMode.fromIndex < ds.targetIndex && index in (colMode.fromIndex + 1)..ds.targetIndex ->
                firstRowCellId?.let { columnAnimStates[it]?.value } ?: -colMode.columnWidthPx
            colMode != null && colMode.fromIndex > ds.targetIndex && index in ds.targetIndex until colMode.fromIndex ->
                firstRowCellId?.let { columnAnimStates[it]?.value } ?: colMode.columnWidthPx
            !finishingIsRow && finishingTargetIndex == index && finishingTargetIndex >= 0 -> finishingAnimatable.value
            else -> 0f
        }
    }

    val borderSeparationThresholdPx = 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusTarget()
            .onFocusChanged { state ->
                if (!state.hasFocus) {
                    focusedRowIndex = null
                    focusedColumnIndex = null
                    focusedRowId = null
                    focusedCellId = null
                    selectedRowIndex = null
                    selectedColumnIndex = null
                    showRowMenu = false
                    showColumnMenu = false
                }
            }
            .onPreviewKeyEvent { event ->
                false
            }
            .onSizeChanged { tableWidthPx = it.width }
            .padding(end = menuButtonSize)
    ) {
        Row {
            // 行菜单按钮列（固定在滚动外面）
            Column {
                Spacer(modifier = Modifier.height(menuButtonSize))
                block.rows.forEachIndexed { rowIndex, row ->
                    val rowDs = dragState
                    val rowMode = (rowDs?.mode as? DragMode.Row)
                    val isDraggingRow = rowMode != null && rowMode.fromIndex == rowIndex

                    val rowOffset = if (rowDs != null) {
                        if (isDraggingRow) {
                            rowDs.dragOffset
                        } else {
                            val rTarget = when {
                                rowMode != null && rowMode.fromIndex < rowDs.targetIndex &&
                                    rowIndex in (rowMode.fromIndex + 1)..rowDs.targetIndex -> -rowMode.rowHeightPx
                                rowMode != null && rowMode.fromIndex > rowDs.targetIndex &&
                                    rowIndex in rowDs.targetIndex until rowMode.fromIndex -> rowMode.rowHeightPx
                                else -> 0f
                            }
                            val rAnim = rowAnimStates.getOrPut(row.rowId) {
                                androidx.compose.animation.core.Animatable(0f)
                            }
                            LaunchedEffect(rTarget) {
                                rAnim.animateTo(rTarget, tween(tableConfig.dragPlaceholderAnimationMs))
                            }
                            rAnim.value
                        }
                    } else if (finishingIsRow && finishingTargetIndex == rowIndex && finishingTargetIndex >= 0) {
                        finishingAnimatable.value
                    } else {
                        0f
                    }

                    val isRowSelected = selectedRowIndex == rowIndex
                    val dragGestureModifier = Modifier.pointerInput(rowIndex, block.rows.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    val rowHeight = rowHeightsPx[rowIndex] ?: cellMinHeightPx
                                    dragState = DragState(
                                        mode = DragMode.Row(fromIndex = rowIndex, rowHeightPx = rowHeight),
                                        targetIndex = rowIndex,
                                        wasSelected = selectedRowIndex == rowIndex
                                    )
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val current = dragState ?: return@detectDragGesturesAfterLongPress
                                    val newOffset = current.dragOffset + dragAmount.y
                                    val mode = current.mode
                                    if (mode is DragMode.Row) {
                                            val targetIndex = if (newOffset > 0) {
                                                var acc = 0f
                                                var target = mode.fromIndex
                                                for (i in (mode.fromIndex + 1)..block.rows.lastIndex) {
                                                    acc += rowHeightsPx[i] ?: cellMinHeightPx
                                                    if (newOffset >= acc - (rowHeightsPx[i] ?: cellMinHeightPx) / 2f) {
                                                        target = i
                                                    } else break
                                                }
                                                target
                                            } else {
                                                var acc = 0f
                                                var target = mode.fromIndex
                                                for (i in (mode.fromIndex - 1) downTo 0) {
                                                    acc += rowHeightsPx[i] ?: cellMinHeightPx
                                                    if (-newOffset >= acc - (rowHeightsPx[i] ?: cellMinHeightPx) / 2f) {
                                                        target = i
                                                    } else break
                                                }
                                                target
                                            }
                                            dragState = current.copy(dragOffset = newOffset, targetIndex = targetIndex)
                                        }
                                    },
                                    onDragEnd = {
                                        val current = dragState
                                        if (current != null && current.mode is DragMode.Row) {
                                            val from = current.mode.fromIndex
                                            val to = current.targetIndex
                                            if (to >= 0 && from != to) {
                                                    val focusedRowIdBefore = focusedRowId
                                                    val focusedCellIdBefore = focusedCellId
                                                    blockState.moveTableRow(block.id, from, to)
                                                    val focusedRow = focusedRowIndex
                                                    if (focusedRow != null) {
                                                        val newFocusedRow = when {
                                                            focusedRow == from -> to
                                                            from < focusedRow && to >= focusedRow -> focusedRow - 1
                                                            from > focusedRow && to <= focusedRow -> focusedRow + 1
                                                            else -> focusedRow
                                                        }
                                                        focusedRowIndex = newFocusedRow
                                                        if (focusedRowIdBefore != null && focusedCellIdBefore != null) {
                                                            pendingFocusCellId = Pair(focusedRowIdBefore, focusedCellIdBefore)
                                                        }
                                                    }
                                                if (current.wasSelected) {
                                                    selectedRowIndex = to
                                                    finishingStartOffset = current.dragOffset - (to - from) * current.mode.rowHeightPx
                                                    finishingTargetIndex = to
                                                    finishingIsRow = true
                                                }
                                            }
                                        }
                                        dragState = null
                                    },
                                    onDragCancel = { dragState = null }
                                )
                            }

                        val rowHeightDp = with(density) { (rowHeightsPx[rowIndex] ?: 0f).toDp() }
                        val isRowLifted = isDraggingRow || (finishingIsRow && finishingTargetIndex == rowIndex && finishingTargetIndex >= 0)
                        val showRowButton = focusedRowIndex == rowIndex || selectedRowIndex == rowIndex

                        TableRowButton(
                            menuButtonSize = menuButtonSize,
                            tableConfig = tableConfig,
                            rowOffset = rowOffset,
                            rowHeightDp = rowHeightDp,
                            isRowLifted = isRowLifted,
                            showRowButton = showRowButton,
                            isSelectedRow = selectedRowIndex == rowIndex,
                            showRowMenu = showRowMenu,
                            dragGestureModifier = if (showRowButton) dragGestureModifier else Modifier,
                            onRowClick = {
                                if (selectedRowIndex == rowIndex) {
                                    showRowMenu = true
                                } else {
                                    selectedRowIndex = rowIndex
                                    selectedColumnIndex = null
                                }
                            },
                            onInsertAbove = {
                                blockState.insertTableRow(block.id, rowIndex, insertAbove = true)
                                showRowMenu = false
                                selectedRowIndex = null
                            },
                            onInsertBelow = {
                                blockState.insertTableRow(block.id, rowIndex, insertAbove = false)
                                showRowMenu = false
                                selectedRowIndex = null
                            },
                            onInsertTableAbove = {
                                blockState.insertTableAbove(block.id, block.rows.size, block.columnCount)
                                showRowMenu = false
                                selectedRowIndex = null
                            },
                            onDeleteRow = {
                                blockState.deleteTableRow(block.id, rowIndex)
                                showRowMenu = false
                                selectedRowIndex = null
                            },
                            onDeleteTable = {
                                // 行菜单里点"删除当前表格":直接根据当前 table 的 id
                                // 调 `deleteTable`,不依赖 `selectedTableBlockId` 选中态
                                // (row 选中态是 TableRenderer 局部的 selectedRowIndex,
                                //  不会同步到 BlockState.selectedTableBlockId)。
                                blockState.deleteTable(block.id)
                                showRowMenu = false
                                selectedRowIndex = null
                            },
                            onDismissMenu = { showRowMenu = false },
                        )
                }
            }

            // 可滚动区域：列按钮 + 单元格
            Row(
                modifier = if (needScroll) {
                    Modifier.horizontalScroll(scrollState)
                } else {
                    Modifier
                }
            ) {
                Column {
                    // 列菜单按钮行
                    Row {
                        block.rows.firstOrNull()?.cells?.forEachIndexed { columnIndex, cell ->
                            val ds = dragState
                            val colMode = (ds?.mode as? DragMode.Column)
                            val isDraggingCol = colMode != null && colMode.fromIndex == columnIndex

                            val scrollDelta = if (isDraggingCol) {
                                (scrollState.value - ds.initialScrollValue).toFloat()
                            } else 0f

                            val colOffset = if (ds != null) {
                                if (isDraggingCol) {
                                    ds.dragOffset + scrollDelta
                                } else {
                                    val target = when {
                                        colMode != null && colMode.fromIndex < ds.targetIndex &&
                                            columnIndex in (colMode.fromIndex + 1)..ds.targetIndex -> -colMode.columnWidthPx
                                        colMode != null && colMode.fromIndex > ds.targetIndex &&
                                            columnIndex in ds.targetIndex until colMode.fromIndex -> colMode.columnWidthPx
                                        else -> 0f
                                    }
                                    val anim = columnAnimStates.getOrPut(cell.cellId) {
                                        androidx.compose.animation.core.Animatable(0f)
                                    }
                                    LaunchedEffect(target) {
                                        anim.animateTo(target, tween(tableConfig.dragPlaceholderAnimationMs))
                                    }
                                    anim.value
                                }
                            } else if (!finishingIsRow && finishingTargetIndex == columnIndex && finishingTargetIndex >= 0) {
                                finishingAnimatable.value
                            } else {
                                0f
                            }

                            val colDragGestureModifier = Modifier.pointerInput(columnIndex, block.columnCount) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            dragState = DragState(
                                                mode = DragMode.Column(
                                                    fromIndex = columnIndex,
                                                    columnWidthPx = columnWidthPx
                                                ),
                                                targetIndex = columnIndex,
                                                initialScrollValue = scrollState.value,
                                                wasSelected = selectedColumnIndex == columnIndex
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = dragState ?: return@detectDragGesturesAfterLongPress
                                            val newOffset = current.dragOffset + dragAmount.x
                                            val mode = current.mode
                                            if (mode is DragMode.Column) {
                                                val effectiveOffset = newOffset +
                                                    (scrollState.value - current.initialScrollValue).toFloat()
                                                val targetIndex = if (effectiveOffset > 0) {
                                                    val steps = ((effectiveOffset + mode.columnWidthPx / 2) / mode.columnWidthPx).toInt()
                                                    (mode.fromIndex + steps).coerceAtMost(block.columnCount - 1)
                                                } else {
                                                    val steps = ((-effectiveOffset + mode.columnWidthPx / 2) / mode.columnWidthPx).toInt()
                                                    (mode.fromIndex - steps).coerceAtLeast(0)
                                                }
                                                dragState = current.copy(dragOffset = newOffset, targetIndex = targetIndex)
                                            }
                                        },
                                        onDragEnd = {
                                            val current = dragState
                                            if (current != null && current.mode is DragMode.Column) {
                                                val mode = current.mode
                                                val effectiveOffset = current.dragOffset +
                                                    (scrollState.value - current.initialScrollValue).toFloat()
                                                val finalTarget = if (effectiveOffset > 0) {
                                                    val steps = ((effectiveOffset + mode.columnWidthPx / 2) / mode.columnWidthPx).toInt()
                                                    (mode.fromIndex + steps).coerceAtMost(block.columnCount - 1)
                                                } else {
                                                    val steps = ((-effectiveOffset + mode.columnWidthPx / 2) / mode.columnWidthPx).toInt()
                                                    (mode.fromIndex - steps).coerceAtLeast(0)
                                                }
                                                val from = mode.fromIndex
                                                if (from != finalTarget) {
                                                    val focusedRowIdBefore = focusedRowId
                                                    val focusedCellIdBefore = focusedCellId
                                                    blockState.moveTableColumn(block.id, from, finalTarget)
                                                    val focusedCol = focusedColumnIndex
                                                    if (focusedCol != null) {
                                                        val newFocusedCol = when {
                                                            focusedCol == from -> finalTarget
                                                            from < focusedCol && finalTarget >= focusedCol -> focusedCol - 1
                                                            from > focusedCol && finalTarget <= focusedCol -> focusedCol + 1
                                                            else -> focusedCol
                                                        }
                                                        focusedColumnIndex = newFocusedCol
                                                        if (focusedRowIdBefore != null && focusedCellIdBefore != null) {
                                                            pendingFocusCellId = Pair(focusedRowIdBefore, focusedCellIdBefore)
                                                        }
                                                    }
                                                    if (current.wasSelected) {
                                                        selectedColumnIndex = finalTarget
                                                        finishingStartOffset = effectiveOffset - (finalTarget - from) * mode.columnWidthPx
                                                        finishingTargetIndex = finalTarget
                                                        finishingIsRow = false
                                                    }
                                                }
                                            }
                                            dragState = null
                                        },
                                        onDragCancel = { dragState = null }
                                    )
                                }

                            val isColLifted = isDraggingCol || (!finishingIsRow && finishingTargetIndex == columnIndex && finishingTargetIndex >= 0)
                            val showColumnButton = focusedColumnIndex == columnIndex || selectedColumnIndex == columnIndex

                            TableColumnButton(
                                menuButtonSize = menuButtonSize,
                                columnWidthDp = columnWidthDp,
                                colOffset = colOffset,
                                isColLifted = isColLifted,
                                showColumnButton = showColumnButton,
                                isSelectedCol = selectedColumnIndex == columnIndex,
                                showColumnMenu = showColumnMenu,
                                colDragGestureModifier = if (showColumnButton) colDragGestureModifier else Modifier,
                                onColumnClick = {
                                    if (selectedColumnIndex == columnIndex) {
                                        showColumnMenu = true
                                    } else {
                                        selectedColumnIndex = columnIndex
                                        selectedRowIndex = null
                                    }
                                },
                                onInsertLeft = {
                                    val restoreRowId = focusedRowId
                                    val restoreCellId = focusedCellId
                                    blockState.insertTableColumn(block.id, columnIndex, insertLeft = true)
                                    if (restoreRowId != null && restoreCellId != null) {
                                        pendingFocusCellId = Pair(restoreRowId, restoreCellId)
                                    }
                                    showColumnMenu = false
                                    selectedColumnIndex = null
                                },
                                onInsertRight = {
                                    val restoreRowId = focusedRowId
                                    val restoreCellId = focusedCellId
                                    blockState.insertTableColumn(block.id, columnIndex, insertLeft = false)
                                    if (restoreRowId != null && restoreCellId != null) {
                                        pendingFocusCellId = Pair(restoreRowId, restoreCellId)
                                    }
                                    showColumnMenu = false
                                    selectedColumnIndex = null
                                },
                                onDeleteColumn = {
                                    blockState.deleteTableColumn(block.id, columnIndex)
                                    showColumnMenu = false
                                    selectedColumnIndex = null
                                },
                                onDismissMenu = { showColumnMenu = false },
                            )
                        }
                    }

                    // 表格行（只有单元格，行按钮在外面）
                    block.rows.forEachIndexed { rowIndex, row ->
                        val tblRowDs = dragState
                        val tblRowMode = (tblRowDs?.mode as? DragMode.Row)
                        val isTblDraggingRow = tblRowMode != null && tblRowMode.fromIndex == rowIndex

                        val tblRowOffset = if (tblRowDs != null) {
                            if (isTblDraggingRow) {
                                tblRowDs.dragOffset
                            } else {
                                val tblTarget = when {
                                    tblRowMode != null && tblRowMode.fromIndex < tblRowDs.targetIndex &&
                                        rowIndex in (tblRowMode.fromIndex + 1)..tblRowDs.targetIndex -> -tblRowMode.rowHeightPx
                                    tblRowMode != null && tblRowMode.fromIndex > tblRowDs.targetIndex &&
                                        rowIndex in tblRowDs.targetIndex until tblRowMode.fromIndex -> tblRowMode.rowHeightPx
                                    else -> 0f
                                }
                                val tblAnim = rowAnimStates.getOrPut(row.rowId) {
                                    androidx.compose.animation.core.Animatable(0f)
                                }
                                LaunchedEffect(tblTarget) {
                                    tblAnim.animateTo(tblTarget, tween(tableConfig.dragPlaceholderAnimationMs))
                                }
                                tblAnim.value
                            }
                        } else if (finishingIsRow && finishingTargetIndex == rowIndex && finishingTargetIndex >= 0) {
                            finishingAnimatable.value
                        } else {
                            0f
                        }

                        val isSelectedRow = selectedRowIndex == rowIndex
                        val isTblRowLifted = isTblDraggingRow || (finishingIsRow && finishingTargetIndex == rowIndex && finishingTargetIndex >= 0)

                        val rowModifier = Modifier
                            .zIndex(if (isTblRowLifted) 1f else 0f)
                            .height(IntrinsicSize.Min)
                            .offset { IntOffset(0, tblRowOffset.roundToInt()) }
                        Row(modifier = rowModifier.onSizeChanged { rowHeightsPx[rowIndex] = it.height.toFloat() }) {
                            row.cells.forEachIndexed { cellIndex, cell ->
                                key(cell.cellId) {
                                    // 计算单元格列偏移（拖拽相关）
                                    val cellDs = dragState
                                    val cellColMode = (cellDs?.mode as? DragMode.Column)
                                    val isCellDraggingCol = cellColMode != null && cellColMode.fromIndex == cellIndex
                                    val cellScrollDelta = cellDs
                                        ?.takeIf { isCellDraggingCol }
                                        ?.let { (scrollState.value - it.initialScrollValue).toFloat() }
                                        ?: 0f
                                    val cellColOffset = if (cellDs != null) {
                                        if (isCellDraggingCol) {
                                            cellDs.dragOffset + cellScrollDelta
                                        } else {
                                            val cellTarget = when {
                                                cellColMode != null && cellColMode.fromIndex < cellDs.targetIndex &&
                                                    cellIndex in (cellColMode.fromIndex + 1)..cellDs.targetIndex -> -cellColMode.columnWidthPx
                                                cellColMode != null && cellColMode.fromIndex > cellDs.targetIndex &&
                                                    cellIndex in cellDs.targetIndex until cellColMode.fromIndex -> cellColMode.columnWidthPx
                                                else -> 0f
                                            }
                                            val cellAnim = columnAnimStates.getOrPut(cell.cellId) {
                                                androidx.compose.animation.core.Animatable(0f)
                                            }
                                            LaunchedEffect(cellTarget) {
                                                cellAnim.animateTo(cellTarget, tween(tableConfig.dragPlaceholderAnimationMs))
                                            }
                                            cellAnim.value
                                        }
                                    } else if (!finishingIsRow && finishingTargetIndex == cellIndex && finishingTargetIndex >= 0) {
                                        finishingAnimatable.value
                                    } else {
                                        0f
                                    }

                                    val isCellLifted = isCellDraggingCol || (!finishingIsRow && finishingTargetIndex == cellIndex && finishingTargetIndex >= 0)
                                    val drawRightBorder = cellIndex == block.columnCount - 1 ||
                                        abs(cellColOffset - columnVisualOffsetAt(cellIndex + 1)) > borderSeparationThresholdPx
                                    val drawBottomBorder = rowIndex == block.rows.lastIndex ||
                                        abs(tblRowOffset - rowVisualOffsetAt(rowIndex + 1)) > borderSeparationThresholdPx

                                    TableCell(
                                        cell = cell,
                                        rowId = row.rowId,
                                        rowIndex = rowIndex,
                                        cellIndex = cellIndex,
                                        columnWidthDp = columnWidthDp,
                                        blockId = block.id,
                                        blockRows = block.rows.size,
                                        blockCols = block.columnCount,
                                        blockState = blockState,
                                        tableConfig = tableConfig,
                                        primaryColor = primaryColor,
                                        isTableSelected = isSelected,
                                        isNavSelected = isNavSelected,
                                        isSelectedRow = isSelectedRow,
                                        isSelectedCol = selectedColumnIndex == cellIndex,
                                        drawRightBorder = drawRightBorder,
                                        drawBottomBorder = drawBottomBorder,
                                        cellColOffset = cellColOffset,
                                        isCellLifted = isCellLifted,
                                        cellStates = cellStates,
                                        cellSyncVersions = cellSyncVersions,
                                        cellFocusRequesters = cellFocusRequesters,
                                        onFocus = { ri, ci, rid, cid, cs, fr ->
                                            focusedRowIndex = ri
                                            focusedColumnIndex = ci
                                            focusedRowId = rid
                                            focusedCellId = cid
                                            selectedRowIndex = null
                                            selectedColumnIndex = null
                                            showRowMenu = false
                                            showColumnMenu = false
                                            blockState.navClearSelection()
                                            blockState.focusTableCell(block.id, rid, cid, cs, fr)
                                        },
                                        onClearSelection = {
                                            selectedRowIndex = null
                                            selectedColumnIndex = null
                                            showRowMenu = false
                                            showColumnMenu = false
                                        },
                                    )
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun ThreeDotsButton(
    horizontal: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = if(horizontal)Feather.MoreHorizontal else Feather.MoreVertical,
            contentDescription = null
        )
    }
}

@Composable
private fun ColumnMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onInsertLeft: () -> Unit,
    onInsertRight: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("在左侧插入列") },
            onClick = onInsertLeft
        )
        DropdownMenuItem(
            text = { Text("在右侧插入列") },
            onClick = onInsertRight
        )
        DropdownMenuItem(
            text = { Text("删除当前列") },
            onClick = onDelete
        )
    }
}

@Composable
private fun RowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onInsertTableAbove: () -> Unit,
    onDelete: () -> Unit,
    onDeleteTable: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("在上方插入行") },
            onClick = onInsertAbove
        )
        DropdownMenuItem(
            text = { Text("在下方插入行") },
            onClick = onInsertBelow
        )
        DropdownMenuItem(
            text = { Text("在上方插入表格") },
            onClick = onInsertTableAbove
        )
        DropdownMenuItem(
            text = { Text("删除当前行") },
            onClick = onDelete
        )
        DropdownMenuItem(
            text = { Text("删除当前表格") },
            onClick = onDeleteTable
        )
    }
}

// 独立的 TableRowButton Composable，隔离行按钮重组范围
@Composable
private fun TableRowButton(
    menuButtonSize: Dp,
    tableConfig: com.taocent.simple.compose.component.blockrichtext.TableConfig,
    rowOffset: Float,
    rowHeightDp: Dp,
    isRowLifted: Boolean,
    showRowButton: Boolean,
    isSelectedRow: Boolean,
    showRowMenu: Boolean,
    dragGestureModifier: Modifier,
    onRowClick: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onInsertTableAbove: () -> Unit,
    onDeleteRow: () -> Unit,
    onDeleteTable: () -> Unit,
    onDismissMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .zIndex(if (isRowLifted) 1f else 0f)
            .width(menuButtonSize)
            .then(if (rowHeightDp > 0.dp) Modifier.height(rowHeightDp) else Modifier.heightIn(min = tableConfig.cellMinHeight))
            .offset { IntOffset(0, rowOffset.roundToInt()) }
            .then(if (showRowButton) dragGestureModifier else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (showRowButton) {
            ThreeDotsButton(
                horizontal = false,
                modifier = Modifier.size(menuButtonSize),
                onClick = onRowClick
            )
            RowMenu(
                expanded = showRowMenu && isSelectedRow,
                onDismiss = onDismissMenu,
                onInsertAbove = onInsertAbove,
                onInsertBelow = onInsertBelow,
                onInsertTableAbove = onInsertTableAbove,
                onDelete = onDeleteRow,
                onDeleteTable = onDeleteTable
            )
        }
    }
}

// 独立的 TableColumnButton Composable，隔离列按钮重组范围
@Composable
private fun TableColumnButton(
    menuButtonSize: Dp,
    columnWidthDp: Dp,
    colOffset: Float,
    isColLifted: Boolean,
    showColumnButton: Boolean,
    isSelectedCol: Boolean,
    showColumnMenu: Boolean,
    colDragGestureModifier: Modifier,
    onColumnClick: () -> Unit,
    onInsertLeft: () -> Unit,
    onInsertRight: () -> Unit,
    onDeleteColumn: () -> Unit,
    onDismissMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .zIndex(if (isColLifted) 1f else 0f)
            .width(columnWidthDp)
            .offset { IntOffset(colOffset.roundToInt(), 0) }
            .then(if (showColumnButton) colDragGestureModifier else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (showColumnButton) {
            ThreeDotsButton(
                horizontal = true,
                modifier = Modifier.size(menuButtonSize),
                onClick = onColumnClick
            )
            ColumnMenu(
                expanded = showColumnMenu && isSelectedCol,
                onDismiss = onDismissMenu,
                onInsertLeft = onInsertLeft,
                onInsertRight = onInsertRight,
                onDelete = onDeleteColumn
            )
        } else {
            Spacer(modifier = Modifier.height(menuButtonSize))
        }
    }
}

// 独立的 TableCell Composable，隔离单元格重组范围
@Composable
@OptIn(ExperimentalBlockRichTextApi::class)
private fun TableCell(
    cell: TableBlock.TableCell,
    rowId: String,
    rowIndex: Int,
    cellIndex: Int,
    columnWidthDp: Dp,
    blockId: String,
    blockRows: Int,
    blockCols: Int,
    blockState: BlockState,
    tableConfig: com.taocent.simple.compose.component.blockrichtext.TableConfig,
    primaryColor: Color,
    isTableSelected: Boolean,
    isNavSelected: Boolean,
    isSelectedRow: Boolean,
    isSelectedCol: Boolean,
    drawRightBorder: Boolean,
    drawBottomBorder: Boolean,
    cellColOffset: Float,
    isCellLifted: Boolean,
    cellStates: MutableMap<String, RichTextState>,
    cellSyncVersions: MutableMap<String, Int>,
    cellFocusRequesters: MutableMap<Pair<String, String>, FocusRequester>,
    onFocus: (rowIndex: Int, cellIndex: Int, rowId: String, cellId: String, cellState: RichTextState, focusRequester: FocusRequester) -> Unit,
    onClearSelection: () -> Unit,
) {
    val isHeader = rowIndex == 0
    val isEvenRow = rowIndex % 2 == 0
    val backgroundColor = when {
        isHeader -> tableConfig.headerColor
        isEvenRow -> tableConfig.headerColor
        else -> tableConfig.cellColor
    }
    val isFirstRow = rowIndex == 0
    val isLastRow = rowIndex == blockRows - 1
    val isFirstCol = cellIndex == 0
    val isLastCol = cellIndex == blockCols - 1
    val isOuterCell = isTableSelected && (isFirstRow || isLastRow || isFirstCol || isLastCol)

    // getCellState 逻辑移入 TableCell 内部
    val key = "${blockId}_${rowId}_${cell.cellId}"
    val content = cell.content
    val cellState = cellStates.getOrPut(key) {
        val newState = RichTextState()
        if (content.isNotEmpty()) {
            newState.restoreTextFieldValue(
                TextFieldValue(
                    annotatedString = content,
                    selection = TextRange(content.length)
                )
            )
        }
        newState
    }
    if (cellState.textFieldValue.annotatedString != content &&
        cellSyncVersions[key] != blockState.contentSyncVersion
    ) {
        cellState.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = content,
                selection = TextRange(content.length)
            )
        )
    }
    cellSyncVersions[key] = blockState.contentSyncVersion

    // FocusRequester 使用 DisposableEffect 注册到 cellFocusRequesters
    val cellFocusRequester = remember(rowId, cell.cellId) { FocusRequester() }
    DisposableEffect(rowId, cell.cellId) {
        cellFocusRequesters[Pair(rowId, cell.cellId)] = cellFocusRequester
        onDispose {
            cellFocusRequesters.remove(Pair(rowId, cell.cellId))
        }
    }

    val cellModifier = Modifier
        .width(columnWidthDp)
        .fillMaxHeight()
        .offset { IntOffset(cellColOffset.roundToInt(), 0) }
        .background(backgroundColor)
        .drawWithContent {
            drawContent()
            val primaryStroke = tableConfig.selectedHighlightBorderWidth.toPx()
            val normalStroke = tableConfig.cellBorderWidth.toPx()

            drawLine(
                tableConfig.borderColor,
                Offset(normalStroke / 2, 0f),
                Offset(normalStroke / 2, size.height),
                normalStroke
            )
            drawLine(
                tableConfig.borderColor,
                Offset(0f, normalStroke / 2),
                Offset(size.width, normalStroke / 2),
                normalStroke
            )
            if (drawRightBorder) {
                drawLine(
                    tableConfig.borderColor,
                    Offset(size.width - normalStroke / 2, 0f),
                    Offset(size.width - normalStroke / 2, size.height),
                    normalStroke
                )
            }
            if (drawBottomBorder) {
                drawLine(
                    tableConfig.borderColor,
                    Offset(0f, size.height - normalStroke / 2),
                    Offset(size.width, size.height - normalStroke / 2),
                    normalStroke
                )
            }

            if (isTableSelected) {
                if (isFirstRow) {
                    drawLine(
                        primaryColor,
                        Offset(0f, primaryStroke / 2),
                        Offset(size.width, primaryStroke / 2),
                        primaryStroke
                    )
                }
                if (isLastRow) {
                    drawLine(
                        primaryColor,
                        Offset(0f, size.height - primaryStroke / 2),
                        Offset(size.width, size.height - primaryStroke / 2),
                        primaryStroke
                    )
                }
                if (isFirstCol) {
                    drawLine(
                        primaryColor,
                        Offset(primaryStroke / 2, 0f),
                        Offset(primaryStroke / 2, size.height),
                        primaryStroke
                    )
                }
                if (isLastCol) {
                    drawLine(
                        primaryColor,
                        Offset(size.width - primaryStroke / 2, 0f),
                        Offset(size.width - primaryStroke / 2, size.height),
                        primaryStroke
                    )
                }
            }

            if (isNavSelected && cellIndex == 0) {
                drawLine(
                    primaryColor,
                    Offset(primaryStroke / 2, 0f),
                    Offset(primaryStroke / 2, size.height),
                    primaryStroke
                )
            }

            if (isSelectedRow) {
                drawLine(
                    primaryColor,
                    Offset(0f, primaryStroke / 2),
                    Offset(size.width, primaryStroke / 2),
                    primaryStroke
                )
                drawLine(
                    primaryColor,
                    Offset(0f, size.height - primaryStroke / 2),
                    Offset(size.width, size.height - primaryStroke / 2),
                    primaryStroke
                )
                if (isFirstCol) {
                    drawLine(
                        primaryColor,
                        Offset(primaryStroke / 2, 0f),
                        Offset(primaryStroke / 2, size.height),
                        primaryStroke
                    )
                }
                if (isLastCol) {
                    drawLine(
                        primaryColor,
                        Offset(size.width - primaryStroke / 2, 0f),
                        Offset(size.width - primaryStroke / 2, size.height),
                        primaryStroke
                    )
                }
            }

            if (isSelectedCol) {
                drawLine(
                    primaryColor,
                    Offset(primaryStroke / 2, 0f),
                    Offset(primaryStroke / 2, size.height),
                    primaryStroke
                )
                drawLine(
                    primaryColor,
                    Offset(size.width - primaryStroke / 2, 0f),
                    Offset(size.width - primaryStroke / 2, size.height),
                    primaryStroke
                )
                if (isFirstRow) {
                    drawLine(
                        primaryColor,
                        Offset(0f, primaryStroke / 2),
                        Offset(size.width, primaryStroke / 2),
                        primaryStroke
                    )
                }
                if (isLastRow) {
                    drawLine(
                        primaryColor,
                        Offset(0f, size.height - primaryStroke / 2),
                        Offset(size.width, size.height - primaryStroke / 2),
                        primaryStroke
                    )
                }
            }
        }

    Box(modifier = cellModifier
        .zIndex(if (isCellLifted) 1f else 0f)
        .clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null
        ) { cellFocusRequester.requestFocus() }
    ) {
        TableCellEditor(
            state = cellState,
            isHeader = isHeader,
            focusRequester = cellFocusRequester,
            onFocus = { focusRequester ->
                onFocus(rowIndex, cellIndex, rowId, cell.cellId, cellState, focusRequester)
            },
            onClearSelection = onClearSelection,
            onContentChange = {
                blockState.updateTableCellContent(
                    tableId = blockId,
                    rowId = rowId,
                    cellId = cell.cellId,
                    content = cellState.textFieldValue.annotatedString
                )
            },
            onUndo = { blockState.undo() },
            onRedo = { blockState.redo() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
