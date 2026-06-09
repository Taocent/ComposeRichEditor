package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.FloatingToolbarActiveInfo
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.FloatingToolbarHostState
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.LocalFloatingToolbarHost
import com.taocent.simple.compose.component.richtext.core.internal.ui.CallbackTextContextMenuProvider
import com.taocent.simple.compose.component.richtext.core.internal.ui.CallbackTextToolbar
import com.taocent.simple.compose.component.richtext.core.internal.ui.RichTextBasicTextField
import com.taocent.simple.compose.component.richtext.core.internal.ui.RichTextClipboardActions
import com.taocent.simple.compose.component.richtext.core.internal.ui.handleRichTextKeyEvent
import com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig
import com.taocent.simple.compose.component.richtext.core.platform.rememberRichTextPlatformAdapter
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun RichTextTextField(
    state: RichTextState,
    modifier: Modifier = Modifier,
    placeholder: String = "请输入文本...",
    focusRequester: FocusRequester = remember { FocusRequester() },
    floatingToolbarHost: FloatingToolbarHostState? = LocalFloatingToolbarHost.current,
    onUserValueChange: ((TextFieldValue, TextFieldValue) -> Boolean)? = null,
    onUndo: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null
) {
    val clipboard = LocalClipboard.current
    val config = LocalRichTextConfig.current
    val platformAdapter = rememberRichTextPlatformAdapter()
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var showingContextToolbar by remember { mutableStateOf(false) }
    var androidLastCollapsedCursor by remember { mutableStateOf<Int?>(null) }
    var androidSameCursorTapCount by remember { mutableStateOf(0) }
    var suppressedToolbarSelection by remember { mutableStateOf<TextRange?>(null) }
    // 区分"用户主动拖动产生的非折叠选区"与"BasicTextField 长按 word selection 产生的非折叠选区":
    // - 用户主动拖动选区(isDragging 期间发生过 collapsed → collapsed 的光标移动)时,工具栏应显示
    // - 长按 word selection(BasicTextField 自己把 collapsed 一次性变成 non-collapsed)时,
    //   如果用户已经主动 hide 了工具栏(短按同位置),我们不希望 LaunchedEffect 再次把工具栏顶出来
    var userDraggedSelection by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textFieldGlobalPosition by remember { mutableStateOf(IntOffset.Zero) }
    var textFieldWidthPx by remember { mutableStateOf(0) }
    var textFieldHeightPx by remember { mutableStateOf(0) }
    var isTextFieldFocused by remember { mutableStateOf(false) }

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

    fun activeInfo(): FloatingToolbarActiveInfo {
        return FloatingToolbarActiveInfo(
            state = state,
            position = textFieldGlobalPosition,
            widthPx = textFieldWidthPx,
            heightPx = textFieldHeightPx,
            anchor = toolbarAnchor(),
            isDragging = false,
        )
    }

    val clipboardActions = remember(state, clipboard, config) {
        RichTextClipboardActions(clipboard, state, config)
    }

    LaunchedEffect(state.textFieldValue.selection, isDragging, showingContextToolbar, floatingToolbarHost, userDraggedSelection) {
        if (isDragging) {
            showingContextToolbar = false
            floatingToolbarHost?.clearActive(state)
        } else if (!state.textFieldValue.selection.collapsed) {
            // 不要 reset showingContextToolbar:
            // 长按后 BasicTextField 内部可能把 selection 从 collapsed 变成非折叠
            // (开始 word selection),但此时 showingContextToolbar 仍然代表"工具栏应该显示",
            // 必须保留,否则 hide→show 的 toggle 会因为标志被清零而失效。
            delay(config.floatingToolbar.showDelayMs)
            // 延时期间选区可能已经塌缩或切换到其他块,丢弃上报
            if (state.textFieldValue.selection.collapsed) return@LaunchedEffect
            val sel = state.textFieldValue.selection
            val annotated = state.textFieldValue.annotatedString
            val urlAnnotations = annotated.getStringAnnotations("URL", sel.min, sel.max)
            val isEntireHyperlink = urlAnnotations.any { it.start <= sel.min && it.end >= sel.max }
            if (isEntireHyperlink) {
                floatingToolbarHost?.clearActive(state)
            } else if (userDraggedSelection || showingContextToolbar || suppressedToolbarSelection != sel) {
                // 用户主动拖动产生的非折叠选区 → 应该显示工具栏
                floatingToolbarHost?.reportActive(activeInfo())
            } else if (!showingContextToolbar) {
                // 长按 word selection 产生的非折叠选区,但用户已经主动 hide 了工具栏
                // (短按同位置) → 不应该让工具栏又冒出来
                floatingToolbarHost?.clearActive(state)
            }
            // else: 长按 word selection,showingContextToolbar = true → 保持工具栏显示
        } else if (!showingContextToolbar) {
            floatingToolbarHost?.clearActive(state)
        }
    }

    DisposableEffect(state, floatingToolbarHost) {
        onDispose {
            floatingToolbarHost?.clearActive(state)
        }
    }

    fun showContextToolbar() {
        isDragging = false
        showingContextToolbar = true
        suppressedToolbarSelection = null
        // 无条件记录"工具栏已显示"对应的锚点光标和 tap 计数,
        // 这样长按触发的 word selection(selection 变成非折叠)也不会让 toggle 失效
        if (platformAdapter.requiresCollapsedCursorContextToolbarToggle) {
            androidLastCollapsedCursor = state.textFieldValue.selection.start
            androidSameCursorTapCount = 2
        }
        floatingToolbarHost?.reportActive(activeInfo())
    }

    fun hideContextToolbar() {
        showingContextToolbar = false
        suppressedToolbarSelection = state.textFieldValue.selection.takeIf { !it.collapsed }
        floatingToolbarHost?.clearActive(state)
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
            // 用 host 的 active 状态作为"工具栏是否真的在显示"的真实依据,
            // 因为 LaunchedEffect 在非折叠分支可能重置过 showingContextToolbar,
            // 但 host.active 仍然指向 state(被 LaunchedEffect 再次 report 上去)
            val isToolbarShown = showingContextToolbar ||
                floatingToolbarHost?.active?.state === state
            if (isToolbarShown) {
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
                                if (event.changes.any { it.isConsumed }) continue
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val pressOffset = event.changes.firstOrNull()?.position
                                        // 工具栏当前已显示的话,先把 BasicTextField 紧跟着会
                                        // 触发的 long-press → showTextContextMenu 吞掉,
                                        // 否则 BasicTextField 会在 long-press timeout 后
                                        // 重新调 onShow() 把刚 hide 掉的工具栏又显示回来。
                                        if (showingContextToolbar ||
                                            floatingToolbarHost?.active?.state === state
                                        ) {
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
                        val consumed = onUserValueChange?.invoke(oldValue, newValue) == true
                        if (!consumed) {
                            state.onValueChange(newValue, platformAdapter.supportsImeComposition)
                        }
                    },
                    onTextLayoutResult = { textLayoutResult = it },
                    onFocusChanged = { focused ->
                        isTextFieldFocused = focused
                        if (!focused) {
                            showingContextToolbar = false
                            isDragging = false
                            userDraggedSelection = false
                            suppressedToolbarSelection = null
                            androidLastCollapsedCursor = null
                            androidSameCursorTapCount = 0
                            floatingToolbarHost?.clearActive(state)
                        }
                    },
                    onPositioned = { position, widthPx, heightPx ->
                        textFieldWidthPx = widthPx
                        textFieldHeightPx = heightPx
                        textFieldGlobalPosition = position
                        floatingToolbarHost?.updateActivePosition(
                            state = state,
                            position = position,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            anchor = toolbarAnchor(),
                        )
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
                            onUndo = onUndo,
                            onRedo = onRedo
                        )
                    }
                )
            }
            // 浮动工具栏已上移到编辑器顶层(全局唯一),
            // 这里不再渲染,改由 [LocalFloatingToolbarHost] 提供状态给顶层。
        }
    }
}
