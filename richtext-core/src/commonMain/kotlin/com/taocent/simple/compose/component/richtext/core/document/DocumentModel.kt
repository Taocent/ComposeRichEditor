package com.taocent.simple.compose.component.richtext.core.document

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit

data class DocumentModel(
    val blocks: List<BlockModel> = listOf(BlockModel.Text())
)

sealed interface BlockModel {
    val id: String

    data class Text(
        override val id: String = generateDocumentBlockId(),
        val type: TextBlockType = TextBlockType.Paragraph,
        val inlineContent: InlineModel = InlineModel(),
        val paragraphStyle: ParagraphModel = ParagraphModel()
    ) : BlockModel

    data class Table(
        override val id: String = generateDocumentBlockId(),
        val rows: List<TableRowModel>,
    ) : BlockModel
}

enum class TextBlockType {
    Paragraph,
    Heading,
    Quote,
    Code,
    Todo
}

data class TableRowModel(
    val id: String = generateDocumentBlockId(),
    val cells: List<TableCellModel>,
    val isHeader: Boolean = false
)

data class TableCellModel(
    val id: String = generateDocumentBlockId(),
    val document: DocumentModel = DocumentModel(emptyList())
)

data class InlineModel(
    val text: String = "",
    val styleRuns: List<InlineStyleRun> = emptyList(),
    val annotations: List<InlineAnnotation> = emptyList()
)

data class InlineStyleRun(
    val start: Int,
    val end: Int,
    val style: InlineStyleModel
)

data class InlineAnnotation(
    val start: Int,
    val end: Int,
    val tag: String,
    val value: String
)

data class InlineStyleModel(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val lineThrough: Boolean = false,
    val color: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val baselineShift: BaselineShift? = null
) {
    fun toSpanStyle(): SpanStyle {
        val decorations = buildList {
            if (underline) add(TextDecoration.Underline)
            if (lineThrough) add(TextDecoration.LineThrough)
        }
        return SpanStyle(
            fontWeight = if (bold) androidx.compose.ui.text.font.FontWeight.Bold else null,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
            textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations),
            color = color,
            background = background,
            fontSize = fontSize,
            baselineShift = baselineShift
        )
    }
}

data class ParagraphModel(
    val textAlign: TextAlign = TextAlign.Left
) {
    fun toParagraphStyle(): ParagraphStyle {
        return ParagraphStyle(textAlign = textAlign)
    }
}

private var documentBlockIdCounter = 0L

internal fun generateDocumentBlockId(): String {
    return "doc_block_${++documentBlockIdCounter}"
}
