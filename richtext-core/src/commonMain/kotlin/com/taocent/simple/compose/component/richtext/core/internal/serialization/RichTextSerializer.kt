package com.taocent.simple.compose.component.richtext.core.internal.serialization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object RichTextSerializer {

    fun toJson(annotated: AnnotatedString): String {
        val sb = StringBuilder()
        sb.append("{\"text\":")
        sb.appendJsonString(annotated.text)

        val mergedSpans = mergeAdjacentSpans(annotated.spanStyles)
        sb.append(",\"spans\":[")
        mergedSpans.forEachIndexed { index, range ->
            if (index > 0) sb.append(",")
            sb.append("{\"start\":${range.start},\"end\":${range.end}")
            spanStyleToJson(sb, range.item)
            sb.append("}")
        }
        sb.append("]")

        val mergedParagraphs = mergeAdjacentParagraphs(annotated.paragraphStyles)
        sb.append(",\"paragraphs\":[")
        mergedParagraphs.forEachIndexed { index, range ->
            if (index > 0) sb.append(",")
            sb.append("{\"start\":${range.start},\"end\":${range.end}")
            paragraphStyleToJson(sb, range.item)
            sb.append("}")
        }
        sb.append("]")

        sb.append(",\"annotations\":[")
        annotated.getStringAnnotations(0, annotated.text.length).forEachIndexed { index, range ->
            if (index > 0) sb.append(",")
            sb.append("{\"start\":${range.start},\"end\":${range.end},\"tag\":")
            sb.appendJsonString(range.tag)
            sb.append(",\"value\":")
            sb.appendJsonString(range.item)
            sb.append("}")
        }
        sb.append("]}")

        return sb.toString()
    }

    private fun mergeAdjacentSpans(
        spans: List<AnnotatedString.Range<SpanStyle>>
    ): List<AnnotatedString.Range<SpanStyle>> {
        if (spans.size <= 1) return spans
        val sorted = spans.sortedBy { it.start }
        val merged = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start == current.end && stylesEqual(current.item, next.item)) {
                current = AnnotatedString.Range(current.item, current.start, next.end)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun mergeAdjacentParagraphs(
        paragraphs: List<AnnotatedString.Range<ParagraphStyle>>
    ): List<AnnotatedString.Range<ParagraphStyle>> {
        if (paragraphs.size <= 1) return paragraphs
        val sorted = paragraphs.sortedBy { it.start }
        val merged = mutableListOf<AnnotatedString.Range<ParagraphStyle>>()
        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.start == current.end && paragraphStylesEqual(current.item, next.item)) {
                current = AnnotatedString.Range(current.item, current.start, next.end)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun paragraphStylesEqual(a: ParagraphStyle, b: ParagraphStyle): Boolean {
        return a.textAlign == b.textAlign
    }

    private fun stylesEqual(a: SpanStyle, b: SpanStyle): Boolean {
        return a.fontWeight == b.fontWeight &&
                a.fontStyle == b.fontStyle &&
                a.textDecoration == b.textDecoration &&
                a.color == b.color &&
                a.background == b.background &&
                a.fontSize == b.fontSize &&
                a.baselineShift == b.baselineShift
    }

    fun parseJsonObject(json: String): Map<String, Any?> {
        return JsonParser(json).parseObject()
    }

    fun fromJson(json: String): AnnotatedString {
        return fromJsonMap(parseJsonObject(json))
    }

    fun fromJsonMap(root: Map<*, *>): AnnotatedString {
        val text = root["text"] as? String ?: ""
        val spans: List<Any?> = root["spans"] as? List<*> ?: emptyList()
        val paragraphs: List<Any?> = root["paragraphs"] as? List<*> ?: emptyList()
        val annotations: List<Any?> = root["annotations"] as? List<*> ?: emptyList()

        val builder = AnnotatedString.Builder(text)

        for (span in spans) {
            val map = span as? Map<*, *> ?: continue
            val start = (map["start"] as? Double)?.toInt() ?: continue
            val end = (map["end"] as? Double)?.toInt() ?: continue
            val style = parseSpanStyle(map)
            builder.addStyle(style, start, end)
        }

        for (paragraph in paragraphs) {
            val map = paragraph as? Map<*, *> ?: continue
            val start = (map["start"] as? Double)?.toInt() ?: continue
            val end = (map["end"] as? Double)?.toInt() ?: continue
            val style = parseParagraphStyle(map)
            builder.addStyle(style, start, end)
        }

        for (annotation in annotations) {
            val map = annotation as? Map<*, *> ?: continue
            val start = (map["start"] as? Double)?.toInt() ?: continue
            val end = (map["end"] as? Double)?.toInt() ?: continue
            val tag = map["tag"] as? String ?: continue
            val value = map["value"] as? String ?: continue
            builder.addStringAnnotation(tag, value, start, end)
        }

        return builder.toAnnotatedString()
    }

    private fun spanStyleToJson(sb: StringBuilder, style: SpanStyle) {
        style.fontWeight?.let { sb.append(",\"fontWeight\":\"${it.weight}\"") }
        style.fontStyle?.let {
            val name = if (it == FontStyle.Italic) "italic" else "normal"
            sb.append(",\"fontStyle\":\"$name\"")
        }
        style.textDecoration?.let { decos ->
            if (decos.contains(TextDecoration.Underline)) sb.append(",\"underline\":true")
            if (decos.contains(TextDecoration.LineThrough)) sb.append(",\"lineThrough\":true")
        }
        if (style.color != Color.Unspecified) {
            sb.append(",\"color\":\"${style.color.toArgbHex()}\"")
        }
        if (style.background != Color.Unspecified) {
            sb.append(",\"bg\":\"${style.background.toArgbHex()}\"")
        }
        if (style.fontSize != TextUnit.Unspecified) {
            sb.append(",\"fontSize\":${style.fontSize.value}")
        }
        if (style.baselineShift == BaselineShift.Superscript) {
            sb.append(",\"superscript\":true")
        }
        if (style.baselineShift == BaselineShift.Subscript) {
            sb.append(",\"subscript\":true")
        }
    }

    private fun paragraphStyleToJson(sb: StringBuilder, style: ParagraphStyle) {
        when (style.textAlign) {
            TextAlign.Center -> sb.append(",\"textAlign\":\"center\"")
            TextAlign.Right -> sb.append(",\"textAlign\":\"right\"")
            TextAlign.Left -> sb.append(",\"textAlign\":\"left\"")
            else -> Unit
        }
    }

    private fun parseSpanStyle(map: Map<*, *>): SpanStyle {
        var style = SpanStyle()

        val fontWeight = map["fontWeight"] as? String
        if (fontWeight != null) {
            val weight = fontWeight.toIntOrNull() ?: when (fontWeight) {
                "bold" -> 700
                "normal" -> 400
                else -> 400
            }
            style = style.copy(fontWeight = FontWeight(weight))
        }

        val fontStyle = map["fontStyle"] as? String
        if (fontStyle == "italic") {
            style = style.copy(fontStyle = FontStyle.Italic)
        }

        val decorations = mutableListOf<TextDecoration>()
        if (map["underline"] == true) decorations.add(TextDecoration.Underline)
        if (map["lineThrough"] == true) decorations.add(TextDecoration.LineThrough)
        if (decorations.isNotEmpty()) {
            style = style.copy(textDecoration = TextDecoration.combine(decorations))
        }

        val color = map["color"] as? String
        if (color != null) {
            style = style.copy(color = color.parseArgbColor())
        }

        val bg = map["bg"] as? String
        if (bg != null) {
            style = style.copy(background = bg.parseArgbColor())
        }

        val fontSize = map["fontSize"] as? Double
        if (fontSize != null) {
            style = style.copy(fontSize = fontSize.sp)
        }

        if (map["superscript"] == true) {
            style = style.copy(baselineShift = BaselineShift.Superscript)
        }
        if (map["subscript"] == true) {
            style = style.copy(baselineShift = BaselineShift.Subscript)
        }

        return style
    }

    private fun parseParagraphStyle(map: Map<*, *>): ParagraphStyle {
        val align = when (map["textAlign"] as? String) {
            "center" -> TextAlign.Center
            "right" -> TextAlign.Right
            else -> TextAlign.Left
        }
        return ParagraphStyle(textAlign = align)
    }

    private fun Color.toArgbHex(): String {
        val argb = (this.value shr 32).toLong() and 0xFFFFFFFFL
        return "#${argb.toString(16).padStart(8, '0').uppercase()}"
    }

    private fun String.parseArgbColor(): Color {
        val hex = this.removePrefix("#")
        return try {
            val argb = hex.toLong(16).toInt()
            Color(argb)
        } catch (_: Exception) {
            Color.Unspecified
        }
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        for (c in value) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}

private class JsonParser(private val input: String) {
    private var pos = 0

    fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        expect('{')
        skipWhitespace()
        if (peek() == '}') { pos++; return map }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            map[key] = parseValue()
            skipWhitespace()
            if (peek() == ',') { pos++; continue }
            break
        }
        expect('}')
        return map
    }

    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        expect('[')
        skipWhitespace()
        if (peek() == ']') { pos++; return list }
        while (true) {
            skipWhitespace()
            list.add(parseValue())
            skipWhitespace()
            if (peek() == ',') { pos++; continue }
            break
        }
        expect(']')
        return list
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        return when (peek()) {
            '"' -> parseString()
            '{' -> parseObject()
            '[' -> parseArray()
            't' -> { expectLiteral("true"); true }
            'f' -> { expectLiteral("false"); false }
            'n' -> { expectLiteral("null"); null }
            else -> parseNumber()
        }
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < input.length && input[pos] != '"') {
            if (input[pos] == '\\') {
                pos++
                when {
                    pos >= input.length -> break
                    input[pos] == '"' -> sb.append('"')
                    input[pos] == '\\' -> sb.append('\\')
                    input[pos] == 'n' -> sb.append('\n')
                    input[pos] == 'r' -> sb.append('\r')
                    input[pos] == 't' -> sb.append('\t')
                    input[pos] == 'u' -> {
                        val hex = input.substring(pos + 1, (pos + 5).coerceAtMost(input.length))
                        sb.append(hex.toInt(16).toChar())
                        pos += 4
                    }
                    else -> sb.append(input[pos])
                }
            } else {
                sb.append(input[pos])
            }
            pos++
        }
        expect('"')
        return sb.toString()
    }

    private fun parseNumber(): Double {
        val start = pos
        if (pos < input.length && input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) pos++
        if (pos < input.length && input[pos] == '.') {
            pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        return input.substring(start, pos).toDouble()
    }

    private fun peek(): Char = if (pos < input.length) input[pos] else '\u0000'

    private fun expect(c: Char) {
        skipWhitespace()
        if (pos >= input.length || input[pos] != c) {
            throw IllegalArgumentException("Expected '$c' at position $pos, got '${if (pos < input.length) input[pos] else "EOF"}'")
        }
        pos++
    }

    private fun expectLiteral(literal: String) {
        if (!input.startsWith(literal, pos)) {
            throw IllegalArgumentException("Expected '$literal' at position $pos")
        }
        pos += literal.length
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }
}
