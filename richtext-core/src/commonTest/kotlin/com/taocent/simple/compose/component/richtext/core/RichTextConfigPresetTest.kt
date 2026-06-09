package com.taocent.simple.compose.component.richtext.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证 [RichTextConfig] Preset 工厂的字段值。
 */
class RichTextConfigPresetTest {

    // region minimal()

    @Test
    fun minimalDisablesAllSmartPaste() {
        val config = RichTextConfig.minimal()
        assertFalse(config.smartPasteJsonEnabled)
        assertFalse(config.smartPasteHtmlEnabled)
        assertFalse(config.smartPasteMarkdownEnabled)
    }

    @Test
    fun minimalPreservesDefaultPresetColors() {
        val config = RichTextConfig.minimal()
        assertEquals(DefaultPresetColors, config.presetColors)
        assertEquals(DefaultPresetFontSizes, config.presetFontSizes)
        assertEquals(DefaultPresetBackgroundColors, config.presetBackgroundColors)
    }

    @Test
    fun minimalPreservesDefaultEmoji() {
        val config = RichTextConfig.minimal()
        assertEquals(DefaultEmojiList, config.emojiList)
    }

    @Test
    fun minimalPreservesDefaultUndoHistory() {
        val config = RichTextConfig.minimal()
        assertEquals(100, config.maxUndoHistory)
        assertEquals(500L, config.undoMergeIntervalMs)
    }

    // endregion

    // region default()

    @Test
    fun defaultEnablesAllSmartPaste() {
        val config = RichTextConfig.default()
        assertTrue(config.smartPasteJsonEnabled)
        assertTrue(config.smartPasteHtmlEnabled)
        assertTrue(config.smartPasteMarkdownEnabled)
    }

    @Test
    fun defaultMatchesFieldDefaults() {
        // RichTextConfig.default() 应等同 RichTextConfig()(无参数)
        assertEquals(RichTextConfig(), RichTextConfig.default())
    }

    // endregion

    // region complex()

    @Test
    fun complexExpandsUndoHistory() {
        val config = RichTextConfig.complex()
        assertEquals(200, config.maxUndoHistory)
        assertEquals(300L, config.undoMergeIntervalMs)
    }

    @Test
    fun complexExpandsTableInsertLimits() {
        val config = RichTextConfig.complex()
        assertEquals(30, config.table.maxInsertRows)
        assertEquals(8, config.table.maxInsertColumns)
    }

    @Test
    fun complexPreservesSmartPasteDefaults() {
        // complex() = default().copy(...),所以 SmartPaste 开关保持 default 值
        val config = RichTextConfig.complex()
        assertTrue(config.smartPasteJsonEnabled)
        assertTrue(config.smartPasteHtmlEnabled)
        assertTrue(config.smartPasteMarkdownEnabled)
    }

    // endregion

    // region 互不污染

    @Test
    fun presetsAreIndependentInstances() {
        val minimal = RichTextConfig.minimal()
        val default = RichTextConfig.default()
        val complex = RichTextConfig.complex()
        // 三者独立,不会因为 Preset 共享而相互影响
        assertFalse(minimal.smartPasteJsonEnabled)
        assertTrue(default.smartPasteJsonEnabled)
        assertTrue(complex.smartPasteJsonEnabled)
    }

    // endregion
}
