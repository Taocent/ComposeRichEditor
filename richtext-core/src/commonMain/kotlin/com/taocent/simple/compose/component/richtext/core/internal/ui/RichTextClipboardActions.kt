package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.taocent.simple.compose.component.richtext.core.RichTextConfig
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardGetText
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardSetText

class RichTextClipboardActions(
    private val clipboard: Clipboard,
    private val state: RichTextState,
    private val config: RichTextConfig,
) {
    suspend fun copySelection() {
        val selected = selectedText() ?: return
        clipboardSetText(clipboard, state, selected)
    }

    suspend fun cutSelection() {
        val selected = selectedText() ?: return
        clipboardSetText(clipboard, state, selected)
        state.deleteSelection()
    }

    suspend fun paste(allowSmartPaste: Boolean) {
        val clipAnnotated = clipboardGetText(clipboard, state).getOrNull()
        if (clipAnnotated == null || clipAnnotated.text.isEmpty()) return
        val clipText = clipAnnotated.text
        val hasRichStyles = clipAnnotated.spanStyles.isNotEmpty() ||
            clipAnnotated.paragraphStyles.any { it.item.textAlign != TextAlign.Unspecified }
        if (hasRichStyles) {
            state.insertAnnotatedString(clipAnnotated)
            return
        }
        val validIds = config.customEmojis.map { it.id }.toSet()
        val displaySize = state.currentFontSize
            .takeIf { it != TextUnit.Unspecified }
            ?: 16.sp
        val hasEmojiPattern = Regex("\\[([a-zA-Z0-9_]+)]")
            .findAll(clipText)
            .any { it.groupValues[1] in validIds }
        if (hasEmojiPattern) {
            state.insertTextWithEmojis(clipText, validIds, displaySize)
            return
        }
        val anySmartPasteEnabled = config.smartPasteJsonEnabled ||
            config.smartPasteHtmlEnabled ||
            config.smartPasteMarkdownEnabled
        if (allowSmartPaste && anySmartPasteEnabled) {
            state.smartPaste(
                clipText,
                jsonEnabled = config.smartPasteJsonEnabled,
                htmlEnabled = config.smartPasteHtmlEnabled,
                markdownEnabled = config.smartPasteMarkdownEnabled,
                parsers = config.pasteParsers
            )
        } else {
            state.insertText(clipText)
        }
    }

    private fun selectedText(): AnnotatedString? {
        val selection = state.textFieldValue.selection
        if (selection.collapsed) return null
        return state.textFieldValue.annotatedString.subSequence(selection.min, selection.max)
    }
}
