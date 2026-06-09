package com.taocent.simple.compose.component.blockrichtext

import com.taocent.simple.compose.component.richtext.core.RichTextConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 验证 :blockrichtext 模块的 [DefaultCustomEmojiIcons] 注入行为。
 *
 * 背景修复:[BlockRichTextEditor] 之前默认 `config = RichTextConfig()`,导致
 * `customEmojiIcons` 是空 map,EmojiPanel 与 RichTextTextField 的
 * CustomEmojiCanvasOverlay 拿不到 ImageVector,emoji 不渲染。
 * 修复后未传 config 时自动注入 [DefaultCustomEmojiIcons]。
 */
class DefaultCustomEmojiIconsTest {

    @Test
    fun defaultEmojiIconsContainsExpectedKeys() {
        assertTrue("heart" in DefaultCustomEmojiIcons)
        assertTrue("star" in DefaultCustomEmojiIcons)
        assertTrue("thumbsup" in DefaultCustomEmojiIcons)
        assertTrue("check" in DefaultCustomEmojiIcons)
        assertTrue("fire" in DefaultCustomEmojiIcons)
    }

    @Test
    fun defaultEmojiIconsAreNonEmpty() {
        assertTrue(DefaultCustomEmojiIcons.isNotEmpty())
    }

    @Test
    fun defaultEmojiIconsValuesAreNotNull() {
        DefaultCustomEmojiIcons.forEach { (id, icon) ->
            assertNotNull(icon, "Icon for id '$id' should not be null")
        }
    }

    @Test
    fun richTextConfigDefaultStillHasEmptyCustomEmojiIcons() {
        // 锁定 upstream 行为:跨模块无依赖
        val config = RichTextConfig()
        assertTrue(config.customEmojiIcons.isEmpty())
    }

    @Test
    fun canMergeDefaultEmojiIconsIntoConfig() {
        val config = RichTextConfig().copy(customEmojiIcons = DefaultCustomEmojiIcons)
        assertEquals(DefaultCustomEmojiIcons, config.customEmojiIcons)
    }
}
