package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG

private data class EmojiDrawSpec(
    val id: String,
    val icon: ImageVector,
    val start: Int,
    val lineColor: Color,
    val fontSize: TextUnit,
    val baselineShift: BaselineShift?,
    val hasItalic: Boolean,
    val hasStrikethrough: Boolean,
    val hasUnderline: Boolean,
)

private data class EmojiLayoutBounds(
    val box: Rect,
    val baseline: Float,
)

private data class EmojiDrawItem(
    val id: String,
    val left: Float,
    val top: Float,
    val size: Float,
    val lineColor: Color,
    val hasItalic: Boolean,
    val hasStrikethrough: Boolean,
    val hasUnderline: Boolean,
)

@Composable
fun CustomEmojiCanvasOverlay(
    annotatedString: AnnotatedString,
    layoutResult: TextLayoutResult?,
    emojiIcons: Map<String, ImageVector>,
    primaryColor: Color,
    currentColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val drawSpecs = remember(annotatedString, emojiIcons, primaryColor, currentColor) {
        val emojiAnnotations = annotatedString.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, annotatedString.length)
        emojiAnnotations.mapNotNull { range ->
            val icon = emojiIcons[range.item] ?: return@mapNotNull null
            if (range.start !in 0 until annotatedString.length) return@mapNotNull null
            val spanStyle = annotatedString.spanStyles
                .lastOrNull { it.start <= range.start && it.end > range.start }
                ?.item
            val lineColor = spanStyle?.color
                ?.takeIf { it != Color.Unspecified && it != Color.Transparent }
                ?: currentColor.takeIf { it != Color.Unspecified }
                ?: primaryColor
            val textDecoration = spanStyle?.textDecoration
            EmojiDrawSpec(
                id = range.item,
                icon = icon,
                start = range.start,
                lineColor = lineColor,
                fontSize = spanStyle?.fontSize ?: TextUnit.Unspecified,
                baselineShift = spanStyle?.baselineShift?.takeIf { it != BaselineShift.None },
                hasItalic = spanStyle?.fontStyle == FontStyle.Italic,
                hasStrikethrough = textDecoration?.contains(TextDecoration.LineThrough) == true,
                hasUnderline = textDecoration?.contains(TextDecoration.Underline) == true,
            )
        }
    }
    if (layoutResult == null || drawSpecs.isEmpty()) return

    val layoutBounds = remember(layoutResult, drawSpecs) {
        drawSpecs.associate { spec ->
            spec.start to runCatching {
                val line = layoutResult.getLineForOffset(spec.start)
                EmojiLayoutBounds(
                    box = layoutResult.getBoundingBox(spec.start),
                    baseline = layoutResult.getLineBaseline(line)
                )
            }.getOrNull()
        }
    }

    val drawItems = remember(drawSpecs, layoutBounds, density) {
        drawSpecs.mapNotNull { spec ->
            val bounds = layoutBounds[spec.start] ?: return@mapNotNull null
            val emojiSize = spec.fontSize
                .takeIf { it != TextUnit.Unspecified }
                ?.let { with(density) { it.toPx() } }
                ?: bounds.box.height
            val baselineShiftValue = spec.baselineShift?.multiplier ?: 0f
            EmojiDrawItem(
                id = spec.id,
                left = bounds.box.left,
                top = bounds.baseline - emojiSize * (0.85f + baselineShiftValue),
                size = emojiSize,
                lineColor = spec.lineColor,
                hasItalic = spec.hasItalic,
                hasStrikethrough = spec.hasStrikethrough,
                hasUnderline = spec.hasUnderline,
            )
        }
    }
    if (drawItems.isEmpty()) return

    val painterSpecs = remember(drawSpecs) { drawSpecs.distinctBy { it.id } }
    val painters = painterSpecs.associate { it.id to rememberVectorPainter(it.icon) }

    Canvas(modifier = modifier) {
        drawItems.forEach { item ->
            val painter = painters[item.id] ?: return@forEach
            drawCustomEmojiItem(
                item = item,
                painter = painter,
                tintColor = primaryColor,
            )
        }
    }
}

private fun DrawScope.drawCustomEmojiItem(
    item: EmojiDrawItem,
    painter: Painter,
    tintColor: Color,
) {
    withTransform({
        translate(left = item.left, top = item.top)
        if (item.hasItalic) {
            rotate(degrees = -8f, pivot = Offset(item.size / 2f, item.size / 2f))
        }
    }) {
        with(painter) {
            draw(
                size = Size(item.size, item.size),
                colorFilter = ColorFilter.tint(tintColor)
            )
        }
        val strokeWidth = item.size * 0.08f
        if (item.hasStrikethrough) {
            drawLine(
                color = item.lineColor,
                start = Offset(0f, item.size / 2f),
                end = Offset(item.size, item.size / 2f),
                strokeWidth = strokeWidth
            )
        }
        if (item.hasUnderline) {
            drawLine(
                color = item.lineColor,
                start = Offset(0f, item.size - strokeWidth),
                end = Offset(item.size, item.size - strokeWidth),
                strokeWidth = strokeWidth
            )
        }
    }
}
