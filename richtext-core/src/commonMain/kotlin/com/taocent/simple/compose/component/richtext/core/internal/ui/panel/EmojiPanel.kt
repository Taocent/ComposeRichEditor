package com.taocent.simple.compose.component.richtext.core.internal.ui.panel

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

private enum class EmojiTab { CUSTOM, UNICODE }

@Composable
fun EmojiPanel(
    onEmojiSelected: (String) -> Unit,
    onCustomEmojiSelected: (String) -> Unit = {}
) {
    val config = LocalRichTextConfig.current
    val emojiConfig = config.emojiPanel
    var currentTab by remember { mutableStateOf(EmojiTab.CUSTOM) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = emojiConfig.tabPaddingH, vertical = emojiConfig.tabPaddingV),
            horizontalArrangement = Arrangement.spacedBy(emojiConfig.tabSpacing)
        ) {
            TabChip(
                label = "图标",
                selected = currentTab == EmojiTab.CUSTOM,
                onClick = { currentTab = EmojiTab.CUSTOM },
                emojiConfig = emojiConfig
            )
            TabChip(
                label = "表情",
                selected = currentTab == EmojiTab.UNICODE,
                onClick = { currentTab = EmojiTab.UNICODE },
                emojiConfig = emojiConfig
            )
        }

        when (currentTab) {
            EmojiTab.CUSTOM -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.customEmojiGridColumns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = emojiConfig.gridPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(emojiConfig.gridHSpacing),
                    verticalArrangement = Arrangement.spacedBy(emojiConfig.gridVSpacing),
                    contentPadding = PaddingValues(emojiConfig.gridContentPadding)
                ) {
                    items(config.customEmojis) { emoji ->
                        val icon = config.customEmojiIcons[emoji.id]
                        Box(
                            modifier = Modifier
                                .size(emojiConfig.itemBoxSize)
                                .clip(RoundedCornerShape(emojiConfig.itemRadius))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onCustomEmojiSelected(emoji.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (icon != null) {
                                Image(
                                    imageVector = icon,
                                    contentDescription = emoji.name,
                                    modifier = Modifier.size(config.customEmojiItemSize),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }
                }
            }
            EmojiTab.UNICODE -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(config.emojiGridColumns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = emojiConfig.gridPaddingH),
                    horizontalArrangement = Arrangement.spacedBy(emojiConfig.gridHSpacing),
                    verticalArrangement = Arrangement.spacedBy(emojiConfig.gridVSpacing),
                    contentPadding = PaddingValues(emojiConfig.gridContentPadding)
                ) {
                    items(config.emojiList) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(emojiConfig.itemBoxSize)
                                .clip(RoundedCornerShape(emojiConfig.itemRadius))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { onEmojiSelected(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = config.emojiItemSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    emojiConfig: com.taocent.simple.compose.component.richtext.core.EmojiPanelConfig
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(emojiConfig.tabChipRadius))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = emojiConfig.tabChipPaddingH, vertical = emojiConfig.tabChipPaddingV)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
