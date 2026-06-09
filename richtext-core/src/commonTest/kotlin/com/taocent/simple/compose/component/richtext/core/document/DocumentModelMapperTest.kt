package com.taocent.simple.compose.component.richtext.core.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.taocent.simple.compose.component.richtext.core.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentModelMapperTest {

    @Test
    fun annotatedStringCanMapToTextDocumentModel() {
        val annotated = AnnotatedString.Builder("hello").apply {
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    textDecoration = TextDecoration.Underline,
                    color = Color.Red,
                    fontSize = 20.sp
                ),
                0,
                5
            )
            addStyle(ParagraphStyle(textAlign = TextAlign.Center), 0, 5)
            addStringAnnotation("url", "https://example.com", 0, 5)
        }.toAnnotatedString()

        val document = DocumentModelMapper.fromAnnotatedString(annotated, blockId = "block_1")
        val block = assertIs<BlockModel.Text>(document.blocks.single())

        assertEquals("block_1", block.id)
        assertEquals(TextBlockType.Paragraph, block.type)
        assertEquals("hello", block.inlineContent.text)
        assertEquals(TextAlign.Center, block.paragraphStyle.textAlign)
        assertEquals(1, block.inlineContent.styleRuns.size)
        assertTrue(block.inlineContent.styleRuns.single().style.bold)
        assertTrue(block.inlineContent.styleRuns.single().style.italic)
        assertTrue(block.inlineContent.styleRuns.single().style.underline)
        assertEquals("url", block.inlineContent.annotations.single().tag)
    }

    @Test
    fun documentModelCanRenderToAnnotatedString() {
        val document = DocumentModel(
            blocks = listOf(
                BlockModel.Text(
                    id = "block_1",
                    inlineContent = InlineModel(
                        text = "hello",
                        styleRuns = listOf(
                            InlineStyleRun(
                                start = 0,
                                end = 5,
                                style = InlineStyleModel(
                                    bold = true,
                                    lineThrough = true,
                                    background = Color.Yellow
                                )
                            )
                        ),
                        annotations = listOf(
                            InlineAnnotation(0, 5, "mention", "user_1")
                        )
                    ),
                    paragraphStyle = ParagraphModel(TextAlign.Right)
                )
            )
        )

        val annotated = DocumentModelMapper.toAnnotatedString(document)

        assertEquals("hello", annotated.text)
        assertEquals(FontWeight.Bold, annotated.spanStyles.single().item.fontWeight)
        assertTrue(annotated.spanStyles.single().item.textDecoration?.contains(TextDecoration.LineThrough) == true)
        assertEquals(TextAlign.Right, annotated.paragraphStyles.single().item.textAlign)
        assertEquals("user_1", annotated.getStringAnnotations("mention", 0, 5).single().item)
    }

    @Test
    fun richTextStateCanExposeAndRestoreDocumentModel() {
        val state = RichTextState()
        val annotated = AnnotatedString.Builder("hello").apply {
            addStyle(ParagraphStyle(textAlign = TextAlign.Center), 0, 5)
        }.toAnnotatedString()
        state.restoreTextFieldValue(TextFieldValue(annotated, TextRange(5)))

        val document = state.documentModel
        val block = assertIs<BlockModel.Text>(document.blocks.single())
        assertEquals(TextAlign.Center, block.paragraphStyle.textAlign)

        val restored = RichTextState()
        restored.loadFromDocumentModel(document)

        assertEquals("hello", restored.plainText)
        assertEquals(TextAlign.Center, restored.textFieldValue.annotatedString.paragraphStyles.single().item.textAlign)
    }
}
