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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smile
import com.taocent.simple.compose.component.DetailScreen
import com.taocent.simple.compose.component.richtext.RichTextEditor
import com.taocent.simple.compose.component.richtext.rememberRichTextState

@Composable
fun TestRichTextEditor(onBack: () -> Unit, showTopBar: Boolean = true) {
    val state = rememberRichTextState()

    if (showTopBar) {
        DetailScreen(title = "富文本编辑器", onBack = onBack) { padding ->
            RichTextEditorContent(state, Modifier.padding(padding))
        }
    } else {
        RichTextEditorContent(state, Modifier.fillMaxSize())
    }
}

@Composable
private fun RichTextEditorContent(
    state: com.taocent.simple.compose.component.richtext.RichTextState,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxSize(),
            placeholder = "在此输入富文本内容...\n\n支持加粗、斜体、下划线、颜色和字号设置"
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
                    • 选中文本后点击 B/I/U 按钮切换格式
                    • 点击颜色圆点设置文字颜色
                    • 点击字号按钮调整字体大小
                    • 点击 ✕ 清除选中区域格式
                    • 未选中文本时设置格式，后续输入的文字将应用该格式
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
