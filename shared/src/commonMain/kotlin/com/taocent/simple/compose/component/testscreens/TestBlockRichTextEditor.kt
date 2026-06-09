package com.taocent.simple.compose.component.testscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.DetailScreen
import com.taocent.simple.compose.component.blockrichtext.BlockRichTextEditor
import com.taocent.simple.compose.component.blockrichtext.BlockState
import com.taocent.simple.compose.component.blockrichtext.ExperimentalBlockRichTextApi
import com.taocent.simple.compose.component.blockrichtext.rememberBlockState

@OptIn(ExperimentalBlockRichTextApi::class)
@Composable
fun TestBlockRichTextEditor(onBack: () -> Unit, showTopBar: Boolean = true) {
    val state = rememberBlockState()

    if (showTopBar) {
        DetailScreen(title = "块级富文本编辑器", onBack = onBack) { padding ->
            BlockRichTextEditorContent(state, Modifier.padding(padding))
        }
    } else {
        BlockRichTextEditorContent(state, Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalBlockRichTextApi::class)
@Composable
private fun BlockRichTextEditorContent(
    state: BlockState,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        BlockRichTextEditor(
            state = state,
            modifier = Modifier.fillMaxSize()
        )

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "使用说明",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """
                    • 块级富文本编辑器支持多文本块
                    • 每个文本块独立编辑，支持完整的富文本格式化
                    • 选中文本后浮动工具栏支持格式化、复制、剪切、粘贴、全选
                    • 底部工具栏支持撤销、重做、表情、文字样式、超链接、导出
                    • 撤销/重做跨块全局管理
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
