package com.taocent.simple.compose.component.richtext.core

import kotlin.js.Date

/**
 * JS/WasmJS 平台:返回 Date.now()(epoch millis)。
 */
internal actual fun currentTimeMillis(): Long = Date.now().toLong()
