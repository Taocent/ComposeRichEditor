package com.taocent.simple.compose.component.richtext.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

private object AndroidRichTextPlatformAdapter : RichTextPlatformAdapter {
    override val supportsSystemContextMenu: Boolean = true
    override val supportsRichClipboard: Boolean = true
    override val supportsImeComposition: Boolean = true
    override val requiresCollapsedCursorContextToolbarToggle: Boolean = true

    override fun Modifier.suppressNativeContextMenu(onContextMenuRequested: () -> Unit): Modifier = this
}

@Composable
actual fun rememberRichTextPlatformAdapter(): RichTextPlatformAdapter {
    return remember { AndroidRichTextPlatformAdapter }
}
