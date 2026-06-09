package com.taocent.simple.compose.component.richtext.core.paste

import com.taocent.simple.compose.component.richtext.core.PasteResult
import com.taocent.simple.compose.component.richtext.core.RichTextError

fun interface PasteErrorReporter {
    fun report(error: RichTextError, source: String)
}

interface PasteParser {
    val name: String

    fun canParse(input: String): Boolean

    fun parse(input: String, errorReporter: PasteErrorReporter? = null): PasteResult<RichDocumentFragment>
}

class PasteParserPipeline(
    private val parsers: List<PasteParser>
) {
    fun parse(input: String, errorReporter: PasteErrorReporter? = null): RichDocumentFragment {
        val parser = parsers.firstOrNull { it.canParse(input) } ?: PlainTextPasteParser
        return when (val result = parser.parse(input, errorReporter)) {
            is PasteResult.Success -> result.value
            is PasteResult.Failure -> {
                errorReporter?.report(result.error, "PasteParserPipeline.${parser.name}")
                PlainTextPasteParser.parse(input, errorReporter).getOrNull()
                    ?: RichDocumentFragment.fromAnnotatedString(androidx.compose.ui.text.AnnotatedString(input))
            }
        }
    }
}
