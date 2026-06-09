package com.taocent.simple.compose.component

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SimpleComposeComponent",
    ) {
        App()
    }
}