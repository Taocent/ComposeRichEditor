package com.taocent.simple.compose.component.richtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun paragraphAlignAppliesToCurrentLine() {
        val state = RichTextState("first\nsecond")
        state.restoreTextFieldValue(
            state.textFieldValue.copy(selection = TextRange(7))
        )
        state.setTextAlign(TextAlign.Center)
        val paragraph = state.textFieldValue.annotatedString.paragraphStyles.first { it.item.textAlign == TextAlign.Center }
        assertEquals(TextAlign.Center, paragraph.item.textAlign)
        assertEquals(6, paragraph.start)
        assertEquals(12, paragraph.end)
    }

    @Test
    fun paragraphAlignAppliesToSelectedLines() {
        val state = RichTextState("one\ntwo\nthree")
        state.restoreTextFieldValue(
            state.textFieldValue.copy(selection = TextRange(1, 6))
        )
        state.setTextAlign(TextAlign.Right)
        val ranges = state.textFieldValue.annotatedString.paragraphStyles
            .filter { it.item.textAlign == TextAlign.Right }
            .map { it.start to it.end }
        assertEquals(listOf(0 to 8), ranges)
    }

    @Test
    fun paragraphAlignSurvivesJsonRoundTrip() {
        val state = RichTextState("hello")
        state.setTextAlign(TextAlign.Center)
        val restored = RichTextState.fromJson(state.toJson())
        assertEquals(TextAlign.Center, restored.paragraphStyles.first().item.textAlign)
    }

    @Test
    fun paragraphAlignSurvivesTextInsertion() {
        val state = RichTextState("hello")
        state.setTextAlign(TextAlign.Right)
        state.onValueChange(
            TextFieldValue(
                annotatedString = androidx.compose.ui.text.AnnotatedString("hello!"),
                selection = TextRange(6)
            )
        )
        assertEquals(TextAlign.Right, state.textFieldValue.annotatedString.paragraphStyles.first().item.textAlign)
    }

    @Test
    fun emptyParagraphAlignKeepsCurrentStyle() {
        val state = RichTextState()
        state.setTextAlign(TextAlign.Center)
        assertEquals(TextAlign.Center, state.currentTextAlign)
        assertEquals(TextAlign.Center, state.textFieldValue.annotatedString.paragraphStyles.first().item.textAlign)
    }

    @Test
    fun emptyParagraphAlignAppliesToFirstTypedCharacter() {
        val state = RichTextState()
        state.setTextAlign(TextAlign.Center)
        state.onValueChange(
            TextFieldValue(
                annotatedString = androidx.compose.ui.text.AnnotatedString("1"),
                selection = TextRange(1)
            )
        )
        assertEquals(TextAlign.Center, state.currentTextAlign)
        assertEquals(TextAlign.Center, state.textFieldValue.annotatedString.paragraphStyles.first().item.textAlign)
        assertEquals(0, state.textFieldValue.annotatedString.paragraphStyles.first().start)
        assertEquals(1, state.textFieldValue.annotatedString.paragraphStyles.first().end)
    }

    @Test
    fun paragraphAlignSurvivesDeletingToEmptyText() {
        val state = RichTextState("1")
        state.setTextAlign(TextAlign.Right)
        state.restoreTextFieldValue(
            state.textFieldValue.copy(selection = TextRange(0, 1))
        )
        state.deleteSelection()
        assertEquals(TextAlign.Right, state.currentTextAlign)
        assertEquals(TextAlign.Right, state.textFieldValue.annotatedString.paragraphStyles.first().item.textAlign)
        assertEquals(0, state.textFieldValue.annotatedString.paragraphStyles.first().start)
        assertEquals(0, state.textFieldValue.annotatedString.paragraphStyles.first().end)
    }

    @Test
    fun emptyParagraphAlignAppliesToNewline() {
        val state = RichTextState()
        state.setTextAlign(TextAlign.Center)
        state.onValueChange(
            TextFieldValue(
                annotatedString = androidx.compose.ui.text.AnnotatedString("\n"),
                selection = TextRange(1)
            )
        )
        assertEquals(TextAlign.Center, state.currentTextAlign)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        assertEquals(1, styles.size, "末尾虚拟空行不应写入空 paragraphStyle range,实际 $styles")
        assertEquals(0, styles[0].start)
        assertEquals(1, styles[0].end)
        assertEquals(TextAlign.Center, styles[0].item.textAlign)
    }

    @Test
    fun newlineAfterCenteredLineKeepsTrailingEmptyLineCentered() {
        val state = RichTextState()
        state.setTextAlign(TextAlign.Center)
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        assertEquals(TextAlign.Center, state.currentTextAlign)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        val firstLine = styles.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull(firstLine, "段 1 (0..2) 应保持 Center,实际 styles=$styles")
        assertEquals(TextAlign.Center, firstLine.item.textAlign)
        assertTrue(styles.none { it.start == 2 && it.end == 2 }, "回车后的末尾空行不应写入空 range,实际 styles=$styles")
    }

    @Test
    fun newlineAfterLeftLineDoesNotCreateTrailingEmptyLineRange() {
        val state = RichTextState()
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        assertEquals(TextAlign.Left, state.currentTextAlign)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        val firstLine = styles.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull(firstLine, "段 1 (0..2) 应保持 Left,实际 styles=$styles")
        assertEquals(TextAlign.Left, firstLine.item.textAlign)
        assertTrue(styles.none { it.start == 2 && it.end == 2 }, "Left 末尾空行不应生成空 range,实际 styles=$styles")
    }

    @Test
    fun multiLineAfterNewlineHasNoVisualGap() {
        // 回归测试: 输入 "1" → 按回车 → 输入 "2", 不应该在 "1" 和 "2" 之间多出空行
        // 即使段落样式存在,相同样式的连续段也应合并成一个 range,避免把 "1\n" 单独排成带空尾行的 paragraph
        val state = RichTextState()
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n2"), selection = TextRange(3))
        )
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        assertEquals(1, styles.size, "相同 Left 段落样式应合并为 1 段,实际 ${styles}")
        assertEquals(0, styles[0].start)
        assertEquals(3, styles[0].end)
        assertEquals(TextAlign.Left, styles[0].item.textAlign)
    }

    @Test
    fun setTextAlignOnSecondLineKeepsFirstLineAlign() {
        val state = RichTextState()
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n1"), selection = TextRange(3))
        )
        assertEquals(1, state.textFieldValue.annotatedString.paragraphStyles.size)
        state.setTextAlign(TextAlign.Center)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        val firstLine = styles.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull(firstLine, "段 1 (0..2) 应当保留 Left,实际 styles=$styles")
        assertEquals(TextAlign.Left, firstLine.item.textAlign)
        val secondLine = styles.firstOrNull { it.start == 2 && it.end == 3 }
        assertNotNull(secondLine, "段 2 (2..3) 应当变为 Center,实际 styles=$styles")
        assertEquals(TextAlign.Center, secondLine.item.textAlign)
    }

    @Test
    fun setTextAlignOnSecondLineBackToLeftMergesSameAlign() {
        val state = RichTextState()
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n1"), selection = TextRange(3))
        )
        state.setTextAlign(TextAlign.Center)
        state.setTextAlign(TextAlign.Left)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        assertEquals(1, styles.size, "相邻 Left 段落样式应重新合并,实际 styles=$styles")
        assertEquals(0, styles[0].start)
        assertEquals(3, styles[0].end)
        assertEquals(TextAlign.Left, styles[0].item.textAlign)
    }

    @Test
    fun setTextAlignOnTrailingNewlineKeepsPreviousLine() {
        // 回归测试:
        // 1) 设置 Center
        // 2) 输入 "1" → 段 1 (0..1) Center
        // 3) 按回车 → 段 1 (0..2) "1\n" Center
        // 4) 在末尾虚拟空行(光标位置 2)setTextAlign(Left)
        //    → 段 1 应该保持 Center("1" 居中),末尾虚拟空行样式只保存在 currentTextAlign
        val state = RichTextState()
        state.setTextAlign(TextAlign.Center)
        val builder = androidx.compose.ui.text.AnnotatedString.Builder("1\n")
        builder.addStyle(
            androidx.compose.ui.text.ParagraphStyle(textAlign = TextAlign.Center),
            0,
            2
        )
        state.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = builder.toAnnotatedString(),
                selection = TextRange(2)
            )
        )
        // 验证前置条件
        assertEquals("1\n", state.textFieldValue.text)
        // 在末尾虚拟空行 setTextAlign(Left)
        state.setTextAlign(TextAlign.Left)
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        // 段 1: "1\n" 范围 [0, 2) 应该保持 Center
        val firstLine = styles.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull(firstLine, "段 1 (0..2) 应当被保留,实际 styles=$styles text='${state.textFieldValue.text}'")
        assertEquals(TextAlign.Center, firstLine.item.textAlign)
        assertTrue(styles.none { it.start == 2 && it.end == 2 }, "末尾虚拟空行不应写入 paragraphStyle,实际 styles=$styles")
        assertEquals(TextAlign.Left, state.currentTextAlign)
        state.onValueChange(
            TextFieldValue(
                annotatedString = androidx.compose.ui.text.AnnotatedString("1\n2"),
                selection = TextRange(3)
            )
        )
        val afterInputStyles = state.textFieldValue.annotatedString.paragraphStyles
        val secondLine = afterInputStyles.firstOrNull { it.start == 2 && it.end == 3 }
        assertNotNull(secondLine, "段 2 (2..3) 应当存在,实际 styles=$afterInputStyles")
        assertEquals(TextAlign.Left, secondLine.item.textAlign)
    }

    @Test
    fun inputOnTrailingEmptyLineUsesTrailingEmptyLineAlign() {
        val state = RichTextState()
        val builder = androidx.compose.ui.text.AnnotatedString.Builder("1\n")
        builder.addStyle(
            androidx.compose.ui.text.ParagraphStyle(textAlign = TextAlign.Left),
            0,
            2
        )
        state.restoreTextFieldValue(
            TextFieldValue(
                annotatedString = builder.toAnnotatedString(),
                selection = TextRange(2)
            )
        )
        state.setTextAlign(TextAlign.Center)
        state.onValueChange(
            TextFieldValue(
                annotatedString = androidx.compose.ui.text.AnnotatedString("1\n字"),
                selection = TextRange(3)
            )
        )
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        val firstLine = styles.firstOrNull { it.start == 0 && it.end == 2 }
        assertNotNull(firstLine, "段 1 (0..2) 应当保持 Left,实际 styles=$styles")
        assertEquals(TextAlign.Left, firstLine.item.textAlign)
        val secondLine = styles.firstOrNull { it.start == 2 && it.end == 3 }
        assertNotNull(secondLine, "末尾空行输入后应继承空行 Center 样式,实际 styles=$styles")
        assertEquals(TextAlign.Center, secondLine.item.textAlign)
    }

    @Test
    fun deleteLastCharacterOnTrailingLineKeepsTrailingEmptyLineAlign() {
        val state = RichTextState()
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1"), selection = TextRange(1))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        state.setTextAlign(TextAlign.Center)
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n字"), selection = TextRange(3))
        )
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n"), selection = TextRange(2))
        )
        assertEquals(TextAlign.Center, state.currentTextAlign)
        val afterDeleteStyles = state.textFieldValue.annotatedString.paragraphStyles
        assertTrue(afterDeleteStyles.none { it.start == 2 && it.end == 2 }, "删除后不应生成末尾空 range,实际 styles=$afterDeleteStyles")
        state.onValueChange(
            TextFieldValue(annotatedString = androidx.compose.ui.text.AnnotatedString("1\n新"), selection = TextRange(3))
        )
        val styles = state.textFieldValue.annotatedString.paragraphStyles
        val secondLine = styles.firstOrNull { it.start == 2 && it.end == 3 }
        assertNotNull(secondLine, "再次输入应继续继承空行 Center 样式,实际 styles=$styles")
        assertEquals(TextAlign.Center, secondLine.item.textAlign)
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
        state.format { bold() }
        assertTrue(state.currentBold)
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

    @Test
    fun formatBlockSuperscriptToggles() {
        val state = RichTextState()
        state.format { superscript() }
        assertTrue(state.currentSuperscript)
    }

    @Test
    fun formatBlockSubscriptToggles() {
        val state = RichTextState()
        state.format { subscript() }
        assertTrue(state.currentSubscript)
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
    }

    @Test
    fun defaultPanelConfigIsSane() {
        val panel = PanelConfig()
        assertTrue(panel.defaultHeight.value > 0)
        assertTrue(panel.crossfadeAnimationMs > 0)
    }

    // endregion
}
