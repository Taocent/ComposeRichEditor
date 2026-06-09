package com.taocent.simple.compose.component.richtext.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

private object JvmRichTextPlatformAdapter : RichTextPlatformAdapter {
    override val supportsSystemContextMenu: Boolean = true
    override val supportsRichClipboard: Boolean = true
    override val supportsImeComposition: Boolean = true
    override val requiresCollapsedCursorContextToolbarToggle: Boolean = false

    @OptIn(ExperimentalComposeUiApi::class)
    override fun Modifier.suppressNativeContextMenu(onContextMenuRequested: () -> Unit): Modifier {
        return pointerInput(onContextMenuRequested) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.button == PointerButton.Secondary) {
                        event.changes.forEach { it.consume() }
                        onContextMenuRequested()
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberRichTextPlatformAdapter(): RichTextPlatformAdapter {
    return remember { JvmRichTextPlatformAdapter }
}
