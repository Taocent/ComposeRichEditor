package com.taocent.simple.compose.component.richtext.core

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 面板相关配置 (RichTextEditor / TextStylePanel / PanelSection / ExportJsonDialog) */
@Immutable
data class PanelConfig(
    /** RichTextEditor: 面板默认高度 */
    val defaultHeight: Dp = 260.dp,
    /** RichTextEditor: 面板切换交叉淡入淡出动画时长(ms) */
    val crossfadeAnimationMs: Int = 200,
    /** RichTextEditor: 链接对话框关闭后恢复焦点延迟(ms) */
    val linkDialogFocusRestoreDelayMs: Long = 200L,
    /** PanelSection: 标题与内容间距 */
    val sectionSpacing: Dp = 6.dp,
    /** TextStylePanel: 面板水平内边距 */
    val stylePanelPaddingH: Dp = 12.dp,
    /** TextStylePanel: 面板垂直内边距 */
    val stylePanelPaddingV: Dp = 10.dp,
    /** TextStylePanel: 区块间距 */
    val stylePanelSpacing: Dp = 10.dp,
    /** TextStylePanel: 芯片间距 */
    val stylePanelChipSpacing: Dp = 6.dp,
    /** ExportJsonDialog: JSON 预览最大高度 */
    val exportDialogMaxHeight: Dp = 400.dp,
)

/** 字号项 */
@Immutable
data class FontSizeItem(
    val size: TextUnit,
    val label: String
)

val DefaultPresetColors = listOf(
    Color(0xFF000000),
    Color(0xFFE53935),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFF6D4C41),
)

val DefaultPresetFontSizes = listOf(
    FontSizeItem(12.sp, "12"),
    FontSizeItem(14.sp, "14"),
    FontSizeItem(16.sp, "16"),
    FontSizeItem(18.sp, "18"),
    FontSizeItem(20.sp, "20"),
    FontSizeItem(24.sp, "24"),
    FontSizeItem(30.sp, "30"),
)

val DefaultPresetBackgroundColors = listOf(
    Color(0x80FFFF00),
    Color(0x8000FF00),
    Color(0x8000FFFF),
    Color(0x80FF00FF),
    Color(0x80FFA500),
    Color(0x80808080),
)

val DefaultEmojiList = listOf(
    "😀", "😂", "🥰", "😎", "🤔", "😢", "😡", "🥳",
    "❤️", "👍", "👎", "✌️", "🙏", "👏", "💪", "🤝",
    "🔥", "⭐", "🎉", "💯", "📝", "✅", "❌", "⚠️",
)
