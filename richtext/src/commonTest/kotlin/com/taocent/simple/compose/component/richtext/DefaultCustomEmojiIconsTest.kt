package com.taocent.simple.compose.component.richtext

import com.taocent.simple.compose.component.richtext.core.RichTextConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证 :richtext 模块的 [DefaultCustomEmojiIcons] 与 [RichTextConfig.customEmojiIcons] 的集成。
 *
 * 背景修复:之前 [RichTextConfig.customEmojiIcons] 默认是空 map,导致 [EmojiPanel] 与
 * [com.taocent.simple.compose.component.richtext.RichTextTextField] 的
 * [com.taocent.simple.compose.component.richtext.RichTextTextField] CustomEmojiCanvasOverlay 都拿不到 ImageVector。
 * 修复后 [RichTextEditor] 会在未显式传 config 时自动注入 [DefaultCustomEmojiIcons]。
 */
class DefaultCustomEmojiIconsTest {

    @Test
    fun defaultEmojiIconsContainsExpectedKeys() {
        // 默认表情应至少包含最常用的几个 key
        assertTrue("heart" in DefaultCustomEmojiIcons)
        assertTrue("star" in DefaultCustomEmojiIcons)
        assertTrue("thumbsup" in DefaultCustomEmojiIcons)
        assertTrue("check" in DefaultCustomEmojiIcons)
        assertTrue("fire" in DefaultCustomEmojiIcons)
    }

    @Test
    fun defaultEmojiIconsAreNonEmpty() {
        // 必须有非空图标映射
        assertTrue(DefaultCustomEmojiIcons.isNotEmpty())
    }

    @Test
    fun defaultEmojiIconsValuesAreNotNull() {
        // 每个 key 都应有 ImageVector 绑定
        DefaultCustomEmojiIcons.forEach { (id, icon) ->
            assertNotNull(icon, "Icon for id '$id' should not be null")
        }
    }

    @Test
    fun richTextConfigDefaultStillHasEmptyCustomEmojiIcons() {
        // RichTextConfig 默认值不变(仍是空 map,跨模块无法引用 :richtext 资源)
        val config = RichTextConfig()
        assertTrue(config.customEmojiIcons.isEmpty())
    }

    @Test
    fun canMergeDefaultEmojiIconsIntoConfig() {
        // 调用方手动 copy() 时能正确合并
        val config = RichTextConfig().copy(customEmojiIcons = DefaultCustomEmojiIcons)
        assertEquals(DefaultCustomEmojiIcons, config.customEmojiIcons)
    }
}
