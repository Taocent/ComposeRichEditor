package com.taocent.simple.compose.component.richtext.core.internal.serialization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

internal object MarkdownToAnnotatedString {

    fun parse(markdown: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = markdown.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()

            when {
                trimmed.matches(Regex("^#{1,6}\\s+.*")) -> {
                    val level = trimmed.indexOf(' ')
                    val text = trimmed.substring(level + 1)
                    val fontSize = when (level) {
                        1 -> 24.sp
                        2 -> 20.sp
                        3 -> 18.sp
                        4 -> 16.sp
                        5 -> 14.sp
                        else -> 13.sp
                    }
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize))
                    parseInline(builder, text)
                    builder.pop()
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    val codeText = codeLines.joinToString("\n")
                    builder.pushStyle(
                        SpanStyle(
                            background = Color(0x1A808080),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                    builder.append(codeText)
                    builder.pop()
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.startsWith("> ") -> {
                    builder.pushStyle(
                        SpanStyle(
                            color = Color(0xFF666666),
                            fontStyle = FontStyle.Italic
                        )
                    )
                    builder.append("\u2502 ")
                    parseInline(builder, trimmed.substring(2))
                    builder.pop()
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.matches(Regex("^[-*+]\\s+.*")) -> {
                    val text = trimmed.substring(trimmed.indexOf(' ') + 1)
                    builder.append("\u2022 ")
                    parseInline(builder, text)
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val dotIdx = trimmed.indexOf('.')
                    val num = trimmed.substring(0, dotIdx)
                    val text = trimmed.substring(trimmed.indexOf(' ', dotIdx) + 1)
                    builder.append("$num. ")
                    parseInline(builder, text)
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.matches(Regex("^[-*_]{3,}\\s*$")) -> {
                    builder.pushStyle(SpanStyle(color = Color(0xFFCCCCCC)))
                    builder.append("\u2500".repeat(30))
                    builder.pop()
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> {
                    val checked = trimmed.startsWith("- [x] ")
                    val text = trimmed.substring(6)
                    builder.append(if (checked) "\u2611 " else "\u2610 ")
                    parseInline(builder, text)
                    if (i < lines.size - 1) builder.append('\n')
                }

                trimmed.isNotEmpty() -> {
                    parseInline(builder, trimmed)
                    if (i < lines.size - 1) builder.append('\n')
                }

                else -> {
                    builder.append('\n')
                }
            }
            i++
        }

        return builder.toAnnotatedString()
    }

    private fun parseInline(builder: AnnotatedString.Builder, text: String) {
        var i = 0
        while (i < text.length) {
            when {
                i + 1 < text.length && text[i] == '*' && text[i + 1] == '*' -> {
                    val end = findClosingDelimiter(text, i + 2, "**")
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        parseInline(builder, text.substring(i + 2, end))
                        builder.pop()
                        i = end + 2
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                i + 1 < text.length && text[i] == '_' && text[i + 1] == '_' -> {
                    val end = findClosingDelimiter(text, i + 2, "__")
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        parseInline(builder, text.substring(i + 2, end))
                        builder.pop()
                        i = end + 2
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '*' -> {
                    val end = findClosingDelimiter(text, i + 1, "*")
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        parseInline(builder, text.substring(i + 1, end))
                        builder.pop()
                        i = end + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '_' && (i == 0 || !text[i - 1].isLetterOrDigit()) -> {
                    val end = findClosingDelimiter(text, i + 1, "_")
                    if (end > 0 && (end + 1 >= text.length || !text[end + 1].isLetterOrDigit())) {
                        builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        parseInline(builder, text.substring(i + 1, end))
                        builder.pop()
                        i = end + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                i + 1 < text.length && text[i] == '~' && text[i + 1] == '~' -> {
                    val end = findClosingDelimiter(text, i + 2, "~~")
                    if (end > 0) {
                        builder.pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        parseInline(builder, text.substring(i + 2, end))
                        builder.pop()
                        i = end + 2
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > 0) {
                        val code = text.substring(i + 1, end)
                        builder.pushStyle(
                            SpanStyle(
                                background = Color(0x1A808080),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                        builder.append(code)
                        builder.pop()
                        i = end + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '!' && i + 1 < text.length && text[i + 1] == '[' -> {
                    val closeBracket = text.indexOf(']', i + 2)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && closeParen > 0) {
                        val alt = text.substring(i + 2, closeBracket)
                        builder.append(alt)
                        i = closeParen + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '[' -> {
                    val closeBracket = text.indexOf(']', i + 1)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen > 0) text.indexOf(')', openParen) else -1
                    if (closeBracket > 0 && closeParen > 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(openParen + 1, closeParen)
                        val start = builder.length
                        builder.pushStyle(
                            SpanStyle(
                                color = androidx.compose.ui.graphics.Color(0xFF1E88E5),
                                textDecoration = TextDecoration.Underline
                            )
                        )
                        builder.append(linkText)
                        builder.pop()
                        builder.addStringAnnotation("URL", url, start, builder.length)
                        i = closeParen + 1
                    } else {
                        builder.append(text[i])
                        i++
                    }
                }

                text[i] == '<' && i + 1 < text.length -> {
                    val closeIdx = text.indexOf('>', i + 1)
                    if (closeIdx > 0) {
                        val tag = text.substring(i + 1, closeIdx)
                        if (tag == "br" || tag == "br/") {
                            builder.append('\n')
                            i = closeIdx + 1
                            continue
                        }
                        if (tag.startsWith("a ")) {
                            val hrefMatch = Regex("""href=["']([^"']+)["']""").find(tag)
                            val href = hrefMatch?.groupValues?.get(1) ?: ""
                            val endTag = text.indexOf("</a>", closeIdx)
                            if (endTag > 0) {
                                val linkText = text.substring(closeIdx + 1, endTag)
                                val start = builder.length
                                builder.pushStyle(
                                    SpanStyle(
                                        color = androidx.compose.ui.graphics.Color(0xFF1E88E5),
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                                builder.append(linkText)
                                builder.pop()
                                builder.addStringAnnotation("URL", href, start, builder.length)
                                i = endTag + 4
                                continue
                            }
                        }
                    }
                    builder.append(text[i])
                    i++
                }

                else -> {
                    builder.append(text[i])
                    i++
                }
            }
        }
    }

    private fun findClosingDelimiter(text: String, fromIndex: Int, delimiter: String): Int {
        var i = fromIndex
        while (i <= text.length - delimiter.length) {
            if (text.substring(i, i + delimiter.length) == delimiter) {
                if (delimiter.length == 1 && i + 1 < text.length && text[i + 1] == delimiter[0]) {
                    i += 2
                    continue
                }
                return i
            }
            i++
        }
        return -1
    }
}
