package com.taocent.simple.compose.component.blockrichtext.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun InterceptDeleteBackwardTextInput(
    onDeleteBackward: () -> Boolean,
    content: @Composable () -> Unit
) {
    val interceptor = remember(onDeleteBackward) {
        PlatformTextInputInterceptor { request, nextHandler ->
            val wrappedRequest = object : PlatformTextInputMethodRequest by request {
                override val onEditCommand: (List<EditCommand>) -> Unit = { commands ->
                    val consumed = commands.any { it.isDeleteBackwardCommand() } && onDeleteBackward()
                    if (!consumed) {
                        request.onEditCommand(commands)
                    }
                }
            }
            nextHandler.startInputMethod(wrappedRequest)
        }
    }
    InterceptPlatformTextInput(
        interceptor = interceptor,
        content = content
    )
}

private fun EditCommand.isDeleteBackwardCommand(): Boolean = when (this) {
    is DeleteSurroundingTextCommand -> lengthBeforeCursor > 0 && lengthAfterCursor == 0
    is DeleteSurroundingTextInCodePointsCommand -> lengthBeforeCursor > 0 && lengthAfterCursor == 0
    else -> false
}
