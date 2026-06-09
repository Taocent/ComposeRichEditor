package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 覆盖 [RichTextFormat] DSL 与 [RichTextConfig] 默认值契约。
 */
class RichTextFormatTest {

    // region RichTextFormat DSL

    @Test
    fun formatBlockAppliesBold() {
        val state = RichTextState()
        state.format { bold() }
        assertTrue(state.currentBold)
    }

    @Test
    fun formatBlockAppliesMultipleToggles() {
        val state = RichTextState()
        state.format {
            bold()
            italic()
            underline()
            strikethrough()
        }
        assertTrue(state.currentBold)
        assertTrue(state.currentItalic)
        assertTrue(state.currentUnderline)
        assertTrue(state.currentStrikethrough)
    }

    @Test
    fun formatBlockAppliesColorAndBackground() {
        val state = RichTextState()
        val fg = Color(0xFF112233)
        val bg = Color(0xFF445566)
        state.format {
            color(fg)
            backgroundColor(bg)
        }
        assertEquals(fg, state.currentColor)
        assertEquals(bg, state.currentBackground)
    }

    @Test
    fun formatBlockAppliesFontSize() {
        val state = RichTextState()
        state.format { fontSize(24.sp) }
        assertEquals(24.sp, state.currentFontSize)
    }

    @Test
    fun formatBlockInsertHyperlink() {
        val state = RichTextState()
        state.format { hyperlink(url = "https://example.com", text = "click") }
        val text = state.textFieldValue.text
        assertTrue(text.contains("click"))
        val links = state.textFieldValue.annotatedString
            .getStringAnnotations(RichTextState.HYPERLINK_TAG, 0, text.length)
        assertTrue(links.isNotEmpty())
        assertEquals("https://example.com", links.first().item)
    }

    @Test
    fun formatBlockClearFormatting() {
        val state = RichTextState()
        state.format {
            bold()
            italic()
            color(Color(0xFF112233))
        }
        assertTrue(state.currentBold)
        state.format { clearFormatting() }
        assertEquals(false, state.currentBold)
        assertEquals(false, state.currentItalic)
    }

    @Test
    fun formatBlockPropertySettersAreIdempotent() {
        val state = RichTextState()
        // 先开 bold
        state.format { bold() }
        assertTrue(state.currentBold)
        // 再次设置 bold 不会反转
        state.format { bold = true }
        assertTrue(state.currentBold)
    }

    @Test
    fun formatBlockPropertySetterTogglesOff() {
        val state = RichTextState()
        state.format { bold() }
        assertTrue(state.currentBold)
        state.format { bold = false }
        assertEquals(false, state.currentBold)
    }

    // endregion

    // region RichTextConfig 默认值

    @Test
    fun defaultRichTextConfigHasExpectedStructure() {
        val config = RichTextConfig()
        assertTrue(config.presetColors.isNotEmpty())
        assertTrue(config.presetFontSizes.isNotEmpty())
        assertTrue(config.presetBackgroundColors.isNotEmpty())
        assertTrue(config.emojiList.isNotEmpty())
        assertTrue(config.maxUndoHistory > 0)
        assertTrue(config.undoMergeIntervalMs > 0)
    }

    @Test
    fun defaultTableConfigHasReasonableBounds() {
        val table = TableConfig()
        assertTrue(table.maxInsertRows > 0)
        assertTrue(table.maxInsertColumns > 0)
        assertTrue(table.dragEdgeThreshold.value > 0)
    }

    @Test
    fun defaultPanelConfigIsSane() {
        val panel = PanelConfig()
        assertTrue(panel.defaultHeight.value > 0)
        assertTrue(panel.crossfadeAnimationMs > 0)
    }

    // endregion
}
