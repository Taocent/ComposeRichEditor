package com.taocent.simple.compose.component.richtext.core

/**
 * JVM 平台:返回 System.currentTimeMillis()。
 */
internal actual fun currentTimeMillis(): Long = java.lang.System.currentTimeMillis()
