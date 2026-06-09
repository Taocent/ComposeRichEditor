package com.taocent.simple.compose.component.richtext.core.internal.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign

internal object ParagraphStyleUtils {

    fun normalizedTextAlign(align: TextAlign): TextAlign {
        return if (align == TextAlign.Unspecified) TextAlign.Left else align
    }

    fun textAlignAt(annotated: AnnotatedString, position: Int): TextAlign {
        val text = annotated.text
        if (text.isEmpty()) {
            val style = annotated.paragraphStyles.lastOrNull { range ->
                range.start == 0 && range.end == 0
            }?.item ?: return TextAlign.Left
            return normalizedTextAlign(style.textAlign)
        }
        val pos = position.coerceIn(0, text.length)
        // 末尾虚拟空行: 文本以 \n 结尾,光标正好在 text.length 处,
        // 此时应命中末尾虚拟空行的 align,而不是被拽回到段 1 内部
        val isTrailingVirtualLine = pos == text.length && text.last() == '\n'
        val lookup = when {
            isTrailingVirtualLine -> pos
            pos >= text.length -> text.length - 1
            text[pos] == '\n' && pos > 0 -> pos - 1
            else -> pos
        }
        val style = if (isTrailingVirtualLine) {
            annotated.paragraphStyles.lastOrNull { range ->
                range.start == pos && range.end == pos
            }?.item ?: annotated.paragraphStyles.lastOrNull { range ->
                range.end == text.length && range.start < range.end
            }?.item ?: return TextAlign.Left
        } else {
            annotated.paragraphStyles.lastOrNull { range ->
                lookup >= range.start && lookup < range.end
            }?.item ?: return TextAlign.Left
        }
        return normalizedTextAlign(style.textAlign)
    }

    fun lineRangeAt(text: String, position: Int): TextRange {
        if (text.isEmpty()) return TextRange.Zero
        val pos = position.coerceIn(0, text.length)
        // 末尾虚拟空行: 文本以 \n 结尾,光标正好在 text.length 处,
        // 此时应返回末尾虚拟空行范围 (text.length, text.length),
        // 而不是被拽回到段 1 内部的最后字符
        if (pos == text.length && text.last() == '\n') {
            return TextRange(text.length, text.length)
        }
        val lookup = when {
            pos >= text.length -> text.length - 1
            text[pos] == '\n' && pos > 0 -> pos - 1
            else -> pos
        }
        val start = if (lookup <= 0) {
            0
        } else {
            text.lastIndexOf('\n', lookup - 1).let { if (it < 0) 0 else it + 1 }
        }
        val lineBreak = text.indexOf('\n', start)
        val end = if (lineBreak < 0) text.length else lineBreak + 1
        return TextRange(start, end)
    }

    fun lineRangesForSelection(text: String, selection: TextRange): List<TextRange> {
        if (text.isEmpty()) return listOf(TextRange.Zero)
        if (selection.collapsed) return listOf(lineRangeAt(text, selection.start))
        val start = selection.min.coerceIn(0, text.length)
        val end = selection.max.coerceIn(0, text.length)
        val result = mutableListOf<TextRange>()
        var lineStart = lineRangeAt(text, start).start
        while (lineStart <= text.length) {
            val lineBreak = text.indexOf('\n', lineStart)
            val contentEnd = if (lineBreak < 0) text.length else lineBreak
            val styleEnd = if (lineBreak < 0) text.length else lineBreak + 1
            val intersects = lineStart < end && contentEnd >= start
            val emptyLineIntersects = lineStart == contentEnd && lineStart >= start && lineStart < end
            if (intersects || emptyLineIntersects) {
                result.add(TextRange(lineStart, styleEnd))
            }
            if (styleEnd >= text.length || contentEnd >= end) break
            lineStart = styleEnd
        }
        return result.ifEmpty { listOf(lineRangeAt(text, start)) }
    }

    fun applyParagraphTextAligns(
        annotated: AnnotatedString,
        aligns: List<TextAlign>
    ): AnnotatedString {
        val paragraphStyles = paragraphRanges(annotated.text).mapIndexedNotNull { index, range ->
            if (range.start == range.end && annotated.text.isNotEmpty()) return@mapIndexedNotNull null
            AnnotatedString.Range(
                ParagraphStyle(textAlign = normalizedTextAlign(aligns.getOrElse(index) { TextAlign.Left })),
                range.start,
                range.end
            )
        }
        return rebuildWithParagraphStyles(annotated, paragraphStyles, keepNonOverlappingExisting = false)
    }

