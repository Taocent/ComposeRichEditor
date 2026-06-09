package com.taocent.simple.compose.component.richtext.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

private object WasmJsRichTextPlatformAdapter : RichTextPlatformAdapter {
    override val supportsSystemContextMenu: Boolean = true
    override val supportsRichClipboard: Boolean = false
    override val supportsImeComposition: Boolean = true
    override val requiresCollapsedCursorContextToolbarToggle: Boolean = false

    override fun Modifier.suppressNativeContextMenu(onContextMenuRequested: () -> Unit): Modifier = this
}

@Composable
actual fun rememberRichTextPlatformAdapter(): RichTextPlatformAdapter {
    return remember { WasmJsRichTextPlatformAdapter }
}
