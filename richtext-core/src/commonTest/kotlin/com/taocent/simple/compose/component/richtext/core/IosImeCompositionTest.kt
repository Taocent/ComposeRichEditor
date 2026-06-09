package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosImeCompositionTest {

    @Test
    fun iosCompositionCommitKeepsCommittedCandidateText() {
        val state = RichTextState()

        state.onValueChange(
            TextFieldValue(
                annotatedString = AnnotatedString("shishang"),
                selection = TextRange(8),
                composition = TextRange(0, 8)
            ),
            supportsImeComposition = false
        )

        state.onValueChange(
            TextFieldValue(
                annotatedString = AnnotatedString("时尚"),
                selection = TextRange(2),
                composition = null
            ),
            supportsImeComposition = false
        )

        assertEquals("时尚", state.plainText)
        assertEquals(TextRange(2), state.textFieldValue.selection)
        assertNull(state.textFieldValue.composition)
    }
}
