package com.taocent.simple.compose.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS 平台 shared 逻辑:验证 [IOSPlatform] 名称遵循
 * "iOS <version>" 格式。
 */
class SharedLogicIOSTest {

    @Test
    fun iosPlatformNameHasIosPrefix() {
        val platform = getPlatform()
        assertNotNull(platform)
        val name = platform.name
        assertTrue(
            name.startsWith("iOS", ignoreCase = true) || name.contains("iPhone"),
            "expected iOS-related platform name, got: $name"
        )
    }

    @Test
    fun iosPlatformNameIsStable() {
        val a = getPlatform().name
        val b = getPlatform().name
        // 同一进程内平台名应保持一致
        assertEquals(a, b)
    }
}
