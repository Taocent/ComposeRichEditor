package com.taocent.simple.compose.component.richtext.core.internal.serialization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

internal object HtmlToAnnotatedString {

    fun parse(html: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val styleStack = mutableListOf<StyleEntry>()
        var i = 0
        val text = html.trim()

        while (i < text.length) {
            if (text[i] == '<') {
                val closeIdx = text.indexOf('>', i)
                if (closeIdx == -1) {
                    builder.append(text[i])
                    i++
                    continue
                }
                val tagContent = text.substring(i + 1, closeIdx).trim()
                val isClosing = tagContent.startsWith("/")
                val tagBody = if (isClosing) tagContent.substring(1).trim() else tagContent

                if (isClosing) {
                    val tagName = tagBody.substringBefore(' ').lowercase()
                    val idx = styleStack.indexOfLast { it.tagName == tagName }
                    if (idx >= 0) {
                        val entry = styleStack.removeAt(idx)
                        entry.style?.let { builder.pop() }
                        if (entry.isHyperlink) builder.pop()
                    }
                } else {
                    val tagName = tagBody.substringBefore(' ').lowercase()
                    val attrs = parseAttributes(tagBody)
                    val style = tagToStyle(tagName, attrs)
                    val isHyperlink = tagName == "a"
                    val isVoid = tagName in listOf("br", "hr", "img")

                    if (tagName == "br") {
                        builder.append('\n')
                    } else if (tagName == "p" || tagName == "div") {
                        if (builder.length > 0 && !builder.endsWithNewline()) {
                            builder.append('\n')
                        }
                    } else if (!isVoid) {
                        if (isHyperlink) {
                            val href = attrs["href"] ?: ""
                            builder.addStringAnnotation("URL", href, builder.length, builder.length)
                        }
                        style?.let { builder.pushStyle(it) }
                        styleStack.add(StyleEntry(tagName, style, isHyperlink))
                    }
                }
                i = closeIdx + 1
            } else if (text[i] == '&') {
                val (entity, endIdx) = decodeEntity(text, i)
                builder.append(entity)
                i = endIdx
            } else {
                builder.append(text[i])
                i++
            }
        }

        for (entry in styleStack.reversed()) {
            entry.style?.let { builder.pop() }
        }

        return builder.toAnnotatedString()
    }

