package com.taocent.simple.compose.component.richtext.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taocent.simple.compose.component.richtext.core.paste.PasteParser

/**
 * :richtext-core 统一配置 — 整合 :richtext 专属 UI Configs 与 :blockrichtext 专属 TableConfig + 撤销配置。
 * 顶层 val([DefaultPresetColors] 等)由[Defaults.kt]提供。
 * [DefaultCustomEmojiIcons] 由 :richtext 模块提供(Lucide ImageVector 映射),:blockrichtext 可不提供或自行覆盖。
 */

/** 文本输入框相关配置 (RichTextTextField) */
@Immutable
data class TextFieldConfig(
    /** RichTextTextField: 输入区域内边距 */
    val innerPadding: Dp = 7.5.dp,
    /** RichTextTextField: 默认文本字号 */
    val defaultFontSize: TextUnit = 16.sp,
    /** RichTextTextField: 占位文本透明度 */
    val placeholderAlpha: Float = 0.5f,
)

/** 浮动工具栏相关配置 (FloatingToolbar / ToolbarCategoryItem) */
@Immutable
data class FloatingToolbarConfig(
    /** FloatingToolbar: 工具栏圆角 */
    val shapeRadius: Dp = 10.dp,
    /** FloatingToolbar: 按钮圆角 */
    val buttonRadius: Dp = 6.dp,
    /** FloatingToolbar: 工具栏垂直内边距 */
    val verticalPadding: Dp = 8.dp,
    /** FloatingToolbar: 工具栏水平内边距 */
    val horizontalPadding: Dp = 6.dp,
    /** FloatingToolbar: 格式按钮大小 */
    val buttonSize: Dp = 32.dp,
    /** FloatingToolbar: 按钮水平内边距 */
    val buttonHPadding: Dp = 10.dp,
    /** FloatingToolbar: 按钮垂直内边距 */
    val buttonVPadding: Dp = 6.dp,
    /** FloatingToolbar: 格式按钮字号 */
    val buttonFontSize: TextUnit = 13.sp,
    /** FloatingToolbar: 上标/下标按钮字号 */
    val scriptFontSize: TextUnit = 11.sp,
    /** FloatingToolbar: 分隔线宽度 */
    val dividerWidth: Dp = 1.dp,
    /** FloatingToolbar: 分隔线高度 */
    val dividerHeight: Dp = 20.dp,
    /** FloatingToolbar: 分隔线间距 */
    val dividerSpacing: Dp = 4.dp,
    /** FloatingToolbar: 工具栏上方间距 */
    val abovePadding: Dp = 8.dp,
    /** FloatingToolbar: 光标底部额外内边距 */
    val cursorPadding: Dp = 20.dp,
    /** RichTextTextField: 选中文本后显示浮动工具栏延迟(ms) */
    val showDelayMs: Long = 150L,
    /** ToolbarCategoryItem: 按钮水平内边距 */
    val categoryPaddingH: Dp = 16.dp,
    /** ToolbarCategoryItem: 按钮垂直内边距 */
    val categoryPaddingV: Dp = 8.dp,
    /** ToolbarCategoryItem: 图标大小 */
    val categoryIconSize: Dp = 22.dp,
    /**
     * FloatingToolbar: 工具栏内容最大宽度。
     *
     * 之前是 `textFieldWidthPx / 2`,在表格单元格这种窄字段下会被挤到无法装下所有按钮;
     * 改为配置项,默认 320dp 足以容纳 B/I/U/S/x²/x₂ + 复制/剪切/粘贴/全选 全部按钮,
     * 也不依赖所属字段的宽度,因此在 TextBlock 和 TableCell 内表现一致。
     */
    val maxWidth: Dp = 320.dp,
)

/** 样式芯片相关配置 (StyleChip) */
@Immutable
data class StyleChipConfig(
    /** StyleChip: 圆角 */
    val radius: Dp = 16.dp,
    /** StyleChip: 边框宽度 */
    val borderWidth: Dp = 1.dp,
    /** StyleChip: 水平内边距 */
    val contentPaddingH: Dp = 12.dp,
    /** StyleChip: 垂直内边距 */
    val contentPaddingV: Dp = 4.dp,
    /** StyleChip: 高度 */
    val height: Dp = 32.dp,
)

/** 样式切换按钮相关配置 (StyleTextToggle) */
@Immutable
data class StyleTextToggleConfig(
    /** StyleTextToggle: 圆角 */
    val radius: Dp = 16.dp,
    /** StyleTextToggle: 边框宽度 */
    val borderWidth: Dp = 1.dp,
    /** StyleTextToggle: 水平内边距 */
    val contentPaddingH: Dp = 12.dp,
    /** StyleTextToggle: 垂直内边距 */
    val contentPaddingV: Dp = 5.dp,
)

/** 颜色圆点相关配置 (ColorDot) */
@Immutable
data class ColorDotConfig(
    /** ColorDot: 选中时大小 */
    val selectedSize: Dp = 30.dp,
    /** ColorDot: 未选中时大小 */
    val size: Dp = 26.dp,
    /** ColorDot: 选中时边框宽度 */
    val selectedBorderWidth: Dp = 2.5.dp,
    /** ColorDot: 未选中时边框宽度 */
    val borderWidth: Dp = 1.5.dp,
)

