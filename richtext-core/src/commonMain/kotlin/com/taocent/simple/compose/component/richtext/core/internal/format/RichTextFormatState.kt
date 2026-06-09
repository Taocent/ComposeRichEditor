package com.taocent.simple.compose.component.richtext.core.internal.format

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit

/**
 * 公开的样式状态接口 — UI 子模块通过此接口调用核心格式 API,
 * 避免直接依赖具体实现。
 */
interface RichTextFormatState {
    val currentFontSize: TextUnit
    val currentColor: Color
    val currentBackground: Color
    val currentBold: Boolean
    val currentItalic: Boolean
    val currentUnderline: Boolean
    val currentStrikethrough: Boolean
    val currentSuperscript: Boolean
    val currentSubscript: Boolean
    val currentTextAlign: TextAlign

    fun currentSpanStyle(): SpanStyle

    fun toggleBold()
    fun toggleItalic()
    fun toggleUnderline()
    fun toggleStrikethrough()
    fun toggleSuperscript()
    fun toggleSubscript()
    fun clearFormatting()
    fun setFontSize(size: TextUnit)
    fun setColor(color: Color)
    fun setBackground(color: Color)
    fun setTextAlign(align: TextAlign)

    fun saveSelection()
    fun restoreSavedSelection()

    fun undo()
    fun redo()

    fun insertText(text: String)
    fun insertCustomEmoji(emojiId: String, displaySize: TextUnit)
    fun insertHyperlink(text: String, url: String)

    fun toJson(): String
}
