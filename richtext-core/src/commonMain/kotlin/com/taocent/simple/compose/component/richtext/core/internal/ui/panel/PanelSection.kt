package com.taocent.simple.compose.component.richtext.core.internal.ui.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig

@Composable
fun PanelSection(
    title: String,
    content: @Composable () -> Unit
) {
    val panelConfig = LocalRichTextConfig.current.panel
    Column(verticalArrangement = Arrangement.spacedBy(panelConfig.sectionSpacing)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        content()
    }
}
