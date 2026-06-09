package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.internal.format.FormatManager

class RichTextFormat internal constructor(
    private val state: RichTextState,
    private val manager: FormatManager
) {
    val hasSelection: Boolean
        get() = state.hasSelection

    var bold: Boolean
        get() = state.currentBold
        set(value) { if (value != state.currentBold) manager.toggleBold() }

    var italic: Boolean
        get() = state.currentItalic
        set(value) { if (value != state.currentItalic) manager.toggleItalic() }

    var underline: Boolean
        get() = state.currentUnderline
        set(value) { if (value != state.currentUnderline) manager.toggleUnderline() }

    var strikethrough: Boolean
        get() = state.currentStrikethrough
        set(value) { if (value != state.currentStrikethrough) manager.toggleStrikethrough() }

    var superscript: Boolean
        get() = state.currentSuperscript
        set(value) { if (value != state.currentSuperscript) manager.toggleSuperscript() }

    var subscript: Boolean
        get() = state.currentSubscript
        set(value) { if (value != state.currentSubscript) manager.toggleSubscript() }

    fun bold() { manager.toggleBold() }
    fun italic() { manager.toggleItalic() }
    fun underline() { manager.toggleUnderline() }
    fun strikethrough() { manager.toggleStrikethrough() }
    fun superscript() { manager.toggleSuperscript() }
    fun subscript() { manager.toggleSubscript() }

    fun color(color: Color) {
        manager.setColor(color)
    }

    fun backgroundColor(color: Color) {
        manager.setBackground(color)
    }

    fun fontSize(size: TextUnit) {
        manager.setFontSize(size)
    }

    fun textAlign(align: TextAlign) {
        manager.setTextAlign(align)
    }

    fun alignLeft() {
        manager.setTextAlign(TextAlign.Left)
    }

    fun alignCenter() {
        manager.setTextAlign(TextAlign.Center)
    }

    fun alignRight() {
        manager.setTextAlign(TextAlign.Right)
    }

    fun clearFormatting() {
        manager.clearFormatting()
    }

    fun hyperlink(url: String, text: String = "") {
        state.insertHyperlink(text, url)
    }
}
