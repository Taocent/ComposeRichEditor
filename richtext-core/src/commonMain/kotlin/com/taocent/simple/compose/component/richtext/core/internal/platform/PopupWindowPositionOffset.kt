package com.taocent.simple.compose.component.richtext.core.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

/**
 * 浮动工具栏位置修正值(像素,需要在传给 `Popup.calculatePosition` 之前从
 * `textFieldGlobalPosition` 中减去)。
 *
 * # 根因
 *
 * CMP 1.11.x 的 `Popup` 内部在调用 `popupPositionProvider.calculatePosition` 时
 * 传入的是 **"without insets"** 坐标系(`boundsWithoutInsets` /
 * `sizeWithoutInsets`),最终 `layer.boundsInWindow` 会再
 * `+ platformInsets.top / left`,所以 `calculatePosition` 期望 anchor 处于
 * "以 safe-area top-left 为原点"的坐标系。
 *
 * 但 iOS 的 `positionInWindow()` 内部走
 * `PlatformWindowContext.convertLocalToWindowPosition(...)` →
 * `UIView.convert(point, to: window)`,返回的 y 是 **以 UIWindow 0,0 为原点**
 * (从屏幕顶部算起,包含 status bar / dynamic island),与 Popup 期望的坐标系错开
 * `WindowInsets.statusBars.getTop(density)` 像素。
 *
 * 结果:Popup 实际渲染位置
 * ```
 *   = (textFieldGlobalPosition.y + anchorTop - popupHeight - topPadding - 0)
 *     + statusBars.top
 *   = 期望 y + statusBars.top
 * ```
 * 即工具栏偏下整个 status bar 高度,直接盖住文本。
 *
 * # 修法
 *
 * iOS 平台返回 `IntOffset(0, statusBars.getTop(density))`,使用方在计算
 * `anchorXInWindow / anchorTopInWindow` 时减去这个值,使其与 `calculatePosition`
 * 期望的 "without insets" 坐标系对齐。
 *
 * Android / Desktop / Web 上 `positionInWindow()` 已经处于 "without insets"
 * 坐标系,不需要修正,返回 [IntOffset.Zero]。
 */
@Composable
internal expect fun rememberPopupWindowPositionOffset(): IntOffset
