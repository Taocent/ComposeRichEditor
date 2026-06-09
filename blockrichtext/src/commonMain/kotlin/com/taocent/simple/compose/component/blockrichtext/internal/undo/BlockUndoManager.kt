package com.taocent.simple.compose.component.blockrichtext.internal.undo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.taocent.simple.compose.component.blockrichtext.DocumentBlock
import com.taocent.simple.compose.component.blockrichtext.internal.block.EditorCursor
import kotlin.time.TimeSource

internal class BlockUndoManager(
    private val maxSize: Int = 100,
    private val mergeIntervalMs: Long = 500
) {

    data class BlockSnapshot(
        val blockId: String,
        val value: TextFieldValue
    )

    data class DocumentSnapshot(
        val blocks: List<DocumentBlock>,
        val textValues: Map<String, TextFieldValue>,
        val cursor: EditorCursor,
        val focusedBlockId: String,
        val selectedTableBlockId: String?,
        val navSelectedTableId: String?,
        val navSourceBlockId: String?
    )

    private sealed interface UndoEntry

    private class BlockUndoEntry(
        val blockId: String,
        val before: TextFieldValue,
        val after: TextFieldValue
    ) : UndoEntry

    private class DocumentUndoEntry(
        val before: DocumentSnapshot,
        val after: DocumentSnapshot,
        val mergeKey: String?
    ) : UndoEntry

    sealed interface UndoResult {
        data class Block(val snapshot: BlockSnapshot) : UndoResult
        data class Document(val snapshot: DocumentSnapshot) : UndoResult
    }

    private val undoStack = ArrayDeque<UndoEntry>()
    private val redoStack = ArrayDeque<UndoEntry>()
    private var lastRecordMark = TimeSource.Monotonic.markNow()
    private var preventMerge = false

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun updateAvailability() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun record(blockId: String, before: TextFieldValue, after: TextFieldValue) {
        if (before == after) return
        val now = TimeSource.Monotonic.markNow()

        if (!preventMerge && (now - lastRecordMark).inWholeMilliseconds < mergeIntervalMs
            && undoStack.isNotEmpty()
        ) {
            val top = undoStack.last()
            if (top is BlockUndoEntry && top.blockId == blockId) {
                val mergedBefore = top.before
                val mergedAfter = after
                if (mergedBefore != mergedAfter) {
                    undoStack.removeLast()
                    undoStack.addLast(BlockUndoEntry(blockId, mergedBefore, mergedAfter))
                    lastRecordMark = now
                    updateAvailability()
                    return
                }
            }
        }

        undoStack.addLast(BlockUndoEntry(blockId, before, after))
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
        lastRecordMark = now
        preventMerge = false
        updateAvailability()
    }

    fun recordDocument(before: DocumentSnapshot, after: DocumentSnapshot, mergeKey: String? = null) {
        if (before == after) return
        val now = TimeSource.Monotonic.markNow()

        if (!preventMerge && mergeKey != null && (now - lastRecordMark).inWholeMilliseconds < mergeIntervalMs
            && undoStack.isNotEmpty()
        ) {
            val top = undoStack.last()
            if (top is DocumentUndoEntry && top.mergeKey == mergeKey) {
                if (top.before != after) {
                    undoStack.removeLast()
                    undoStack.addLast(DocumentUndoEntry(top.before, after, mergeKey))
                    redoStack.clear()
                    lastRecordMark = now
                    updateAvailability()
                    return
                }
            }
        }

        undoStack.addLast(DocumentUndoEntry(before, after, mergeKey))
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
        lastRecordMark = now
        preventMerge = mergeKey == null
        updateAvailability()
    }

    fun undo(): UndoResult? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeLast()
        redoStack.addLast(entry)
        preventMerge = true
        val result = when (entry) {
            is BlockUndoEntry -> UndoResult.Block(BlockSnapshot(entry.blockId, entry.before))
            is DocumentUndoEntry -> UndoResult.Document(entry.before)
        }
        updateAvailability()
        return result
    }

    fun redo(): UndoResult? {
        if (redoStack.isEmpty()) return null
        val entry = redoStack.removeLast()
        undoStack.addLast(entry)
        preventMerge = true
        val result = when (entry) {
            is BlockUndoEntry -> UndoResult.Block(BlockSnapshot(entry.blockId, entry.after))
            is DocumentUndoEntry -> UndoResult.Document(entry.after)
        }
        updateAvailability()
        return result
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordMark = TimeSource.Monotonic.markNow()
        preventMerge = false
        updateAvailability()
    }
}