/** 表情面板相关配置 (EmojiPanel) */
@Immutable
data class EmojiPanelConfig(
    /** EmojiPanel: Tab 行水平内边距 */
    val tabPaddingH: Dp = 8.dp,
    /** EmojiPanel: Tab 行垂直内边距 */
    val tabPaddingV: Dp = 4.dp,
    /** EmojiPanel: Tab 间距 */
    val tabSpacing: Dp = 8.dp,
    /** EmojiPanel: 网格水平内边距 */
    val gridPaddingH: Dp = 8.dp,
    /** EmojiPanel: 网格水平间距 */
    val gridHSpacing: Dp = 4.dp,
    /** EmojiPanel: 网格垂直间距 */
    val gridVSpacing: Dp = 4.dp,
    /** EmojiPanel: 网格内容内边距 */
    val gridContentPadding: Dp = 4.dp,
    /** EmojiPanel: 表情项大小 */
    val itemBoxSize: Dp = 40.dp,
    /** EmojiPanel: 表情项圆角 */
    val itemRadius: Dp = 8.dp,
    /** EmojiPanel: Tab 芯片圆角 */
    val tabChipRadius: Dp = 12.dp,
    /** EmojiPanel: Tab 芯片水平内边距 */
    val tabChipPaddingH: Dp = 12.dp,
    /** EmojiPanel: Tab 芯片垂直内边距 */
    val tabChipPaddingV: Dp = 4.dp,
)

/** 表格相关配置 (TableRenderer / TableCellEditor / TableInsertDialog) */
@Immutable
data class TableConfig(
    /** TableRenderer: 表头行背景色 */
    val headerColor: Color = Color(0xFFF5F5F5),
    /** TableRenderer: 单元格背景色 */
    val cellColor: Color = Color.White,
    /** TableRenderer: 单元格边框颜色 */
    val borderColor: Color = Color(0xFFE0E0E0),
    /** TableRenderer: 触发横向滚动的列数阈值 */
    val maxColumnsBeforeScroll: Int = 3,
    /** TableRenderer: 行/列菜单按钮大小 */
    val menuButtonSize: Dp = 12.dp,
    /** TableRenderer / TableCellEditor: 单元格最小高度 */
    val cellMinHeight: Dp = 36.dp,
    /** TableRenderer: 单元格边框宽度 */
    val cellBorderWidth: Dp = 0.5.dp,
    /**
     * TableRenderer: 选中/导航高亮的边框宽度 — 行级高亮(选中行)与列级高亮(选中列)
     * 共用一个字段,实现完全对称(都用 `Modifier.border` 绘制,不被 cell 框线挡住)。
     *
     * 历史: 之前拆成 `selectedRowBorderWidth` + `highlightStrokeWidth` 两个字段,
     * 列级用 `drawBehind` 在 cell 内部画线,被 cell 自身 `border()` 盖住 — 现统一
     * 用 border 实现后,一个字段就够。默认 1dp,比 cellBorderWidth(0.5dp) 略粗,
     * 视觉上更显眼。
     */
    val selectedHighlightBorderWidth: Dp = 1.dp,
    /** TableRenderer: 滚动模式下列宽内边距 */
    val columnScrollPadding: Dp = 4.dp,
    /** TableRenderer: 拖拽边缘自动滚动阈值 */
    val dragEdgeThreshold: Dp = 40.dp,
    /** TableRenderer: 拖拽自动滚动帧间隔(ms) */
    val dragAutoScrollIntervalMs: Long = 16L,
    /** TableRenderer: 拖拽自动滚动步长(px) */
    val dragAutoScrollStepPx: Int = 8,
    /** TableRenderer: 拖拽结束回弹动画时长(ms) */
    val dragFinishAnimationMs: Int = 200,
    /** TableRenderer: 拖拽占位动画时长(ms) */
    val dragPlaceholderAnimationMs: Int = 150,
    /** TableRenderer: 表格选中后自动清除延迟(ms) */
    val selectionAutoClearDelayMs: Long = 1000L,
    /** TableInsertDialog: 行数范围上限 */
    val maxInsertRows: Int = 20,
    /** TableInsertDialog: 列数范围上限 */
    val maxInsertColumns: Int = 6,
    /** TableInsertDialog: 输入框宽度 */
    val insertDialogFieldWidth: Dp = 100.dp,
)

/**
 * 统一 RichTextConfig — 包含 :richtext 专属 UI 字段 与 :blockrichtext 专属 TableConfig + 撤销配置。
 * 顶层 default values([DefaultPresetColors] 等) 由 Defaults.kt 提供。
 *
 * [customEmojiIcons] 默认空 map;使用 [com.taocent.simple.compose.component.richtext.RichTextEditor]
 * 时会由 :richtext 模块自动注入 [com.taocent.simple.compose.component.richtext.DefaultCustomEmojiIcons];
 * 使用 [RichTextTextField] / 其他非 Editor 入口时,需调用方自行合并。
 */
