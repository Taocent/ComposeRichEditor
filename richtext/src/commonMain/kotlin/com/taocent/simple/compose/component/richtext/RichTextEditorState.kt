package com.taocent.simple.compose.component.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 工具栏 / 面板的展开分类 — 供 [RichTextToolbar] / [RichTextPanel] 协调使用。
 *
 * 公开枚举允许使用方通过 [RichTextEditorState.activeCategory] 观察当前展开分类,
 * 决定是否在外部 UI 中显示"输入中"指示器等。增加新分类时,需同步在 [RichTextToolbar] /
 * [RichTextPanel] 中加入对应按钮与渲染分支。
 */
enum class ToolCategory {
    /** EmojiPanel 已展开 */
    EMOJI,

    /** TextStylePanel 已展开 */
    TEXT_STYLE,

    /** 链接对话框已展开(目前由 [RichTextEditorState.showLinkDialog] 跟踪,保留用于未来 inline 链接模式) */
    LINK,
}

/**
 * [RichTextEditor] 拆解后,跨多个 Composable 共享的可观察状态容器。
 *
 * 作用:让 [RichTextToolbar] / [RichTextPanel] / [RichTextEditorDialogs] 三个独立组件
 * 共享展开分类、对话框可见性、键盘高度记忆等 UI 状态,使用方既可以一行 `RichTextEditor(state)`
 * 拿到开箱即用体验,也可以自行拼装三件套。
 *
 * @see rememberRichTextEditorState
 */
@Stable
class RichTextEditorState {
    /**
     * 当前展开的工具分类 — `null` 表示未展开任何面板。
     *
     * 类型为 [ToolCategory]?(内部枚举,使用方应通过 IDE 自动补全理解可能的取值,
     * 避免硬编码具体常量名)。
     */
    var activeCategory: ToolCategory? by mutableStateOf(null)
        private set

    /** 链接对话框是否可见。 */
    var showLinkDialog: Boolean by mutableStateOf(false)
        private set

    /** 导出 JSON 对话框是否可见。 */
    var showExportDialog: Boolean by mutableStateOf(false)
        private set

    internal var restoreKeyboardAfterDialog: Boolean by mutableStateOf(false)
        private set

    /**
     * 键盘可见期间记录的最大高度(用于键盘隐藏后仍保持面板高度稳定,
     * 避免 `Surface` 高度跳变)。
     */
    internal var rememberedKeyboardHeight: Dp by mutableStateOf(0.dp)

    // region 写操作 API — Toolbar 调用

    internal fun toggleCategory(category: ToolCategory) {
        activeCategory = if (activeCategory == category) null else category
    }

    internal fun clearCategory() {
        activeCategory = null
    }

    internal fun requestLinkDialog(restoreKeyboardAfterDismiss: Boolean = false) {
        restoreKeyboardAfterDialog = restoreKeyboardAfterDismiss
        showLinkDialog = true
    }

    internal fun dismissLinkDialog() {
        showLinkDialog = false
    }

    internal fun requestExportDialog(restoreKeyboardAfterDismiss: Boolean = false) {
        restoreKeyboardAfterDialog = restoreKeyboardAfterDismiss
        showExportDialog = true
    }

    internal fun dismissExportDialog() {
        showExportDialog = false
    }

    internal fun consumeKeyboardRestoreRequest(): Boolean {
        val shouldRestore = restoreKeyboardAfterDialog
        restoreKeyboardAfterDialog = false
        return shouldRestore
    }

    /** 关闭所有面板 / 对话框 — 切换页面或失焦时使用。 */
    fun dismissAll() {
        activeCategory = null
        showLinkDialog = false
        showExportDialog = false
        restoreKeyboardAfterDialog = false
    }
}

/**
 * 工厂:[RichTextEditorState] 的 `remember` 包装。
 * 调用方应在 [RichTextEditor] / [RichTextToolbar] 上层 `remember` 一次,
 * 同一组 Composable 共享。
 */
@Composable
fun rememberRichTextEditorState(): RichTextEditorState = remember { RichTextEditorState() }
