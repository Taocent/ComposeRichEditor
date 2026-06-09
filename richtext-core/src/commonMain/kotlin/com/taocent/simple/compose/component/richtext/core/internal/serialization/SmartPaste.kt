package com.taocent.simple.compose.component.richtext.core.internal.serialization

import androidx.compose.ui.text.AnnotatedString
import com.taocent.simple.compose.component.richtext.core.RichTextError
import com.taocent.simple.compose.component.richtext.core.paste.PasteErrorReporter
import com.taocent.simple.compose.component.richtext.core.paste.PasteParser
import com.taocent.simple.compose.component.richtext.core.paste.PasteParserPipeline
import com.taocent.simple.compose.component.richtext.core.paste.RichDocumentFragment
import com.taocent.simple.compose.component.richtext.core.paste.defaultPasteParsers

internal typealias SmartPasteErrorReporter = (RichTextError, String) -> Unit

internal object SmartPaste {

    fun detectAndParse(text: String, errorReporter: SmartPasteErrorReporter? = null): AnnotatedString {
        return detectAndParseFragment(text, errorReporter).annotatedString
    }

    fun detectAndParse(
        text: String,
        errorReporter: SmartPasteErrorReporter?,
        jsonEnabled: Boolean,
        htmlEnabled: Boolean,
        markdownEnabled: Boolean,
        extraParsers: List<PasteParser> = emptyList(),
    ): AnnotatedString {
        return detectAndParseFragment(
            text = text,
            errorReporter = errorReporter,
            jsonEnabled = jsonEnabled,
            htmlEnabled = htmlEnabled,
            markdownEnabled = markdownEnabled,
            extraParsers = extraParsers,
        ).annotatedString
    }

    fun detectAndParseFragment(
        text: String,
        errorReporter: SmartPasteErrorReporter? = null,
        jsonEnabled: Boolean = true,
        htmlEnabled: Boolean = true,
        markdownEnabled: Boolean = true,
        extraParsers: List<PasteParser> = emptyList(),
    ): RichDocumentFragment {
        val reporter = errorReporter?.let { callback ->
            PasteErrorReporter { error, source -> callback(error, source) }
        }
        return PasteParserPipeline(extraParsers + defaultPasteParsers(jsonEnabled, htmlEnabled, markdownEnabled))
            .parse(text, reporter)
    }
}
