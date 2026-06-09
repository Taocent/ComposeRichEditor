package com.taocent.simple.compose.component.blockrichtext.internal.platform

import androidx.compose.runtime.Composable

@Composable
internal actual fun InterceptDeleteBackwardTextInput(
    onDeleteBackward: () -> Boolean,
    content: @Composable () -> Unit
) {
    content()
}
