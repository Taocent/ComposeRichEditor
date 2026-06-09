package com.taocent.simple.compose.component.richtext.core.internal.paragraph

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ParagraphModelManagerTest {

    @Test
    fun centeredParagraphEnterCreatesCenteredTrailingParagraph() {
        val manager = ParagraphModelManager(AnnotatedString("1"))
        val centered = manager.setTextAlign(AnnotatedString("1"), TextRange(1), TextAlign.Center)

        manager.syncAfterTextChange(
            oldText = "1",
            newAnnotated = AnnotatedString("1\n"),
            editStart = 1,
            removedCount = 0,
            insertedText = "\n",
            fallbackTextAlign = TextAlign.Center
        )

        assertEquals(TextAlign.Center, centered.paragraphStyles.single().item.textAlign)
        assertEquals(listOf(TextAlign.Center, TextAlign.Center), manager.models.map { it.textAlign })
    }

    @Test
    fun leftParagraphEnterDoesNotGenerateEmptyParagraphStyleRange() {
        val manager = ParagraphModelManager(AnnotatedString("1"))

        val annotated = manager.syncAfterTextChange(
            oldText = "1",
            newAnnotated = AnnotatedString("1\n"),
            editStart = 1,
            removedCount = 0,
            insertedText = "\n",
            fallbackTextAlign = TextAlign.Left
        )

        assertEquals(listOf(TextAlign.Left, TextAlign.Left), manager.models.map { it.textAlign })
        assertFalse(annotated.paragraphStyles.any { it.start == it.end && annotated.text.isNotEmpty() })
    }

    @Test
    fun middleEmptyParagraphCanHaveIndependentAlign() {
        val manager = ParagraphModelManager(AnnotatedString("1\n\n"))

        val annotated = manager.setTextAlign(
            annotated = AnnotatedString("1\n\n"),
            selection = TextRange(2),
            align = TextAlign.Right
        )

        assertEquals(listOf(TextAlign.Left, TextAlign.Right, TextAlign.Left), manager.models.map { it.textAlign })
        val middle = annotated.paragraphStyles.single { it.start == 2 && it.end == 3 }
        assertEquals(TextAlign.Right, middle.item.textAlign)
    }

    @Test
    fun deletingLastCharacterKeepsTrailingParagraphAlign() {
        val manager = ParagraphModelManager(
            AnnotatedString.Builder("1\nx").apply {
                addStyle(ParagraphStyle(textAlign = TextAlign.Left), 0, 2)
                addStyle(ParagraphStyle(textAlign = TextAlign.Center), 2, 3)
            }.toAnnotatedString()
        )

        manager.syncAfterTextChange(
            oldText = "1\nx",
            newAnnotated = AnnotatedString("1\n"),
            editStart = 2,
            removedCount = 1,
            insertedText = ""
        )

        assertEquals(listOf(TextAlign.Left, TextAlign.Center), manager.models.map { it.textAlign })
    }

    @Test
    fun multiParagraphSelectionUpdatesOnlySelectedParagraphs() {
        val manager = ParagraphModelManager(AnnotatedString("1\n2\n3"))

        manager.setTextAlign(
            annotated = AnnotatedString("1\n2\n3"),
            selection = TextRange(2, 4),
            align = TextAlign.Right
        )

        assertEquals(listOf(TextAlign.Left, TextAlign.Right, TextAlign.Left), manager.models.map { it.textAlign })
    }

    @Test
    fun ordinaryInsertKeepsExistingParagraphModels() {
        val manager = ParagraphModelManager(AnnotatedString("hello"))
        manager.setTextAlign(AnnotatedString("hello"), TextRange(0), TextAlign.Center)

        val annotated = manager.syncAfterTextChange(
            oldText = "hello",
            newAnnotated = AnnotatedString("heXllo"),
            editStart = 2,
            removedCount = 0,
            insertedText = "X"
        )

        assertEquals(listOf(TextAlign.Center), manager.models.map { it.textAlign })
        assertEquals(TextAlign.Center, annotated.paragraphStyles.single().item.textAlign)
        assertEquals(0, annotated.paragraphStyles.single().start)
        assertEquals(6, annotated.paragraphStyles.single().end)
    }

    @Test
    fun ordinaryDeleteKeepsExistingParagraphModels() {
        val manager = ParagraphModelManager(
            AnnotatedString.Builder("one\ntwo").apply {
                addStyle(ParagraphStyle(textAlign = TextAlign.Left), 0, 4)
                addStyle(ParagraphStyle(textAlign = TextAlign.Right), 4, 7)
            }.toAnnotatedString()
        )

        val annotated = manager.syncAfterTextChange(
            oldText = "one\ntwo",
            newAnnotated = AnnotatedString("one\nto"),
            editStart = 5,
            removedCount = 1,
            insertedText = ""
        )

        assertEquals(listOf(TextAlign.Left, TextAlign.Right), manager.models.map { it.textAlign })
        assertEquals(TextAlign.Left, annotated.paragraphStyles[0].item.textAlign)
        assertEquals(TextAlign.Right, annotated.paragraphStyles[1].item.textAlign)
    }
}
