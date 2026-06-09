package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RichTextEmojiTest {

    @Test
    fun customEmojiInsertedAfterHyperlinkDoesNotInheritUnderline() {
        val state = RichTextState()

        state.insertHyperlink("link", "https://example.com")
        state.insertCustomEmoji("heart", 16.sp)

        val annotated = state.textFieldValue.annotatedString
        val emoji = annotated.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, annotated.length).single()
        val emojiStyles = annotated.spanStyles.filter { it.start < emoji.end && it.end > emoji.start }

        assertTrue(emojiStyles.isNotEmpty())
        assertFalse(emojiStyles.any { it.item.textDecoration?.contains(TextDecoration.Underline) == true })
    }
}
