package com.taocent.simple.compose.component.richtext.core.internal.undo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.time.TimeSource

internal sealed class UndoOperation {
    data class TextChange(
        val offset: Int,
        val removed: String,
        val inserted: String
    ) : UndoOperation()

    data class SpanChange(
        val previousSpans: List<AnnotatedString.Range<SpanStyle>>,
        val newSpans: List<AnnotatedString.Range<SpanStyle>>
    ) : UndoOperation()

    data class Composite(
        val operations: List<UndoOperation>
    ) : UndoOperation()
}

internal class UndoManager(
    private val maxSize: Int = 100,
    private val mergeIntervalMs: Long = 500
) {

    private class UndoEntry(
        val diff: UndoOperation,
        val previousSpans: List<AnnotatedString.Range<SpanStyle>>,
        val newSpans: List<AnnotatedString.Range<SpanStyle>>,
        val previousAnnotations: List<AnnotatedString.Range<String>> = emptyList(),
        val newAnnotations: List<AnnotatedString.Range<String>> = emptyList()
    )

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

    fun record(value: TextFieldValue) {
        val now = TimeSource.Monotonic.markNow()
        val diff = computeTextDiff("", value.annotatedString.text)

        if (diff is UndoOperation.TextChange && diff.removed.isEmpty() && diff.inserted.isEmpty()) return

        val entry = UndoEntry(diff = diff, previousSpans = emptyList(), newSpans = emptyList())

        if (!preventMerge && (now - lastRecordMark).inWholeMilliseconds < mergeIntervalMs && undoStack.isNotEmpty()) {
            val top = undoStack.last()
            val merged = tryMergeDiffs(top.diff, entry.diff)
            if (merged != null) {
                undoStack.removeLast()
                undoStack.addLast(UndoEntry(merged, top.previousSpans, entry.newSpans, top.previousAnnotations, entry.newAnnotations))
                lastRecordMark = now
                updateAvailability()
                return
            }
        }

        undoStack.addLast(entry)
        if (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        lastRecordMark = now
        preventMerge = false
        updateAvailability()
    }

    fun record(currentValue: TextFieldValue, newValue: TextFieldValue) {
        val now = TimeSource.Monotonic.markNow()
        val oldText = currentValue.annotatedString.text
        val newText = newValue.annotatedString.text
        val oldSpans = currentValue.annotatedString.spanStyles
        val newSpans = newValue.annotatedString.spanStyles
        val oldAnns = currentValue.annotatedString.getStringAnnotations(0, oldText.length).toList()
        val newAnns = newValue.annotatedString.getStringAnnotations(0, newText.length).toList()

        val textDiff = computeTextDiff(oldText, newText)
        val hasTextChange = textDiff !is UndoOperation.TextChange ||
            textDiff.removed.isNotEmpty() || textDiff.inserted.isNotEmpty()
        val hasSpanChange = oldSpans != newSpans
        val hasAnnotationChange = oldAnns != newAnns

        if (!hasTextChange && !hasSpanChange && !hasAnnotationChange) return

        if (preventMerge) {
            val diff = when {
                hasTextChange && (hasSpanChange || hasAnnotationChange) -> UndoOperation.Composite(
                    listOf(textDiff, UndoOperation.SpanChange(oldSpans, newSpans))
                )
                hasSpanChange -> UndoOperation.SpanChange(oldSpans, newSpans)
                else -> textDiff
            }
            undoStack.addLast(UndoEntry(diff, oldSpans, newSpans, oldAnns, newAnns))
            if (undoStack.size > maxSize) undoStack.removeFirst()
            redoStack.clear()
            lastRecordMark = now
            preventMerge = false
            updateAvailability()
            return
        }

        if (hasTextChange && (now - lastRecordMark).inWholeMilliseconds < mergeIntervalMs && undoStack.isNotEmpty()) {
            val top = undoStack.last()
            val topTextDiff = when (val d = top.diff) {
                is UndoOperation.TextChange -> d
                is UndoOperation.Composite -> d.operations.filterIsInstance<UndoOperation.TextChange>().firstOrNull()
                else -> null
            }
            if (topTextDiff != null) {
                val merged = tryMergeDiffs(topTextDiff, textDiff)
                if (merged != null) {
                    undoStack.removeLast()
                    val newDiff = if (hasSpanChange || hasAnnotationChange) {
                        UndoOperation.Composite(listOf(merged, UndoOperation.SpanChange(oldSpans, newSpans)))
                    } else {
                        merged
                    }
                    undoStack.addLast(UndoEntry(newDiff, top.previousSpans, newSpans, top.previousAnnotations, newAnns))
                    lastRecordMark = now
                    updateAvailability()
                    return
                }
            }
        }

        val diff = when {
            hasTextChange && (hasSpanChange || hasAnnotationChange) -> UndoOperation.Composite(
                listOf(textDiff, UndoOperation.SpanChange(oldSpans, newSpans))
            )
            hasSpanChange -> UndoOperation.SpanChange(oldSpans, newSpans)
            else -> textDiff
        }
        undoStack.addLast(UndoEntry(diff, oldSpans, newSpans, oldAnns, newAnns))
        if (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
        lastRecordMark = now
        preventMerge = false
        updateAvailability()
    }

    fun undo(currentValue: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeLast()

        val curAnns = currentValue.annotatedString.getStringAnnotations(0, currentValue.annotatedString.text.length).toList()
        val redoEntry = UndoEntry(
            diff = entry.diff,
            previousSpans = currentValue.annotatedString.spanStyles,
            newSpans = entry.newSpans,
            previousAnnotations = curAnns,
            newAnnotations = entry.newAnnotations
        )
        redoStack.addLast(redoEntry)
        preventMerge = true

        val value = applyUndoRedo(currentValue, entry, isUndo = true)
        updateAvailability()
        return value
    }

    fun redo(currentValue: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        val entry = redoStack.removeLast()

        val curAnns = currentValue.annotatedString.getStringAnnotations(0, currentValue.annotatedString.text.length).toList()
        val undoEntry = UndoEntry(
            diff = entry.diff,
            previousSpans = currentValue.annotatedString.spanStyles,
            newSpans = entry.newSpans,
            previousAnnotations = curAnns,
            newAnnotations = entry.newAnnotations
        )
        undoStack.addLast(undoEntry)
        preventMerge = true

        val value = applyUndoRedo(currentValue, entry, isUndo = false)
        updateAvailability()
        return value
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastRecordMark = TimeSource.Monotonic.markNow()
        preventMerge = false
        updateAvailability()
    }

    private fun applyUndoRedo(
        currentValue: TextFieldValue,
        entry: UndoEntry,
        isUndo: Boolean
    ): TextFieldValue {
        val op = entry.diff
        val currentText = currentValue.annotatedString.text

        val newText: String
        val resultSpans: List<AnnotatedString.Range<SpanStyle>>
        val resultAnnotations: List<AnnotatedString.Range<String>>

        when (op) {
            is UndoOperation.TextChange -> {
                val appliedOp = if (isUndo) reverseDiff(op) else op
                newText = applyDiff(currentText, appliedOp)
                resultSpans = if (isUndo) entry.previousSpans else entry.newSpans
                resultAnnotations = if (isUndo) entry.previousAnnotations else entry.newAnnotations
            }
            is UndoOperation.SpanChange -> {
                newText = currentText
                resultSpans = if (isUndo) op.previousSpans else op.newSpans
                resultAnnotations = if (isUndo) entry.previousAnnotations else entry.newAnnotations
            }
            is UndoOperation.Composite -> {
                var text = currentText
                val ops = if (isUndo) op.operations.reversed() else op.operations
                for (subOp in ops) {
                    when (subOp) {
                        is UndoOperation.TextChange -> {
                            val appliedOp = if (isUndo) reverseDiff(subOp) else subOp
                            text = applyDiff(text, appliedOp)
                        }
                        else -> {}
                    }
                }
                newText = text
                resultSpans = if (isUndo) entry.previousSpans else entry.newSpans
                resultAnnotations = if (isUndo) entry.previousAnnotations else entry.newAnnotations
            }
        }

        val restoredAnnotated = AnnotatedString.Builder(newText.length).apply {
            pushStyle(SpanStyle())
            append(newText)
            pop()
            resultSpans.forEach { addStyle(it.item, it.start, it.end) }
            resultAnnotations.forEach { ann ->
                val safeStart = ann.start.coerceIn(0, newText.length)
                val safeEnd = ann.end.coerceIn(0, newText.length)
                if (safeStart < safeEnd) {
                    addStringAnnotation(ann.tag, ann.item, safeStart, safeEnd)
                }
            }
        }.toAnnotatedString()

        val cursorPos = computeCursorAfterUndoRedo(op, isUndo, newText.length)

        return TextFieldValue(
            annotatedString = restoredAnnotated,
            selection = TextRange(cursorPos.coerceIn(0, newText.length))
        )
    }

    private fun computeCursorAfterUndoRedo(op: UndoOperation, isUndo: Boolean, textLength: Int): Int {
        return when (op) {
            is UndoOperation.TextChange -> {
                if (isUndo) op.offset else (op.offset + op.inserted.length).coerceAtMost(textLength)
            }
            is UndoOperation.SpanChange -> textLength
            is UndoOperation.Composite -> {
                val textOp = op.operations.filterIsInstance<UndoOperation.TextChange>().firstOrNull()
                if (textOp != null) {
                    if (isUndo) textOp.offset else (textOp.offset + textOp.inserted.length).coerceAtMost(textLength)
                } else {
                    textLength
                }
            }
        }
    }
}

private fun tryMergeDiffs(first: UndoOperation, second: UndoOperation): UndoOperation? {
    if (first is UndoOperation.TextChange && second is UndoOperation.TextChange) {
        val endOfFirst = first.offset + first.inserted.length
        if (second.offset == first.offset || second.offset == endOfFirst) {
            val combinedRemoved = first.removed + second.removed
            val combinedInserted = if (second.offset == first.offset) {
                second.inserted + first.inserted
            } else {
                first.inserted + second.inserted
            }
            return UndoOperation.TextChange(first.offset, combinedRemoved, combinedInserted)
        }
    }
    return null
}

internal fun computeTextDiff(oldText: String, newText: String): UndoOperation {
    if (oldText == newText) {
        return UndoOperation.TextChange(0, "", "")
    }

    val minLen = minOf(oldText.length, newText.length)
    var commonPrefix = 0
    while (commonPrefix < minLen && oldText[commonPrefix] == newText[commonPrefix]) {
        commonPrefix++
    }

    var oldSuffix = oldText.length
    var newSuffix = newText.length
    while (oldSuffix > commonPrefix && newSuffix > commonPrefix &&
        oldText[oldSuffix - 1] == newText[newSuffix - 1]
    ) {
        oldSuffix--
        newSuffix--
    }

    val removed = oldText.substring(commonPrefix, oldSuffix)
    val inserted = newText.substring(commonPrefix, newSuffix)

    return UndoOperation.TextChange(commonPrefix, removed, inserted)
}

internal fun reverseDiff(op: UndoOperation): UndoOperation {
    return when (op) {
        is UndoOperation.TextChange -> UndoOperation.TextChange(op.offset, op.inserted, op.removed)
        is UndoOperation.SpanChange -> UndoOperation.SpanChange(op.newSpans, op.previousSpans)
        is UndoOperation.Composite -> UndoOperation.Composite(
            op.operations.reversed().map { reverseDiff(it) }
        )
    }
}

internal fun applyDiff(text: String, op: UndoOperation): String {
    return when (op) {
        is UndoOperation.TextChange -> {
            val before = text.substring(0, op.offset.coerceAtMost(text.length))
            val after = text.substring((op.offset + op.removed.length).coerceAtMost(text.length))
            before + op.inserted + after
        }
        is UndoOperation.SpanChange -> text
        is UndoOperation.Composite -> {
            op.operations.fold(text) { acc, subOp -> applyDiff(acc, subOp) }
        }
    }
}
