package com.taocent.simple.compose.component.richtext.core.internal.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.internal.format.RichTextFormatState

@Composable
fun LinkDialog(
    state: RichTextFormatState,
    restoreSelection: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var linkDisplayText by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        title = { Text("插入超链接") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = linkDisplayText,
                    onValueChange = { linkDisplayText = it },
                    label = { Text("显示文本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("链接地址 (URL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (linkDisplayText.isNotEmpty()) {
                        focusManager.clearFocus()
                        restoreSelection()
                        state.insertHyperlink(linkDisplayText, linkUrl)
                        onConfirm()
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onDismiss()
                }
            ) { Text("取消") }
        }
    )
}
