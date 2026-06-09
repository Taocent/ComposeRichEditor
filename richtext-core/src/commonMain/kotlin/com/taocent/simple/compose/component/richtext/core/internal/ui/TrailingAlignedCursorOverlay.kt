package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.richtext.core.RichTextState
import kotlinx.coroutines.delay

@Composable
fun TrailingAlignedCursorOverlay(
    state: RichTextState,
    layoutResult: TextLayoutResult?,
    isFocused: Boolean,
    widthPx: Int,
    cursorColor: Color,
    modifier: Modifier = Modifier,
) {
    val shouldDraw = state.shouldDrawTrailingAlignedCursor(isFocused)
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(shouldDraw, state.textFieldValue.selection, state.currentTextAlign) {
        cursorVisible = true
        while (shouldDraw) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }
    if (!shouldDraw || !cursorVisible) return
    val result = layoutResult ?: return
    val cursorOffset = state.textFieldValue.selection.start
    if (cursorOffset > result.layoutInput.text.length) return
    val cursorWidth = with(LocalDensity.current) { 1.dp.toPx() }
    Canvas(modifier = modifier) {
        val rect = result.getCursorRect(cursorOffset)
        val x = when (state.currentTextAlign) {
            TextAlign.Center -> widthPx / 2f
            TextAlign.Right -> widthPx.toFloat()
            else -> return@Canvas
        }.coerceIn(0f, size.width)
        drawLine(
            color = cursorColor,
            start = Offset(x, rect.top),
            end = Offset(x, rect.bottom),
            strokeWidth = cursorWidth
        )
    }
}

fun RichTextState.shouldDrawTrailingAlignedCursor(isFocused: Boolean): Boolean {
    val selection = textFieldValue.selection
    return isFocused &&
        selection.collapsed &&
        selection.start == plainText.length &&
        plainText.endsWith('\n') &&
        currentTextAlign != TextAlign.Left
}
