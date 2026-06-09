package com.taocent.simple.compose.component.richtext.core.internal.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@Composable
fun ToolbarCategoryItem(
    icon: @Composable () -> Unit,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tbConfig = LocalRichTextConfig.current.floatingToolbar
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(containerColor)
            .alpha(if (enabled) 1f else 0.38f)
            .focusProperties { canFocus = false }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = tbConfig.categoryPaddingH, vertical = tbConfig.categoryPaddingV)
    ) {
        Box(modifier = Modifier.size(tbConfig.categoryIconSize), contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
