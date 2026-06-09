package com.taocent.simple.compose.component.blockrichtext.internal.block

internal sealed interface EditorCursor {
    data class Text(
        val blockId: String,
        val selectionStart: Int = 0,
        val selectionEnd: Int = selectionStart
    ) : EditorCursor

    data class TableCell(
        val tableId: String,
        val rowId: String,
        val cellId: String
    ) : EditorCursor

    data class BlockAnchor(
        val blockId: String,
        val position: AnchorPosition
    ) : EditorCursor
}

internal enum class AnchorPosition {
    Before,
    After
}