    fun paragraphRanges(text: String): List<TextRange> {
        if (text.isEmpty()) return listOf(TextRange.Zero)
        val result = mutableListOf<TextRange>()
        var lineStart = 0
        while (lineStart <= text.length) {
            val lineBreak = text.indexOf('\n', lineStart)
            val styleEnd = if (lineBreak < 0) text.length else lineBreak + 1
            result.add(TextRange(lineStart, styleEnd))
            if (lineBreak < 0) break
            lineStart = lineBreak + 1
        }
        return result
    }

    private fun rebuildWithParagraphStyles(
        annotated: AnnotatedString,
        replacements: List<AnnotatedString.Range<ParagraphStyle>>,
        keepNonOverlappingExisting: Boolean
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(annotated.length)
        builder.append(annotated.text)
        for (span in annotated.spanStyles) {
            builder.addStyle(span.item, span.start, span.end)
        }
        for (annotation in annotated.getStringAnnotations(0, annotated.length)) {
            builder.addStringAnnotation(annotation.tag, annotation.item, annotation.start, annotation.end)
        }
        val mergedParagraphs = if (keepNonOverlappingExisting) {
            val kept = annotated.paragraphStyles.flatMap { it.minus(replacements) }
            (kept + replacements).mergeAdjacentSameStyles()
        } else {
            replacements.mergeAdjacentSameStyles()
        }
        for (paragraph in mergedParagraphs) {
            builder.addStyle(paragraph.item, paragraph.start, paragraph.end)
        }
        return builder.toAnnotatedString()
    }

    private fun AnnotatedString.Range<ParagraphStyle>.overlaps(
        other: AnnotatedString.Range<ParagraphStyle>
    ): Boolean {
        // 空 range(start == end)不覆盖任何字符,因此:
        // - 两个空 range:仅在 start 完全相同时视为重叠(同一位置的标记)
        // - 空 range 与非空 range:不重叠(因为非空 range 的 end 数值虽然可能等于
        //   空 range 的 start,但 [start, end) 是开区间,end 位置不在范围内)
        if (start == end && other.start == other.end) {
            return start == other.start
        }
        if (start == end || other.start == other.end) {
            return false
        }
        return start < other.end && end > other.start
    }

    private fun AnnotatedString.Range<ParagraphStyle>.minus(
        replacements: List<AnnotatedString.Range<ParagraphStyle>>
    ): List<AnnotatedString.Range<ParagraphStyle>> {
        var segments = listOf(this)
        for (replacement in replacements) {
            segments = segments.flatMap { segment -> segment.minus(replacement) }
        }
        return segments
    }

    private fun AnnotatedString.Range<ParagraphStyle>.minus(
        replacement: AnnotatedString.Range<ParagraphStyle>
    ): List<AnnotatedString.Range<ParagraphStyle>> {
        if (!replacement.overlaps(this)) return listOf(this)
        val result = mutableListOf<AnnotatedString.Range<ParagraphStyle>>()
        if (start < replacement.start) {
            result.add(AnnotatedString.Range(item, start, replacement.start))
        }
        if (replacement.end < end) {
            result.add(AnnotatedString.Range(item, replacement.end, end))
        }
        return result
    }

    private fun List<AnnotatedString.Range<ParagraphStyle>>.mergeAdjacentSameStyles(): List<AnnotatedString.Range<ParagraphStyle>> {
        if (isEmpty()) return this
        val result = mutableListOf<AnnotatedString.Range<ParagraphStyle>>()
        for (range in sortedBy { it.start }) {
            val previous = result.lastOrNull()
            if (
                previous != null &&
                previous.start < previous.end &&
                range.start < range.end &&
                previous.end == range.start &&
                previous.item == range.item
            ) {
                result[result.lastIndex] = AnnotatedString.Range(previous.item, previous.start, range.end)
            } else {
                result.add(range)
            }
        }
        return result
    }
}
