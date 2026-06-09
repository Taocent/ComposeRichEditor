package com.taocent.simple.compose.component.richtext.core

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * WasmJS 平台:返回 `Clock.System.now()` 转 epoch millis。
 *
 * 注意:在 Kotlin 2.3 的 `wasmJs` 目标中,`kotlin.js.Date` 已从 `kotlin.js` 包移除(未解析),
 * 改用 `kotlin.time.Clock.System.now().toEpochMilliseconds()`。
 */
@OptIn(ExperimentalTime::class)
internal actual fun currentTimeMillis(): Long =
    Clock.System.now().toEpochMilliseconds()
