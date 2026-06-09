package com.taocent.simple.compose.component.richtext.core.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

object DocumentModelMapper {
    fun fromAnnotatedString(
        annotated: AnnotatedString,
        blockId: String = generateDocumentBlockId(),
        type: TextBlockType = TextBlockType.Paragraph
    ): DocumentModel {
        return DocumentModel(
            blocks = listOf(
                BlockModel.Text(
                    id = blockId,
                    type = type,
                    inlineContent = InlineModel(
                        text = annotated.text,
                        styleRuns = annotated.spanStyles.map { range ->
                            InlineStyleRun(
                                start = range.start,
                                end = range.end,
                                style = range.item.toInlineStyleModel()
                            )
                        },
                        annotations = annotated.getStringAnnotations(0, annotated.length).map { range ->
                            InlineAnnotation(
                                start = range.start,
                                end = range.end,
                                tag = range.tag,
                                value = range.item
                            )
                        }
                    ),
                    paragraphStyle = ParagraphModel(
                        textAlign = annotated.paragraphStyles.firstOrNull()?.item?.textAlign
                            ?.takeIf { it != TextAlign.Unspecified }
                            ?: TextAlign.Left
                    )
                )
            )
        )
    }

    fun toAnnotatedString(document: DocumentModel): AnnotatedString {
        val textBlock = document.blocks.firstOrNull() as? BlockModel.Text
            ?: return AnnotatedString("")
        return textBlock.toAnnotatedString()
    }

    fun BlockModel.Text.toAnnotatedString(): AnnotatedString {
        val builder = AnnotatedString.Builder(inlineContent.text)
        inlineContent.styleRuns.forEach { run ->
            val start = run.start.coerceIn(0, inlineContent.text.length)
            val end = run.end.coerceIn(start, inlineContent.text.length)
            if (start < end) {
                builder.addStyle(run.style.toSpanStyle(), start, end)
            }
        }
        if (inlineContent.text.isNotEmpty()) {
            builder.addStyle(paragraphStyle.toParagraphStyle(), 0, inlineContent.text.length)
        }
        inlineContent.annotations.forEach { annotation ->
            val start = annotation.start.coerceIn(0, inlineContent.text.length)
            val end = annotation.end.coerceIn(start, inlineContent.text.length)
            if (start < end) {
                builder.addStringAnnotation(annotation.tag, annotation.value, start, end)
            }
        }
        return builder.toAnnotatedString()
    }

    private fun SpanStyle.toInlineStyleModel(): InlineStyleModel {
        return InlineStyleModel(
            bold = fontWeight?.weight == FontWeight.Bold.weight || (fontWeight?.weight ?: 0) >= FontWeight.Bold.weight,
            italic = fontStyle == FontStyle.Italic,
            underline = textDecoration?.contains(TextDecoration.Underline) == true,
            lineThrough = textDecoration?.contains(TextDecoration.LineThrough) == true,
            color = color.takeIf { it != Color.Unspecified } ?: Color.Unspecified,
            background = background.takeIf { it != Color.Unspecified } ?: Color.Unspecified,
            fontSize = fontSize.takeIf { it != TextUnit.Unspecified } ?: TextUnit.Unspecified,
            baselineShift = baselineShift?.takeIf { it != BaselineShift.None }
        )
    }
}
