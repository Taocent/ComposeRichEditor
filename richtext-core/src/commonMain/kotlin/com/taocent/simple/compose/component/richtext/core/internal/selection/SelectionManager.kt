package com.taocent.simple.compose.component.richtext.core.internal.selection

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG

internal class SelectionManager(private val state: RichTextState) {

    internal data class ParsedStyles(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikethrough: Boolean = false,
        val superscript: Boolean = false,
        val subscript: Boolean = false,
        val color: Color? = null,
        val background: Color? = null,
        val fontSize: TextUnit? = null
    )

    fun syncStyleFromSelection() {
        if (state.justToggledStyle) {
            return
        }

        val selection = state.textFieldValue.selection
        val rawPos = if (selection.collapsed) selection.start else selection.min
        val pos = rawPos.coerceAtMost(state.plainText.length - 1)

        val text = state.textFieldValue.annotatedString
        state.currentTextAlign = state.textAlignForSelection(selection)
        if (pos < 0 || pos >= text.length) {
            resetStyles()
            return
        }

        val wasCoerced = rawPos != pos

        if (selection.collapsed && rawPos > 0) {
            val effectivePos = getEffectiveLeftPosition(text, rawPos)
            val resolvedPos = effectivePos.coerceAtMost(text.length - 1)
            val isInsideHyperlink = text.getStringAnnotations(
                RichTextState.HYPERLINK_TAG, 0, text.length
            ).any { resolvedPos >= it.start && resolvedPos < it.end }
            val isInsideEmoji = text.getStringAnnotations(
                CUSTOM_EMOJI_TAG, 0, text.length
            ).any { resolvedPos >= it.start && resolvedPos < it.end }
            if (!isInsideHyperlink && !isInsideEmoji && resolvedPos >= 0 && resolvedPos < text.length) {
                applyParsedStyles(parseStylesAt(text, resolvedPos))
            } else {
                resetStyles()
            }
            return
        }

        if (selection.collapsed && rawPos <= 0) {
            resetStyles()
            return
        }

        if (wasCoerced) {
            resetStyles()
            return
        }

        val insideLink = state.isInsideHyperlink(selection.start)
        val insideEmoji = text.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, text.length)
            .any { pos >= it.start && pos < it.end }
        val stylesAtPos = if (!insideLink && !insideEmoji) parseStylesAt(text, pos) else ParsedStyles()

        val hasAnyStyle = stylesAtPos.bold || stylesAtPos.italic || stylesAtPos.underline ||
            stylesAtPos.strikethrough ||
            stylesAtPos.superscript || stylesAtPos.subscript ||
            stylesAtPos.color != null || stylesAtPos.background != null || stylesAtPos.fontSize != null

        if (hasAnyStyle) {
            applyParsedStyles(stylesAtPos)
            return
        }

        resetStyles()
    }

    private fun getEffectiveLeftPosition(text: AnnotatedString, cursorPos: Int): Int {
        var leftPos = cursorPos - 1
        val hyperlinks = text.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        val emojis = text.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, text.length)
        while (leftPos >= 0) {
            val hyperlinkAtLeft = hyperlinks.firstOrNull { leftPos >= it.start && leftPos < it.end }
            if (hyperlinkAtLeft != null) {
                leftPos = hyperlinkAtLeft.start - 1
                continue
            }
            val emojiAtLeft = emojis.firstOrNull { leftPos >= it.start && leftPos < it.end }
            if (emojiAtLeft != null) {
                leftPos = emojiAtLeft.start - 1
                continue
            }
            break
        }
        return maxOf(leftPos, 0)
    }

    fun isPropertyUniformInSelection(predicate: (SpanStyle) -> Boolean): Boolean {
        val selection = state.textFieldValue.selection
        if (selection.collapsed) return false
        val text = state.textFieldValue.annotatedString
        val rangeStart = selection.min
        val rangeEnd = selection.max
        
        val matchingSpans = text.spanStyles
            .filter { span ->
                span.start < rangeEnd && span.end > rangeStart && predicate(span.item)
            }
            .map { span ->
                maxOf(span.start, rangeStart) to minOf(span.end, rangeEnd)
            }
            .sortedBy { it.first }
        
        if (matchingSpans.isEmpty()) return false
        
        var coveredEnd = rangeStart
        for ((start, end) in matchingSpans) {
            if (start > coveredEnd) return false
            coveredEnd = maxOf(coveredEnd, end)
        }
        return coveredEnd >= rangeEnd
    }

    fun isSelectionAllBold(): Boolean =
        isPropertyUniformInSelection { it.fontWeight == FontWeight.Bold }

    fun isSelectionAllItalic(): Boolean =
        isPropertyUniformInSelection { it.fontStyle == FontStyle.Italic }

    fun isSelectionAllUnderlined(): Boolean =
        isPropertyUniformInSelection { it.textDecoration?.contains(TextDecoration.Underline) == true }

    fun isSelectionAllStrikethrough(): Boolean =
        isPropertyUniformInSelection { it.textDecoration?.contains(TextDecoration.LineThrough) == true }

    fun isSelectionAllSuperscript(): Boolean =
        isPropertyUniformInSelection { it.baselineShift == BaselineShift.Superscript }

    fun isSelectionAllSubscript(): Boolean =
        isPropertyUniformInSelection { it.baselineShift == BaselineShift.Subscript }

    private fun parseStylesAt(text: AnnotatedString, pos: Int): ParsedStyles {
        var bold = false
        var italic = false
        var underline = false
        var strikethrough = false
        var superscript = false
        var subscript = false
        var color: Color? = null
        var background: Color? = null
        var fontSize: TextUnit? = null
        for (spanStyle in text.spanStyles) {
            if (pos >= spanStyle.start && pos < spanStyle.end) {
                val style = spanStyle.item
                if (style.fontWeight == FontWeight.Bold) bold = true
                if (style.fontStyle == FontStyle.Italic) italic = true
                if (style.textDecoration?.contains(TextDecoration.Underline) == true) underline = true
                if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) strikethrough = true
                if (style.baselineShift == BaselineShift.Superscript) superscript = true
                if (style.baselineShift == BaselineShift.Subscript) subscript = true
                if (style.color != Color.Unspecified) color = style.color
                if (style.background != Color.Unspecified) background = style.background
                if (style.fontSize != TextUnit.Unspecified) fontSize = style.fontSize
            }
        }
        return ParsedStyles(bold, italic, underline, strikethrough, superscript, subscript, color, background, fontSize)
    }

    private fun applyParsedStyles(styles: ParsedStyles) {
        state.currentBold = styles.bold
        state.currentItalic = styles.italic
        state.currentUnderline = styles.underline
        state.currentStrikethrough = styles.strikethrough
        state.currentSuperscript = styles.superscript
        state.currentSubscript = styles.subscript
        state.currentColor = styles.color ?: Color.Unspecified
        state.currentBackground = styles.background ?: Color.Unspecified
        state.currentFontSize = styles.fontSize ?: TextUnit.Unspecified
    }

    private fun resetStyles() {
        state.currentBold = false
        state.currentItalic = false
        state.currentUnderline = false
        state.currentStrikethrough = false
        state.currentSuperscript = false
        state.currentSubscript = false
        state.currentColor = Color.Unspecified
        state.currentBackground = Color.Unspecified
        state.currentFontSize = TextUnit.Unspecified
    }
}
