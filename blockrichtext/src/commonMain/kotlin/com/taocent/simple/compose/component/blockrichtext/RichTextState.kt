package com.taocent.simple.compose.component.blockrichtext

/**
 * :blockrichtext 类型别名与 Re-export — 保持原包名导入,实现细节下沉到 :richtext-core。
 * 顶层 val/default values 通过 import 跨文件共享。
 *
 * 注:[RichTextConfig] 与 [LocalRichTextConfig] 实际定义在 :richtext-core,
 * 但通过顶层 typealias 暴露同名 API。
 */

typealias PanelConfig = com.taocent.simple.compose.component.richtext.core.PanelConfig
typealias TextFieldConfig = com.taocent.simple.compose.component.richtext.core.TextFieldConfig
typealias FloatingToolbarConfig = com.taocent.simple.compose.component.richtext.core.FloatingToolbarConfig
typealias StyleChipConfig = com.taocent.simple.compose.component.richtext.core.StyleChipConfig
typealias StyleTextToggleConfig = com.taocent.simple.compose.component.richtext.core.StyleTextToggleConfig
typealias ColorDotConfig = com.taocent.simple.compose.component.richtext.core.ColorDotConfig
typealias EmojiPanelConfig = com.taocent.simple.compose.component.richtext.core.EmojiPanelConfig
typealias TableConfig = com.taocent.simple.compose.component.richtext.core.TableConfig

typealias RichTextState = com.taocent.simple.compose.component.richtext.core.RichTextState
typealias RichTextFormat = com.taocent.simple.compose.component.richtext.core.RichTextFormat
typealias RichTextConfig = com.taocent.simple.compose.component.richtext.core.RichTextConfig
typealias FontSizeItem = com.taocent.simple.compose.component.richtext.core.FontSizeItem
typealias CustomEmoji = com.taocent.simple.compose.component.richtext.core.internal.emoji.CustomEmoji

/**
 * Re-export [LocalRichTextConfig] — 委托到 :richtext-core,保持导入路径
 * `com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig`。
 */
val LocalRichTextConfig: androidx.compose.runtime.ProvidableCompositionLocal<RichTextConfig>
    get() = com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig
