package com.taocent.simple.compose.component.richtext.core.internal.platform

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

/**
 * iOS 平台:返回 status bar / dynamic island 的 top inset 像素。
 *
 * 浮动工具栏在传给 `Popup.calculatePosition` 之前应从这个值中减掉,使其与
 * `calculatePosition` 期望的 "without insets" 坐标系对齐,避免工具栏偏下盖住文本。
 *
 * 详见 commonMain 注释。
 */
@Composable
internal actual fun rememberPopupWindowPositionOffset(): IntOffset {
    val density = LocalDensity.current
    val statusBarsTopPx = WindowInsets.statusBars.getTop(density)
    return IntOffset(x = 0, y = statusBarsTopPx)
}