    private fun tagToStyle(tag: String, attrs: Map<String, String>): SpanStyle? {
        val base = when (tag) {
            "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u", "ins" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript)
            "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript)
            "code" -> SpanStyle(
                background = Color(0x1A808080),
                fontSize = 14.sp
            )
            "mark" -> SpanStyle(background = Color(0xFFFFFF00))
            "small" -> SpanStyle(fontSize = 12.sp)
            "span" -> parseSpanStyle(attrs)
            else -> return null
        }
        return base
    }

    private fun parseSpanStyle(attrs: Map<String, String>): SpanStyle? {
        val style = attrs["style"] ?: return null
        var result = SpanStyle()
        val declarations = style.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        for (decl in declarations) {
            val colonIdx = decl.indexOf(':')
            if (colonIdx < 0) continue
            val prop = decl.substring(0, colonIdx).trim().lowercase()
            val value = decl.substring(colonIdx + 1).trim()
            when (prop) {
                "font-weight" -> {
                    val weight = when (value.lowercase()) {
                        "bold", "bolder" -> FontWeight.Bold
                        "lighter" -> FontWeight.Light
                        "normal" -> FontWeight.Normal
                        else -> value.toIntOrNull()?.let { FontWeight(it) }
                    }
                    weight?.let { result = result.copy(fontWeight = it) }
                }
                "font-style" -> {
                    if (value.lowercase() == "italic") {
                        result = result.copy(fontStyle = FontStyle.Italic)
                    }
                }
                "text-decoration" -> {
                    val decorations = mutableListOf<TextDecoration>()
                    if (value.contains("underline")) decorations.add(TextDecoration.Underline)
                    if (value.contains("line-through")) decorations.add(TextDecoration.LineThrough)
                    if (decorations.isNotEmpty()) {
                        result = result.copy(textDecoration = TextDecoration.combine(decorations))
                    }
                }
                "color" -> {
                    parseCssColor(value)?.let { result = result.copy(color = it) }
                }
                "background-color", "background" -> {
                    parseCssColor(value)?.let { result = result.copy(background = it) }
                }
                "font-size" -> {
                    parseCssFontSize(value)?.let { result = result.copy(fontSize = it) }
                }
                "vertical-align" -> {
                    when (value.lowercase()) {
                        "super", "sup" -> result = result.copy(baselineShift = BaselineShift.Superscript)
                        "sub" -> result = result.copy(baselineShift = BaselineShift.Subscript)
                    }
                }
            }
        }
        return if (result == SpanStyle()) null else result
    }

    private fun parseCssColor(value: String): Color? {
        val v = value.trim().lowercase()
        return when {
            v.startsWith("#") -> {
                val hex = v.removePrefix("#")
                try {
                    when (hex.length) {
                        3 -> {
                            val r = "${hex[0]}${hex[0]}".toInt(16)
                            val g = "${hex[1]}${hex[1]}".toInt(16)
                            val b = "${hex[2]}${hex[2]}".toInt(16)
                            Color(r, g, b)
                        }
                        6 -> {
                            val r = hex.substring(0, 2).toInt(16)
                            val g = hex.substring(2, 4).toInt(16)
                            val b = hex.substring(4, 6).toInt(16)
                            Color(r, g, b)
                        }
                        8 -> {
                            val a = hex.substring(0, 2).toInt(16)
                            val r = hex.substring(2, 4).toInt(16)
                            val g = hex.substring(4, 6).toInt(16)
                            val b = hex.substring(6, 8).toInt(16)
                            Color(r, g, b, a)
                        }
                        else -> null
                    }
                } catch (_: Exception) { null }
            }
            v.startsWith("rgb(") -> {
                val parts = v.removePrefix("rgb(").removeSuffix(")")
                    .split(",").map { it.trim().removeSuffix("%").trim().toFloatOrNull() }
                if (parts.size >= 3 && parts.all { it != null }) {
                    Color(parts[0]!!.toInt(), parts[1]!!.toInt(), parts[2]!!.toInt())
                } else null
            }
            v.startsWith("rgba(") -> {
                val parts = v.removePrefix("rgba(").removeSuffix(")")
                    .split(",").map { it.trim() }
                if (parts.size >= 4) {
                    val r = parts[0].toFloatOrNull()?.toInt()
                    val g = parts[1].toFloatOrNull()?.toInt()
                    val b = parts[2].toFloatOrNull()?.toInt()
                    val a = (parts[3].toFloatOrNull()?.times(255))?.toInt()
                    if (r != null && g != null && b != null && a != null) {
                        Color(r, g, b, a)
                    } else null
                } else null
            }
            else -> NAMED_COLORS[v]
        }
    }

    private fun parseCssFontSize(value: String): androidx.compose.ui.unit.TextUnit? {
        val v = value.trim().lowercase()
        return when {
            v.endsWith("px") -> v.removeSuffix("px").toFloatOrNull()?.let { it.sp }
            v.endsWith("pt") -> v.removeSuffix("pt").toFloatOrNull()?.let { it.sp }
            v.endsWith("em") -> v.removeSuffix("em").toFloatOrNull()?.let { (it * 16).sp }
            v.endsWith("rem") -> v.removeSuffix("rem").toFloatOrNull()?.let { (it * 16).sp }
            v.endsWith("%") -> v.removeSuffix("%").toFloatOrNull()?.let { (it / 100 * 16).sp }
            else -> v.toFloatOrNull()?.let { it.sp }
        }
    }

    private fun parseAttributes(tagBody: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val body = tagBody.substringAfter(' ', "")
        var i = 0
        while (i < body.length) {
            while (i < body.length && body[i].isWhitespace()) i++
            if (i >= body.length) break
            val nameStart = i
            while (i < body.length && body[i] != '=' && !body[i].isWhitespace()) i++
            val name = body.substring(nameStart, i).lowercase()
            while (i < body.length && body[i].isWhitespace()) i++
            if (i < body.length && body[i] == '=') {
                i++
                while (i < body.length && body[i].isWhitespace()) i++
                val value = if (i < body.length && (body[i] == '"' || body[i] == '\'')) {
                    val quote = body[i]
                    i++
                    val valStart = i
                    while (i < body.length && body[i] != quote) i++
                    val v = body.substring(valStart, i)
                    if (i < body.length) i++
                    v
                } else {
                    val valStart = i
                    while (i < body.length && !body[i].isWhitespace()) i++
                    body.substring(valStart, i)
                }
                attrs[name] = value
            } else {
                attrs[name] = ""
            }
        }
        return attrs
    }

    private fun decodeEntity(text: String, start: Int): Pair<String, Int> {
        val semiIdx = text.indexOf(';', start)
        if (semiIdx < 0 || semiIdx - start > 10) return Pair("&", start + 1)
        val entity = text.substring(start + 1, semiIdx)
        val decoded = when (entity) {
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            "nbsp" -> "\u00A0"
            "mdash" -> "\u2014"
            "ndash" -> "\u2013"
            "hellip" -> "\u2026"
            "copy" -> "\u00A9"
            "reg" -> "\u00AE"
            "trade" -> "\u2122"
            "euro" -> "\u20AC"
            "pound" -> "\u00A3"
            "yen" -> "\u00A5"
            "cent" -> "\u00A2"
            "deg" -> "\u00B0"
            "plusmn" -> "\u00B1"
            "times" -> "\u00D7"
            "divide" -> "\u00F7"
            else -> {
                if (entity.startsWith("#x")) {
                    entity.substring(2).toIntOrNull(16)?.toChar()?.toString()
                } else if (entity.startsWith("#")) {
                    entity.substring(1).toIntOrNull()?.toChar()?.toString()
                } else null
            }
        }
        return Pair(decoded ?: "&$entity;", semiIdx + 1)
    }

    private fun AnnotatedString.Builder.endsWithNewline(): Boolean {
        val result = this.toAnnotatedString()
        return result.text.isNotEmpty() && result.text.last() == '\n'
    }

    private data class StyleEntry(
        val tagName: String,
        val style: SpanStyle?,
        val isHyperlink: Boolean = false
    )

    private val NAMED_COLORS = mapOf(
        "black" to Color(0xFF000000),
        "white" to Color(0xFFFFFFFF),
        "red" to Color(0xFFFF0000),
        "green" to Color(0xFF008000),
        "blue" to Color(0xFF0000FF),
        "yellow" to Color(0xFFFFFF00),
        "cyan" to Color(0xFF00FFFF),
        "magenta" to Color(0xFFFF00FF),
        "gray" to Color(0xFF808080),
        "grey" to Color(0xFF808080),
        "silver" to Color(0xFFC0C0C0),
        "maroon" to Color(0xFF800000),
        "olive" to Color(0xFF808000),
        "lime" to Color(0xFF00FF00),
        "aqua" to Color(0xFF00FFFF),
        "teal" to Color(0xFF008080),
        "navy" to Color(0xFF000080),
        "fuchsia" to Color(0xFFFF00FF),
        "purple" to Color(0xFF800080),
        "orange" to Color(0xFFFFA500),
        "pink" to Color(0xFFFFC0CB),
        "brown" to Color(0xFFA52A2A),
        "coral" to Color(0xFFFF7F50),
        "gold" to Color(0xFFFFD700),
        "indigo" to Color(0xFF4B0082),
        "khaki" to Color(0xFFF0E68C),
        "lavender" to Color(0xFFE6E6FA),
        "tomato" to Color(0xFFFF6347),
        "turquoise" to Color(0xFF40E0D0),
        "violet" to Color(0xFFEE82EE),
        "wheat" to Color(0xFFF5DEB3)
    )
}
