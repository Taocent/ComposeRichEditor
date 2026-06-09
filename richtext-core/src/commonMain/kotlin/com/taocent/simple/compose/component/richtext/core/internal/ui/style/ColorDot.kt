package com.taocent.simple.compose.component.richtext.core.internal.ui.style

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@Composable
fun ColorDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dotConfig = LocalRichTextConfig.current.colorDot
    Box(
        modifier = Modifier
            .size(if (isSelected) dotConfig.selectedSize else dotConfig.size)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(dotConfig.selectedBorderWidth, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier.border(dotConfig.borderWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                }
            )
            .clickable(onClick = onClick)
    )
}
