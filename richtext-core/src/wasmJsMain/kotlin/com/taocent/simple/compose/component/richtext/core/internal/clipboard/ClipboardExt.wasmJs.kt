@file:OptIn(ExperimentalWasmJsInterop::class)

package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString
import kotlinx.coroutines.await

@JsName("readBlobAsText")
private val readBlobAsTextFn: (JsAny) -> Promise<JsString> = js("(b) => b.text()")

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(): ClipEntry =
    ClipEntry.withPlainText(text)

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString? {
    val item = clipboardItems.toArray().firstOrNull() ?: return null
    val blob = try {
        item.getType("text/plain".toJsString()).await<JsAny>()
    } catch (_: Throwable) {
        return null
    }
    val raw: String = try {
        readBlobAsTextFn(blob).await<JsString>().toString()
    } catch (_: Throwable) {
        return null
    }
    return raw.takeIf { it.isNotEmpty() }?.let { AnnotatedString(it) }
}
