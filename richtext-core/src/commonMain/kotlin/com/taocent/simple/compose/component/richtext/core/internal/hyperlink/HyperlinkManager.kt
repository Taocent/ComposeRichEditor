package com.taocent.simple.compose.component.richtext.core.internal.hyperlink

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import com.taocent.simple.compose.component.richtext.core.LinkColor
import com.taocent.simple.compose.component.richtext.core.RichTextState

internal class HyperlinkManager(private val state: RichTextState) {

    fun insertHyperlink(displayText: String, url: String) {
        if (displayText.isEmpty()) return
        val selection = state.textFieldValue.selection
        val insertPos = selection.min
        val annotated = state.textFieldValue.annotatedString

        val builder = AnnotatedString.Builder()
        if (insertPos > 0) {
            builder.append(annotated.subSequence(0, insertPos))
        }

        val hyperlinkStart = builder.length
        builder.pushStyle(SpanStyle(color = LinkColor, textDecoration = TextDecoration.Underline))
        builder.pushStringAnnotation(RichTextState.HYPERLINK_TAG, url)
        builder.append(displayText)
        builder.pop()
        builder.pop()
        val hyperlinkEnd = builder.length

        if (selection.max < annotated.length) {
            builder.append(annotated.subSequence(selection.max, annotated.length))
        }

        val rawAnnotated = builder.toAnnotatedString()
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = annotated.text,
            newAnnotated = rawAnnotated,
            editStart = insertPos,
            removedCount = selection.max - insertPos,
            insertedText = displayText,
            fallbackTextAlign = state.currentTextAlign
        )

        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(hyperlinkEnd)
            )
        )
        state.syncStyleFromSelection()
    }

    fun isInsideHyperlink(position: Int): Boolean {
        val text = state.textFieldValue.annotatedString
        if (position < 0 || position >= text.length) return false
        val links = text.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        return links.any { position >= it.start && position < it.end }
    }

    fun getHyperlinkAtPosition(position: Int): TextRange? {
        val text = state.textFieldValue.annotatedString
        if (position < 0 || position >= text.length) return null
        val links = text.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        val matched = links.firstOrNull { position >= it.start && position < it.end }
        return matched?.let { TextRange(it.start, it.end) }
    }

    fun snapCursorOutOfHyperlinks(
        oldSelection: TextRange,
        newValue: TextFieldValue
    ): TextFieldValue {
        val newSelection = snapCursorSelection(newValue.annotatedString, oldSelection, newValue.selection)
        return if (newSelection != newValue.selection) newValue.copy(selection = newSelection) else newValue
    }

    fun snapCursorSelection(
        annotated: AnnotatedString,
        oldSelection: TextRange,
        newSelection: TextRange
    ): TextRange {
        val hyperlinks = annotated.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, annotated.length)
        if (hyperlinks.isEmpty()) return newSelection

        var selection = newSelection

        for (link in hyperlinks) {
            val linkStart = link.start
            val linkEnd = link.end

            if (selection.collapsed) {
                val cursorPos = selection.start
                if (cursorPos > linkStart && cursorPos < linkEnd) {
                    val oldCursorPos = oldSelection.start
                    selection = if (oldCursorPos <= linkStart) {
                        TextRange(linkEnd)
                    } else {
                        TextRange(linkStart)
                    }
                }
            } else {
                val anchor = selection.start
                val cursor = selection.end
                val selMin = minOf(anchor, cursor)
                val selMax = maxOf(anchor, cursor)
                if (selMin < linkEnd && selMax > linkStart &&
                    (selMin > linkStart || selMax < linkEnd)
                ) {
                    if (anchor > linkStart && anchor < linkEnd) {
                        val oldAnchor = oldSelection.start
                        selection = if (oldAnchor <= linkStart) {
                            TextRange(linkStart, linkEnd)
                        } else {
                            TextRange(linkEnd, linkStart)
                        }
                    } else if (anchor <= linkStart) {
                        selection = TextRange(anchor, linkEnd)
                    } else {
                        selection = TextRange(anchor, linkStart)
                    }
                }
            }
        }

        return selection
    }

    fun isSelectionFullyInHyperlink(): Boolean {
        val selection = state.textFieldValue.selection
        if (selection.collapsed) return false
        val annotated = state.textFieldValue.annotatedString
        val links = annotated.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, annotated.length)
        val rangeMin = selection.min
        val rangeMax = selection.max
        return links.any { it.start <= rangeMin && it.end >= rangeMax }
    }
}
