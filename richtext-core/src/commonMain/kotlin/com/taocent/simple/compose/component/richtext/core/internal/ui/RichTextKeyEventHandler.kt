package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.taocent.simple.compose.component.richtext.core.RichTextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun handleRichTextKeyEvent(
    event: KeyEvent,
    state: RichTextState,
    clipboardActions: RichTextClipboardActions,
    coroutineScope: CoroutineScope,
    allowSmartPaste: Boolean,
    dispatchRegisteredCommands: Boolean = false,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
): Boolean {
    if (dispatchRegisteredCommands && state.keyCommands.dispatch(event)) return true
    if (event.type != KeyEventType.KeyDown) return false
    val isModifierPressed = event.isCtrlPressed || event.isMetaPressed
    if (!isModifierPressed) return false
    return when (event.key) {
        Key.Z -> {
            if (event.isShiftPressed) {
                onRedo?.invoke() ?: state.redo()
            } else {
                onUndo?.invoke() ?: state.undo()
            }
            true
        }
        Key.Y -> {
            onRedo?.invoke() ?: state.redo()
            true
        }
        Key.C -> {
            coroutineScope.launch { clipboardActions.copySelection() }
            true
        }
        Key.X -> {
            coroutineScope.launch { clipboardActions.cutSelection() }
            true
        }
        Key.V -> {
            coroutineScope.launch { clipboardActions.paste(allowSmartPaste = allowSmartPaste) }
            true
        }
        else -> false
    }
}
