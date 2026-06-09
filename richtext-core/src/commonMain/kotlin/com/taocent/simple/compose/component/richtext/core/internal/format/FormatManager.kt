package com.taocent.simple.compose.component.richtext.core.internal.format

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG

internal class FormatManager(private val state: RichTextState) {

    fun toggleBold() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allBold = state.isSelectionAllBold()
            applyFormatToSelection { style ->
                style.copy(fontWeight = if (allBold) FontWeight.Normal else FontWeight.Bold)
            }
        } else {
            state.currentBold = !state.currentBold
            state.justToggledStyle = true
        }
    }

    fun toggleItalic() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allItalic = state.isSelectionAllItalic()
            applyFormatToSelection { style ->
                style.copy(fontStyle = if (allItalic) FontStyle.Normal else FontStyle.Italic)
            }
        } else {
            state.currentItalic = !state.currentItalic
            state.justToggledStyle = true
        }
    }

    fun toggleUnderline() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allUnderlined = state.isSelectionAllUnderlined()
            applyFormatToSelection { style ->
                val current = style.textDecoration ?: TextDecoration.None
                val newDecoration = if (allUnderlined) {
                    TextDecoration.valueOf(current.mask and TextDecoration.Underline.mask.inv())
                } else {
                    current + TextDecoration.Underline
                }
                style.copy(textDecoration = newDecoration)
            }
        } else {
            state.currentUnderline = !state.currentUnderline
            state.justToggledStyle = true
        }
    }

    fun toggleStrikethrough() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allStrikethrough = state.isSelectionAllStrikethrough()
            applyFormatToSelection { style ->
                val current = style.textDecoration ?: TextDecoration.None
                val newDecoration = if (allStrikethrough) {
                    TextDecoration.valueOf(current.mask and TextDecoration.LineThrough.mask.inv())
                } else {
                    current + TextDecoration.LineThrough
                }
                style.copy(textDecoration = newDecoration)
            }
        } else {
            state.currentStrikethrough = !state.currentStrikethrough
            state.justToggledStyle = true
        }
    }

    fun toggleSuperscript() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allSuperscript = state.isSelectionAllSuperscript()
            applyFormatToSelection { style ->
                style.copy(
                    baselineShift = if (allSuperscript) BaselineShift.None else BaselineShift.Superscript
                )
            }
        } else {
            state.currentSuperscript = !state.currentSuperscript
            if (state.currentSuperscript) state.currentSubscript = false
            state.justToggledStyle = true
        }
    }

    fun toggleSubscript() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            val allSubscript = state.isSelectionAllSubscript()
            applyFormatToSelection { style ->
                style.copy(
                    baselineShift = if (allSubscript) BaselineShift.None else BaselineShift.Subscript
                )
            }
        } else {
            state.currentSubscript = !state.currentSubscript
            if (state.currentSubscript) state.currentSuperscript = false
            state.justToggledStyle = true
        }
    }

    fun setColor(color: Color) {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            applyFormatToSelection { style -> style.copy(color = color) }
        } else {
            state.currentColor = color
            state.justToggledStyle = true
        }
    }

    fun setFontSize(size: TextUnit) {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            applyFormatToSelection { style -> style.copy(fontSize = size) }
        } else {
            state.currentFontSize = size
            state.justToggledStyle = true
        }
    }

    fun setBackground(color: Color) {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            applyFormatToSelection { style -> style.copy(background = color) }
        } else {
            state.currentBackground = color
            state.justToggledStyle = true
        }
    }

    fun setTextAlign(align: TextAlign) {
        val normalized = ParagraphStyleUtils.normalizedTextAlign(align)
        val selection = state.textFieldValue.selection
        state.setParagraphTextAligns(selection, normalized)
        state.currentTextAlign = normalized
        state.justToggledStyle = false
        state.syncStyleFromSelection()
    }

    fun clearFormatting() {
        if (state.hasSelection) {
            if (state.isSelectionFullyInHyperlink()) return
            applyFormatToSelection { SpanStyle() }
        } else {
            state.currentBold = false
            state.currentItalic = false
            state.currentUnderline = false
            state.currentStrikethrough = false
            state.currentSuperscript = false
            state.currentSubscript = false
            state.currentColor = Color.Unspecified
            state.currentBackground = Color.Unspecified
            state.currentFontSize = TextUnit.Unspecified
            state.justToggledStyle = true
        }
    }

    private fun applyFormatToSelection(transform: (SpanStyle) -> SpanStyle) {
        val selection = state.textFieldValue.selection
        if (selection.collapsed) return

        val annotated = state.textFieldValue.annotatedString
        val rangeStart = selection.min
        val rangeEnd = selection.max

        val hyperlinkAnnotations = annotated.getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, annotated.length)
            .filter { it.start < rangeEnd && it.end > rangeStart }
        val hyperlinkRanges = hyperlinkAnnotations.map { it.start to it.end }

        val newSpans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        for (span in annotated.spanStyles) {
            when {
                span.end <= rangeStart || span.start >= rangeEnd -> {
                    newSpans.add(span)
                }
                span.start >= rangeStart && span.end <= rangeEnd -> {
                    val segments = splitRangeByHyperlinks(span.start, span.end, hyperlinkRanges)
                    for ((segStart, segEnd) in segments) {
                        val insideHyperlink = hyperlinkRanges.any { (hlStart, hlEnd) ->
                            segStart >= hlStart && segEnd <= hlEnd
                        }
                        if (insideHyperlink) {
                            newSpans.add(AnnotatedString.Range(span.item, segStart, segEnd))
                        } else {
                            newSpans.add(AnnotatedString.Range(transform(span.item), segStart, segEnd))
                        }
                    }
                }
                span.start < rangeStart && span.end > rangeEnd -> {
                    newSpans.add(AnnotatedString.Range(span.item, span.start, rangeStart))
                    val segments = splitRangeByHyperlinks(rangeStart, rangeEnd, hyperlinkRanges)
                    for ((segStart, segEnd) in segments) {
                        val insideHyperlink = hyperlinkRanges.any { (hlStart, hlEnd) ->
                            segStart >= hlStart && segEnd <= hlEnd
                        }
                        if (insideHyperlink) {
                            newSpans.add(AnnotatedString.Range(span.item, segStart, segEnd))
                        } else {
                            newSpans.add(AnnotatedString.Range(transform(span.item), segStart, segEnd))
                        }
                    }
                    newSpans.add(AnnotatedString.Range(span.item, rangeEnd, span.end))
                }
                span.start < rangeStart -> {
                    newSpans.add(AnnotatedString.Range(span.item, span.start, rangeStart))
                    val segments = splitRangeByHyperlinks(rangeStart, span.end, hyperlinkRanges)
                    for ((segStart, segEnd) in segments) {
                        val insideHyperlink = hyperlinkRanges.any { (hlStart, hlEnd) ->
                            segStart >= hlStart && segEnd <= hlEnd
                        }
                        if (insideHyperlink) {
                            newSpans.add(AnnotatedString.Range(span.item, segStart, segEnd))
                        } else {
                            newSpans.add(AnnotatedString.Range(transform(span.item), segStart, segEnd))
                        }
                    }
                }
                else -> {
                    val segments = splitRangeByHyperlinks(span.start, rangeEnd, hyperlinkRanges)
                    for ((segStart, segEnd) in segments) {
                        val insideHyperlink = hyperlinkRanges.any { (hlStart, hlEnd) ->
                            segStart >= hlStart && segEnd <= hlEnd
                        }
                        if (insideHyperlink) {
                            newSpans.add(AnnotatedString.Range(span.item, segStart, segEnd))
                        } else {
                            newSpans.add(AnnotatedString.Range(transform(span.item), segStart, segEnd))
                        }
                    }
                    newSpans.add(AnnotatedString.Range(span.item, rangeEnd, span.end))
                }
            }
        }

        val coveredByExisting = annotated.spanStyles
            .filter { it.start < rangeEnd && it.end > rangeStart }
            .map { maxOf(it.start, rangeStart) to minOf(it.end, rangeEnd) }
        val uncoveredRanges = computeUncoveredRanges(rangeStart, rangeEnd, coveredByExisting)
        for ((start, end) in uncoveredRanges) {
            val defaultStyle = transform(SpanStyle())
            val segments = splitRangeByHyperlinks(start, end, hyperlinkRanges)
            for ((segStart, segEnd) in segments) {
                val insideHyperlink = hyperlinkRanges.any { (hlStart, hlEnd) ->
                    segStart >= hlStart && segEnd <= hlEnd
                }
                if (!insideHyperlink) {
                    newSpans.add(AnnotatedString.Range(defaultStyle, segStart, segEnd))
                }
            }
        }

        val builder = AnnotatedString.Builder(annotated.length)
        builder.append(annotated.text)
        for (span in newSpans) {
            builder.addStyle(span.item, span.start, span.end)
        }
        for (paragraph in annotated.paragraphStyles) {
            builder.addStyle(paragraph.item, paragraph.start, paragraph.end)
        }
        for (annotation in annotated.getStringAnnotations(0, annotated.length)) {
            builder.addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
        }
        for (emoji in annotated.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, annotated.length)) {
            val origStyle = annotated.spanStyles
                .lastOrNull { it.start <= emoji.start && it.end > emoji.start }
                ?.item
            builder.addStyle(
                SpanStyle(
                    fontSize = origStyle?.fontSize?.takeIf { it != TextUnit.Unspecified }
                        ?: TextUnit.Unspecified,
                    color = Color.Transparent
                ),
                emoji.start,
                emoji.end
            )
        }
        val newAnnotated = builder.toAnnotatedString()

        state.updateTextFieldValue(
            state.textFieldValue.copy(
                annotatedString = newAnnotated,
                selection = selection
            )
        )
        state.justToggledStyle = false
        state.syncStyleFromSelection()
    }

    private fun splitRangeByHyperlinks(
        start: Int,
        end: Int,
        hyperlinkRanges: List<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {
        val relevantHyperlinks = hyperlinkRanges
            .filter { it.second > start && it.first < end }
            .sortedBy { it.first }
        if (relevantHyperlinks.isEmpty()) return listOf(start to end)

        val segments = mutableListOf<Pair<Int, Int>>()
        var currentPos = start
        for ((hlStart, hlEnd) in relevantHyperlinks) {
            val clampedHlStart = maxOf(hlStart, start)
            val clampedHlEnd = minOf(hlEnd, end)
            if (currentPos < clampedHlStart) {
                segments.add(currentPos to clampedHlStart)
            }
            segments.add(clampedHlStart to clampedHlEnd)
            currentPos = clampedHlEnd
        }
        if (currentPos < end) {
            segments.add(currentPos to end)
        }
        return segments
    }

    private fun computeUncoveredRanges(
        rangeStart: Int,
        rangeEnd: Int,
        coveredRanges: List<Pair<Int, Int>>
    ): List<Pair<Int, Int>> {
        if (coveredRanges.isEmpty()) return listOf(rangeStart to rangeEnd)
        val sorted = coveredRanges.sortedBy { it.first }
        val result = mutableListOf<Pair<Int, Int>>()
        var currentPos = rangeStart
        for ((start, end) in sorted) {
            if (currentPos < start) {
                result.add(currentPos to start)
            }
            currentPos = maxOf(currentPos, end)
        }
        if (currentPos < rangeEnd) {
            result.add(currentPos to rangeEnd)
        }
        return result
    }

}
