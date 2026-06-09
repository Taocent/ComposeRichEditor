package com.taocent.simple.compose.component.richtext.core.internal.paragraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import com.taocent.simple.compose.component.richtext.core.internal.format.ParagraphStyleUtils

internal data class RichParagraphModel(
    var textAlign: TextAlign = TextAlign.Left
)

internal class ParagraphModelManager(
    annotated: AnnotatedString
) {
    var models by mutableStateOf(buildModels(annotated))
        private set

    fun restoreFromAnnotated(annotated: AnnotatedString) {
        models = buildModels(annotated)
    }

    fun restore(models: List<RichParagraphModel>) {
        this.models = models.map { it.copy() }
    }

    fun snapshot(): List<RichParagraphModel> {
        return models.map { it.copy() }
    }

    fun paragraphIndexAt(text: String, offset: Int): Int {
        if (text.isEmpty()) return 0
        val pos = offset.coerceIn(0, text.length)
        var index = 0
        var start = 0
        while (start <= text.length) {
            val lineBreak = text.indexOf('\n', start)
            val end = if (lineBreak < 0) text.length else lineBreak + 1
            if (pos < end || lineBreak < 0) return index.coerceAtMost(models.lastIndex.coerceAtLeast(0))
            index++
            start = lineBreak + 1
        }
        return index.coerceAtMost(models.lastIndex.coerceAtLeast(0))
    }

    fun setTextAlign(
        annotated: AnnotatedString,
        selection: TextRange,
        align: TextAlign
    ): AnnotatedString {
        ensureFor(annotated)
        val indexes = paragraphIndexesForSelection(annotated.text, selection)
        for (index in indexes) {
            models[index].textAlign = ParagraphStyleUtils.normalizedTextAlign(align)
        }
        models = models.map { it.copy() }
        return applyTo(annotated)
    }

    fun textAlignForSelection(
        annotated: AnnotatedString,
        selection: TextRange
    ): TextAlign {
        ensureFor(annotated)
        if (models.isEmpty()) return TextAlign.Left
        val indexes = paragraphIndexesForSelection(annotated.text, selection)
        val first = models[indexes.first()].textAlign
        return if (indexes.all { models[it].textAlign == first }) first else TextAlign.Left
    }

    fun syncAfterTextChange(
        oldText: String,
        newAnnotated: AnnotatedString,
        editStart: Int,
        removedCount: Int,
        insertedText: String,
        fallbackTextAlign: TextAlign? = null
    ): AnnotatedString {
        val oldModels = models.ifEmpty { buildModels(newAnnotated) }
        if (canKeepParagraphModels(oldText, editStart, removedCount, insertedText)) {
            if (models.isEmpty()) {
                models = oldModels
            }
            return applyTo(newAnnotated)
        }
        models = buildEditedModels(
            oldText = oldText,
            oldModels = oldModels,
            newText = newAnnotated.text,
            editStart = editStart,
            removedCount = removedCount,
            insertedText = insertedText,
            fallbackTextAlign = fallbackTextAlign
        )
        return applyTo(newAnnotated)
    }

    private fun canKeepParagraphModels(
        oldText: String,
        editStart: Int,
        removedCount: Int,
        insertedText: String
    ): Boolean {
        if (insertedText.any { it == '\n' }) return false
        if (removedCount <= 0) return true
        val start = editStart.coerceIn(0, oldText.length)
        val end = (editStart + removedCount).coerceIn(start, oldText.length)
        return oldText.indexOf('\n', start).let { it < 0 || it >= end }
    }

    fun applyTo(annotated: AnnotatedString): AnnotatedString {
        return ParagraphStyleUtils.applyParagraphTextAligns(
            annotated,
            models.map { it.textAlign }
        )
    }

    private fun ensureFor(annotated: AnnotatedString) {
        val count = ParagraphStyleUtils.paragraphRanges(annotated.text).size
        if (models.size == count) return
        models = buildModels(annotated)
    }

    private fun buildModels(annotated: AnnotatedString): List<RichParagraphModel> {
        return ParagraphStyleUtils.paragraphRanges(annotated.text).map { range ->
            RichParagraphModel(ParagraphStyleUtils.textAlignAt(annotated, range.start))
        }
    }

    private fun paragraphIndexesForSelection(text: String, selection: TextRange): List<Int> {
        if (models.isEmpty()) return listOf(0)
        if (selection.collapsed) return listOf(paragraphIndexAt(text, selection.start))
        val ranges = ParagraphStyleUtils.lineRangesForSelection(text, selection)
        return ranges.map { range -> paragraphIndexAt(text, range.start) }.distinct().ifEmpty {
            listOf(paragraphIndexAt(text, selection.min))
        }
    }

    private fun buildEditedModels(
        oldText: String,
        oldModels: List<RichParagraphModel>,
        newText: String,
        editStart: Int,
        removedCount: Int,
        insertedText: String,
        fallbackTextAlign: TextAlign?
    ): List<RichParagraphModel> {
        val newCount = ParagraphStyleUtils.paragraphRanges(newText).size
        val insertedNewlines = insertedText.count { it == '\n' }
        if (insertedNewlines > 0) {
            val baseIndex = paragraphIndexAt(oldText, editStart).coerceIn(0, oldModels.lastIndex.coerceAtLeast(0))
            val baseAlign = ParagraphStyleUtils.normalizedTextAlign(fallbackTextAlign ?: oldModels.getOrNull(baseIndex)?.textAlign ?: TextAlign.Left)
            val result = oldModels.map { it.copy() }.toMutableList()
            repeat(insertedNewlines) {
                result.add(baseIndex + 1, RichParagraphModel(baseAlign))
            }
            return fitCount(result, newCount, baseAlign)
        }
        if (removedCount > 0) {
            val startIndex = paragraphIndexAt(oldText, editStart).coerceIn(0, oldModels.lastIndex.coerceAtLeast(0))
            val endIndex = paragraphIndexAt(oldText, editStart + removedCount).coerceIn(0, oldModels.lastIndex.coerceAtLeast(0))
            val result = oldModels.map { it.copy() }.toMutableList()
            if (endIndex > startIndex) {
                for (index in endIndex downTo startIndex + 1) {
                    if (index in result.indices) result.removeAt(index)
                }
            }
            return fitCount(result, newCount, oldModels.getOrNull(startIndex)?.textAlign ?: TextAlign.Left)
        }
        val align = fallbackTextAlign ?: oldModels.getOrNull(paragraphIndexAt(oldText, editStart))?.textAlign ?: TextAlign.Left
        return fitCount(oldModels.map { it.copy() }, newCount, align)
    }

    private fun fitCount(
        models: List<RichParagraphModel>,
        count: Int,
        align: TextAlign
    ): List<RichParagraphModel> {
        val result = models.toMutableList()
        while (result.size > count) result.removeAt(result.lastIndex)
        while (result.size < count) result.add(RichParagraphModel(ParagraphStyleUtils.normalizedTextAlign(align)))
        if (result.isEmpty()) result.add(RichParagraphModel(TextAlign.Left))
        return result
    }
}
