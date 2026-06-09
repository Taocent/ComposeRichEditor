package com.taocent.simple.compose.component

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform