package com.taocent.simple.compose.component.richtext.core.internal.ui.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@Composable
fun StyleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipConfig = LocalRichTextConfig.current.styleChip
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(chipConfig.radius),
        border = BorderStroke(
            chipConfig.borderWidth,
            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = chipConfig.contentPaddingH, vertical = chipConfig.contentPaddingV),
        modifier = Modifier.height(chipConfig.height)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
