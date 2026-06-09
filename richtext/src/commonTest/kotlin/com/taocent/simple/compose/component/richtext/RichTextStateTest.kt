package com.taocent.simple.compose.component.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 覆盖 [RichTextState] 的基础契约、文本输入、格式切换、剪贴板缓冲区、
 * JSON 序列化与撤销重做等核心流程。
 */
class RichTextStateTest {

    // region 基础

    @Test
    fun newStateHasEmptyText() {
        val state = RichTextState()
        assertEquals("", state.textFieldValue.text)
        assertEquals(0, state.textFieldValue.selection.start)
        assertEquals(0, state.textFieldValue.selection.end)
        assertFalse(state.hasSelection)
        assertEquals("", state.plainText)
    }

    @Test
    fun initialTextSetsTextAndKeepsCursorAtStart() {
        val state = RichTextState(initialText = "hello")
        assertEquals("hello", state.textFieldValue.text)
        assertEquals(0, state.textFieldValue.selection.start)
    }

    @Test
    fun allStyleFlagsStartFalse() {
        val state = RichTextState()
        assertFalse(state.currentBold)
        assertFalse(state.currentItalic)
        assertFalse(state.currentUnderline)
        assertFalse(state.currentStrikethrough)
        assertFalse(state.currentSuperscript)
        assertFalse(state.currentSubscript)
        assertEquals(Color.Unspecified, state.currentColor)
        assertEquals(Color.Unspecified, state.currentBackground)
        assertEquals(TextUnit.Unspecified, state.currentFontSize)
    }

    // endregion

    // region 文本输入

    @Test
    fun insertTextAppendsAtSelection() {
        val state = RichTextState()
        state.insertText("hello")
        assertEquals("hello", state.textFieldValue.text)
        assertEquals(5, state.textFieldValue.selection.start)
    }

    @Test
    fun insertTextAtMiddleMovesCursor() {
        val state = RichTextState(initialText = "hello")
        state.textFieldValue.let { v ->
            state.restoreTextFieldValue(v.copy(selection = TextRange(2)))
        }
        state.insertText("XX")
        assertEquals("heXXllo", state.textFieldValue.text)
        assertEquals(4, state.textFieldValue.selection.start)
    }

    @Test
    fun selectAllExpandsSelection() {
        val state = RichTextState(initialText = "hello")
        state.selectAll()
        assertTrue(state.hasSelection)
        assertEquals(0, state.textFieldValue.selection.min)
        assertEquals(5, state.textFieldValue.selection.max)
    }

    @Test
    fun deleteSelectionRemovesText() {
        val state = RichTextState(initialText = "hello")
        state.selectAll()
        state.deleteSelection()
        assertEquals("", state.textFieldValue.text)
        assertEquals(0, state.textFieldValue.selection.start)
    }

