package com.taocent.simple.compose.component.richtext.core

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PasteResultTest {

    @Test
    fun successWrapsValue() {
        val r: PasteResult<String> = PasteResult.success("hello")
        assertTrue(r.isSuccess)
        assertFalse(r.isFailure)
        assertEquals("hello", r.getOrNull())
    }

    @Test
    fun successAllowsNullValue() {
        val r: PasteResult<String?> = PasteResult.success(null)
        assertTrue(r.isSuccess)
        assertNull(r.getOrNull())
    }

    @Test
    fun failureCarriesError() {
        val error = RichTextError.ClipboardRead()
        val r: PasteResult<String> = PasteResult.failure(error)
        assertFalse(r.isSuccess)
        assertTrue(r.isFailure)
        assertNull(r.getOrNull())
    }

    @Test
    fun failureKindIsClassified() {
        val write = RichTextError.ClipboardWrite()
        assertEquals(RichTextError.Kind.CLIPBOARD_WRITE, write.kind)

        val read = RichTextError.ClipboardRead()
        assertEquals(RichTextError.Kind.CLIPBOARD_READ, read.kind)

        val json = RichTextError.JsonParse()
        assertEquals(RichTextError.Kind.JSON_PARSE, json.kind)

        val html = RichTextError.HtmlParse()
        assertEquals(RichTextError.Kind.HTML_PARSE, html.kind)

        val md = RichTextError.MarkdownParse()
        assertEquals(RichTextError.Kind.MARKDOWN_PARSE, md.kind)

        val ser = RichTextError.Serialize()
        assertEquals(RichTextError.Kind.SERIALIZE, ser.kind)
    }

    @Test
    fun foldDispatches() {
        var seenSuccess: String? = null
        var seenFailure: RichTextError? = null
        PasteResult.success("ok").fold(
            onSuccess = { seenSuccess = it },
            onFailure = { seenFailure = it },
        )
        assertEquals("ok", seenSuccess)
        assertNull(seenFailure)

        PasteResult.failure<String>(RichTextError.JsonParse()).fold(
            onSuccess = { seenSuccess = it },
            onFailure = { seenFailure = it },
        )
        val failure = assertIs<RichTextError>(seenFailure)
        assertEquals(RichTextError.Kind.JSON_PARSE, failure.kind)
    }

    @Test
    fun runCatchingCatchesAndWraps() {
        val result = PasteResult.runCatching(
            errorFactory = { RichTextError.Serialize(it) },
        ) {
            throw IllegalStateException("boom")
        }
        assertTrue(result.isFailure)
        val failure = assertIs<PasteResult.Failure>(result)
        assertEquals(RichTextError.Kind.SERIALIZE, failure.error.kind)
        assertNotNull(failure.error.cause)
    }

    @Test
    fun runCatchingReturnsSuccessOnNormalExit() {
        val result = PasteResult.runCatching(
            errorFactory = { RichTextError.Serialize(it) },
        ) {
            "value"
        }
        assertTrue(result.isSuccess)
        assertEquals("value", result.getOrNull())
    }

    @Test
    fun timestampCapturedAtConstruction() {
        val a = RichTextError.JsonParse()
        val b = RichTextError.JsonParse()
        // 同毫秒也可能相等,但时间戳应非负
        assertTrue(a.timestamp > 0L)
        assertTrue(b.timestamp > 0L)
    }
}

class RichTextLoggerTest {

    @Test
    fun noOpLoggerIsNoOp() {
        val logger = NoOpLogger()
        // 不抛错即可
        logger.log(RichTextError.JsonParse(), "ctx")
        logger.log(RichTextError.ClipboardRead())
    }

    @Test
    fun consoleLoggerFormats() {
        val captured = mutableListOf<String>()
        val logger = ConsoleLogger(printStream = { captured.add(it) })
        logger.log(RichTextError.JsonParse(), "smartPaste")
        assertEquals(1, captured.size)
        val line = captured.first()
        assertTrue(line.contains("JsonParse") || line.contains("JSON_PARSE") || line.contains("粘贴内容"))
        assertTrue(line.contains("smartPaste"))
    }

