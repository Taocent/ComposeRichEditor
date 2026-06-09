package com.taocent.simple.compose.component.blockrichtext.internal.block.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.blockrichtext.RichTextState
import com.taocent.simple.compose.component.blockrichtext.RichTextTextField
import com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig

@Composable
internal fun TableCellEditor(
    state: RichTextState,
    isHeader: Boolean,
    focusRequester: FocusRequester,
    onFocus: (FocusRequester) -> Unit,
    onClearSelection: () -> Unit,
    onContentChange: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalRichTextConfig.current
    Box(
        modifier = modifier
            .heightIn(min = config.table.cellMinHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        RichTextTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                onClearSelection()
                            }
                        }
                    }
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocus(focusRequester)
                    }
                },
            placeholder = "",
            focusRequester = focusRequester,
            onUndo = onUndo,
            onRedo = onRedo
        )
    }

    LaunchedEffect(state.textFieldValue) {
        if (state.textFieldValue.composition != null) return@LaunchedEffect
        onContentChange()
        onClearSelection()
    }
}
