package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 覆盖 [TextBlock] 与 [TableBlock] 数据模型的基础契约。
 */
@OptIn(ExperimentalBlockRichTextApi::class)
class DocumentBlockTest {

    // region TextBlock

    @Test
    fun newTextBlockHasUniqueIdAndEmptyContent() {
        val a = TextBlock()
        val b = TextBlock()
        assertTrue(a.id.isNotEmpty())
        assertTrue(b.id.isNotEmpty())
        assertNotEquals(a.id, b.id)
        assertEquals(AnnotatedString(""), a.content)
    }

    @Test
    fun textBlockContentEqualsConstructorArg() {
        val block = TextBlock(content = AnnotatedString("hello"))
        assertEquals("hello", block.content.text)
    }

    @Test
    fun textBlockCopyWithNewContentKeepsId() {
        val block = TextBlock(content = AnnotatedString("old"))
        val updated = block.copy(content = AnnotatedString("new"))
        assertEquals(block.id, updated.id)
        assertEquals("new", updated.content.text)
    }

    // endregion

    // region TableBlock

    @Test
    fun tableBlockCreateBuildsHeaderAndRows() {
        val table = TableBlock.create(rows = 3, columns = 2)
        assertEquals(3, table.rows.size)
        assertEquals(2, table.columnCount)
        // 第一行是表头
        assertTrue(table.rows.first().isHeader)
        // 其余行不是表头
        for (i in 1 until table.rows.size) {
            assertEquals(false, table.rows[i].isHeader)
        }
    }

    @Test
    fun tableBlockCreateCellsHaveUniqueIds() {
        val table = TableBlock.create(rows = 2, columns = 2)
        val ids = table.rows.flatMap { row -> row.cells.map { it.cellId } }
        assertEquals(4, ids.size)
        assertEquals(ids.size, ids.toSet().size, "cell ids should be unique")
    }

    @Test
    fun tableBlockCreateRowsHaveUniqueIds() {
        val table = TableBlock.create(rows = 3, columns = 1)
        val ids = table.rows.map { it.rowId }
        assertEquals(3, ids.size)
        assertEquals(ids.size, ids.toSet().size, "row ids should be unique")
    }

    @Test
    fun tableBlockDefaultColumnCountDerivedFromRows() {
        val rows = listOf(
            TableBlock.TableRow(cells = listOf(TableBlock.TableCell(), TableBlock.TableCell()))
        )
        val table = TableBlock(rows = rows)
        assertEquals(2, table.columnCount)
    }

    @Test
    fun tableBlockEmptyRowsResultsInZeroColumns() {
        val table = TableBlock(rows = emptyList())
        assertEquals(0, table.columnCount)
    }

    @Test
    fun tableBlockRowDefaultsToNonHeader() {
        val row = TableBlock.TableRow(cells = listOf(TableBlock.TableCell()))
        assertEquals(false, row.isHeader)
    }

    @Test
    fun tableBlockRowCanBeMarkedAsHeader() {
        val row = TableBlock.TableRow(cells = listOf(TableBlock.TableCell()), isHeader = true)
        assertEquals(true, row.isHeader)
    }

    @Test
    fun tableBlockCellDefaultsToEmptyContent() {
        val cell = TableBlock.TableCell()
        assertEquals("", cell.content.text)
    }

    @Test
    fun tableBlockCellCarriesCustomContent() {
        val cell = TableBlock.TableCell(content = AnnotatedString("x"))
        assertEquals("x", cell.content.text)
    }

    @Test
    fun tableBlockCopyPreservesIdentity() {
        val original = TableBlock.create(rows = 2, columns = 2)
        val newRow = TableBlock.TableRow(cells = listOf(TableBlock.TableCell()))
        val copy = original.copy(rows = original.rows + newRow)
        assertEquals(original.id, copy.id)
        assertEquals(3, copy.rows.size)
    }

    // endregion
}
