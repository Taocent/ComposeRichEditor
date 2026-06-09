package com.taocent.simple.compose.component.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 创建一个 [RichTextState] 实例并通过 [remember] 持有，避免 recomposition 时丢失状态。
 */
@Composable
fun rememberRichTextState(
    initialText: String = "",
    maxUndoHistory: Int = 100,
    undoMergeIntervalMs: Long = 500
): RichTextState {
    return remember {
        RichTextState(
            initialText = initialText,
            maxUndoHistory = maxUndoHistory,
            undoMergeIntervalMs = undoMergeIntervalMs
        )
    }
}