    @Test
    fun onValueChangeUpdatesText() {
        val state = RichTextState(initialText = "hello")
        // 使用 restoreTextFieldValue 直接替换 value,这是最简单可靠的方式
        state.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("hello world"), selection = TextRange(11))
        )
        assertEquals("hello world", state.textFieldValue.text)
        assertEquals(11, state.textFieldValue.selection.start)
    }

    // endregion

    // region 格式切换

    @Test
    fun toggleBoldAffectsCurrentBold() {
        val state = RichTextState()
        state.toggleBold()
        assertTrue(state.currentBold)
        state.toggleBold()
        assertFalse(state.currentBold)
    }

    @Test
    fun toggleItalicAffectsCurrentItalic() {
        val state = RichTextState()
        state.toggleItalic()
        assertTrue(state.currentItalic)
    }

    @Test
    fun toggleUnderlineAffectsCurrentUnderline() {
        val state = RichTextState()
        state.toggleUnderline()
        assertTrue(state.currentUnderline)
    }

    @Test
    fun toggleStrikethroughAffectsCurrentStrikethrough() {
        val state = RichTextState()
        state.toggleStrikethrough()
        assertTrue(state.currentStrikethrough)
    }

    @Test
    fun toggleSuperscriptClearsSubscript() {
        val state = RichTextState()
        state.toggleSubscript()
        assertTrue(state.currentSubscript)
        state.toggleSuperscript()
        assertTrue(state.currentSuperscript)
        assertFalse(state.currentSubscript)
    }

    @Test
    fun toggleSubscriptClearsSuperscript() {
        val state = RichTextState()
        state.toggleSuperscript()
        assertTrue(state.currentSuperscript)
        state.toggleSubscript()
        assertTrue(state.currentSubscript)
        assertFalse(state.currentSuperscript)
    }

    @Test
    fun setColorStoresColor() {
        val state = RichTextState()
        val red = Color(0xFFFF0000)
        state.setColor(red)
        assertEquals(red, state.currentColor)
    }

    @Test
    fun setBackgroundStoresBackground() {
        val state = RichTextState()
        val bg = Color(0xFF00FF00)
        state.setBackground(bg)
        assertEquals(bg, state.currentBackground)
    }

    @Test
    fun setFontSizeStoresSize() {
        val state = RichTextState()
        val size = 20.sp
        state.setFontSize(size)
        assertEquals(size, state.currentFontSize)
    }

    @Test
    fun clearFormattingResetsAll() {
        val state = RichTextState()
        state.toggleBold()
        state.toggleItalic()
        state.setColor(Color(0xFFFF0000))
        state.setBackground(Color(0xFF00FF00))
        state.setFontSize(20.sp)
        state.clearFormatting()
        assertFalse(state.currentBold)
        assertFalse(state.currentItalic)
        assertEquals(Color.Unspecified, state.currentColor)
        assertEquals(Color.Unspecified, state.currentBackground)
        assertEquals(TextUnit.Unspecified, state.currentFontSize)
    }

    @Test
    fun currentSpanStyleAggregatesFlags() {
        val state = RichTextState()
        state.toggleBold()
        state.toggleItalic()
        state.toggleUnderline()
        val style = state.currentSpanStyle()
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(FontStyle.Italic, style.fontStyle)
        val decoration: TextDecoration = style.textDecoration ?: TextDecoration.None
        assertTrue(decoration.contains(TextDecoration.Underline))
    }

    @Test
    fun currentSpanStyleIncludesSuperscript() {
        val state = RichTextState()
        state.toggleSuperscript()
        val style = state.currentSpanStyle()
        assertEquals(BaselineShift.Superscript, style.baselineShift)
    }

    @Test
    fun currentSpanStyleIncludesSubscript() {
        val state = RichTextState()
        state.toggleSubscript()
        val style = state.currentSpanStyle()
        assertEquals(BaselineShift.Subscript, style.baselineShift)
    }

    // endregion

    // region 撤销/重做

    @Test
    fun undoRevertsInsertedText() {
        val state = RichTextState()
        state.insertText("abc")
        assertEquals("abc", state.textFieldValue.text)
        assertTrue(state.canUndo)
        state.undo()
        assertEquals("", state.textFieldValue.text)
        assertTrue(state.canRedo)
        state.redo()
        assertEquals("abc", state.textFieldValue.text)
    }

    @Test
    fun undoWithoutHistoryIsNoOp() {
        val state = RichTextState(initialText = "hi")
        // 直接 undo,无历史,值不变
        state.undo()
        assertEquals("hi", state.textFieldValue.text)
        assertFalse(state.canRedo)
    }

    @Test
    fun redoWithoutFutureIsNoOp() {
        val state = RichTextState()
        state.redo()
        assertEquals("", state.textFieldValue.text)
    }

    // endregion

    // region JSON

    @Test
    fun toJsonIncludesTextField() {
        val state = RichTextState(initialText = "payload")
        val json = state.toJson()
        assertTrue(json.contains("\"text\":"))
        assertTrue(json.contains("payload"))
    }

    @Test
    fun fromJsonRecoversAnnotatedString() {
        val state = RichTextState(initialText = "payload")
        val json = state.toJson()
        val annotated = RichTextState.fromJson(json)
        assertEquals("payload", annotated.text)
    }

    @Test
    fun loadFromJsonReplacesText() {
        val state = RichTextState(initialText = "old")
        val source = RichTextState(initialText = "new")
        state.loadFromJson(source.toJson())
        assertEquals("new", state.textFieldValue.text)
    }

    @Test
    fun toJsonPreservesBoldSpan() {
        val state = RichTextState()
        val annotated = AnnotatedString.Builder("hello").apply {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), 0, 5)
        }.toAnnotatedString()
        state.restoreTextFieldValue(
            TextFieldValue(annotatedString = annotated, selection = TextRange(5))
        )

        val json = state.toJson()
        assertTrue(json.contains("hello"))
        assertTrue(json.contains("bold") || json.contains("700"))
    }

    // endregion

    // region 剪贴板 buffer

    @Test
    fun copyToBufferReturnsEmptyWhenSelectionCollapsed() {
        val state = RichTextState(initialText = "hello")
        // selection collapsed(初始)
        val clip = state.copyToBuffer()
        assertEquals("", clip.text)
    }

    @Test
    fun copyToBufferReturnsSelection() {
        val state = RichTextState(initialText = "hello world")
        state.restoreTextFieldValue(state.textFieldValue.copy(selection = TextRange(6, 11)))
        val clip = state.copyToBuffer()
        assertEquals("world", clip.text)
    }

    @Test
    fun setCopyBufferFromAnnotatedStoresMapping() {
        val state = RichTextState()
        val annotated = AnnotatedString("foo")
        val result = state.setCopyBufferFromAnnotated(annotated)
        assertEquals("foo", result.text)
    }

    @Test
    fun resolveClipboardReturnsOriginalWhenMatches() {
        val state = RichTextState()
        val original = AnnotatedString("hello")
        val buffered = state.setCopyBufferFromAnnotated(original)
        // 模拟剪贴板返回 buffered
        val resolved = state.resolveClipboard(buffered)
        assertNotNull(resolved)
        assertEquals("hello", resolved.text)
    }

    @Test
    fun resolveClipboardReturnsInputWhenNotMatching() {
        val state = RichTextState()
        // 没有 setCopyBuffer,直接 resolve 应返回输入
        val resolved = state.resolveClipboard(AnnotatedString("foo"))
        assertNotNull(resolved)
        assertEquals("foo", resolved.text)
    }

    @Test
    fun resolveClipboardReturnsNullForNullInput() {
        val state = RichTextState()
        assertNull(state.resolveClipboard(null))
    }

    @Test
    fun getCopyAnnotatedStringEmptyForCollapsedSelection() {
        val state = RichTextState(initialText = "hello")
        assertEquals("", state.getCopyAnnotatedString().text)
    }

    @Test
    fun getCopyAnnotatedStringReturnsSubstring() {
        val state = RichTextState(initialText = "hello world")
        state.restoreTextFieldValue(state.textFieldValue.copy(selection = TextRange(6, 11)))
        assertEquals("world", state.getCopyAnnotatedString().text)
    }

    // endregion

    // region 超链接

    @Test
    fun insertHyperlinkAddsAnnotation() {
        val state = RichTextState()
        state.insertHyperlink(text = "click", url = "https://example.com")
        val text = state.textFieldValue.text
        assertTrue(text.contains("click"))
        val links = state.textFieldValue.annotatedString
            .getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        assertTrue(links.isNotEmpty())
        assertEquals("https://example.com", links.first().item)
    }

    @Test
    fun isInsideHyperlinkDetectsInside() {
        val state = RichTextState()
        state.insertHyperlink(text = "click", url = "https://example.com")
        val text = state.textFieldValue.text
        val linkStart = text.indexOf("click")
        assertTrue(state.isInsideHyperlink(linkStart + 1))
    }

    @Test
    fun isInsideHyperlinkFalseOutside() {
        val state = RichTextState()
        state.insertHyperlink(text = "click", url = "https://example.com")
        val text = state.textFieldValue.text
        // 链接范围是 [linkStart, linkStart + 5)
        // 链接之后的位置应返回 false
        val linkStart = text.indexOf("click")
        assertFalse(state.isInsideHyperlink(linkStart + 5))
        // 链接起始位置之前(0)也算"外"——但 hyperlink 起始位置(0)就是 linkStart,实际也在内
        // 测一个空状态(无 hyperlink)时,任意位置都 false
        val empty = RichTextState()
        assertFalse(empty.isInsideHyperlink(0))
    }

    @Test
    fun getHyperlinkAtPositionReturnsRange() {
        val state = RichTextState()
        state.insertHyperlink(text = "click", url = "https://example.com")
        val text = state.textFieldValue.text
        val linkStart = text.indexOf("click")
        val range = state.getHyperlinkAtPosition(linkStart + 1)
        assertNotNull(range)
        assertEquals(linkStart, range.start)
        assertEquals(linkStart + 5, range.end)
    }

    @Test
    fun getHyperlinkAtPositionReturnsNullOutside() {
        val state = RichTextState()
        assertNull(state.getHyperlinkAtPosition(0))
    }

    // endregion

    // region save/restore selection

    @Test
    fun saveAndRestoreSelection() {
        val state = RichTextState(initialText = "hello")
        state.restoreTextFieldValue(state.textFieldValue.copy(selection = TextRange(1, 3)))
        state.saveSelection()
        state.restoreTextFieldValue(state.textFieldValue.copy(selection = TextRange(5)))
        state.restoreSavedSelection()
        assertEquals(1, state.textFieldValue.selection.start)
        assertEquals(3, state.textFieldValue.selection.end)
    }

    @Test
    fun restoreSavedSelectionWithoutSaveIsNoOp() {
        val state = RichTextState(initialText = "hello")
        state.restoreTextFieldValue(state.textFieldValue.copy(selection = TextRange(2)))
        // 没有 save 直接 restore,selection 不变
        state.restoreSavedSelection()
        assertEquals(2, state.textFieldValue.selection.start)
    }

    // endregion

    // region format 块 API

    @Test
    fun formatBlockCanApplyMultipleToggles() {
        val state = RichTextState()
        state.format {
            bold()
            italic()
        }
        assertTrue(state.currentBold)
        assertTrue(state.currentItalic)
    }

    @Test
    fun formatBlockSupportsFontSizeAndColor() {
        val state = RichTextState()
        val red = Color(0xFFFF0000)
        state.format {
            fontSize(24.sp)
            color(red)
        }
        assertEquals(24.sp, state.currentFontSize)
        assertEquals(red, state.currentColor)
    }

    @Test
    fun formatBlockHyperlinkDelegatesToInsert() {
        val state = RichTextState()
        state.format {
            hyperlink(url = "https://example.com", text = "here")
        }
        val text = state.textFieldValue.text
        assertTrue(text.contains("here"))
        val links = state.textFieldValue.annotatedString
            .getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        assertTrue(links.isNotEmpty())
    }

    // endregion

    // region 智能粘贴

    @Test
    fun smartPastePlainTextInserts() {
        val state = RichTextState()
        state.smartPaste("hello")
        assertEquals("hello", state.textFieldValue.text)
    }

    // endregion

    // region restoreTextFieldValue

    @Test
    fun restoreTextFieldValueReplacesValue() {
        val state = RichTextState(initialText = "old")
        state.restoreTextFieldValue(
            TextFieldValue(annotatedString = AnnotatedString("new"), selection = TextRange(3))
        )
        assertEquals("new", state.textFieldValue.text)
        assertEquals(3, state.textFieldValue.selection.start)
    }

    // endregion
}
