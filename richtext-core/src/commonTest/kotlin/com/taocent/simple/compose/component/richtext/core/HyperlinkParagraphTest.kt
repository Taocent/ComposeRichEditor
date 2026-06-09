package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals

class HyperlinkParagraphTest {

    @Test
    fun insertHyperlinkAtCursorKeepsParagraphStyleContinuous() {
        val state = RichTextState()
        val initial = AnnotatedString.Builder("1---左").apply {
            addStyle(ParagraphStyle(textAlign = TextAlign.Left), 0, 5)
        }.toAnnotatedString()
        state.restoreTextFieldValue(TextFieldValue(initial, TextRange(1)))

        state.insertHyperlink("超链接", "https://example.com")

        assertEquals("1超链接---左", state.plainText)
        assertEquals(TextRange(4), state.textFieldValue.selection)
        assertEquals(1, state.textFieldValue.annotatedString.paragraphStyles.size)
        val paragraph = state.textFieldValue.annotatedString.paragraphStyles.single()
        assertEquals(0, paragraph.start)
        assertEquals(state.plainText.length, paragraph.end)
        assertEquals(TextAlign.Left, paragraph.item.textAlign)
    }
}
