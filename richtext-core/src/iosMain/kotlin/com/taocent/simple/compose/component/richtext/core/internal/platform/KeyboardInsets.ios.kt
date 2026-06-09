package com.taocent.simple.compose.component.richtext.core.internal.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSValue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberKeyboardHeight(): Dp {
    var keyboardHeight by remember { mutableStateOf(0.0) }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val showObserver = center.addObserverForName(
            name = "UIKeyboardWillShowNotification",
            `object` = null,
            queue = null
        ) { notification ->
            val frameValue = notification?.userInfo?.get("UIKeyboardFrameEndUserInfoKey") as? NSValue
            frameValue?.let { value ->
                keyboardHeight = memScoped {
                    val rect = alloc<CGRect>()
                    value.getValue(rect.ptr, sizeOf<CGRect>().toULong())
                    rect.size.height
                }
            }
        }
        val hideObserver = center.addObserverForName(
            name = "UIKeyboardWillHideNotification",
            `object` = null,
            queue = null
        ) { _ -> keyboardHeight = 0.0 }
        onDispose {
            center.removeObserver(showObserver)
            center.removeObserver(hideObserver)
        }
    }

    return keyboardHeight.toFloat().dp
}
