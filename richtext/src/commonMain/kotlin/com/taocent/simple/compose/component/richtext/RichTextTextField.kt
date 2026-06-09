package com.taocent.simple.compose.component.richtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.FloatingToolbar
import com.taocent.simple.compose.component.richtext.core.internal.ui.CallbackTextContextMenuProvider
import com.taocent.simple.compose.component.richtext.core.internal.ui.CallbackTextToolbar
import com.taocent.simple.compose.component.richtext.core.internal.ui.RichTextBasicTextField
import com.taocent.simple.compose.component.richtext.core.internal.ui.RichTextClipboardActions
import com.taocent.simple.compose.component.richtext.core.internal.ui.handleRichTextKeyEvent
import com.taocent.simple.compose.component.richtext.core.platform.rememberRichTextPlatformAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RichTextTextField(
    state: RichTextState,
    modifier: Modifier = Modifier,
    placeholder: String = "请输入文本...",
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUserValueChange: (() -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null
) {
    val clipboard = LocalClipboard.current
    val config = LocalRichTextConfig.current
    val platformAdapter = rememberRichTextPlatformAdapter()
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var showToolbar by remember { mutableStateOf(false) }
    var showingContextToolbar by remember { mutableStateOf(false) }
    var androidLastCollapsedCursor by remember { mutableStateOf<Int?>(null) }
    var androidSameCursorTapCount by remember { mutableStateOf(0) }
    var suppressedToolbarSelection by remember { mutableStateOf<TextRange?>(null) }
    // 区分"用户主动拖动产生的非折叠选区"与"BasicTextField 长按 word selection 产生的非折叠选区":
    // - 用户主动拖动选区(isDragging 期间发生过 collapsed → collapsed 的光标移动)时,工具栏应显示
    // - 长按 word selection(BasicTextField 自己把 collapsed 一次性变成 non-collapsed)时,
    //   如果用户已经主动 hide 了工具栏(短按同位置),我们不希望 LaunchedEffect 再次把工具栏顶出来
    var userDraggedSelection by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var textFieldGlobalPosition by remember { mutableStateOf(IntOffset.Zero) }
    var textFieldWidthPx by remember { mutableStateOf(0) }
    var textFieldHeightPx by remember { mutableStateOf(0) }

    fun toolbarAnchor(): IntOffset {
        val layout = textLayoutResult ?: return IntOffset.Zero
        val textLength = state.textFieldValue.annotatedString.length
        val selection = state.textFieldValue.selection
        val cursorPos = if (selection.collapsed) selection.start else selection.end
        val cursorRect = layout.getCursorRect(cursorPos.coerceIn(0, textLength))
        return IntOffset(
            x = ((cursorRect.left + cursorRect.right) / 2).toInt(),
            y = cursorRect.bottom.toInt(),
        )
    }

    val clipboardActions = remember(state, clipboard, config) {
        RichTextClipboardActions(clipboard, state, config)
    }

    LaunchedEffect(state.textFieldValue.selection, isDragging, showingContextToolbar, userDraggedSelection) {
        if (isDragging) {
            showingContextToolbar = false
            showToolbar = false
        } else if (!state.textFieldValue.selection.collapsed) {
            // 不要 reset showingContextToolbar:
            // 长按后 BasicTextField 内部可能把 selection 从 collapsed 变成非折叠
            // (开始 word selection),但此时 showingContextToolbar 仍然代表"工具栏应该显示",
            // 必须保留,否则 hide→show 的 toggle 会因为标志被清零而失效。
            delay(config.floatingToolbar.showDelayMs)
            val sel = state.textFieldValue.selection
            val annotated = state.textFieldValue.annotatedString
            val urlAnnotations = annotated.getStringAnnotations("URL", sel.min, sel.max)
            val isEntireHyperlink = urlAnnotations.any { it.start <= sel.min && it.end >= sel.max }
            if (isEntireHyperlink) {
                showToolbar = false
            } else if (userDraggedSelection || showingContextToolbar || suppressedToolbarSelection != sel) {
                // 用户主动拖动产生的非折叠选区 → 应该显示工具栏
                showToolbar = true
            } else if (!showingContextToolbar) {
                // 长按 word selection 产生的非折叠选区,但用户已经主动 hide 了工具栏
                // (短按同位置) → 不应该让工具栏又冒出来
                showToolbar = false
            }
            // else: 长按 word selection,showingContextToolbar = true → 保持工具栏显示
        } else if (!showingContextToolbar) {
            showToolbar = false
        }
    }

    fun showContextToolbar() {
        isDragging = false
        showingContextToolbar = true
        suppressedToolbarSelection = null
        showToolbar = true
        // 无条件记录 tap 计数,这样长按触发的 word selection(selection 变成非折叠)也不会让 toggle 失效
        if (platformAdapter.requiresCollapsedCursorContextToolbarToggle) {
            androidLastCollapsedCursor = state.textFieldValue.selection.start
            androidSameCursorTapCount = 2
        }
    }

    fun hideContextToolbar() {
        showingContextToolbar = false
        suppressedToolbarSelection = state.textFieldValue.selection.takeIf { !it.collapsed }
        showToolbar = false
    }

    fun handleAndroidCollapsedCursorPress(localOffset: Offset): Boolean {
        if (!platformAdapter.requiresCollapsedCursorContextToolbarToggle) return false
        val selection = state.textFieldValue.selection
        val targetCursor = textLayoutResult
            ?.getOffsetForPosition(localOffset)
            ?: selection.start
        // 折叠选区 + 点击位置与光标不同 → 移动光标,直接 hide
        if (selection.collapsed && targetCursor != selection.start) {
            androidLastCollapsedCursor = targetCursor
            androidSameCursorTapCount = 0
            hideContextToolbar()
            return false
        }
        if (androidLastCollapsedCursor == targetCursor) {
            androidSameCursorTapCount += 1
        } else {
            androidLastCollapsedCursor = targetCursor
            androidSameCursorTapCount = 1
        }
        if (androidSameCursorTapCount >= 2) {
            // 用 showToolbar(本字段的 host 状态)作为"工具栏是否真的在显示"的真实依据
            if (showToolbar && showingContextToolbar) {
                hideContextToolbar()
            } else {
                showContextToolbar()
            }
            return true
        }
        return false
    }

    // 缓存 CallbackTextContextMenuProvider 实例,供 CompositionLocalProvider 和
    // Press handler 共同引用,这样 Press handler 可以在 down 时调 suppressNext()
    // 吞掉 BasicTextField 紧跟着会触发的 long-press → showTextContextMenu。
    val contextMenuProvider = remember { CallbackTextContextMenuProvider(::showContextToolbar) }
    val textToolbarImpl = remember { CallbackTextToolbar(::showContextToolbar) }

    Box(modifier = modifier) {
        CompositionLocalProvider(
            LocalTextToolbar provides textToolbarImpl,
            LocalTextContextMenuToolbarProvider provides contextMenuProvider,
            LocalTextContextMenuDropdownProvider provides contextMenuProvider,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(config.textField.innerPadding)
                    .then(
                        with(platformAdapter) {
                            Modifier.suppressNativeContextMenu(::showContextToolbar)
                        }
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val pressOffset = event.changes.firstOrNull()?.position
                                        // 工具栏当前已显示的话,先把 BasicTextField 紧跟着会
                                        // 触发的 long-press → showTextContextMenu 吞掉,
                                        // 否则 BasicTextField 会在 long-press timeout 后
                                        // 重新调 onShow() 把刚 hide 掉的工具栏又显示回来。
                                        if (showToolbar && showingContextToolbar) {
                                            contextMenuProvider.suppressNext()
                                            suppressedToolbarSelection = state.textFieldValue.selection.takeIf { !it.collapsed }
                                        }
                                        // 新一次 down 开始,重置 userDraggedSelection
                                        // (让 LaunchedEffect 重新基于本次 down 的行为判断)
                                        userDraggedSelection = false
                                        if (pressOffset == null || !handleAndroidCollapsedCursorPress(pressOffset)) {
                                            showingContextToolbar = false
                                            isDragging = true
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        isDragging = false
                                    }
                                    PointerEventType.Move -> {}
                                }
                            }
                        }
                    }
            ) {
                RichTextBasicTextField(
                    state = state,
                    config = config,
                    placeholder = placeholder,
                    focusRequester = focusRequester,
                    onValueChange = { newValue ->
                        val oldValue = state.textFieldValue
                        if (platformAdapter.requiresCollapsedCursorContextToolbarToggle && newValue.selection.collapsed && newValue.selection.start != oldValue.selection.start) {
                            androidLastCollapsedCursor = newValue.selection.start
                            androidSameCursorTapCount = 1
                            showingContextToolbar = false
                            suppressedToolbarSelection = null
                        }
                        if (newValue.selection.collapsed) {
                            suppressedToolbarSelection = null
                        }
                        if (isDragging && !newValue.selection.collapsed) {
                            userDraggedSelection = true
                        }
                        onUserValueChange?.invoke()
                        state.onValueChange(newValue, platformAdapter.supportsImeComposition)
                    },
                    onTextLayoutResult = { textLayoutResult = it },
                    onPositioned = { position, widthPx, heightPx ->
                        textFieldWidthPx = widthPx
                        textFieldHeightPx = heightPx
                        textFieldGlobalPosition = position
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    onPreviewKeyEvent = { event ->
                        handleRichTextKeyEvent(
                            event = event,
                            state = state,
                            clipboardActions = clipboardActions,
                            coroutineScope = coroutineScope,
                            allowSmartPaste = true,
                            dispatchRegisteredCommands = true,
                            onUndo = onUndo,
                            onRedo = onRedo
                        )
                    }
                )
            }

            FloatingToolbar(
                state = state,
                textFieldGlobalPosition = textFieldGlobalPosition,
                textFieldHeightPx = textFieldHeightPx,
                anchor = toolbarAnchor(),
                showToolbar = showToolbar,
                onDismiss = {},
                onCopy = {
                    coroutineScope.launch { clipboardActions.copySelection() }
                },
                onCut = {
                    coroutineScope.launch { clipboardActions.cutSelection() }
                },
                onPaste = {
                    coroutineScope.launch { clipboardActions.paste(allowSmartPaste = false) }
                },
                onSelectAll = {
                    state.selectAll()
                }
            )
        }
    }
}
