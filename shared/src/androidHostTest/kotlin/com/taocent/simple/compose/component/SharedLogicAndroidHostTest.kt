package com.taocent.simple.compose.component

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android 平台 shared 逻辑:验证 [AndroidPlatform] 名称以
 * "Android <sdkInt>" 形式呈现。
 */
class SharedLogicAndroidHostTest {

    @Test
    fun androidPlatformNameHasAndroidPrefix() {
        val platform = getPlatform()
        assertNotNull(platform)
        val name = platform.name
        assertTrue(
            name.startsWith("Android", ignoreCase = true),
            "expected 'Android' prefix, got: $name"
        )
    }
}
