package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider

/**
 * 永远不弹任何菜单的 [TextContextMenuProvider]:在编辑器顶层通过
 * [androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider]
 * 和 [androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider]
 * 提供后,可以屏蔽:
 * - Android 1.11+ 新机制下的 `view.startActionMode(...)` 系统 ActionMode
 *   (被 [androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider]
 *   默认值 [AndroidTextContextMenuToolbarProvider] 触发)
 * - iOS / Desktop 选中文字后弹的 Compose 浮层 / iOS EditMenu
 *
 * 必须 `public` —— `internal` 在 KMP 中不会跨 module 共享,本对象要暴露给
 * 上层编辑器(richtext / blockrichtext)使用。
 *
 * 注意:
 * - Compose 1.11 起,`isNewContextMenuEnabled` 默认 `true`(androidx 1.11.1),
 *   `TextField` 选词后**不再**走旧的 [androidx.compose.ui.platform.TextToolbar]
 *   路径,而是走 `LocalTextContextMenuToolbarProvider`。
 *   因此上一轮的 [EmptyTextToolbar] 已经不能屏蔽这些平台,本对象才是 1.11 起的主拦截点。
 * - 但 KMP 1.11.0(Dektop / iOS / js / wasmJs)目前 `isNewContextMenuEnabled = false`,
 *   旧机制仍生效,所以 [EmptyTextToolbar] 在 KMP 1.11.0 下仍是必要的 fallback。
 */
object EmptyTextContextMenuProvider : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
    }
}

class CallbackTextContextMenuProvider(
    private val onShow: () -> Unit
) : TextContextMenuProvider {
    /**
     * 吞掉标记:为 true 时下一次 [showTextContextMenu] 调用会被 no-op 掉并清除标记。
     *
     * 用途 —— Android "长按显示浮动工具栏 → 点击隐藏" 场景:
     * - 长按时 BasicTextField 自己会等待 long-press timeout(~500ms)然后调用
     *   [showTextContextMenu],我们借此时机显示工具栏。
     * - 用户看到工具栏后再次按住(同位置)想"点掉"工具栏,我们的 Press handler 在 down 时
     *   主动 hide 工具栏;但用户手指通常不会立刻松开,BasicTextField 会判定为新一次 long-press,
     *   紧跟着再次调用 [showTextContextMenu],导致工具栏被重新显示,看起来"点不掉"。
     * - 解决:Press handler 在 down 时如果发现工具栏当前已显示,先调 [suppressNext] 把
     *   BasicTextField 紧跟着会触发的 [showTextContextMenu] 吞掉。
     *
     * 注意:首次长按时(工具栏未显示)不要调用本方法,否则首次 long-press 触发的
     * [showTextContextMenu] 也会被吞,工具栏就显示不出来了。
     */
    private var pendingSuppress: Boolean = false

    fun suppressNext() {
        pendingSuppress = true
    }

    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        if (pendingSuppress) {
            pendingSuppress = false
            return
        }
        onShow()
    }
}