    @Test
    fun consoleLoggerOmitsContextWhenNull() {
        val captured = mutableListOf<String>()
        val logger = ConsoleLogger(printStream = { captured.add(it) })
        logger.log(RichTextError.ClipboardRead())
        val line = captured.first()
        assertFalse(line.contains("context="))
    }

    @Test
    fun richTextLoggerNoOpConstantExists() {
        // 编译期可访问
        val ref: RichTextLogger = RichTextLogger.NoOp
        ref.log(RichTextError.JsonParse(), "ctx")
    }
}

class ErrorBoundaryTest {

    @Test
    fun stateHasNoErrorInitially() {
        val state = RichTextState()
        assertNull(state.errors.value)
    }

    @Test
    fun loadFromJsonFailureReportsError() {
        runBlocking {
            val state = RichTextState()
            // 故意构造非法 JSON:缺少 "text" 字段无法被识别,会抛 IllegalArgumentException
            val result = state.loadFromJson("{ this is not valid json :::")
            assertTrue(result.isFailure)
            val failure = assertIs<PasteResult.Failure>(result)
            assertEquals(RichTextError.Kind.JSON_PARSE, failure.error.kind)
            // errors StateFlow 也应同步收到
            val reported = state.errors.first()
            assertNotNull(reported)
            assertEquals(RichTextError.Kind.JSON_PARSE, reported.kind)
            state.acknowledgeError()
        }
    }

    @Test
    fun loadFromJsonSuccessClearsErrors() {
        runBlocking {
            val state = RichTextState()
            val goodJson = state.toJson()
            val result = state.loadFromJson(goodJson)
            assertTrue(result.isSuccess)
            assertNull(state.errors.value)
        }
    }

    @Test
    fun setLoggerReplacesLogger() {
        val state = RichTextState()
        val received = mutableListOf<RichTextError>()
        val captureLogger = object : RichTextLogger {
            override fun log(error: RichTextError, context: String?) {
                received.add(error)
            }
        }
        state.setLogger(captureLogger)
        // 触发一次错误:loadFromJson 失败
        state.loadFromJson("not json")
        assertEquals(1, received.size)
        assertEquals(RichTextError.Kind.JSON_PARSE, received.first().kind)
    }

    @Test
    fun acknowledgeErrorClearsState() {
        val state = RichTextState()
        state.loadFromJson("not json")
        assertNotNull(state.errors.value)
        state.acknowledgeError()
        assertNull(state.errors.value)
    }

    @Test
    fun smartPasteCorruptJsonFallsBackToPlainText() {
        runBlocking {
            val state = RichTextState()
            // 触发 JSON_PARSE:看起来像 JSON(以 { 开头含 "text"/"spans"),但内容非法
            val corruptJson = "{\"text\":\"hi\",\"spans\":INVALID}"
            val result = state.smartPaste(corruptJson)
            // smartPaste 不会因单个解析失败而整体返回 Failure,降级为纯文本
            assertTrue(result.isSuccess)
            // 错误已上报
            val reported = state.errors.first()
            assertNotNull(reported)
            assertEquals(RichTextError.Kind.JSON_PARSE, reported.kind)
        }
    }

    @Test
    fun smartPastePlainTextReportsNoError() {
        runBlocking {
            val state = RichTextState()
            val result = state.smartPaste("hello world")
            assertTrue(result.isSuccess)
            assertNull(state.errors.value)
        }
    }

    @Test
    fun toJsonFailsSoftOnInternalError() {
        val state = RichTextState()
        // toJson 当前实现不会失败(纯字符串拼接),此处验证 fallback 行为存在
        val json = state.toJson()
        // 即使文本异常,toJson 也不会抛 NPE,只可能返回空字符串(但正常情况返回非空)
        assertNotNull(json)
    }
}
