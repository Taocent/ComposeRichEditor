package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString
import com.taocent.simple.compose.component.richtext.core.PasteResult
import com.taocent.simple.compose.component.richtext.core.RichTextError
import com.taocent.simple.compose.component.richtext.core.RichTextState

/**
 * 上报错误的回调类型 — 由 [RichTextState] 注入,内部模块解耦。
 */
internal typealias ClipboardErrorReporter = (RichTextError, String) -> Unit

/**
 * 内部剪贴板控制器:基于新的 [Clipboard] 接口(suspend API),
 * 在剪贴板读写前后与 [RichTextState] 的 emoji-id 缓冲协议保持同步。
 */
internal class RichTextClipboardManager(
    private val clipboard: Clipboard,
    private val state: RichTextState,
    private val errorReporter: ClipboardErrorReporter? = null,
) {
    /**
     * 写入文本到剪贴板。失败时返回 [PasteResult.Failure],错误同时通过 reporter 上报。
     */
    suspend fun setText(annotatedString: AnnotatedString): PasteResult<Unit> = try {
        val clipAnnotated = state.setCopyBufferFromAnnotated(annotatedString)
        clipboard.setClipEntry(clipAnnotated.toClipEntry())
        PasteResult.success(Unit)
    } catch (e: Throwable) {
        val error = RichTextError.ClipboardWrite(e)
        errorReporter?.invoke(error, "RichTextClipboardManager.setText")
        PasteResult.failure(error)
    }

    /**
     * 从剪贴板读取文本。成功时 [PasteResult.Success.value] 为 AnnotatedString(可能为空字符串但不会 null);
     * 剪贴板无内容或权限拒绝时 [PasteResult.Failure]。
     */
    suspend fun getText(): PasteResult<AnnotatedString> = try {
        val entry = clipboard.getClipEntry()
        if (entry == null) {
            PasteResult.success(AnnotatedString(""))
        } else {
            val text = entry.toAnnotatedStringOrNull()
            if (text == null) {
                PasteResult.success(AnnotatedString(""))
            } else {
                val resolved = state.resolveClipboard(text) ?: AnnotatedString("")
                PasteResult.success(resolved)
            }
        }
    } catch (e: Throwable) {
        val error = RichTextError.ClipboardRead(e)
        errorReporter?.invoke(error, "RichTextClipboardManager.getText")
        PasteResult.failure(error)
    }
}

/**
 * 将 [AnnotatedString] 写入剪贴板前,构造一个携带其纯文本的 [ClipEntry]。
 * public 暴露以便 :richtext / :blockrichtext 内部 UI 使用。
 */
expect fun AnnotatedString.toClipEntry(): ClipEntry

/**
 * 从剪贴板 [ClipEntry] 中读取纯文本并包装为 [AnnotatedString]。
 * 在 Web 平台上由于 [Clipboard.getClipEntry] 的异步特性,实现为 suspend,
 * 其他平台可同步读取,实际签名差异由编译器允许。
 */
expect suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString?

/**
 * Public 跨模块剪贴板写入函数 — 供 :richtext / :blockrichtext 内部 UI 调用。
 * 内部封装 emoji-id 缓冲协议,失败时通过 [RichTextState.reportError] 上报。
 */
suspend fun clipboardSetText(
    clipboard: Clipboard,
    state: RichTextState,
    text: AnnotatedString,
): PasteResult<Unit> {
    return RichTextClipboardManager(clipboard, state, state::reportError).setText(text)
}

/**
 * Public 跨模块剪贴板读取函数 — 失败时通过 [RichTextState.reportError] 上报。
 */
suspend fun clipboardGetText(
    clipboard: Clipboard,
    state: RichTextState,
): PasteResult<AnnotatedString> {
    return RichTextClipboardManager(clipboard, state, state::reportError).getText()
}