@Immutable
data class RichTextConfig(
    /** TextStylePanel: 预设文字颜色列表 */
    val presetColors: List<Color> = DefaultPresetColors,
    /** TextStylePanel: 预设字号列表 */
    val presetFontSizes: List<FontSizeItem> = DefaultPresetFontSizes,
    /** TextStylePanel: 预设背景高亮色列表 */
    val presetBackgroundColors: List<Color> = DefaultPresetBackgroundColors,

    /** EmojiPanel: Unicode 表情列表 */
    val emojiList: List<String> = DefaultEmojiList,
    /** EmojiPanel: Unicode 表情网格列数 */
    val emojiGridColumns: Int = 8,
    /** EmojiPanel: Unicode 表情字号 */
    val emojiItemSize: TextUnit = 20.sp,
    /** EmojiPanel: 自定义表情列表 */
    val customEmojis: List<CustomEmoji> = DefaultCustomEmojis,
    /** EmojiPanel: 自定义表情图标映射 */
    val customEmojiIcons: Map<String, ImageVector> = emptyMap(),
    /** EmojiPanel: 自定义表情网格列数 */
    val customEmojiGridColumns: Int = 6,
    /** EmojiPanel: 自定义表情图标大小 */
    val customEmojiItemSize: Dp = 24.dp,

    /** BlockRichTextEditor(:blockrichtext): 最大撤销历史数 */
    val maxUndoHistory: Int = 100,
    /** BlockRichTextEditor(:blockrichtext): 撤销合并间隔(ms) */
    val undoMergeIntervalMs: Long = 500,

    /** RichTextTextField: 启用 JSON 智能粘贴 */
    val smartPasteJsonEnabled: Boolean = true,
    /** RichTextTextField: 启用 HTML 智能粘贴 */
    val smartPasteHtmlEnabled: Boolean = true,
    /** RichTextTextField: 启用 Markdown 智能粘贴 */
    val smartPasteMarkdownEnabled: Boolean = true,
    /** RichTextTextField: 自定义智能粘贴解析器,优先级高于内置解析器 */
    val pasteParsers: List<PasteParser> = emptyList(),

    /**
     * 错误日志接收器 — 默认 [NoOpLogger] 不输出;生产环境可注入 Crashlytics / Sentry / Logcat 适配器。
     * 错误同时会通过 [RichTextState.errors] 暴露给 UI(用于 snackbar 等)。
     */
    val logger: RichTextLogger = RichTextLogger.NoOp,

    /** 面板相关配置 (RichTextEditor / TextStylePanel / PanelSection / ExportJsonDialog) */
    val panel: PanelConfig = PanelConfig(),
    /** 文本输入框相关配置 (RichTextTextField) */
    val textField: TextFieldConfig = TextFieldConfig(),
    /** 浮动工具栏相关配置 (FloatingToolbar / ToolbarCategoryItem) */
    val floatingToolbar: FloatingToolbarConfig = FloatingToolbarConfig(),
    /** 样式芯片相关配置 (StyleChip) */
    val styleChip: StyleChipConfig = StyleChipConfig(),
    /** 样式切换按钮相关配置 (StyleTextToggle) */
    val styleTextToggle: StyleTextToggleConfig = StyleTextToggleConfig(),
    /** 颜色圆点相关配置 (ColorDot) */
    val colorDot: ColorDotConfig = ColorDotConfig(),
    /** 表情面板相关配置 (EmojiPanel) */
    val emojiPanel: EmojiPanelConfig = EmojiPanelConfig(),
    /** 表格相关配置 (TableRenderer / TableCellEditor / TableInsertDialog — :blockrichtext 专用) */
    val table: TableConfig = TableConfig(),
) {
    companion object {
        /**
         * 最小配置 — 关闭 SmartPaste(无 JSON/HTML/Markdown 解析),适合纯展示 / 只读富文本场景。
         * 保留 presetColors / emoji 等基础数据,使用方按需 copy() 扩展。
         */
        fun minimal(): RichTextConfig = RichTextConfig(
            smartPasteJsonEnabled = false,
            smartPasteHtmlEnabled = false,
            smartPasteMarkdownEnabled = false,
        )

        /**
         * 标准配置 — 等同字段默认值,启用 SmartPaste,适合 90% 富文本编辑场景。
         * 这是大多数使用方接入时应该选择的预设。
         */
        fun default(): RichTextConfig = RichTextConfig()

        /**
         * 复杂配置 — 在 [default] 基础上加大撤销历史(100 → 200)、缩短合并间隔(500ms → 300ms),
         * 并放宽表格插入上限,适合需要频繁撤销 / 大表格的文档编辑场景。
         */
        fun complex(): RichTextConfig = default().copy(
            maxUndoHistory = 200,
            undoMergeIntervalMs = 300,
            table = TableConfig(
                maxInsertRows = 30,
                maxInsertColumns = 8,
            ),
        )
    }
}

val LocalRichTextConfig = staticCompositionLocalOf { RichTextConfig() }
