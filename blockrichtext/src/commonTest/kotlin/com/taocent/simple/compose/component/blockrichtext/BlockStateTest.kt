package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.taocent.simple.compose.component.blockrichtext.internal.block.EditorCursor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 覆盖 [BlockState] 文档结构、文本块、表格块、删除/合并、选择高亮、
 * 焦点、undo/redo 与 JSON 序列化等核心契约。
 */
class BlockStateTest {

    // region 初始结构

    @Test
    fun newBlockStateHasSingleEmptyTextBlock() {
        val state = BlockState()
        assertEquals(1, state.blocks.size)
        val first = state.blocks.first()
        assertTrue(first is TextBlock)
        assertEquals("", (first).content.text)
    }

    @Test
    fun newBlockStateHasFocusedBlockMatchingFirst() {
        val state = BlockState()
        assertEquals(state.blocks.first().id, state.focusedBlockId)
    }

    // endregion

    // region TextBlock 内容编辑

    @Test
    fun updateBlockContentPropagatesTextToBlockModel() {
        val state = BlockState()
        val blockId = state.focusedBlockId
        val richTextState = state.getBlockState(blockId)
        richTextState.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("hello"), selection = TextRange(5))
        )
        state.updateBlockContent(blockId)
        val block = state.blocks.first { it.id == blockId } as TextBlock
        assertEquals("hello", block.content.text)
    }

    @Test
    fun insertBlockAfterAddsTextBlock() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val inserted = state.insertBlockAfter(firstId)
        assertEquals(2, state.blocks.size)
        assertEquals(inserted.id, state.blocks[1].id)
        assertTrue(state.blocks[1] is TextBlock)
    }

    @Test
    fun deleteBlockRemovesIt() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val inserted = state.insertBlockAfter(firstId)
        assertEquals(2, state.blocks.size)

        state.deleteBlock(inserted.id)
        assertEquals(1, state.blocks.size)
        assertEquals(firstId, state.blocks.first().id)
    }

    @Test
    fun deleteLastBlockIsNoOp() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        state.deleteBlock(firstId)
        // 不应清空所有 block
        assertEquals(1, state.blocks.size)
    }

    @Test
    fun emptyTextValueChangeAtStartSelectsPreviousTable() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks[0] as TableBlock
        val textAfter = state.blocks[1] as TextBlock
        val emptyAtStart = TextFieldValue(text = "", selection = TextRange(0))

        val consumed = state.tryHandleTextBlockValueChangeBeforeApply(textAfter.id, emptyAtStart, emptyAtStart)

        assertTrue(consumed)
        assertEquals(table.id, state.selectedTableBlockId)
        assertEquals(2, state.blocks.size)
    }

    @Test
    fun secondEmptyTextValueChangeAtStartDeletesSelectedPreviousTable() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks[0] as TableBlock
        val textAfter = state.blocks[1] as TextBlock
        val emptyAtStart = TextFieldValue(text = "", selection = TextRange(0))

        state.tryHandleTextBlockValueChangeBeforeApply(textAfter.id, emptyAtStart, emptyAtStart)
        val consumed = state.tryHandleTextBlockValueChangeBeforeApply(textAfter.id, emptyAtStart, emptyAtStart)

        assertTrue(consumed)
        assertNull(state.selectedTableBlockId)
        assertEquals(1, state.blocks.size)
        assertEquals(textAfter.id, state.blocks.first().id)
        assertTrue(state.blocks.none { it.id == table.id })
    }

    @Test
    fun textValueChangeDoesNotDeletePreviousTableWhenCursorNotAtStart() {
        val state = BlockState()
        state.insertTable(2, 2)
        val textAfter = state.blocks[1] as TextBlock
        val oldValue = TextFieldValue(text = "a", selection = TextRange(1))
        val newValue = TextFieldValue(text = "a", selection = TextRange(1))

        val consumed = state.tryHandleTextBlockValueChangeBeforeApply(textAfter.id, oldValue, newValue)

        assertFalse(consumed)
        assertNull(state.selectedTableBlockId)
        assertEquals(2, state.blocks.size)
    }

    @Test
    fun mergeWithPreviousBlockJoinsTwoTextBlocks() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val firstState = state.getBlockState(firstId)
        firstState.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("foo"), selection = TextRange(3))
        )
        state.updateBlockContent(firstId)
        val inserted = state.insertBlockAfter(firstId)
        val secondState = state.getBlockState(inserted.id)
        secondState.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("bar"), selection = TextRange(3))
        )
        state.updateBlockContent(inserted.id)

        state.mergeWithPreviousBlock(inserted.id)
        assertEquals(1, state.blocks.size)
        val merged = state.blocks.first() as TextBlock
        assertEquals("foobar", merged.content.text)
    }

    // endregion

    // region TableBlock 操作

    @Test
    fun insertTableReplacesEmptyTextBlock() {
        val state = BlockState()
        val v0 = state.focusRequestVersion
        state.insertTable(2, 3)
        assertTrue(state.blocks.any { it is TableBlock })
        val table = state.blocks.first { it is TableBlock } as TableBlock
        assertEquals(2, table.rows.size)
        assertEquals(3, table.columnCount)
        // 第一行是表头
        assertTrue(table.rows.first().isHeader)
        assertFalse(table.rows[1].isHeader)
        // 空文本块场景下,焦点应转移到表格下方新建的文本块,
        // 并自增 focusRequestVersion 让顶层 LaunchedEffect 真正调用 requestFocus()
        val textAfterTable = state.blocks[1] as TextBlock
        assertEquals(textAfterTable.id, state.focusedBlockId)
        assertEquals(v0 + 1, state.focusRequestVersion)
    }

    @Test
    fun insertTableInMiddleOfTextSplitsIntoOneTextBlockAfterTable() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        state.getBlockState(firstId).restoreTextFieldValue(
            TextFieldValue(text = "12345", selection = TextRange(3))
        )

        state.insertTable(2, 2)

        assertEquals(3, state.blocks.size)
        val beforeText = state.blocks[0] as TextBlock
        val table = state.blocks[1] as TableBlock
        val afterText = state.blocks[2] as TextBlock
        assertEquals(firstId, beforeText.id)
        assertEquals("123", beforeText.content.text)
        assertEquals(2, table.rows.size)
        assertEquals("45", afterText.content.text)
        assertEquals(afterText.id, state.focusedBlockId)
    }

    @Test
    fun insertTableRowAtTopInsertsAbove() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock

        state.insertTableRow(table.id, afterRowIndex = 0, insertAbove = true)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(3, updated.rows.size)
    }

    @Test
    fun moveTableRowReordersRows() {
        val state = BlockState()
        state.insertTable(3, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val firstRowId = table.rows[0].rowId

        state.moveTableRow(table.id, fromIndex = 0, toIndex = 2)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(firstRowId, updated.rows[2].rowId)
    }

    @Test
    fun moveTableRowSameIndexIsNoOp() {
        val state = BlockState()
        state.insertTable(2, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val snapshot = (state.blocks.first { it.id == table.id } as TableBlock).rows
        state.moveTableRow(table.id, 0, 0)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(snapshot.map { it.rowId }, updated.rows.map { it.rowId })
    }

    @Test
    fun moveTableColumnReordersCells() {
        val state = BlockState()
        state.insertTable(2, 3)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val firstCellId = table.rows[0].cells[0].cellId

        state.moveTableColumn(table.id, fromIndex = 0, toIndex = 2)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(firstCellId, updated.rows[0].cells[2].cellId)
    }

    @Test
    fun moveTableColumnOutOfRangeIsNoOp() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val snapshot = (state.blocks.first { it.id == table.id } as TableBlock).rows
        state.moveTableColumn(table.id, -1, 1)
        state.moveTableColumn(table.id, 0, 99)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(snapshot.map { it.cells.map { c -> c.cellId } }, updated.rows.map { it.cells.map { c -> c.cellId } })
    }

    @Test
    fun updateTableCellContentReplacesText() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val row = table.rows.first()
        val cell = row.cells.first()

        state.updateTableCellContent(table.id, row.rowId, cell.cellId, AnnotatedString("X"))
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        val updatedCell = updated.rows.first().cells.first()
        assertEquals("X", updatedCell.content.text)
    }

    @Test
    fun updateTableCellContentIgnoresUnchangedContent() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val row = table.rows.first()
        val cell = row.cells.first()
        state.updateTableCellContent(table.id, row.rowId, cell.cellId, AnnotatedString("X"))
        val before = (state.blocks.first { it.id == table.id } as TableBlock)
        // 同样内容再次调用不应改变状态
        state.updateTableCellContent(table.id, row.rowId, cell.cellId, AnnotatedString("X"))
        val after = (state.blocks.first { it.id == table.id } as TableBlock)
        assertEquals(before, after)
    }

    @Test
    fun updateTableCellContentIgnoresUnknownIds() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val before = state.blocks.first { it.id == table.id } as TableBlock

        state.updateTableCellContent(table.id, "bad-row", "bad-cell", AnnotatedString("X"))
        state.updateTableCellContent("bad-table", "bad-row", "bad-cell", AnnotatedString("X"))

        val after = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(before.rows.first().cells.first().content.text, after.rows.first().cells.first().content.text)
    }

    @Test
    fun updateTableReplacesBlock() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val replacement = TableBlock(
            id = table.id,
            rows = listOf(
                TableBlock.TableRow(cells = listOf(TableBlock.TableCell(content = AnnotatedString("a")))),
                TableBlock.TableRow(cells = listOf(TableBlock.TableCell(content = AnnotatedString("b"))))
            ),
            columnCount = 1
        )
        state.updateTable(table.id, replacement)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(2, updated.rows.size)
        assertEquals("a", updated.rows[0].cells[0].content.text)
        assertEquals("b", updated.rows[1].cells[0].content.text)
    }

    // endregion

    // region 表格选择与删除

    @Test
    fun tryDeleteAtBlockStartSelectsTableAbove() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val secondBlock = state.insertBlockAfter(table.id)

        val selected = state.tryDeleteAtBlockStart(secondBlock.id)
        assertTrue(selected)
        assertEquals(table.id, state.selectedTableBlockId)
    }

    @Test
    fun tryDeleteAtBlockStartReturnsFalseForFirstBlock() {
        val state = BlockState()
        val first = state.focusedBlockId
        assertFalse(state.tryDeleteAtBlockStart(first))
    }

    @Test
    fun trySelectTableAboveReturnsTrueForAdjacentTable() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val after = state.insertBlockAfter(table.id)

        assertTrue(state.trySelectTableAbove(after.id))
        assertEquals(table.id, state.selectedTableBlockId)
    }

    @Test
    fun deleteSelectedTableMergesNeighbouringTextBlocks() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val firstState = state.getBlockState(firstId)
        firstState.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("foo"), selection = TextRange(3))
        )
        state.updateBlockContent(firstId)
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val after = state.insertBlockAfter(table.id)
        val afterState = state.getBlockState(after.id)
        afterState.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("bar"), selection = TextRange(3))
        )
        state.updateBlockContent(after.id)

        // 第一次 backspace: 选中 table
        val text = state.getBlockState(firstId)
        val pos = text.textFieldValue.text.length
        // 通过 trySelectTableAbove 模拟选中
        state.trySelectTableAbove(after.id)
        assertEquals(table.id, state.selectedTableBlockId)

        // 第二次 backspace: 删除 table,合并前后 TextBlock
        val v0 = state.focusRequestVersion
        state.deleteSelectedTable()
        assertFalse(state.blocks.any { it is TableBlock })
        // 合并应得到 "foobar"
        val first = state.blocks.first() as TextBlock
        assertEquals("foobar", first.content.text)
        assertEquals(firstId, state.focusedBlockId)
        assertEquals(v0 + 1, state.focusRequestVersion)
        val cursor = state.editorCursor as EditorCursor.Text
        assertEquals(firstId, cursor.blockId)
        assertEquals(3, cursor.selectionStart)
        assertEquals(3, cursor.selectionEnd)
    }

    @Test
    fun clearTableSelectionClearsSelectedTableBlockId() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val after = state.insertBlockAfter(table.id)
        state.trySelectTableAbove(after.id)
        assertEquals(table.id, state.selectedTableBlockId)
        state.clearTableSelection()
        assertNull(state.selectedTableBlockId)
    }

    // endregion

    // region nav 导航

    @Test
    fun navSelectTableClearsTableSelection() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val after = state.insertBlockAfter(table.id)
        state.trySelectTableAbove(after.id)
        assertEquals(table.id, state.selectedTableBlockId)

        state.navSelectTable(table.id, after.id)
        assertNull(state.selectedTableBlockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals(after.id, state.navSourceBlockId)
    }

    @Test
    fun navGoUpAtFirstTableReturnsFalse() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        state.navSelectTable(table.id, state.focusedBlockId)
        assertFalse(state.navGoUp())
    }

    @Test
    fun navGoDownAtLastTableReturnsFalse() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        // 移除其后的 TextBlock,使 table 成为最后一个 block
        val lastId = state.blocks.last().id
        if (lastId != table.id) {
            state.deleteBlock(lastId)
        }
        state.navSelectTable(table.id, state.focusedBlockId)
        assertFalse(state.navGoDown())
    }

    @Test
    fun navigateFromTableMovesToAdjacentTextBlock() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val before = state.insertBlockAfter(state.focusedBlockId) // 在 table 前插入
        // 重新拉取 ids
        val tableIndex = state.blocks.indexOfFirst { it.id == table.id }
        val after = (state.blocks.getOrNull(tableIndex + 1) as? TextBlock)

        assertNotNull(after)
        val moved = state.navigateFromTable(table.id, direction = 1)
        assertTrue(moved)
        assertEquals(after.id, state.focusedBlockId)
    }

    @Test
    fun navigateFromTableReturnsFalseForOutOfBounds() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        assertFalse(state.navigateFromTable(table.id, direction = -1))
        assertFalse(state.navigateFromTable("missing", direction = 1))
    }

    // endregion

    // region JSON 序列化/反序列化

    @Test
    fun toJsonProducesBlockJson() {
        val state = BlockState()
        state.insertTable(1, 2)
        val json = state.toJson()
        assertTrue(json.startsWith("{\"blocks\":["))
        assertTrue(json.contains("\"type\":\"text\""))
        assertTrue(json.contains("\"type\":\"table\""))
    }

    @Test
    fun loadFromJsonClearsExistingBlocks() {
        val state = BlockState()
        state.insertTable(2, 2)
        assertTrue(state.blocks.size >= 2)

        // 加载一个空 doc(只有初始 TextBlock)
        val source = BlockState()
        val sourceJson = source.toJson()
        state.loadFromJson(sourceJson)
        // loadFromJson 后只剩 1 个初始 TextBlock
        assertEquals(1, state.blocks.size)
        assertTrue(state.blocks.first() is TextBlock)
    }

    @Test
    fun pasteJsonOnInvalidJsonKeepsOriginalState() {
        val state = BlockState()
        val originalBlocks = state.blocks.toList()
        assertFalse(state.pasteJson("garbage"))
        assertEquals(originalBlocks.size, state.blocks.size)
        assertEquals(originalBlocks.map { it.id }, state.blocks.map { it.id })
    }

    @Test
    fun pasteJsonClearsTableCellFocus() {
        val state = BlockState()
        state.insertTable(1, 1)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val after = state.insertBlockAfter(table.id)
        val text = state.getBlockState(after.id)
        text.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("abc"), selection = TextRange(3))
        )
        state.updateBlockContent(after.id)

        // 模拟剪贴板粘贴
        val source = BlockState()
        source.insertTable(1, 1)
        state.pasteJson(source.toJson())
        assertNull(state.focusedTableCellState)
    }

    // endregion

    // region 样式状态代理

    @Test
    fun blockStateProxiesCurrentBoldToFocusedBlock() {
        val state = BlockState()
        state.toggleBold()
        assertTrue(state.currentBold)
        // 再次切换,应反映在 focused block state 上
        state.toggleBold()
        assertFalse(state.currentBold)
    }

    @Test
    fun blockStateSetColorProxiesToFocusedBlock() {
        val state = BlockState()
        val red = androidx.compose.ui.graphics.Color(0xFFFF0000)
        state.setColor(red)
        // active state 的 currentColor 已更新
        assertEquals(red, state.currentColor)
    }

    // endregion

    // region 富文本内部内容(RichTextState 间接验证)

    @Test
    fun blockStateInsertHyperlinkDelegatesToFocusedBlock() {
        val state = BlockState()
        state.insertHyperlink(text = "click", url = "https://example.com")
        val focused = state.getBlockState(state.focusedBlockId)
        val links = focused.textFieldValue.annotatedString
            .getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, focused.textFieldValue.text.length)
        assertTrue(links.isNotEmpty())
        assertEquals("https://example.com", links.first().item)
        assertTrue(focused.textFieldValue.text.contains("click"))
    }

    // endregion

    // region SpanStyle 保持(Style 持久化)

    @Test
    fun blockStateKeepsSpanStyleAfterUpdateBlockContent() {
        val state = BlockState()
        val blockId = state.focusedBlockId
        val richTextState = state.getBlockState(blockId)
        val annotated = AnnotatedString.Builder("hello").apply {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, 5)
        }.toAnnotatedString()
        richTextState.restoreTextFieldValue(
            TextFieldValue(annotatedString = annotated, selection = TextRange(5))
        )
        state.updateBlockContent(blockId)

        val block = state.blocks.first { it.id == blockId } as TextBlock
        val spans = block.content.spanStyles
        assertTrue(spans.any { it.item.fontWeight == FontWeight.Bold && it.start == 0 && it.end == 5 })
    }

    // endregion

    // region 焦点请求语义(focusRequestVersion 契约)
    //
    // 背景:BlockRichTextEditor 顶层 `LaunchedEffect(focusRequestVersion)` 会
    // 调用 `requestFocus()`,从而在移动端触发软键盘。
    // 因此 `focusRequestVersion` 只能由"用户显式触发的操作"自增,以避免
    // LazyColumn 滚动/重组让某个 text block 首次进入组合时被动抢走焦点。

    @Test
    fun newBlockStateHasZeroFocusRequestVersion() {
        val state = BlockState()
        assertEquals(0, state.focusRequestVersion)
    }

    @Test
    fun focusBlockDoesNotIncrementFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val v0 = state.focusRequestVersion
        // focusBlock 走 setTextCursor(默认 requestFocus = false) 路径,
        // 不应自增 version,否则在用户主动点击 text block 切换焦点时,
        // 顶层会重复 requestFocus,移动端也会弹键盘(用户其实已经在点,弹是合理的,
        // 但 version 增长会让其他无关 effect 重跑)。
        state.focusBlock(firstId)
        assertEquals(v0, state.focusRequestVersion)
        state.focusBlock("nonexistent")
        assertEquals(v0, state.focusRequestVersion)
    }

    @Test
    fun requestFocusedBlockFocusIncrementsVersion() {
        val state = BlockState()
        assertEquals(0, state.focusRequestVersion)
        state.requestFocusedBlockFocus()
        assertEquals(1, state.focusRequestVersion)
        state.requestFocusedBlockFocus()
        assertEquals(2, state.focusRequestVersion)
    }

    @Test
    fun insertBlockAfterDoesNotIncrementFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val v0 = state.focusRequestVersion
        // insertBlockAfter 走 setFocusedBlockCompat(默认 requestFocus = false),
        // 不应自增 version —— 因为这是文档结构变化,不是用户对光标位置的显式请求。
        state.insertBlockAfter(firstId)
        assertEquals(v0, state.focusRequestVersion)
    }

    @Test
    fun deleteBlockDoesNotIncrementFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        val inserted = state.insertBlockAfter(firstId)
        val v0 = state.focusRequestVersion
        state.deleteBlock(inserted.id)
        assertEquals(v0, state.focusRequestVersion)
    }

    @Test
    fun navGoUpToTextBlockIncrementsFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        // 让 firstText 有内容(避免 insertTable 时被原地替换)
        val firstRich = state.getBlockState(firstId)
        firstRich.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("a"), selection = TextRange(1))
        )
        state.updateBlockContent(firstId)
        // 插入 table(firstText 因 cursorPos == contentLength 被推到 [0],table 入 [1],textBlockAfter 入 [2])
        state.insertTable(2, 2)
        val tableId = state.blocks.first { it is TableBlock }.id
        // nav 源指向 firstText,这样 navGoUp 时能走到 firstText
        state.navSelectTable(tableId, firstId)
        val v0 = state.focusRequestVersion
        // 上一格是 firstText(text),setTextCursor(..., requestFocus = true) 自增 version
        state.navGoUp()
        assertEquals(v0 + 1, state.focusRequestVersion)
    }

    @Test
    fun navGoDownToTextBlockIncrementsFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        // 让 firstText 有内容,避免 insertTable 原地替换
        val firstRich = state.getBlockState(firstId)
        firstRich.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("a"), selection = TextRange(1))
        )
        state.updateBlockContent(firstId)
        // 插入 table,结构: [firstText, table, textBlockAfter]
        state.insertTable(2, 2)
        val tableId = state.blocks.first { it is TableBlock }.id
        // 把焦点先放回 firstText(不传 requestFocus,保持 v0 不变)
        state.focusBlock(firstId)
        state.navSelectTable(tableId, firstId)
        val v0 = state.focusRequestVersion
        // 下一格是 table 自身;再下一格 textBlockAfter — 等等,navGoDown 实际
        // 是从 navSelectedTableId(table)往下找:table index 1,下一格 [2] = textBlockAfter
        state.navGoDown()
        assertEquals(v0 + 1, state.focusRequestVersion)
    }

    @Test
    fun undoOfDocumentChangeWithTextCursorIncrementsFocusRequestVersion() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        // 通过 insertBlockAfter / deleteBlock 产生 Document undo entry
        // 这两个操作都不自增 version
        val inserted = state.insertBlockAfter(firstId)
        state.deleteBlock(inserted.id)
        val v0 = state.focusRequestVersion
        // 此时 cursor 是 EditorCursor.Text(firstId)(自 init 后未变)
        // undo 走 Document 路径,restoreDocumentSnapshot 检测到 cursor=Text,
        // 自增 version,让顶层 LaunchedEffect 把焦点恢复到 firstId
        state.undo()
        assertEquals(v0 + 1, state.focusRequestVersion)
    }

    @Test
    fun focusRequestVersionAccumulatesAcrossMultipleNavOperations() {
        val state = BlockState()
        val firstId = state.focusedBlockId
        // 让 firstText 有内容(避免 insertTable 时被原地替换)
        val firstRich = state.getBlockState(firstId)
        firstRich.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("a"), selection = TextRange(1))
        )
        state.updateBlockContent(firstId)
        // 插入 table,结构: [firstText, table, textBlockAfter]
        state.insertTable(2, 2)
        val tableId = state.blocks.first { it is TableBlock }.id
        // nav 源指向 firstText,这样 navGoUp 才能找到目标
        state.navSelectTable(tableId, firstId)
        val v0 = state.focusRequestVersion
        // 多次显式操作应该持续自增 version
        state.navGoUp() // → firstText,自增 +1
        state.navSelectTable(tableId, firstId) // 不自增
        state.navGoUp() // → firstText,再自增 +1
        assertEquals(v0 + 2, state.focusRequestVersion)
    }

    // endregion
}
