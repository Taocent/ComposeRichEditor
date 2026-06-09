package com.taocent.simple.compose.component.richtext.core.internal.ui.style

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@Composable
fun StyleTextToggle(
    text: String,
    isActive: Boolean,
    textStyle: TextStyle = TextStyle(),
    onClick: () -> Unit
) {
    val toggleConfig = LocalRichTextConfig.current.styleTextToggle
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(toggleConfig.radius))
            .border(
                width = toggleConfig.borderWidth,
                color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(toggleConfig.radius)
            )
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = toggleConfig.contentPaddingH, vertical = toggleConfig.contentPaddingV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.merge(textStyle),
            color = contentColor
        )
    }
}
