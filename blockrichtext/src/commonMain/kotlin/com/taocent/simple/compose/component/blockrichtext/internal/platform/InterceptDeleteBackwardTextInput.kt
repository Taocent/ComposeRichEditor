package com.taocent.simple.compose.component.blockrichtext.internal.platform

import androidx.compose.runtime.Composable

@Composable
internal expect fun InterceptDeleteBackwardTextInput(
    onDeleteBackward: () -> Boolean,
    content: @Composable () -> Unit
)
