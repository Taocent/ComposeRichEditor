package com.taocent.simple.compose.component.richtext.core.internal.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_PLACEHOLDER
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG

internal class TextEditor(private val state: RichTextState) {

    private var preCompositionValue: TextFieldValue? = null

    fun onValueChange(newValue: TextFieldValue, supportsImeComposition: Boolean = true) {
        val oldValue = state.textFieldValue

        if (!supportsImeComposition && newValue.composition != null) {
            preCompositionValue = null
            state.updateTextFieldValue(newValue)
            return
        }

        if (!supportsImeComposition && oldValue.composition != null) {
            preCompositionValue = null
            state.justToggledStyle = false
            state.updateTextFieldValue(newValue.copy(composition = null))
            state.syncParagraphModelsAfterTextChange(
                oldText = oldValue.annotatedString.text,
                newAnnotated = newValue.annotatedString,
                editStart = oldValue.composition?.start ?: newValue.selection.min,
                removedCount = oldValue.composition?.let { it.end - it.start } ?: 0,
                insertedText = newValue.annotatedString.text,
                fallbackTextAlign = state.currentTextAlign
            )
            state.syncStyleFromSelection()
            return
        }

        if (newValue.composition != null) {
            handleCompositionInput(oldValue, newValue)
            return
        }

        if (oldValue.composition != null) {
            handleCompositionCommit(oldValue, newValue)
            return
        }

        handleNormalInput(oldValue, newValue)
    }

