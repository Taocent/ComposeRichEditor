package com.taocent.simple.compose.component.blockrichtext.internal.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig

/**
 * 表格插入对话框 — :blockrichtext 独有(因为表格功能是 block 级别独有的)。
 * 与 :richtext-core 的通用 dialog(LinkDialog/ExportJsonDialog) 在不同包中。
 */
@Composable
internal fun TableInsertDialog(
    onDismiss: () -> Unit,
    onConfirm: (rows: Int, columns: Int) -> Unit
) {
    val config = LocalRichTextConfig.current
    val tableConfig = config.table
    var rowsText by remember { mutableStateOf("3") }
    var columnsText by remember { mutableStateOf("3") }
    val rows = rowsText.toIntOrNull() ?: 3
    val columns = columnsText.toIntOrNull() ?: 3
    val isValidRows = rows in 1..tableConfig.maxInsertRows
    val isValidColumns = columns in 1..tableConfig.maxInsertColumns
    val canConfirm = isValidRows && isValidColumns

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插入表格") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rowsText,
                        onValueChange = { rowsText = it.filter(Char::isDigit) },
                        label = { Text("行数") },
                        isError = !isValidRows,
                        supportingText = {
                            if (!isValidRows) Text("范围 1-${tableConfig.maxInsertRows}")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).width(tableConfig.insertDialogFieldWidth)
                    )
                    OutlinedTextField(
                        value = columnsText,
                        onValueChange = { columnsText = it.filter(Char::isDigit) },
                        label = { Text("列数") },
                        isError = !isValidColumns,
                        supportingText = {
                            if (!isValidColumns) Text("范围 1-${tableConfig.maxInsertColumns}")
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(rows, columns) },
                enabled = canConfirm
            ) {
                Text("确定", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
