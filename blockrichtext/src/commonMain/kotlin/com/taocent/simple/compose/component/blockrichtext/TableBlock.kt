package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.ui.text.AnnotatedString

@ExperimentalBlockRichTextApi
data class TableBlock(
    override val id: String = generateBlockId(),
    val rows: List<TableRow>,
    val columnCount: Int = rows.firstOrNull()?.cells?.size ?: 0
) : DocumentBlock {

    data class TableRow(
        val rowId: String = generateBlockId(),
        val cells: List<TableCell>,
        val isHeader: Boolean = false
    )

    data class TableCell(
        val cellId: String = generateBlockId(),
        val content: AnnotatedString = AnnotatedString("")
    )

    companion object {
        fun create(rows: Int, columns: Int): TableBlock {
            val tableRows = List(rows) { rowIndex ->
                TableRow(
                    cells = List(columns) { TableCell() },
                    isHeader = rowIndex == 0
                )
            }
            return TableBlock(rows = tableRows, columnCount = columns)
        }
    }
}
