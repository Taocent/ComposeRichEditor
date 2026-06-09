package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.internal.format.RichTextFormatState
import com.taocent.simple.compose.component.blockrichtext.RichTextState
import com.taocent.simple.compose.component.blockrichtext.internal.block.AnchorPosition
import com.taocent.simple.compose.component.blockrichtext.internal.block.EditorCursor
import com.taocent.simple.compose.component.richtext.core.internal.serialization.RichTextSerializer
import com.taocent.simple.compose.component.blockrichtext.internal.undo.BlockUndoManager

class BlockState(
    maxUndoHistory: Int = 100,
    undoMergeIntervalMs: Long = 500
) : RichTextFormatState {

    private val _blocks = mutableStateListOf<DocumentBlock>()
    val blocks: List<DocumentBlock> get() = _blocks

    var focusedBlockId by mutableStateOf("")
        private set

    internal var editorCursor: EditorCursor by mutableStateOf(EditorCursor.Text(""))
        private set

    var selectedTableBlockId by mutableStateOf<String?>(null)
        private set

    var navSelectedTableId by mutableStateOf<String?>(null)
        private set
    var navSourceBlockId by mutableStateOf<String?>(null)
        private set
    var pendingNavRestoreTableId by mutableStateOf<String?>(null)
    var focusRequestVersion by mutableStateOf(0)
        private set
    var contentSyncVersion by mutableStateOf(0)
        private set
    val navFocusRequesters = mutableMapOf<String, androidx.compose.ui.focus.FocusRequester>()

    private val blockStates = mutableMapOf<String, RichTextState>()
    private val blockUndoManager = BlockUndoManager(maxUndoHistory, undoMergeIntervalMs)
    private var suppressContentUndoRecord = false
    private var pendingHyperlinkDeleteSnapshot: BlockUndoManager.DocumentSnapshot? = null
    private var pendingHyperlinkDeleteKey: String? = null

    val canUndo: Boolean
        get() = blockUndoManager.canUndo

    val canRedo: Boolean
        get() = blockUndoManager.canRedo

    init {
        val firstBlock = TextBlock()
        _blocks.add(firstBlock)
        blockStates[firstBlock.id] = createBlockState()
        setTextCursor(firstBlock.id)
    }

    private fun createBlockState(initialText: String = ""): RichTextState {
        return RichTextState(
            initialText = initialText,
            maxUndoHistory = 0,
            undoMergeIntervalMs = 0
        )
    }

    private fun createBlockState(content: AnnotatedString): RichTextState {
        return createBlockState().also {
            it.restoreTextFieldValue(
                TextFieldValue(
                    annotatedString = content,
                    selection = TextRange(content.length)
                )
            )
        }
    }

    fun getBlockState(blockId: String): RichTextState {
        return blockStates[blockId] ?: run {
            val block = _blocks.firstOrNull { it.id == blockId }
            val state = if (block is TextBlock) {
                createBlockState(block.content)
            } else {
                createBlockState()
            }
            blockStates[blockId] = state
            state
        }
    }

    val focusedBlockState: RichTextState
        get() = getBlockState(focusedBlockId)

    var focusedTableCellState: RichTextState? by mutableStateOf(null)
        private set

    var focusedTableCellFocusRequester: androidx.compose.ui.focus.FocusRequester? by mutableStateOf(null)
        private set

    private val activeRichTextState: RichTextState
        get() = focusedTableCellState ?: focusedBlockState

    fun focusBlock(blockId: String) {
        clearTableCellFocus()
        setTextCursor(blockId)
    }

    fun focusTableCell(
        tableId: String,
        rowId: String,
        cellId: String,
        cellState: RichTextState,
        focusRequester: androidx.compose.ui.focus.FocusRequester
    ) {
        setTableCellCursor(tableId, rowId, cellId, cellState, focusRequester)
    }

    private fun setLegacyTableCellFocus(cellState: RichTextState, focusRequester: androidx.compose.ui.focus.FocusRequester) {
        focusedTableCellState = cellState
        focusedTableCellFocusRequester = focusRequester
    }

    fun focusTableCell(cellState: RichTextState, focusRequester: androidx.compose.ui.focus.FocusRequester) {
        setLegacyTableCellFocus(cellState, focusRequester)
    }

    fun requestFocusedBlockFocus() {
        focusRequestVersion++
    }

    private fun setTextFocusOnly(blockId: String, requestFocus: Boolean = false) {
        focusedBlockId = blockId
        if (requestFocus) {
            requestFocusedBlockFocus()
        }
    }

    private fun setFocusedBlockCompat(blockId: String, requestFocus: Boolean = false) {
        focusedBlockId = blockId
        val block = _blocks.firstOrNull { it.id == blockId }
        if (block is TextBlock) {
            val state = getBlockState(blockId)
            val selection = state.textFieldValue.selection
            editorCursor = EditorCursor.Text(blockId, selection.start, selection.end)
        }
        if (requestFocus) {
            requestFocusedBlockFocus()
        }
    }

    private fun setTextCursor(blockId: String, requestFocus: Boolean = false) {
        setTextFocusOnly(blockId, requestFocus)
        val state = getBlockState(blockId)
        val selection = state.textFieldValue.selection
        editorCursor = EditorCursor.Text(blockId, selection.start, selection.end)
    }

    private fun setTableCellCursor(
        tableId: String,
        rowId: String,
        cellId: String,
        cellState: RichTextState,
        focusRequester: androidx.compose.ui.focus.FocusRequester
    ) {
        focusedTableCellState = cellState
        focusedTableCellFocusRequester = focusRequester
        editorCursor = EditorCursor.TableCell(tableId, rowId, cellId)
    }

    private fun clearTableCellFocus() {
        focusedTableCellState = null
        focusedTableCellFocusRequester = null
    }

    private fun setBlockAnchorCursor(blockId: String) {
        navSelectedTableId = blockId
        editorCursor = EditorCursor.BlockAnchor(blockId, AnchorPosition.Before)
    }

    private fun restoreTextFocusIfNeeded(blockId: String?) {
        if (blockId != null && _blocks.any { it.id == blockId && it is TextBlock }) {
            if (editorCursor is EditorCursor.BlockAnchor) {
                setTextFocusOnly(blockId, requestFocus = true)
            } else {
                setTextCursor(blockId, requestFocus = true)
            }
        }
    }

    private fun restoreNearestTextFocus(anchorIndex: Int) {
        findNearestTextBlockId(anchorIndex)?.let {
            if (editorCursor is EditorCursor.BlockAnchor) {
                setTextFocusOnly(it, requestFocus = true)
            } else {
                setTextCursor(it, requestFocus = true)
            }
        }
    }

    fun updateBlockContent(blockId: String) {
        val state = blockStates[blockId] ?: return
        val index = _blocks.indexOfFirst { it.id == blockId }
        if (index >= 0) {
            val block = _blocks[index]
            if (block is TextBlock) {
                val oldValue = TextFieldValue(
                    annotatedString = block.content,
                    selection = state.textFieldValue.selection
                )
                val newValue = state.textFieldValue
                _blocks[index] = block.copy(
                    content = newValue.annotatedString
                )
                if (!suppressContentUndoRecord && oldValue.annotatedString != newValue.annotatedString) {
                    blockUndoManager.record(blockId, oldValue, newValue)
                }
            }
        }
    }

    fun recordChange(blockId: String, before: TextFieldValue, after: TextFieldValue) {
        blockUndoManager.record(blockId, before, after)
    }

    private fun documentSnapshot(): BlockUndoManager.DocumentSnapshot {
        return BlockUndoManager.DocumentSnapshot(
            blocks = _blocks.toList(),
            textValues = blockStates.mapValues { it.value.textFieldValue },
            cursor = editorCursor,
            focusedBlockId = focusedBlockId,
            selectedTableBlockId = selectedTableBlockId,
            navSelectedTableId = navSelectedTableId,
            navSourceBlockId = navSourceBlockId
        )
    }

    private fun recordDocumentChange(before: BlockUndoManager.DocumentSnapshot, mergeKey: String? = null) {
        blockUndoManager.recordDocument(before, documentSnapshot(), mergeKey)
        pendingHyperlinkDeleteSnapshot = null
        pendingHyperlinkDeleteKey = null
    }

    private inline fun withContentUndoSuppressed(block: () -> Unit) {
        val previous = suppressContentUndoRecord
        suppressContentUndoRecord = true
        try {
            block()
        } finally {
            suppressContentUndoRecord = previous
        }
    }

    private fun restoreDocumentSnapshot(snapshot: BlockUndoManager.DocumentSnapshot) = withContentUndoSuppressed {
        _blocks.clear()
        blockStates.clear()
        snapshot.blocks.forEach { block ->
            if (block is TextBlock) {
                val value = snapshot.textValues[block.id] ?: TextFieldValue(
                    annotatedString = block.content,
                    selection = TextRange(block.content.length)
                )
                _blocks.add(block.copy(content = value.annotatedString))
                val state = createBlockState()
                state.restoreTextFieldValue(value)
                blockStates[block.id] = state
            } else {
                _blocks.add(block)
            }
        }
        focusedBlockId = snapshot.focusedBlockId
        editorCursor = snapshot.cursor
        selectedTableBlockId = snapshot.selectedTableBlockId
        navSelectedTableId = snapshot.navSelectedTableId
        navSourceBlockId = snapshot.navSourceBlockId
        clearTableCellFocus()
        contentSyncVersion++
        if (snapshot.cursor is EditorCursor.Text && _blocks.any { it.id == snapshot.focusedBlockId && it is TextBlock }) {
            requestFocusedBlockFocus()
        }
    }

    fun insertBlockAfter(afterBlockId: String): TextBlock {
        val before = documentSnapshot()
        val newBlock = TextBlock()
        val index = _blocks.indexOfFirst { it.id == afterBlockId }
        if (index >= 0) {
            _blocks.add(index + 1, newBlock)
        } else {
            _blocks.add(newBlock)
        }
        blockStates[newBlock.id] = createBlockState()
        setFocusedBlockCompat(newBlock.id)
        recordDocumentChange(before)
        return newBlock
    }

    fun insertTable(rows: Int, columns: Int) {
        val before = documentSnapshot()
        val wasNavSelected = navSelectedTableId != null
        val navTableId = navSelectedTableId
        val restoreFocusBlockId = focusedBlockId
        val tableBlock = TableBlock.create(rows, columns)

        if (wasNavSelected && navTableId != null) {
            val navIndex = _blocks.indexOfFirst { it.id == navTableId }
            if (navIndex >= 0) {
                _blocks.add(navIndex, tableBlock)
                setBlockAnchorCursor(navTableId)
                restoreTextFocusIfNeeded(restoreFocusBlockId)
                recordDocumentChange(before)
                return
            }
        }

        val currentId = focusedBlockId
        val currentBlock = _blocks.firstOrNull { it.id == currentId }
        val index = _blocks.indexOfFirst { it.id == currentId }

        fun shouldInsertTextAfter(insertIndex: Int): Boolean {
            val nextIdx = insertIndex + 1
            return nextIdx >= _blocks.size || _blocks[nextIdx] !is TableBlock
        }

        if (currentBlock is TextBlock) {
            val currentState = getBlockState(currentBlock.id)
            val cursorPos = currentState.textFieldValue.selection.min
            val content = currentState.textFieldValue.annotatedString
            val contentLength = content.length

            when {
                contentLength == 0 -> {
                    // 空文本块插入表格:不创建额外空文本块(项目硬约束:"不应额外创建空文本块")。
                    // 保留原 TextBlock 在 TableBlock 后,作为 TableBlock 后的光标落点 —
                    // 等价于"cursorPos == 0"的拆分:TableBlock 插入在原 TextBlock 前,
                    // 原 TextBlock 移到 index+1。这样 LazyColumn 看到的 key diff 是:
                    //   t1(原 id)位置 0 → 1 → 触发 placement 动画
                    //   tbl1 新 key → 触发 fadeIn(alpha 0→1,220ms 可见)
                    // 而不是旧实现的"原地替换 + 凭空多出一个 t2",后者两个 0 高度 item
                    // 互相覆盖,TableBlock 的 fadeIn 视觉上几乎只有"瞬间切换"感。
                    // 焦点保留在原 TextBlock,requestFocus = true 自增 focusRequestVersion,
                    // 让顶层 LaunchedEffect 重新把焦点放回 t1(原 RichTextState 保留,
                    // 光标位置 / IME 状态 / undo 历史都不丢)。
                    _blocks.add(index, tableBlock)
                    setFocusedBlockCompat(currentBlock.id, requestFocus = true)
                }
                cursorPos == 0 -> {
                    _blocks.add(index, tableBlock)
                    if (shouldInsertTextAfter(index)) {
                        val textBlockAfter = TextBlock()
                        _blocks.add(index + 1, textBlockAfter)
                        blockStates[textBlockAfter.id] = createBlockState()
                        setFocusedBlockCompat(textBlockAfter.id)
                    } else {
                        focusNearestTextBlock(index)
                    }
                }
                cursorPos >= contentLength -> {
                    _blocks.add(index + 1, tableBlock)
                    if (shouldInsertTextAfter(index + 1)) {
                        val textBlockAfter = TextBlock()
                        _blocks.add(index + 2, textBlockAfter)
                        blockStates[textBlockAfter.id] = createBlockState()
                        setFocusedBlockCompat(textBlockAfter.id)
                    } else {
                        focusNearestTextBlock(index + 1)
                    }
                }
                else -> {
                    val leftContent = content.subSequence(0, cursorPos)
                    val rightContent = content.subSequence(cursorPos, contentLength)

                    currentState.restoreTextFieldValue(
                        TextFieldValue(
                            annotatedString = leftContent,
                            selection = TextRange(leftContent.length)
                        )
                    )
                    _blocks[index] = currentBlock.copy(content = leftContent)

                    val rightTextBlock = TextBlock(content = rightContent)
                    val rightState = createBlockState()
                    rightState.restoreTextFieldValue(
                        TextFieldValue(
                            annotatedString = rightContent,
                            selection = TextRange(0)
                        )
                    )
                    blockStates[rightTextBlock.id] = rightState

                    _blocks.add(index + 1, tableBlock)
                    _blocks.add(index + 2, rightTextBlock)
                    setFocusedBlockCompat(rightTextBlock.id)
                }
            }
        } else {
            if (index >= 0) {
                _blocks.add(index + 1, tableBlock)
                if (shouldInsertTextAfter(index + 1)) {
                    val textBlockAfter = TextBlock()
                    _blocks.add(index + 2, textBlockAfter)
                    blockStates[textBlockAfter.id] = createBlockState()
                    setFocusedBlockCompat(textBlockAfter.id)
                } else {
                    focusNearestTextBlock(index + 1)
                }
            } else {
                _blocks.add(tableBlock)
                val textBlockAfter = TextBlock()
                _blocks.add(textBlockAfter)
                blockStates[textBlockAfter.id] = createBlockState()
                setFocusedBlockCompat(textBlockAfter.id)
            }
        }
        recordDocumentChange(before)
    }

    fun insertTableAbove(tableId: String, rows: Int, columns: Int): Boolean {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return false
        val sourceBlockId = findNearestTextBlockId(index)
        val before = documentSnapshot()
        val tableBlock = TableBlock.create(rows, columns)
        _blocks.add(index, tableBlock)
        recordDocumentChange(before)
        if (sourceBlockId != null) {
            navSelectTable(tableId, sourceBlockId)
        } else {
            setBlockAnchorCursor(tableId)
        }
        return true
    }

    fun updateTable(tableId: String, newTable: TableBlock) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index >= 0) {
            val before = documentSnapshot()
            _blocks[index] = newTable
            recordDocumentChange(before)
        }
    }

    fun insertTableRow(tableId: String, afterRowIndex: Int, insertAbove: Boolean) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        val before = documentSnapshot()
        val insertIndex = if (insertAbove) afterRowIndex else afterRowIndex + 1
        val newCellCount = block.columnCount
        val newRow = TableBlock.TableRow(
            cells = List(newCellCount) { TableBlock.TableCell() },
            isHeader = false
        )
        val newRows = block.rows.toMutableList().apply { add(insertIndex, newRow) }
        _blocks[index] = block.copy(rows = newRows)
        recordDocumentChange(before)
    }

    fun insertTableColumn(tableId: String, afterColumnIndex: Int, insertLeft: Boolean) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        val before = documentSnapshot()
        val insertIndex = if (insertLeft) afterColumnIndex else afterColumnIndex + 1
        val newRows = block.rows.map { row ->
            val newCells = row.cells.toMutableList().apply {
                add(insertIndex, TableBlock.TableCell())
            }
            row.copy(cells = newCells)
        }
        _blocks[index] = block.copy(rows = newRows, columnCount = block.columnCount + 1)
        recordDocumentChange(before)
    }

    fun deleteTableRow(tableId: String, rowIndex: Int) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        if (block.rows.size <= 1) return
        val before = documentSnapshot()
        val newRows = block.rows.toMutableList().apply { removeAt(rowIndex) }
        _blocks[index] = block.copy(rows = newRows)
        recordDocumentChange(before)
    }

    fun deleteTableColumn(tableId: String, columnIndex: Int) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        if (block.columnCount <= 1) return
        val before = documentSnapshot()
        val newRows = block.rows.map { row ->
            val newCells = row.cells.toMutableList().apply { removeAt(columnIndex) }
            row.copy(cells = newCells)
        }
        _blocks[index] = block.copy(rows = newRows, columnCount = block.columnCount - 1)
        recordDocumentChange(before)
    }

    /**
     * 直接根据 tableId 删除整个表格块。
     *
     * 与 `deleteTableRow` / `deleteTableColumn` 命名风格一致,接受 tableId 即可,
     * 不依赖 `selectedTableBlockId` 选中态 — 这是为了支持从行/列菜单直接调用的场景
     * (菜单里点"删除当前表格"时,UI 选中态可能还没同步到 BlockState.selectedTableBlockId)。
     *
     * 复用 `deleteSelectedTable` 的删除 + 上下文本块合并 + 焦点处理逻辑,
     * 把选中态判断拆出来:`deleteSelectedTable` 委托给本方法。
     */
    fun deleteTable(tableId: String): Boolean {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return false
        val block = _blocks[index]
        if (block !is TableBlock) return false

        val before = documentSnapshot()
        val previousBlock = if (index > 0) _blocks[index - 1] else null
        val nextBlock = if (index + 1 < _blocks.size) _blocks[index + 1] else null

        _blocks.removeAt(index)
        blockStates.remove(tableId)
        if (selectedTableBlockId == tableId) {
            selectedTableBlockId = null
        }

        if (previousBlock is TextBlock && nextBlock is TextBlock) {
            val prevIndex = _blocks.indexOfFirst { it.id == previousBlock.id }
            if (prevIndex >= 0) {
                val prevState = getBlockState(previousBlock.id)
                val nextState = getBlockState(nextBlock.id)
                val prevContent = prevState.textFieldValue.annotatedString
                val nextContent = nextState.textFieldValue.annotatedString
                val mergedBuilder = AnnotatedString.Builder(prevContent.length + nextContent.length)
                mergedBuilder.append(prevContent)
                mergedBuilder.append(nextContent)
                val merged = mergedBuilder.toAnnotatedString()
                prevState.restoreTextFieldValue(
                    TextFieldValue(
                        annotatedString = merged,
                        selection = TextRange(prevContent.length)
                    )
                )
                _blocks[prevIndex] = previousBlock.copy(content = merged)
                val nextIndex = _blocks.indexOfFirst { it.id == nextBlock.id }
                if (nextIndex >= 0) {
                    _blocks.removeAt(nextIndex)
                    blockStates.remove(nextBlock.id)
                }
                setFocusedBlockCompat(previousBlock.id, requestFocus = true)
            }
        } else if (nextBlock != null) {
            setFocusedBlockCompat(nextBlock.id, requestFocus = true)
        } else if (previousBlock != null) {
            setFocusedBlockCompat(previousBlock.id, requestFocus = true)
        }

        if (_blocks.isEmpty()) {
            val newBlock = TextBlock()
            _blocks.add(newBlock)
            blockStates[newBlock.id] = createBlockState()
            setFocusedBlockCompat(newBlock.id)
        }

        recordDocumentChange(before)
        return true
    }

    fun updateTableCellContent(tableId: String, rowId: String, cellId: String, content: AnnotatedString) {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        val oldContent = block.rows
            .firstOrNull { it.rowId == rowId }
            ?.cells
            ?.firstOrNull { it.cellId == cellId }
            ?.content
        if (oldContent == content) return
        val before = if (!suppressContentUndoRecord) documentSnapshot() else null
        val newRows = block.rows.map { row ->
            if (row.rowId != rowId) {
                row
            } else {
                row.copy(
                    cells = row.cells.map { cell ->
                        if (cell.cellId == cellId) cell.copy(content = content) else cell
                    }
                )
            }
        }
        _blocks[index] = block.copy(rows = newRows)
        if (before != null) {
            recordDocumentChange(before, mergeKey = "cell:$tableId:$rowId:$cellId")
        }
    }

    fun moveTableRow(tableId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        if (fromIndex < 0 || fromIndex >= block.rows.size) return
        if (toIndex < 0 || toIndex >= block.rows.size) return
        val before = documentSnapshot()
        val newRows = block.rows.toMutableList()
        val row = newRows.removeAt(fromIndex)
        newRows.add(toIndex, row)
        _blocks[index] = block.copy(rows = newRows)
        recordDocumentChange(before)
    }

    fun moveTableColumn(tableId: String, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return
        val block = _blocks[index]
        if (block !is TableBlock) return
        if (fromIndex < 0 || fromIndex >= block.columnCount) return
        if (toIndex < 0 || toIndex >= block.columnCount) return
        val before = documentSnapshot()
        val newRows = block.rows.map { row ->
            val newCells = row.cells.toMutableList()
            val cell = newCells.removeAt(fromIndex)
            newCells.add(toIndex, cell)
            row.copy(cells = newCells)
        }
        _blocks[index] = block.copy(rows = newRows)
        recordDocumentChange(before)
    }

    fun tryDeleteAtBlockStart(blockId: String): Boolean {
        val index = _blocks.indexOfFirst { it.id == blockId }
        if (index <= 0) return false
        val previousBlock = _blocks[index - 1]
        if (previousBlock is TableBlock) {
            if (selectedTableBlockId == previousBlock.id) {
                return deleteSelectedTable()
            }
            selectedTableBlockId = previousBlock.id
            return true
        }
        return false
    }

    fun tryHandleTextBlockValueChangeBeforeApply(
        blockId: String,
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): Boolean {
        if (oldValue.text.isNotEmpty()) return false
        if (newValue.text.isNotEmpty()) return false
        if (!oldValue.selection.collapsed || oldValue.selection.min != 0) return false
        if (!newValue.selection.collapsed || newValue.selection.min != 0) return false
        return tryDeleteAtBlockStart(blockId)
    }

    fun navSelectTable(tableId: String, sourceBlockId: String) {
        setBlockAnchorCursor(tableId)
        navSourceBlockId = sourceBlockId
        selectedTableBlockId = null
    }

    fun navClearSelection() {
        navSelectedTableId = null
        navSourceBlockId = null
        val current = editorCursor
        if (current is EditorCursor.BlockAnchor) {
            editorCursor = EditorCursor.Text(focusedBlockId)
        }
    }

    fun navRestoreIfPending() {
        val restoreId = pendingNavRestoreTableId ?: return
        pendingNavRestoreTableId = null
        setBlockAnchorCursor(restoreId)
    }

    fun navGoUp(): Boolean {
        val tableId = navSelectedTableId ?: return false
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index <= 0) return false
        val prevBlock = _blocks[index - 1]
        if (prevBlock is TableBlock) {
            setBlockAnchorCursor(prevBlock.id)
            navSourceBlockId = null
            return true
        }
        if (prevBlock is TextBlock) {
            navClearSelection()
            setTextCursor(prevBlock.id, requestFocus = true)
            return true
        }
        return false
    }

    fun navGoDown(): Boolean {
        val tableId = navSelectedTableId ?: return false
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0 || index >= _blocks.size - 1) return false
        val nextBlock = _blocks[index + 1]
        if (nextBlock is TableBlock) {
            setBlockAnchorCursor(nextBlock.id)
            navSourceBlockId = null
            return true
        }
        if (nextBlock is TextBlock) {
            navClearSelection()
            setTextCursor(nextBlock.id, requestFocus = true)
            return true
        }
        return false
    }

    fun navInsertBeforeTable(): Boolean {
        val tableId = navSelectedTableId ?: return false
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return false
        val before = documentSnapshot()
        val prevBlock = if (index > 0) _blocks[index - 1] else null
        if (prevBlock is TextBlock) {
            val state = getBlockState(prevBlock.id)
            val currentText = state.textFieldValue.text
            state.restoreTextFieldValue(
                androidx.compose.ui.text.input.TextFieldValue(
                    text = currentText + "\n",
                    selection = androidx.compose.ui.text.TextRange(currentText.length + 1)
                )
            )
            setTextCursor(prevBlock.id, requestFocus = true)
            navClearSelection()
            recordDocumentChange(before)
            return true
        } else {
            val newBlock = TextBlock()
            _blocks.add(index, newBlock)
            blockStates[newBlock.id] = createBlockState()
            setTextCursor(newBlock.id, requestFocus = true)
            navClearSelection()
            recordDocumentChange(before)
            return true
        }
    }

    fun navDeleteFromAbove(): Boolean {
        val tableId = navSelectedTableId ?: return false
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index <= 0) return false
        val targetIndex = index - 1
        val prevBlock = _blocks[targetIndex]
        val before = documentSnapshot()
        val deletedFocusedBlock = prevBlock.id == focusedBlockId
        if (prevBlock is TableBlock) {
            if (selectedTableBlockId == prevBlock.id) {
                _blocks.removeAt(targetIndex)
                blockStates.remove(prevBlock.id)
                selectedTableBlockId = null
                recordDocumentChange(before)
            } else {
                selectedTableBlockId = prevBlock.id
                recordDocumentChange(before)
            }
            setBlockAnchorCursor(tableId)
            return true
        }
        if (prevBlock is TextBlock) {
            val state = getBlockState(prevBlock.id)
            val content = state.textFieldValue.annotatedString
            if (content.isNotEmpty()) {
                val deleteStart = content.length - 1
                val links = content.getStringAnnotations(RichTextState.HYPERLINK_TAG, deleteStart, content.length)
                if (links.isNotEmpty()) {
                    val link = links.first()
                    val hyperlinkDeleteKey = "${prevBlock.id}:${link.start}:${link.end}:${link.item}"
                    val selection = state.textFieldValue.selection
                    if (selection.min <= link.start && selection.max >= link.end) {
                        val deleteBefore = if (pendingHyperlinkDeleteKey == hyperlinkDeleteKey) {
                            pendingHyperlinkDeleteSnapshot ?: before
                        } else {
                            before
                        }
                        val builder = AnnotatedString.Builder()
                        if (link.start > 0) builder.append(content.subSequence(0, link.start))
                        if (link.end < content.length) builder.append(content.subSequence(link.end, content.length))
                        val newContent = builder.toAnnotatedString()
                        state.restoreTextFieldValue(
                            TextFieldValue(
                                annotatedString = newContent,
                                selection = TextRange(link.start)
                            )
                        )
                        _blocks[targetIndex] = prevBlock.copy(content = newContent)
                        setBlockAnchorCursor(tableId)
                        if (deletedFocusedBlock) {
                            setTextFocusOnly(prevBlock.id, requestFocus = true)
                        }
                        recordDocumentChange(deleteBefore)
                        return true
                    }
                    state.restoreTextFieldValue(
                        state.textFieldValue.copy(
                            selection = TextRange(link.start, link.end)
                        )
                    )
                    setBlockAnchorCursor(tableId)
                    if (deletedFocusedBlock) {
                        setTextFocusOnly(prevBlock.id, requestFocus = true)
                    }
                    pendingHyperlinkDeleteSnapshot = before
                    pendingHyperlinkDeleteKey = hyperlinkDeleteKey
                    return true
                }
                val newContent = content.subSequence(0, deleteStart)
                state.restoreTextFieldValue(
                    TextFieldValue(
                        annotatedString = newContent,
                        selection = TextRange(newContent.length)
                    )
                )
                _blocks[targetIndex] = prevBlock.copy(content = newContent)
                setBlockAnchorCursor(tableId)
                if (deletedFocusedBlock) {
                    setTextFocusOnly(prevBlock.id, requestFocus = true)
                }
                recordDocumentChange(before)
                return true
            }
            _blocks.removeAt(targetIndex)
            blockStates.remove(prevBlock.id)
            setBlockAnchorCursor(tableId)
            if (deletedFocusedBlock) {
                restoreNearestTextFocus(targetIndex)
            }
            recordDocumentChange(before)
            return true
        }
        return false
    }

    private fun findNearestTextBlockId(anchorIndex: Int): String? {
        val startIndex = anchorIndex.coerceAtLeast(0)
        for (i in startIndex until _blocks.size) {
            val block = _blocks[i]
            if (block is TextBlock) return block.id
        }
        val endIndex = anchorIndex.coerceAtMost(_blocks.lastIndex)
        for (i in endIndex downTo 0) {
            val block = _blocks[i]
            if (block is TextBlock) return block.id
        }
        return null
    }

    private fun focusNearestTextBlock(anchorIndex: Int) {
        findNearestTextBlockId(anchorIndex)?.let {
            setTextCursor(it, requestFocus = true)
        }
    }

    fun navigateFromTable(tableId: String, direction: Int): Boolean {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return false
        val targetIndex = index + direction
        if (targetIndex < 0 || targetIndex >= _blocks.size) return false
        val targetBlock = _blocks[targetIndex]
        if (targetBlock is TextBlock) {
            navClearSelection()
            setFocusedBlockCompat(targetBlock.id)
            return true
        }
        if (targetBlock is TableBlock) {
            setBlockAnchorCursor(targetBlock.id)
            navSourceBlockId = null
            return true
        }
        return false
    }

    fun trySelectTableAbove(blockId: String): Boolean {
        val index = _blocks.indexOfFirst { it.id == blockId }
        if (index <= 0) return false
        val previousBlock = _blocks[index - 1]
        if (previousBlock is TableBlock) {
            selectedTableBlockId = previousBlock.id
            return true
        }
        return false
    }

    fun insertOrExpandTextBeforeTable(tableId: String): Boolean {
        val index = _blocks.indexOfFirst { it.id == tableId }
        if (index < 0) return false
        val before = documentSnapshot()
        val prevBlock = if (index > 0) _blocks[index - 1] else null
        if (prevBlock is TextBlock) {
            val state = getBlockState(prevBlock.id)
            val currentText = state.textFieldValue.text
            state.restoreTextFieldValue(
                androidx.compose.ui.text.input.TextFieldValue(
                    text = currentText + "\n",
                    selection = androidx.compose.ui.text.TextRange(currentText.length + 1)
                )
            )
            setFocusedBlockCompat(prevBlock.id)
            selectedTableBlockId = null
            recordDocumentChange(before)
            return true
        } else {
            val newBlock = TextBlock()
            _blocks.add(index, newBlock)
            blockStates[newBlock.id] = createBlockState()
            setFocusedBlockCompat(newBlock.id)
            selectedTableBlockId = null
            recordDocumentChange(before)
            return true
        }
    }

    fun deleteSelectedTable(): Boolean {
        val tableId = selectedTableBlockId ?: return false
        return deleteTable(tableId)
    }

    fun clearTableSelection() {
        selectedTableBlockId = null
    }

    fun deleteBlock(blockId: String) {
        if (_blocks.size <= 1) return
        val index = _blocks.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val before = documentSnapshot()
        _blocks.removeAt(index)
        blockStates.remove(blockId)
        if (focusedBlockId == blockId) {
            val newFocusIndex = if (index > 0) index - 1 else 0
            setFocusedBlockCompat(_blocks[newFocusIndex].id)
        }
        recordDocumentChange(before)
    }

    fun mergeWithPreviousBlock(blockId: String) {
        val index = _blocks.indexOfFirst { it.id == blockId }
        if (index <= 0) return
        val previousBlock = _blocks[index - 1]
        val currentBlock = _blocks[index]
        if (previousBlock !is TextBlock || currentBlock !is TextBlock) return
        val before = documentSnapshot()
        val previousState = getBlockState(previousBlock.id)
        val currentState = getBlockState(currentBlock.id)
        val previousContent = previousState.textFieldValue.annotatedString
        val currentContent = currentState.textFieldValue.annotatedString
        val mergedBuilder = AnnotatedString.Builder(previousContent.length + currentContent.length)
        mergedBuilder.append(previousContent)
        mergedBuilder.append(currentContent)
        val merged = mergedBuilder.toAnnotatedString()
        previousState.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = merged,
                selection = TextRange(previousContent.length)
            )
        )
        _blocks[index - 1] = previousBlock.copy(content = merged)
        _blocks.removeAt(index)
        blockStates.remove(currentBlock.id)
        setFocusedBlockCompat(previousBlock.id)
        recordDocumentChange(before)
    }

    override fun toJson(): String {
        val parts = _blocks.map { block ->
            when (block) {
                is TextBlock -> {
                    val state = blockStates[block.id]
                    val content = state?.textFieldValue?.annotatedString ?: block.content
                    "{\"type\":\"text\",\"content\":${RichTextSerializer.toJson(content)}}"
                }
                is TableBlock -> {
                    val rowsJson = block.rows.joinToString(",") { row ->
                        val cellsJson = row.cells.joinToString(",") { cell ->
                            "{\"id\":\"${cell.cellId}\",\"content\":${RichTextSerializer.toJson(cell.content)}}"
                        }
                        "{\"id\":\"${row.rowId}\",\"cells\":[$cellsJson]}"
                    }
                    "{\"type\":\"table\",\"rows\":[${rowsJson}],\"columns\":${block.columnCount}}"
                }
            }
        }
        return parts.joinToString(
            prefix = "{\"blocks\":[",
            postfix = "]}"
        )
    }

    fun loadFromJson(json: String) {
        _blocks.clear()
        blockStates.clear()
        blockUndoManager.clear()
        loadBlocksFromJson(json)
        setFocusedBlockCompat(_blocks.first().id)
    }

    fun pasteJson(json: String): Boolean {
        return try {
            if (!isBlockJson(json)) return false
            val before = documentSnapshot()
            _blocks.clear()
            blockStates.clear()
            loadBlocksFromJson(json)
            setFocusedBlockCompat(_blocks.first().id)
            clearTableCellFocus()
            selectedTableBlockId = null
            navClearSelection()
            recordDocumentChange(before)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isBlockJson(json: String): Boolean {
        return parseBlockJsonArray(json).isNotEmpty()
    }

    private fun loadBlocksFromJson(json: String) {
        val blockJsons = parseBlockJsonArray(json)
        if (blockJsons.isEmpty()) {
            val block = TextBlock()
            _blocks.add(block)
            blockStates[block.id] = createBlockState()
        } else {
            for (blockJson in blockJsons) {
                val root = RichTextSerializer.parseJsonObject(blockJson)
                when (root["type"] as? String) {
                    "text" -> loadTextBlock(root)
                    "table" -> loadTableBlock(root)
                    else -> loadLegacyTextBlock(root)
                }
            }
        }
    }

    private fun loadTextBlock(root: Map<String, Any?>) {
        val content = root["content"] as? Map<*, *> ?: return loadLegacyTextBlock(root)
        val annotated = RichTextSerializer.fromJsonMap(content)
        val block = TextBlock(content = annotated)
        _blocks.add(block)
        val state = createBlockState()
        state.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(annotated.text.length)
            )
        )
        blockStates[block.id] = state
    }

    private fun loadLegacyTextBlock(root: Map<String, Any?>) {
        val annotated = RichTextSerializer.fromJsonMap(root)
        val block = TextBlock(content = annotated)
        _blocks.add(block)
        val state = createBlockState()
        state.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(annotated.text.length)
            )
        )
        blockStates[block.id] = state
    }

    private fun loadTableBlock(root: Map<String, Any?>) {
        val rowItems = root["rows"] as? List<*> ?: emptyList<Any?>()
        val rows = rowItems.mapNotNull { rowItem ->
            val rowMap = rowItem as? Map<*, *> ?: return@mapNotNull null
            val cellItems = rowMap["cells"] as? List<*> ?: emptyList<Any?>()
            val cells = cellItems.mapNotNull { cellItem ->
                val cellMap = cellItem as? Map<*, *> ?: return@mapNotNull null
                val contentMap = cellMap["content"] as? Map<*, *>
                val content = if (contentMap != null) {
                    RichTextSerializer.fromJsonMap(contentMap)
                } else {
                    AnnotatedString("")
                }
                TableBlock.TableCell(content = content)
            }
            if (cells.isEmpty()) null else TableBlock.TableRow(cells = cells)
        }
        val columns = (root["columns"] as? Double)?.toInt() ?: rows.firstOrNull()?.cells?.size ?: 1
        val normalizedRows = if (rows.isEmpty()) {
            listOf(TableBlock.TableRow(cells = List(columns.coerceAtLeast(1)) { TableBlock.TableCell() }))
        } else {
            rows.map { row ->
                val cells = when {
                    row.cells.size == columns -> row.cells
                    row.cells.size < columns -> row.cells + List(columns - row.cells.size) { TableBlock.TableCell() }
                    else -> row.cells.take(columns)
                }
                row.copy(cells = cells)
            }
        }
        _blocks.add(TableBlock(rows = normalizedRows, columnCount = columns.coerceAtLeast(1)))
    }

    private fun parseBlockJsonArray(json: String): List<String> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{\"blocks\":[")) return emptyList()
        val arrayContent = trimmed.removePrefix("{\"blocks\":[").removeSuffix("]}")
        if (arrayContent.isBlank()) return emptyList()
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in arrayContent.indices) {
            when (arrayContent[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        results.add(arrayContent.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return results
    }

    // region RichTextFormatState - proxy to active state (text block or table cell)

    override val currentFontSize: TextUnit
        get() = activeRichTextState.currentFontSize
    override val currentColor: Color
        get() = activeRichTextState.currentColor
    override val currentBackground: Color
        get() = activeRichTextState.currentBackground
    override val currentBold: Boolean
        get() = activeRichTextState.currentBold
    override val currentItalic: Boolean
        get() = activeRichTextState.currentItalic
    override val currentUnderline: Boolean
        get() = activeRichTextState.currentUnderline
    override val currentStrikethrough: Boolean
        get() = activeRichTextState.currentStrikethrough
    override val currentSuperscript: Boolean
        get() = activeRichTextState.currentSuperscript
    override val currentSubscript: Boolean
        get() = activeRichTextState.currentSubscript
    override val currentTextAlign: TextAlign
        get() = activeRichTextState.currentTextAlign

    override fun currentSpanStyle(): SpanStyle = activeRichTextState.currentSpanStyle()

    override fun toggleBold() = activeRichTextState.toggleBold()
    override fun toggleItalic() = activeRichTextState.toggleItalic()
    override fun toggleUnderline() = activeRichTextState.toggleUnderline()
    override fun toggleStrikethrough() = activeRichTextState.toggleStrikethrough()
    override fun toggleSuperscript() = activeRichTextState.toggleSuperscript()
    override fun toggleSubscript() = activeRichTextState.toggleSubscript()
    override fun clearFormatting() = activeRichTextState.clearFormatting()
    override fun setFontSize(size: TextUnit) = activeRichTextState.setFontSize(size)
    override fun setColor(color: Color) = activeRichTextState.setColor(color)
    override fun setBackground(color: Color) = activeRichTextState.setBackground(color)
    override fun setTextAlign(align: TextAlign) = activeRichTextState.setTextAlign(align)

    override fun saveSelection() = activeRichTextState.saveSelection()
    override fun restoreSavedSelection() = activeRichTextState.restoreSavedSelection()

    override fun undo() {
        pendingHyperlinkDeleteSnapshot = null
        pendingHyperlinkDeleteKey = null
        when (val result = blockUndoManager.undo() ?: return) {
            is BlockUndoManager.UndoResult.Block -> withContentUndoSuppressed {
                val snapshot = result.snapshot
                val state = getBlockState(snapshot.blockId)
                state.restoreTextFieldValue(snapshot.value)
                setFocusedBlockCompat(snapshot.blockId)
                updateBlockContent(snapshot.blockId)
            }
            is BlockUndoManager.UndoResult.Document -> restoreDocumentSnapshot(result.snapshot)
        }
    }

    override fun redo() {
        pendingHyperlinkDeleteSnapshot = null
        pendingHyperlinkDeleteKey = null
        when (val result = blockUndoManager.redo() ?: return) {
            is BlockUndoManager.UndoResult.Block -> withContentUndoSuppressed {
                val snapshot = result.snapshot
                val state = getBlockState(snapshot.blockId)
                state.restoreTextFieldValue(snapshot.value)
                setFocusedBlockCompat(snapshot.blockId)
                updateBlockContent(snapshot.blockId)
            }
            is BlockUndoManager.UndoResult.Document -> restoreDocumentSnapshot(result.snapshot)
        }
    }

    override fun insertText(text: String) = activeRichTextState.insertText(text)
    override fun insertCustomEmoji(emojiId: String, displaySize: TextUnit) =
        activeRichTextState.insertCustomEmoji(emojiId, displaySize)
    override fun insertHyperlink(text: String, url: String) =
        activeRichTextState.insertHyperlink(text, url)

    // endregion
}
