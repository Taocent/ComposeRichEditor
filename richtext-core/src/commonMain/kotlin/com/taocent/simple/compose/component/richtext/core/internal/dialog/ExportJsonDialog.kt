package com.taocent.simple.compose.component.richtext.core.internal.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.toClipEntry
import kotlinx.coroutines.launch

@Composable
fun ExportJsonDialog(
    jsonContent: String,
    onDismiss: () -> Unit
) {
    val config = LocalRichTextConfig.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val formattedJson = remember(jsonContent) { formatJson(jsonContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出 JSON") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = config.panel.exportDialogMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = formattedJson,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(AnnotatedString(jsonContent).toClipEntry())
                    }
                    onDismiss()
                }
            ) {
                Text("复制")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatJson(json: String): String {
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var escaped = false

    for (c in json) {
        when {
            escaped -> {
                sb.append(c)
                escaped = false
            }
            c == '\\' && inString -> {
                sb.append(c)
                escaped = true
            }
            c == '"' -> {
                sb.append(c)
                inString = !inString
            }
            inString -> sb.append(c)
            c == '{' || c == '[' -> {
                sb.append(c)
                indent++
                sb.append('\n')
                sb.append("  ".repeat(indent))
            }
            c == '}' || c == ']' -> {
                indent--
                sb.append('\n')
                sb.append("  ".repeat(indent))
                sb.append(c)
            }
            c == ',' -> {
                sb.append(c)
                sb.append('\n')
                sb.append("  ".repeat(indent))
            }
            c == ':' -> {
                sb.append(c)
                sb.append(' ')
            }
            c.isWhitespace() -> {}
            else -> sb.append(c)
        }
    }
    return sb.toString()
}
