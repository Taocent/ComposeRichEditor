package com.taocent.simple.compose.component.blockrichtext.internal.block

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.taocent.simple.compose.component.blockrichtext.BlockState
import com.taocent.simple.compose.component.blockrichtext.ExperimentalBlockRichTextApi
import com.taocent.simple.compose.component.blockrichtext.RichTextTextField
import com.taocent.simple.compose.component.blockrichtext.internal.platform.InterceptDeleteBackwardTextInput
import com.taocent.simple.compose.component.blockrichtext.TableBlock
import com.taocent.simple.compose.component.blockrichtext.TextBlock

@Composable
@OptIn(ExperimentalBlockRichTextApi::class)
internal fun TextBlockItem(
    block: TextBlock,
    blockState: BlockState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val blockRichTextState = blockState.getBlockState(block.id)

    InterceptDeleteBackwardTextInput(
        onDeleteBackward = {
            val value = blockRichTextState.textFieldValue
            if (value.text.isEmpty() && value.selection.collapsed && value.selection.min == 0) {
                blockState.tryDeleteAtBlockStart(block.id)
            } else {
                false
            }
        }
    ) {
        RichTextTextField(
            state = blockRichTextState,
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            blockState.navClearSelection()
                        }
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val selection = blockRichTextState.textFieldValue.selection
                    val textLen = blockRichTextState.textFieldValue.text.length
                    if (event.key == Key.Backspace) {
                        if (selection.collapsed && selection.min == 0) {
                            return@onPreviewKeyEvent blockState.tryDeleteAtBlockStart(block.id)
                        }
                    }
                    if (event.key == Key.DirectionUp) {
                        if (selection.collapsed && selection.min == 0) {
                            val index = blockState.blocks.indexOfFirst { it.id == block.id }
                            if (index > 0 && blockState.blocks[index - 1] is com.taocent.simple.compose.component.blockrichtext.TableBlock) {
                                blockState.navSelectTable(blockState.blocks[index - 1].id, block.id)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    if (event.key == Key.DirectionDown) {
                        if (selection.collapsed && selection.min >= textLen) {
                            val index = blockState.blocks.indexOfFirst { it.id == block.id }
                            if (index >= 0 && index < blockState.blocks.size - 1 && blockState.blocks[index + 1] is com.taocent.simple.compose.component.blockrichtext.TableBlock) {
                                blockState.navSelectTable(blockState.blocks[index + 1].id, block.id)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                }
                false
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    val keepNavSelection = blockState.navSelectedTableId != null && blockState.focusedBlockId == block.id
                    blockState.focusBlock(block.id)
                    blockState.clearTableSelection()
                    if (!keepNavSelection) {
                        blockState.navClearSelection()
                    }
                }
            },
        placeholder = "",
        focusRequester = focusRequester,
        onUserValueChange = { oldValue, newValue ->
            val consumed = blockState.tryHandleTextBlockValueChangeBeforeApply(block.id, oldValue, newValue)
            if (!consumed && blockState.editorCursor is EditorCursor.BlockAnchor) {
                blockState.navClearSelection()
            }
            consumed
        },
            onUndo = { blockState.undo() },
            onRedo = { blockState.redo() }
        )
    }

    LaunchedEffect(blockRichTextState.textFieldValue) {
        if (blockRichTextState.textFieldValue.composition != null) return@LaunchedEffect
        blockState.updateBlockContent(block.id)
        if (blockState.editorCursor !is EditorCursor.BlockAnchor) {
            blockState.navClearSelection()
        }
    }
    // 注意:不再在 TextBlockItem 内部监听 `focusedBlockId` 并自动 requestFocus。
    // 旧实现在 LazyColumn 滚动让该 block 首次进入组合时,只要 focusedBlockId == block.id
    // 就会强制 requestFocus(),抢走表格单元格的焦点并导致移动端自动弹出软键盘。
    // 焦点恢复统一由 BlockRichTextEditor 顶层的 LaunchedEffect(focusRequestVersion) 协调,
    // 该 effect 只在用户显式触发的操作(键盘导航、undo/redo 等)自增 version 时才会 requestFocus。
}
