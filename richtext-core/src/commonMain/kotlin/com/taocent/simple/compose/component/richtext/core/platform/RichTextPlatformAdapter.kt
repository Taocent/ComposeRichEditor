package com.taocent.simple.compose.component.richtext.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RichTextPlatformAdapter {
    val supportsSystemContextMenu: Boolean
    val supportsRichClipboard: Boolean
    val supportsImeComposition: Boolean
    val requiresCollapsedCursorContextToolbarToggle: Boolean

    fun Modifier.suppressNativeContextMenu(onContextMenuRequested: () -> Unit): Modifier
}

@Composable
expect fun rememberRichTextPlatformAdapter(): RichTextPlatformAdapter
