package com.taocent.simple.compose.component.richtext

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 直接构造 [RichTextEditorState] 测试其状态管理 API。
 * 不需要 Compose runtime,可在 JVM 上单元测试。
 */
class RichTextEditorStateTest {

    @Test
    fun initialStateIsAllClosed() {
        val s = RichTextEditorState()
        assertNull(s.activeCategory)
        assertFalse(s.showLinkDialog)
        assertFalse(s.showExportDialog)
        assertEquals(0.dp, s.rememberedKeyboardHeight)
    }

    @Test
    fun toggleCategoryOpens() {
        val s = RichTextEditorState()
        s.toggleCategory(ToolCategory.EMOJI)
        assertEquals(ToolCategory.EMOJI, s.activeCategory)
    }

    @Test
    fun toggleCategoryOnSameCloses() {
        val s = RichTextEditorState()
        s.toggleCategory(ToolCategory.EMOJI)
        s.toggleCategory(ToolCategory.EMOJI)
        assertNull(s.activeCategory)
    }

    @Test
    fun toggleCategorySwitches() {
        val s = RichTextEditorState()
        s.toggleCategory(ToolCategory.EMOJI)
        s.toggleCategory(ToolCategory.TEXT_STYLE)
        assertEquals(ToolCategory.TEXT_STYLE, s.activeCategory)
    }

    @Test
    fun clearCategoryResetsToNull() {
        val s = RichTextEditorState()
        s.toggleCategory(ToolCategory.EMOJI)
        s.clearCategory()
        assertNull(s.activeCategory)
    }

    @Test
    fun requestAndDismissLinkDialog() {
        val s = RichTextEditorState()
        assertFalse(s.showLinkDialog)
        s.requestLinkDialog()
        assertTrue(s.showLinkDialog)
        s.dismissLinkDialog()
        assertFalse(s.showLinkDialog)
    }

    @Test
    fun requestAndDismissExportDialog() {
        val s = RichTextEditorState()
        assertFalse(s.showExportDialog)
        s.requestExportDialog()
        assertTrue(s.showExportDialog)
        s.dismissExportDialog()
        assertFalse(s.showExportDialog)
    }

    @Test
    fun dismissAllClearsEverything() {
        val s = RichTextEditorState()
        s.toggleCategory(ToolCategory.EMOJI)
        s.requestLinkDialog()
        s.requestExportDialog()
        s.rememberedKeyboardHeight = 300.dp

        s.dismissAll()
        assertNull(s.activeCategory)
        assertFalse(s.showLinkDialog)
        assertFalse(s.showExportDialog)
        // rememberedKeyboardHeight 不被 dismissAll 清除:它属于会话级高度,跨面板重用
        assertEquals(300.dp, s.rememberedKeyboardHeight)
    }

    @Test
    fun rememberedKeyboardHeightIsMutable() {
        val s = RichTextEditorState()
        s.rememberedKeyboardHeight = 250.dp
        assertEquals(250.dp, s.rememberedKeyboardHeight)
    }

    @Test
    fun toolCategoryEnumHasExpectedValues() {
        // 增加新分类时这里可作为基线
        assertEquals(3, ToolCategory.entries.size)
        assertTrue(ToolCategory.EMOJI in ToolCategory.entries)
        assertTrue(ToolCategory.TEXT_STYLE in ToolCategory.entries)
        assertTrue(ToolCategory.LINK in ToolCategory.entries)
    }
}
