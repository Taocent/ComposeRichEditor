package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object BoundaryNewlineVisualTransformation : VisualTransformation {
    private val zeroWidthSpace = '\u200B'

    override fun filter(text: AnnotatedString): TransformedText {
        val replacedIndexes = text.boundaryNewlineIndexes()
        if (replacedIndexes.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val replacedSet = replacedIndexes.toSet()
        val builder = AnnotatedString.Builder(text.length)
        for (index in text.text.indices) {
            builder.append(if (index in replacedSet) zeroWidthSpace else text.text[index])
        }
        for (span in text.spanStyles) {
            builder.addStyle(span.item, span.start, span.end)
        }
        for (paragraph in text.paragraphStyles) {
            builder.addStyle(paragraph.item, paragraph.start, paragraph.end)
        }
        for (annotation in text.getStringAnnotations(0, text.length)) {
            builder.addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun AnnotatedString.boundaryNewlineIndexes(): List<Int> {
        if (text.isEmpty()) return emptyList()
        return paragraphStyles
            .sortedWith(compareBy<AnnotatedString.Range<androidx.compose.ui.text.ParagraphStyle>> { it.start }.thenBy { it.end })
            .zipWithNext()
            .mapNotNull { (left, right) ->
                val boundary = right.start
                val newlineIndex = boundary - 1
                if (left.end == boundary && newlineIndex in text.indices && text[newlineIndex] == '\n' && left.item != right.item) {
                    newlineIndex
                } else {
                    null
                }
            }
            .distinct()
    }
}
