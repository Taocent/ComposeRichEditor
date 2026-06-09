package com.taocent.simple.compose.component.richtext.core.internal.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.internal.format.RichTextFormatState
import com.taocent.simple.compose.component.richtext.core.internal.ui.style.ColorDot
import com.taocent.simple.compose.component.richtext.core.internal.ui.style.StyleChip
import com.taocent.simple.compose.component.richtext.core.internal.ui.style.StyleTextToggle
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextStylePanel(
    state: RichTextFormatState,
    restoreSelection: () -> Unit,
    onActionCompleted: () -> Unit
) {
    val config = LocalRichTextConfig.current
    val panelConfig = config.panel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = panelConfig.stylePanelPaddingH, vertical = panelConfig.stylePanelPaddingV),
        verticalArrangement = Arrangement.spacedBy(panelConfig.stylePanelSpacing)
    ) {
        PanelSection(title = "字号") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(panelConfig.stylePanelChipSpacing)
            ) {
                config.presetFontSizes.forEach { item ->
                    val isSelected = state.currentFontSize == item.size
                    StyleChip(
                        text = item.label,
                        isSelected = isSelected,
                        onClick = {
                            restoreSelection()
                            state.setFontSize(item.size)
                            onActionCompleted()
                        }
                    )
                }
                if (state.currentFontSize != TextUnit.Unspecified) {
                    StyleChip(
                        text = "默认",
                        isSelected = false,
                        onClick = {
                            restoreSelection()
                            state.setFontSize(TextUnit.Unspecified)
                            onActionCompleted()
                        }
                    )
                }
            }
        }

        PanelSection(title = "样式") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(panelConfig.stylePanelChipSpacing),
                verticalArrangement = Arrangement.spacedBy(panelConfig.stylePanelChipSpacing)
            ) {
                StyleTextToggle(
                    text = "B",
                    isActive = state.currentBold,
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    onClick = {
                        restoreSelection()
                        state.toggleBold()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "I",
                    isActive = state.currentItalic,
                    textStyle = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic),
                    onClick = {
                        restoreSelection()
                        state.toggleItalic()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "U",
                    isActive = state.currentUnderline,
                    textStyle = androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.Underline),
                    onClick = {
                        restoreSelection()
                        state.toggleUnderline()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "S",
                    isActive = state.currentStrikethrough,
                    textStyle = androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough),
                    onClick = {
                        restoreSelection()
                        state.toggleStrikethrough()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "x²",
                    isActive = state.currentSuperscript,
                    onClick = {
                        restoreSelection()
                        state.toggleSuperscript()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "x₂",
                    isActive = state.currentSubscript,
                    onClick = {
                        restoreSelection()
                        state.toggleSubscript()
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "✕",
                    isActive = false,
                    onClick = {
                        restoreSelection()
                        state.clearFormatting()
                        onActionCompleted()
                    }
                )
            }
        }

        PanelSection(title = "段落") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(panelConfig.stylePanelChipSpacing),
                verticalArrangement = Arrangement.spacedBy(panelConfig.stylePanelChipSpacing)
            ) {
                StyleTextToggle(
                    text = "左",
                    isActive = state.currentTextAlign == TextAlign.Left,
                    onClick = {
                        restoreSelection()
                        state.setTextAlign(TextAlign.Left)
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "中",
                    isActive = state.currentTextAlign == TextAlign.Center,
                    onClick = {
                        restoreSelection()
                        state.setTextAlign(TextAlign.Center)
                        onActionCompleted()
                    }
                )
                StyleTextToggle(
                    text = "右",
                    isActive = state.currentTextAlign == TextAlign.Right,
                    onClick = {
                        restoreSelection()
                        state.setTextAlign(TextAlign.Right)
                        onActionCompleted()
                    }
                )
            }
        }

        PanelSection(title = "文字颜色") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                config.presetColors.forEach { color ->
                    val isSelected = state.currentColor == color
                    ColorDot(
                        color = color,
                        isSelected = isSelected,
                        onClick = {
                            restoreSelection()
                            if (isSelected) state.setColor(Color.Unspecified) else state.setColor(color)
                            onActionCompleted()
                        }
                    )
                }
                if (state.currentColor != Color.Unspecified) {
                    StyleChip(
                        text = "默认",
                        isSelected = false,
                        onClick = {
                            restoreSelection()
                            state.setColor(Color.Unspecified)
                            onActionCompleted()
                        }
                    )
                }
            }
        }

        PanelSection(title = "背景颜色") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                config.presetBackgroundColors.forEach { bgColor ->
                    val isSelected = state.currentBackground == bgColor
                    ColorDot(
                        color = bgColor,
                        isSelected = isSelected,
                        onClick = {
                            restoreSelection()
                            if (isSelected) state.setBackground(Color.Unspecified) else state.setBackground(bgColor)
                            onActionCompleted()
                        }
                    )
                }
                if (state.currentBackground != Color.Unspecified) {
                    StyleChip(
                        text = "默认",
                        isSelected = false,
                        onClick = {
                            restoreSelection()
                            state.setBackground(Color.Unspecified)
                            onActionCompleted()
                        }
                    )
                }
            }
        }
    }
}
