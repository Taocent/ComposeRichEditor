package com.taocent.simple.compose.component

import com.taocent.simple.compose.component.blockrichtext.BlockState
import com.taocent.simple.compose.component.blockrichtext.TableBlock
import com.taocent.simple.compose.component.richtext.RichTextState
import com.taocent.simple.compose.component.richtext.core.DefaultEmojiList
import com.taocent.simple.compose.component.richtext.core.DefaultPresetColors
import com.taocent.simple.compose.component.richtext.core.DefaultPresetFontSizes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 跨模块共享的纯逻辑回归测试。
 * 不依赖任何 Android/JS/Wasm 平台特定 API,只验证
 * 平台桥接、共享 UI 资源默认值以及核心状态类(richText、blockState)对输入输出的稳定契约。
 */
class SharedCommonTest {

    // region Platform (expect/actual 桥接)

    @Test
    fun platformHasNonEmptyName() {
        val platform = getPlatform()
        assertNotNull(platform)
        assertTrue(platform.name.isNotEmpty(), "platform name should be non-empty")
    }

    // endregion

    // region 子模块 RichTextConfig 默认值

    @Test
    fun blockrichtextDefaultPresetColorsIsNotEmpty() {
        assertTrue(DefaultPresetColors.isNotEmpty())
    }

    @Test
    fun blockrichtextDefaultEmojiListIsNotEmpty() {
        assertTrue(DefaultEmojiList.isNotEmpty())
        DefaultEmojiList.forEach { emoji ->
            assertTrue(emoji.isNotEmpty())
        }
    }

    @Test
    fun richtextDefaultEmojiListIsNotEmpty() {
        assertTrue(DefaultEmojiList.isNotEmpty())
    }

    @Test
    fun richtextDefaultPresetFontSizesCoversCommonSizes() {
        val labels = DefaultPresetFontSizes.map { it.label }
        assertTrue("12" in labels)
        assertTrue("16" in labels)
        assertTrue("24" in labels)
    }

    // endregion

    // region RichTextState:基础契约

    @Test
    fun richTextStateStartsEmpty() {
        val state = RichTextState()
        assertEquals("", state.textFieldValue.text)
        assertEquals(0, state.textFieldValue.selection.start)
        assertEquals(0, state.textFieldValue.selection.end)
        assertFalse(state.hasSelection)
    }

    @Test
    fun richTextStateAppliesInitialText() {
        val state = RichTextState(initialText = "hello")
        assertEquals("hello", state.textFieldValue.text)
        // 初始 selection 在 TextRange(0),即光标位于开头
        assertEquals(0, state.textFieldValue.selection.start)
        assertEquals(0, state.textFieldValue.selection.end)
    }

    @Test
    fun richTextStateToggleBoldAffectsCurrentBold() {
        val state = RichTextState()
        state.toggleBold()
        assertTrue(state.currentBold)
        state.toggleBold()
        assertFalse(state.currentBold)
    }

    @Test
    fun richTextStateJsonRoundTripPreservesText() {
        val state = RichTextState(initialText = "payload")
        val json = state.toJson()
        assertTrue(json.contains("\"text\":"))
        assertTrue(json.contains("payload"))

        val restored = RichTextState.fromJson(json)
        assertEquals("payload", restored.text)
    }

    @Test
    fun richTextStateSelectAllExpandsSelection() {
        val state = RichTextState(initialText = "hello")
        state.selectAll()
        assertTrue(state.hasSelection)
        assertEquals(0, state.textFieldValue.selection.min)
        assertEquals(5, state.textFieldValue.selection.max)
    }

    // endregion

    // region BlockState:基础结构

    @Test
    fun blockStateStartsWithSingleEmptyTextBlock() {
        val state = BlockState()
        assertEquals(1, state.blocks.size)
        assertEquals(state.focusedBlockId, state.blocks.first().id)
        assertNotNull(state.focusedBlockId)
        assertTrue(state.focusedBlockId.isNotEmpty())
    }

    @Test
    fun blockStateInsertTableAboveInsertsBeforeExisting() {
        val state = BlockState()
        state.insertTable(2, 2)
        val original = state.blocks.first { it is TableBlock }

        val inserted = state.insertTableAbove(original.id, 3, 4)
        assertTrue(inserted)
        assertEquals(3, state.blocks.size)

        val newTable = state.blocks[0] as TableBlock
        assertEquals(3, newTable.rows.size)
        assertEquals(4, newTable.columnCount)
        assertEquals(original.id, state.blocks[1].id)
    }

    @Test
    fun blockStateInsertTableAboveReturnsFalseForUnknownId() {
        val state = BlockState()
        assertFalse(state.insertTableAbove("non-existent-id", 2, 2))
    }

    @Test
    fun blockStateInsertTableRowAppendsRow() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        assertEquals(2, table.rows.size)

        state.insertTableRow(table.id, afterRowIndex = 0, insertAbove = false)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(3, updated.rows.size)
        assertEquals(2, updated.columnCount)
    }

    @Test
    fun blockStateInsertTableColumnIncreasesColumnCount() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock

        state.insertTableColumn(table.id, afterColumnIndex = 0, insertLeft = true)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(3, updated.columnCount)
        assertTrue(updated.rows.all { it.cells.size == 3 })
    }

    @Test
    fun blockStateDeleteTableRowRemovesRow() {
        val state = BlockState()
        state.insertTable(3, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock

        state.deleteTableRow(table.id, 1)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(2, updated.rows.size)
    }

    @Test
    fun blockStateDeleteTableColumnDecreasesColumnCount() {
        val state = BlockState()
        state.insertTable(2, 3)
        val table = state.blocks.first { it is TableBlock } as TableBlock

        state.deleteTableColumn(table.id, 0)
        val updated = state.blocks.first { it.id == table.id } as TableBlock
        assertEquals(2, updated.columnCount)
    }

    @Test
    fun blockStateIsBlockJsonAcceptsWellFormedDocument() {
        val state = BlockState()
        state.insertTable(1, 1)
        val json = state.toJson()
        assertTrue(state.isBlockJson(json))
    }

    @Test
    fun blockStateIsBlockJsonRejectsInvalidInput() {
        val state = BlockState()
        assertFalse(state.isBlockJson(""))
        assertFalse(state.isBlockJson("not json"))
        assertFalse(state.isBlockJson("[]"))
    }

    @Test
    fun blockStatePasteJsonRestoresDocument() {
        val source = BlockState()
        source.insertTable(1, 1)
        val json = source.toJson()
        // 一次 insertTable 在默认空 TextBlock 上会替换该 TextBlock 为 Table,
        // 并在后面追加一个新 TextBlock,所以 source.blocks.size = 2
        assertTrue(source.blocks.size >= 1)
        assertTrue(source.blocks.any { it is TableBlock })

        val target = BlockState()
        assertTrue(target.pasteJson(json))
        assertTrue(target.blocks.any { it is TableBlock })
        assertEquals(source.blocks.size, target.blocks.size)
    }

    // endregion
}
