package com.taocent.simple.compose.component.richtext.core.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * Android 平台:`positionInWindow()` 已经处于 "without insets" 坐标系,
 * 详见 commonMain 注释。
 */
@Composable
internal actual fun rememberPopupWindowPositionOffset(): IntOffset = IntOffset.Zero
