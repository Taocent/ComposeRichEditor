package com.taocent.simple.compose.component.richtext.core.internal.toolbar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import com.taocent.simple.compose.component.richtext.core.RichTextState

/**
 * 文本字段报告给"全局浮动工具栏"的活动信息。
 *
 * 编辑器持有的 [FloatingToolbarHostState] 会把当前拥有非折叠选区的文本字段
 * 信息(状态、位置、尺寸、选区锚点、是否在拖动)聚合到一份 [active] 上,顶层只渲染
 * 一个 [FloatingToolbar] 实例,避免每块/每格重复创建。
 */
data class FloatingToolbarActiveInfo(
    val state: RichTextState,
    val position: IntOffset,
    val widthPx: Int,
    val heightPx: Int,
    val anchor: IntOffset,
    val isDragging: Boolean,
)

/**
 * 全局浮动工具栏的宿主状态。
 *
 * - 每个 [com.taocent.simple.compose.component.blockrichtext.RichTextTextField]
 *   通过 [LocalFloatingToolbarHost] 拿到宿主,把自己的选区变化上报到 [active]。
 * - 顶层编辑器渲染一个 [FloatingToolbar],只显示 [active] 不为 null 的情况。
 * - 文本字段离开组合(滚动出视口、被删除)时通过 [clearActive] 清理,
 *   避免宿主持有已死状态的引用。
 */
@Stable
class FloatingToolbarHostState {
    var active: FloatingToolbarActiveInfo? by mutableStateOf(null)
        private set

    fun reportActive(info: FloatingToolbarActiveInfo) {
        active = info
    }

    fun updateActivePosition(
        state: RichTextState,
        position: IntOffset,
        widthPx: Int,
        heightPx: Int,
        anchor: IntOffset,
    ) {
        val current = active ?: return
        if (current.state !== state) return
        active = current.copy(
            position = position,
            widthPx = widthPx,
            heightPx = heightPx,
            anchor = anchor,
        )
    }

    fun clearActive(state: RichTextState) {
        if (active?.state === state) {
            active = null
        }
    }

    fun dismiss() {
        active = null
    }
}

/**
 * 全局浮动工具栏宿主 CompositionLocal。
 *
 * 文本字段不要求一定处于一个宿主下:独立使用 [com.taocent.simple.compose.component.blockrichtext.RichTextTextField]
 * 时可以不提供,字段不会主动上报;但在编辑器(块级 / 块级富文本)中,
 * 必须由顶层 [CompositionLocalProvider] 提供一份,否则工具栏无法定位当前选区。
 */
val LocalFloatingToolbarHost = compositionLocalOf<FloatingToolbarHostState?> { null }

/**
 * 默认无 host;独立使用 [com.taocent.simple.compose.component.blockrichtext.RichTextTextField]
 * 时不强制要求提供,此时字段不会上报选区、也不会渲染浮动工具栏。
 * 编辑器中必须由顶层 [CompositionLocalProvider] 提供一份以激活工具栏。
 */
