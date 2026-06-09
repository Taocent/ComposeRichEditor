package com.taocent.simple.compose.component.blockrichtext.internal.block

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.taocent.simple.compose.component.blockrichtext.BlockState
import com.taocent.simple.compose.component.blockrichtext.ExperimentalBlockRichTextApi
import com.taocent.simple.compose.component.blockrichtext.RichTextState
import com.taocent.simple.compose.component.blockrichtext.TableBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalBlockRichTextApi::class)
class EditorCursorTest {

    @Test
    fun initialCursorIsTextCursor() {
        val state = BlockState()
        val cursor = state.editorCursor

        assertIs<EditorCursor.Text>(cursor)
        assertEquals(state.focusedBlockId, cursor.blockId)
        assertEquals(0, cursor.selectionStart)
        assertEquals(0, cursor.selectionEnd)
    }

    @Test
    fun focusBlockUpdatesTextCursorAndClearsTableCellFocus() {
        val state = BlockState()
        val firstBlockId = state.focusedBlockId
        val textState = state.getBlockState(firstBlockId)
        textState.restoreTextFieldValue(
            TextFieldValue(
                text = "hello",
                selection = TextRange(2, 4)
            )
        )

        state.focusBlock(firstBlockId)
        val cursor = state.editorCursor

        assertIs<EditorCursor.Text>(cursor)
        assertEquals(firstBlockId, cursor.blockId)
        assertEquals(2, cursor.selectionStart)
        assertEquals(4, cursor.selectionEnd)
        assertNull(state.focusedTableCellState)
        assertNull(state.focusedTableCellFocusRequester)
    }

    @Test
    fun navSelectTableUpdatesBlockAnchorCursor() {
        val state = BlockState()
        val sourceBlockId = state.focusedBlockId
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock

        state.navSelectTable(table.id, sourceBlockId)
        val cursor = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursor)
        assertEquals(table.id, cursor.blockId)
        assertEquals(AnchorPosition.Before, cursor.position)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals(sourceBlockId, state.navSourceBlockId)
    }

    @Test
    fun navClearSelectionRestoresTextCursor() {
        val state = BlockState()
        val sourceBlockId = state.focusedBlockId
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        state.navSelectTable(table.id, sourceBlockId)

        state.navClearSelection()
        val cursor = state.editorCursor

        assertIs<EditorCursor.Text>(cursor)
        assertEquals(state.focusedBlockId, cursor.blockId)
        assertNull(state.navSelectedTableId)
    }

    @Test
    fun insertTableAboveUsesSameAnchorBehaviorAsCurrentTableLeftHighlight() {
        val state = BlockState()
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        val focusBeforeInsert = state.focusedBlockId

        val inserted = state.insertTableAbove(table.id, 3, 4)
        val cursor = state.editorCursor

        assertEquals(true, inserted)
        assertIs<EditorCursor.BlockAnchor>(cursor)
        assertEquals(table.id, cursor.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals(focusBeforeInsert, state.focusedBlockId)
        assertIs<TableBlock>(state.blocks[0])
        assertIs<TableBlock>(state.blocks[1])
        assertEquals(table.id, state.blocks[1].id)
    }

    @Test
    fun navDeleteFromAboveDeletesTextContentAndKeepsBlockAnchorCursor() {
        val state = BlockState()
        val sourceBlockId = state.focusedBlockId
        val sourceState = state.getBlockState(sourceBlockId)
        sourceState.restoreTextFieldValue(
            TextFieldValue(
                text = "ab",
                selection = TextRange(2)
            )
        )
        state.updateBlockContent(sourceBlockId)
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        state.navSelectTable(table.id, sourceBlockId)

        state.navDeleteFromAbove()
        val cursorAfterFirstDelete = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursorAfterFirstDelete)
        assertEquals(table.id, cursorAfterFirstDelete.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals("a", sourceState.textFieldValue.text)

        state.navDeleteFromAbove()
        val cursorAfterSecondDelete = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursorAfterSecondDelete)
        assertEquals(table.id, cursorAfterSecondDelete.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals("", sourceState.textFieldValue.text)
    }

    @Test
    fun navDeleteFromAboveProtectsTrailingHyperlinkAndKeepsBlockAnchorCursor() {
        val state = BlockState()
        val sourceBlockId = state.focusedBlockId
        val sourceState = state.getBlockState(sourceBlockId)
        val content = AnnotatedString.Builder().apply {
            append("before link")
            addStringAnnotation(RichTextState.HYPERLINK_TAG, "https://example.com", 7, 11)
        }.toAnnotatedString()
        sourceState.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = content,
                selection = TextRange(content.length)
            )
        )
        state.updateBlockContent(sourceBlockId)
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        state.navSelectTable(table.id, sourceBlockId)

        state.navDeleteFromAbove()
        val cursor = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursor)
        assertEquals(table.id, cursor.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals("before link", sourceState.textFieldValue.text)
        assertEquals(TextRange(7, 11), sourceState.textFieldValue.selection)

        state.navDeleteFromAbove()
        val cursorAfterSecondDelete = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursorAfterSecondDelete)
        assertEquals(table.id, cursorAfterSecondDelete.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals("before ", sourceState.textFieldValue.text)
        assertEquals(TextRange(7), sourceState.textFieldValue.selection)
    }

    @Test
    fun navDeleteFromAboveRemovesEmptyTextBlockAndKeepsBlockAnchorCursor() {
        val state = BlockState()
        val sourceBlockId = state.focusedBlockId
        state.insertTable(2, 2)
        val table = state.blocks.first { it is TableBlock } as TableBlock
        state.navSelectTable(table.id, sourceBlockId)

        state.navDeleteFromAbove()
        val cursor = state.editorCursor

        assertIs<EditorCursor.BlockAnchor>(cursor)
        assertEquals(table.id, cursor.blockId)
        assertEquals(table.id, state.navSelectedTableId)
        assertEquals(table.id, state.blocks.first().id)
    }
}
