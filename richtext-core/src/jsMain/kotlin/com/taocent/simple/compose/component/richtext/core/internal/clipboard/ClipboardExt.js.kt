package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.js
import kotlinx.coroutines.await

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(): ClipEntry =
    ClipEntry.withPlainText(text)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
actual suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString? {
    val item = clipboardItems.firstOrNull() ?: return null
    val blob = try {
        item.getType("text/plain".toJsString()).await()
    } catch (_: Throwable) {
        return null
    }
    val raw = try {
        js("blob.text()").unsafeCast<Promise<String>>().await()
    } catch (_: Throwable) {
        return null
    }
    return raw.takeIf { it.isNotEmpty() }?.let { AnnotatedString(it) }
}