    fun insertText(text: String) {
        val selection = state.textFieldValue.selection
        val start = selection.min
        val end = selection.max
        val annotated = state.textFieldValue.annotatedString
        val builder = AnnotatedString.Builder()
        if (start > 0) builder.append(annotated.subSequence(0, start))
        builder.pushStyle(state.currentSpanStyle())
        builder.append(text)
        builder.pop()
        if (end < annotated.length) builder.append(annotated.subSequence(end, annotated.length))
        val rawAnnotated = builder.toAnnotatedString()
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = annotated.text,
            newAnnotated = rawAnnotated,
            editStart = start,
            removedCount = end - start,
            insertedText = text,
            fallbackTextAlign = state.currentTextAlign
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(start + text.length)
            )
        )
        state.syncStyleFromSelection()
    }

    fun deleteSelection() {
        val selection = state.textFieldValue.selection
        if (selection.collapsed) return
        val start = selection.min
        val end = selection.max
        val annotated = state.textFieldValue.annotatedString
        val builder = AnnotatedString.Builder()
        if (start > 0) builder.append(annotated.subSequence(0, start))
        if (end < annotated.length) builder.append(annotated.subSequence(end, annotated.length))
        val rawAnnotated = builder.toAnnotatedString()
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = annotated.text,
            newAnnotated = rawAnnotated,
            editStart = start,
            removedCount = end - start,
            insertedText = ""
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(start)
            )
        )
        state.syncStyleFromSelection()
    }

    fun insertAnnotatedString(annotated: AnnotatedString) {
        val selection = state.textFieldValue.selection
        val start = selection.min
        val end = selection.max
        val current = state.textFieldValue.annotatedString
        val builder = AnnotatedString.Builder()
        if (start > 0) builder.append(current.subSequence(0, start))
        builder.append(annotated)
        if (end < current.length) builder.append(current.subSequence(end, current.length))
        val rawAnnotated = builder.toAnnotatedString()
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = current.text,
            newAnnotated = rawAnnotated,
            editStart = start,
            removedCount = end - start,
            insertedText = annotated.text,
            fallbackTextAlign = state.currentTextAlign
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(start + annotated.length)
            )
        )
        state.syncStyleFromSelection()
    }

    fun insertTextWithEmojis(text: String, validEmojiIds: Set<String>, displaySize: TextUnit) {
        val pattern = Regex("\\[([a-zA-Z0-9_]+)]")
        val matches = pattern.findAll(text).toList()
        val validMatches = matches.filter { it.groupValues[1] in validEmojiIds }
        if (validMatches.isEmpty()) {
            insertText(text)
            return
        }
        val selection = state.textFieldValue.selection
        val start = selection.min
        val end = selection.max
        val current = state.textFieldValue.annotatedString
        val builder = AnnotatedString.Builder()
        if (start > 0) builder.append(current.subSequence(0, start))
        val baseStyle = state.currentSpanStyle()
        var lastEnd = 0
        for (match in validMatches) {
            if (match.range.first > lastEnd) {
                builder.append(text.substring(lastEnd, match.range.first))
            }
            val emojiId = match.groupValues[1]
            val emojiStart = builder.length
            val emojiStyle = baseStyle.copy(
                fontSize = displaySize,
                color = Color.Transparent
            )
            builder.pushStyle(emojiStyle)
            builder.append(CUSTOM_EMOJI_PLACEHOLDER)
            builder.pop()
            builder.addStringAnnotation(CUSTOM_EMOJI_TAG, emojiId, emojiStart, builder.length)
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) {
            builder.append(text.substring(lastEnd))
        }
        if (end < current.length) builder.append(current.subSequence(end, current.length))
        val rawAnnotated = builder.toAnnotatedString()
        val insertedText = rawAnnotated.text.substring(start, rawAnnotated.length - (current.length - end))
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = current.text,
            newAnnotated = rawAnnotated,
            editStart = start,
            removedCount = end - start,
            insertedText = insertedText,
            fallbackTextAlign = state.currentTextAlign
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(start + newAnnotated.length - (if (start > 0) start else 0) - (current.length - end))
            )
        )
        state.syncStyleFromSelection()
    }

    fun insertCustomEmoji(emojiId: String, displaySize: TextUnit) {
        val selection = state.textFieldValue.selection
        val start = selection.min
        val end = selection.max
        val current = state.textFieldValue.annotatedString
        val builder = AnnotatedString.Builder()
        if (start > 0) builder.append(current.subSequence(0, start))
        val emojiStart = builder.length
        val baseStyle = if (state.justToggledStyle) {
            state.currentSpanStyle()
        } else {
            val isAfterHyperlink = start > 0 && current
                .getStringAnnotations(RichTextState.HYPERLINK_TAG, start - 1, start)
                .isNotEmpty()
            val leftStyle = if (!isAfterHyperlink && start > 0) {
                current.spanStyles
                    .lastOrNull { it.start <= start - 1 && it.end > start - 1 }
                    ?.item
            } else null
            leftStyle ?: state.currentSpanStyle()
        }
        val emojiStyle = baseStyle.copy(
            fontSize = if (state.justToggledStyle) {
                displaySize
            } else {
                baseStyle.fontSize.takeIf { it != TextUnit.Unspecified } ?: displaySize
            },
            color = Color.Transparent
        )
        builder.pushStyle(emojiStyle)
        builder.append(CUSTOM_EMOJI_PLACEHOLDER)
        builder.pop()
        val emojiEnd = builder.length
        builder.addStringAnnotation(CUSTOM_EMOJI_TAG, emojiId, emojiStart, emojiEnd)
        if (end < current.length) builder.append(current.subSequence(end, current.length))
        val rawAnnotated = builder.toAnnotatedString()
        val newAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = current.text,
            newAnnotated = rawAnnotated,
            editStart = start,
            removedCount = end - start,
            insertedText = CUSTOM_EMOJI_PLACEHOLDER,
            fallbackTextAlign = state.currentTextAlign
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = newAnnotated,
                selection = TextRange(emojiEnd)
            )
        )
        state.syncStyleFromSelection()
    }

    private fun handleCompositionInput(oldValue: TextFieldValue, newValue: TextFieldValue) {
        if (oldValue.composition == null) {
            preCompositionValue = oldValue
        }
        val snapshot = preCompositionValue ?: oldValue
        val paragraphSnapshot = state.paragraphModels.map { it.copy() }
        val comp = newValue.composition!!
        val styled = snapshot.annotatedString
        val newOverallLen = newValue.annotatedString.text.length
        val compLen = comp.end - comp.start
        val nonCompNewLen = newOverallLen - compLen
        if (nonCompNewLen < styled.length) {
            val delStart = comp.start
            val delEnd = comp.start + (styled.length - nonCompNewLen)
            val overlappingLinks = styled.getStringAnnotations(RichTextState.HYPERLINK_TAG, delStart, delEnd)
            if (overlappingLinks.isNotEmpty()) {
                val link = overlappingLinks.first()
                val oldSel = snapshot.selection
                if (oldSel.collapsed || oldSel.min > link.start || oldSel.max < link.end) {
                    preCompositionValue = null
                    state.updateTextFieldValue(
                        snapshot.copy(
                            selection = TextRange(link.start, link.end),
                            composition = null
                        )
                    )
                    return
                }
            }
        }
        val prefixLen = minOf(comp.start, styled.length)
        val builder = AnnotatedString.Builder()
        if (prefixLen > 0) builder.append(styled.subSequence(0, prefixLen))
        val compText = newValue.annotatedString.text.substring(comp.start, comp.end)
        if (compText.isNotEmpty()) {
            builder.pushStyle(state.currentSpanStyle())
            builder.append(compText)
            builder.pop()
        }
        if (prefixLen < styled.length) builder.append(styled.subSequence(prefixLen, styled.length))
        val rawResultAnnotated = builder.toAnnotatedString()
        val resultAnnotated = state.syncParagraphModelsAfterTextChange(
            oldText = styled.text,
            newAnnotated = rawResultAnnotated,
            editStart = prefixLen,
            removedCount = 0,
            insertedText = compText,
            fallbackTextAlign = state.currentTextAlign
        )
        val newSelection = state.snapCursorSelection(resultAnnotated, snapshot.selection, newValue.selection)
        state.updateTextFieldValue(
            TextFieldValue(
                annotatedString = resultAnnotated,
                selection = newSelection,
                composition = newValue.composition
            )
        )
        state.restoreParagraphModels(paragraphSnapshot)
    }

    private fun handleCompositionCommit(oldValue: TextFieldValue, newValue: TextFieldValue) {
        val preComp = preCompositionValue ?: oldValue
        preCompositionValue = null
        val comp = oldValue.composition!!
        val styledAnnotated = preComp.annotatedString
        val prefixLen = minOf(comp.start, styledAnnotated.length)
        val newText = newValue.annotatedString.text
        val committedLen = newText.length - (styledAnnotated.length - prefixLen)
        val committedText = newText.substring(prefixLen, committedLen.coerceAtLeast(prefixLen))
        val builder = AnnotatedString.Builder()
        if (prefixLen > 0) builder.append(styledAnnotated.subSequence(0, prefixLen))
        if (committedText.isNotEmpty()) {
            builder.pushStyle(state.currentSpanStyle())
            builder.append(committedText)
            builder.pop()
        }
        if (prefixLen < styledAnnotated.length) {
            builder.append(styledAnnotated.subSequence(prefixLen, styledAnnotated.length))
        }
        val rawResulting = builder.toAnnotatedString()
        val resulting = state.syncParagraphModelsAfterTextChange(
            oldText = styledAnnotated.text,
            newAnnotated = rawResulting,
            editStart = prefixLen,
            removedCount = 0,
            insertedText = committedText,
            fallbackTextAlign = state.currentTextAlign
        )
        val sel = TextRange(prefixLen + committedText.length)
        val newSelection = state.snapCursorSelection(resulting, preComp.selection, sel)
        val committedValue = TextFieldValue(
            annotatedString = resulting,
            selection = newSelection
        )
        state.recordUndoSnapshot(preComp, committedValue)
        state.skipNextUndoRecord = true
        state.justToggledStyle = false
        state.updateTextFieldValue(committedValue)
        state.syncStyleFromSelection()
    }

    private fun handleNormalInput(oldValue: TextFieldValue, newValue: TextFieldValue) {
        val oldText = oldValue.annotatedString.text
        val newText = newValue.annotatedString.text

        if (oldText == newText) {
            val newSelection = state.snapCursorSelection(oldValue.annotatedString, oldValue.selection, newValue.selection)
            state.updateTextFieldValue(oldValue.copy(selection = newSelection))
            state.syncStyleFromSelection()
            return
        }

        val oldSel = oldValue.selection
        val newLen = newText.length
        val oldLen = oldText.length
        val delStart: Int
        val delEnd: Int
        val insCount: Int
        if (newLen < oldLen) {
            if (oldSel.collapsed) {
                val deletedCount = oldLen - newLen
                if (newValue.selection.min < oldSel.min) {
                    delStart = maxOf(0, oldSel.min - deletedCount)
                    delEnd = oldSel.min
                } else {
                    delStart = oldSel.min
                    delEnd = minOf(oldSel.min + deletedCount, oldLen)
                }
            } else {
                delStart = oldSel.min
                delEnd = oldSel.max
            }
            insCount = -1

            if (delStart < delEnd) {
                val overlappingLinks = oldValue.annotatedString
                    .getStringAnnotations(RichTextState.HYPERLINK_TAG, delStart, delEnd)
                if (overlappingLinks.isNotEmpty()) {
                    val link = overlappingLinks.first()
                    if (oldSel.collapsed ||
                        oldSel.min > link.start ||
                        oldSel.max < link.end
                    ) {
                        state.updateTextFieldValue(
                            oldValue.copy(
                                selection = TextRange(link.start, link.end)
                            )
                        )
                        return
                    }
                }

                val overlappingEmojis = oldValue.annotatedString
                    .getStringAnnotations(CUSTOM_EMOJI_TAG, delStart, delEnd)
                if (oldSel.collapsed && overlappingEmojis.isNotEmpty()) {
                    val emoji = overlappingEmojis.first()
                    val annotated = oldValue.annotatedString
                    val builder = AnnotatedString.Builder()
                    if (emoji.start > 0) builder.append(annotated.subSequence(0, emoji.start))
                    if (emoji.end < annotated.length) builder.append(annotated.subSequence(emoji.end, annotated.length))
                    val rawAnnotated = builder.toAnnotatedString()
                    val resultAnnotated = state.syncParagraphModelsAfterTextChange(
                        oldText = annotated.text,
                        newAnnotated = rawAnnotated,
                        editStart = emoji.start,
                        removedCount = emoji.end - emoji.start,
                        insertedText = ""
                    )
                    state.updateTextFieldValue(
                        TextFieldValue(
                            annotatedString = resultAnnotated,
                            selection = TextRange(emoji.start)
                        )
                    )
                    return
                }
            }
        } else {
            delStart = -1
            delEnd = -1
            insCount = newLen - oldLen
        }

        val annotated = processTextChange(
            oldValue.annotatedString, newValue, delStart, delEnd, insCount, oldSel
        )
        val shouldKeepTrailingEmptyLineAlign =
            newLen < oldLen &&
                newText.endsWith('\n') &&
                newValue.selection.collapsed &&
                newValue.selection.start == newText.length &&
                oldText.lastIndexOf('\n') < delStart
        val previousTextAlign = state.currentTextAlign
        val intermediate = TextFieldValue(
            annotatedString = annotated,
            selection = newValue.selection,
            composition = newValue.composition
        )
        state.justToggledStyle = false
        state.updateTextFieldValue(state.snapCursorOutOfHyperlinks(oldValue.selection, intermediate))
        if (shouldKeepTrailingEmptyLineAlign) {
            state.currentTextAlign = previousTextAlign
        } else {
            state.syncStyleFromSelection()
        }
    }

    private fun processTextChange(
        oldAnnotated: AnnotatedString,
        newValue: TextFieldValue,
        delStart: Int,
        delEnd: Int,
        insCount: Int,
        oldSelection: TextRange
    ): AnnotatedString {
        val oldLen = oldAnnotated.length
        val newText = newValue.annotatedString.text
        val builder = AnnotatedString.Builder()

        if (delStart >= 0 && delEnd > delStart) {
            if (delStart > 0) builder.append(oldAnnotated.subSequence(0, delStart))
            if (delEnd < oldLen) builder.append(oldAnnotated.subSequence(delEnd, oldLen))
            val rawAnnotated = builder.toAnnotatedString()
            return state.syncParagraphModelsAfterTextChange(
                oldText = oldAnnotated.text,
                newAnnotated = rawAnnotated,
                editStart = delStart,
                removedCount = delEnd - delStart,
                insertedText = ""
            )
        }

        val removeStart = oldSelection.min
        val removeEnd = oldSelection.max
        if (removeStart > 0) builder.append(oldAnnotated.subSequence(0, removeStart))
        if (insCount > 0) {
            val newPartEnd = minOf(removeStart + insCount, newText.length)
            val newPart = newText.substring(minOf(removeStart, newPartEnd), newPartEnd)
            if (newPart.isNotEmpty()) {
                builder.pushStyle(state.currentSpanStyle())
                builder.append(newPart)
                builder.pop()
            }
        }
        if (removeEnd < oldLen) builder.append(oldAnnotated.subSequence(removeEnd, oldLen))
        val rawAnnotated = builder.toAnnotatedString()
        val insertedText = if (insCount > 0) {
            val newPartEnd = minOf(removeStart + insCount, newText.length)
            newText.substring(minOf(removeStart, newPartEnd), newPartEnd)
        } else {
            ""
        }
        return state.syncParagraphModelsAfterTextChange(
            oldText = oldAnnotated.text,
            newAnnotated = rawAnnotated,
            editStart = removeStart,
            removedCount = removeEnd - removeStart,
            insertedText = insertedText,
            fallbackTextAlign = state.currentTextAlign
        )
    }
}
