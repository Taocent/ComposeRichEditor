package com.taocent.simple.compose.component.richtext

// 类型别名 — 保持原包名导入,实现细节下沉到 :richtext-core
// 顶层 val/default values 通过 import 跨文件共享,见 DefaultsImports.kt
typealias RichTextState = com.taocent.simple.compose.component.richtext.core.RichTextState
typealias RichTextFormat = com.taocent.simple.compose.component.richtext.core.RichTextFormat
typealias RichTextConfig = com.taocent.simple.compose.component.richtext.core.RichTextConfig
typealias PanelConfig = com.taocent.simple.compose.component.richtext.core.PanelConfig
typealias CustomEmoji = com.taocent.simple.compose.component.richtext.core.internal.emoji.CustomEmoji
typealias FontSizeItem = com.taocent.simple.compose.component.richtext.core.FontSizeItem
typealias TextFieldConfig = com.taocent.simple.compose.component.richtext.core.TextFieldConfig
typealias FloatingToolbarConfig = com.taocent.simple.compose.component.richtext.core.FloatingToolbarConfig
typealias StyleChipConfig = com.taocent.simple.compose.component.richtext.core.StyleChipConfig
typealias StyleTextToggleConfig = com.taocent.simple.compose.component.richtext.core.StyleTextToggleConfig
typealias ColorDotConfig = com.taocent.simple.compose.component.richtext.core.ColorDotConfig
typealias EmojiPanelConfig = com.taocent.simple.compose.component.richtext.core.EmojiPanelConfig
