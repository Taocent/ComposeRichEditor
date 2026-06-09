package com.taocent.simple.compose.component.richtext.core.paste

import androidx.compose.ui.text.AnnotatedString
import com.taocent.simple.compose.component.richtext.core.PasteResult
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.document.BlockModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PasteParserPipelineTest {

    @Test
    fun customParserHasPriorityOverDefaultParsers() {
        val parser = object : PasteParser {
            override val name: String = "custom"

            override fun canParse(input: String): Boolean = input == "# title"

            override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
                return PasteResult.success(RichDocumentFragment.fromAnnotatedString(AnnotatedString("custom result")))
            }
        }

        val fragment = PasteParserPipeline(listOf(parser, MarkdownPasteParser, PlainTextPasteParser)).parse("# title")

        assertEquals("custom result", fragment.annotatedString.text)
    }

    @Test
    fun urlParserCreatesLinkAnnotation() {
        val url = "https://example.com"

        val fragment = PasteParserPipeline(defaultPasteParsers()).parse(url)

        assertEquals(url, fragment.annotatedString.text)
        assertEquals(url, fragment.annotatedString.getStringAnnotations("url", 0, url.length).single().item)
    }

    @Test
    fun tableParserCreatesTableDocumentModel() {
        val fragment = PasteParserPipeline(defaultPasteParsers()).parse("a\tb\nc\td")
        val table = assertIs<BlockModel.Table>(fragment.documentModel?.blocks?.single())

        assertEquals(2, table.rows.size)
        assertEquals(2, table.rows.first().cells.size)
    }

    @Test
    fun codeParserCreatesMonospaceAnnotatedString() {
        val fragment = PasteParserPipeline(defaultPasteParsers()).parse("```\nval a = 1\n```")

        assertEquals("val a = 1", fragment.annotatedString.text)
        assertTrue(fragment.annotatedString.spanStyles.isNotEmpty())
    }

    @Test
    fun richTextStateUsesInjectedPasteParser() {
        val state = RichTextState()
        val parser = object : PasteParser {
            override val name: String = "custom"

            override fun canParse(input: String): Boolean = input == "custom"

            override fun parse(input: String, errorReporter: PasteErrorReporter?): PasteResult<RichDocumentFragment> {
                return PasteResult.success(RichDocumentFragment.fromAnnotatedString(AnnotatedString("parsed")))
            }
        }

        state.smartPaste("custom", parsers = listOf(parser))

        assertEquals("parsed", state.plainText)
    }
}
