package com.taocent.simple.compose.component

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 平台特定 shared 逻辑:在 Desktop/JVM 上跑
 * 验证 [getPlatform] 返回的 [JVMPlatform] 名称与 Java 版本相关,
 * 且平台信息非空。
 */
class SharedLogicDesktopTest {

    @Test
    fun desktopPlatformNameContainsJavaVersion() {
        val platform = getPlatform()
        assertNotNull(platform)
        val name = platform.name
        assertTrue(name.startsWith("Java"), "expected name to start with 'Java', got: $name")
    }
}
