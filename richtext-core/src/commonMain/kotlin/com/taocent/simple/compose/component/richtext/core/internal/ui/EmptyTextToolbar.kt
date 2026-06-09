package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * 永远隐藏的 [TextToolbar]:在编辑器顶层通过 [androidx.compose.ui.platform.LocalTextToolbar]
 * 提供后,可以屏蔽 Compose 1.10 及更早 / KMP 1.11.0(Dektop / iOS / js / wasmJs)中
 * `isNewContextMenuEnabled = false` 时的旧机制路径。
 *
 * 必须 `public` —— `internal` 在 KMP 中不会跨 module 共享,而 [LocalTextToolbar]
 * 在 [androidx.compose.ui] 中定义为 internal default,需要编辑器模块显式覆盖,因此本对象
 * 必须对 [androidx.compose.ui] 可见,只能是 public。
 *
 * **注意**:
 * - Compose 1.11+ 中,当 `isNewContextMenuEnabled = true`(androidx 1.11.1 默认),
 *   `TextField` 选词后**不再**走 [TextToolbar] 路径,而是走
 *   [androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider]
 *   / [androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider]。
 *   在这些场景下,本对象**不能**屏蔽系统工具栏,应由
 *   [EmptyTextContextMenuProvider] 来做主拦截。
 * - 因此编辑器顶层需要同时提供本对象和 [EmptyTextContextMenuProvider],覆盖两套机制。
 */
object EmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus
        get() = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
    }

    override fun hide() {
    }
}

class CallbackTextToolbar(
    private val onShow: () -> Unit
) : TextToolbar {
    override val status: TextToolbarStatus
        get() = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        onShow()
    }

    override fun hide() {
    }
}
