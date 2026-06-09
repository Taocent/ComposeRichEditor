package com.taocent.simple.compose.component.richtext.core.paste

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import com.taocent.simple.compose.component.richtext.core.PasteResult
import com.taocent.simple.compose.component.richtext.core.RichTextError
import com.taocent.simple.compose.component.richtext.core.document.BlockModel
import com.taocent.simple.compose.component.richtext.core.document.DocumentModel
import com.taocent.simple.compose.component.richtext.core.document.InlineModel
import com.taocent.simple.compose.component.richtext.core.document.TableCellModel
import com.taocent.simple.compose.component.richtext.core.document.TableRowModel
import com.taocent.simple.compose.component.richtext.core.internal.serialization.HtmlToAnnotatedString
import com.taocent.simple.compose.component.richtext.core.internal.serialization.MarkdownToAnnotatedString
import com.taocent.simple.compose.component.richtext.core.internal.serialization.RichTextSerializer

object JsonPasteParser : PasteParser {
    override val name: String = "json"

    override fun canParse(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith('{') &&
            trimmed.endsWith('}') &&
            trimmed.contains("\"text\"") &&
            (trimmed.contains("\"spans\"") || trimmed.contains("\"annotations\""))
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        return try {
            PasteResult.success(RichDocumentFragment.fromAnnotatedString(RichTextSerializer.fromJson(input)))
        } catch (e: Throwable) {
            val error = RichTextError.JsonParse(e)
            errorReporter?.report(error, "JsonPasteParser.parse")
            PasteResult.failure(error)
        }
    }
}

object HtmlPasteParser : PasteParser {
    override val name: String = "html"

    override fun canParse(input: String): Boolean {
        if (!input.contains('<') || !input.contains('>')) return false
        val htmlTagPattern = Regex("""</?[a-zA-Z][^>]*>""")
        val matches = htmlTagPattern.findAll(input).toList()
        if (matches.isEmpty()) return false
        val commonTags = setOf(
            "p", "div", "span", "b", "strong", "i", "em", "u", "a",
            "h1", "h2", "h3", "h4", "h5", "h6", "br", "hr",
            "ul", "ol", "li", "table", "tr", "td", "th",
            "blockquote", "pre", "code", "sup", "sub", "del", "s",
            "font", "img", "mark", "small", "big"
        )
        val foundTags = matches.map { match ->
            match.value.removePrefix("</").removePrefix("<").substringBefore('>').substringBefore('/').lowercase()
        }.toSet()
        return foundTags.any { it in commonTags }
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        return try {
            PasteResult.success(RichDocumentFragment.fromAnnotatedString(HtmlToAnnotatedString.parse(input)))
        } catch (e: Throwable) {
            val error = RichTextError.HtmlParse(e)
            errorReporter?.report(error, "HtmlPasteParser.parse")
            PasteResult.failure(error)
        }
    }
}

object MarkdownPasteParser : PasteParser {
    override val name: String = "markdown"

    override fun canParse(input: String): Boolean {
        var score = 0
        val lines = input.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.matches(Regex("^#{1,6}\\s+.*")) -> score += 3
                trimmed.startsWith("```") -> score += 3
                trimmed.matches(Regex("^[-*+]\\s+.*")) -> score += 1
                trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> score += 1
                trimmed.startsWith("> ") -> score += 2
                trimmed.matches(Regex("^[-*_]{3,}\\s*$")) -> score += 2
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") -> score += 2
            }
        }
        val inlinePatterns = listOf(
            Regex("""\*\*[^*]+\*\*"""),
            Regex("""__[^_]+__"""),
            Regex("""(?<!\*)\*(?!\*)[^*]+\*(?!\*)"""),
            Regex("""(?<!_)_(?!_)[^_]+_(?!_)"""),
            Regex("""~~[^~]+~~"""),
            Regex("""`[^`]+`"""),
            Regex("""\[[^\]]+]\([^)]+\)"""),
        )
        for (pattern in inlinePatterns) {
            score += pattern.findAll(input).count() * 2
        }
        return score >= 4
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        return try {
            PasteResult.success(RichDocumentFragment.fromAnnotatedString(MarkdownToAnnotatedString.parse(input)))
        } catch (e: Throwable) {
            val error = RichTextError.MarkdownParse(e)
            errorReporter?.report(error, "MarkdownPasteParser.parse")
            PasteResult.failure(error)
        }
    }
}

object UrlPasteParser : PasteParser {
    override val name: String = "url"

    override fun canParse(input: String): Boolean {
        val trimmed = input.trim()
        return !trimmed.contains('\n') && Regex("""https?://[^\s]+""").matches(trimmed)
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        val trimmed = input.trim()
        val annotated = AnnotatedString.Builder(trimmed).apply {
            addStringAnnotation("url", trimmed, 0, trimmed.length)
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), 0, trimmed.length)
        }.toAnnotatedString()
        return PasteResult.success(RichDocumentFragment.fromAnnotatedString(annotated))
    }
}

object TablePasteParser : PasteParser {
    override val name: String = "table"

    override fun canParse(input: String): Boolean {
        val lines = input.trimEnd().lines().filter { it.isNotEmpty() }
        return lines.size >= 2 && lines.all { it.contains('\t') }
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        val rows = input.trimEnd().lines().filter { it.isNotEmpty() }.map { line ->
            TableRowModel(
                cells = line.split('\t').map { cell ->
                    TableCellModel(
                        document = DocumentModel(
                            blocks = listOf(
                                BlockModel.Text(
                                    inlineContent = InlineModel(text = cell)
                                )
                            )
                        )
                    )
                }
            )
        }
        val document = DocumentModel(blocks = listOf(BlockModel.Table(rows = rows)))
        return PasteResult.success(
            RichDocumentFragment(
                annotatedString = AnnotatedString(input),
                documentModel = document
            )
        )
    }
}

object CodePasteParser : PasteParser {
    override val name: String = "code"

    override fun canParse(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("```") && trimmed.endsWith("```")
    }

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        val content = input.trim().removePrefix("```").removeSuffix("```").trim('\n')
        val annotated = AnnotatedString.Builder(content).apply {
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace), 0, content.length)
        }.toAnnotatedString()
        return PasteResult.success(RichDocumentFragment.fromAnnotatedString(annotated))
    }
}

object PlainTextPasteParser : PasteParser {
    override val name: String = "plain_text"

    override fun canParse(input: String): Boolean = true

    override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
        return PasteResult.success(RichDocumentFragment.fromAnnotatedString(AnnotatedString(input)))
    }
}

fun defaultPasteParsers(
    jsonEnabled: Boolean = true,
    htmlEnabled: Boolean = true,
    markdownEnabled: Boolean = true
): List<PasteParser> {
    return buildList {
        if (jsonEnabled) add(JsonPasteParser)
        if (htmlEnabled) add(HtmlPasteParser)
        if (markdownEnabled) add(MarkdownPasteParser)
        add(UrlPasteParser)
        add(TablePasteParser)
        add(CodePasteParser)
        add(PlainTextPasteParser)
    }
}
