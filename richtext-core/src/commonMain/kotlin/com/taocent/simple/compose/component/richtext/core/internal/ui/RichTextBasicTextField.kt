package com.taocent.simple.compose.component.richtext.core.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import com.taocent.simple.compose.component.richtext.core.RichTextConfig
import com.taocent.simple.compose.component.richtext.core.RichTextState

@Composable
fun RichTextBasicTextField(
    state: RichTextState,
    config: RichTextConfig,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onValueChange: (TextFieldValue) -> Unit,
    onTextLayoutResult: (TextLayoutResult) -> Unit,
    onPositioned: (position: IntOffset, widthPx: Int, heightPx: Int) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onPreviewKeyEvent: (KeyEvent) -> Boolean,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textFieldWidthPx by remember { mutableStateOf(0) }
    var textFieldFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        BasicTextField(
            value = state.textFieldValue,
            onValueChange = onValueChange,
            onTextLayout = {
                textLayoutResult = it
                onTextLayoutResult(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInWindow().let {
                        IntOffset(it.x.toInt(), it.y.toInt())
                    }
                    textFieldWidthPx = coordinates.size.width
                    onPositioned(position, coordinates.size.width, coordinates.size.height)
                }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    textFieldFocused = it.isFocused
                    onFocusChanged(it.isFocused)
                }
                .onPreviewKeyEvent(onPreviewKeyEvent),
            textStyle = TextStyle(
                fontSize = config.textField.defaultFontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = state.currentTextAlign
            ),
            visualTransformation = BoundaryNewlineVisualTransformation,
            cursorBrush = SolidColor(
                if (state.shouldDrawTrailingAlignedCursor(textFieldFocused)) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    propagateMinConstraints = true
                ) {
                    if (state.plainText.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = config.textField.defaultFontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = config.textField.placeholderAlpha
                                )
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )

        CustomEmojiCanvasOverlay(
            annotatedString = state.textFieldValue.annotatedString,
            layoutResult = textLayoutResult,
            emojiIcons = config.customEmojiIcons,
            primaryColor = MaterialTheme.colorScheme.primary,
            currentColor = state.currentColor,
            modifier = Modifier.matchParentSize()
        )
        TrailingAlignedCursorOverlay(
            state = state,
            layoutResult = textLayoutResult,
            isFocused = textFieldFocused,
            widthPx = textFieldWidthPx,
            cursorColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.matchParentSize()
        )
    }
}
