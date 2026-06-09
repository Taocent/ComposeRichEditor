package com.taocent.simple.compose.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
actual fun rememberKeyboardHeight(): Dp {
    val density = LocalDensity.current
    return with(density) { WindowInsets.ime.getBottom(density).toDp() }
}
