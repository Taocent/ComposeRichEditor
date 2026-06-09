package com.taocent.simple.compose.component.richtext

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Hash
import com.composables.icons.feather.Link
import com.composables.icons.feather.Link2
import com.composables.icons.feather.Smile
import com.composables.icons.feather.Tool
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Keyboard
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Undo2
import com.taocent.simple.compose.component.richtext.core.internal.dialog.ExportJsonDialog
import com.taocent.simple.compose.component.richtext.core.internal.dialog.LinkDialog
import com.taocent.simple.compose.component.richtext.core.internal.ui.panel.EmojiPanel
import com.taocent.simple.compose.component.richtext.core.internal.ui.panel.TextStylePanel
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.FloatingToolbar
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.FloatingToolbarHostState
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.LocalFloatingToolbarHost
import com.taocent.simple.compose.component.richtext.core.internal.toolbar.ToolbarCategoryItem
import com.taocent.simple.compose.component.richtext.core.internal.ui.EmptyTextContextMenuProvider
import com.taocent.simple.compose.component.richtext.core.internal.ui.EmptyTextToolbar
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.ui.platform.LocalTextToolbar
import com.taocent.simple.compose.component.richtext.core.internal.platform.rememberKeyboardHeight
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardGetText
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardSetText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * 开箱即用的富文本编辑器 — 内部组合 [RichTextTextField] / [RichTextToolbar] / [RichTextPanel] / [RichTextEditorDialogs]。
 *
 * 如果使用方需要自定义布局(如把工具栏移到顶部、只嵌入 TextField 等),
 * 可直接调用上述独立 Composable 自行拼装:
 * ```
 * Column {
 *     RichTextToolbar(state, editorState, focusRequester) // 独立使用
 *     RichTextTextField(state, modifier = Modifier.weight(1f)) // 独立嵌入任何场景
 *     RichTextPanel(state, editorState, focusRequester)       // 独立使用
 * }
 * RichTextEditorDialogs(state, editorState, focusRequester)   // 单独渲染对话框层
 * ```
 */
@Composable
fun RichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    placeholder: String = "请输入文本...",
    config: RichTextConfig? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    editorState: RichTextEditorState = rememberRichTextEditorState(),
) {
    // 当调用方未传入 config 时,自动注入 :richtext 模块提供的 DefaultCustomEmojiIcons,
    // 使 EmojiPanel 与富文本中的 CustomEmojiCanvasOverlay 都能正常渲染默认 emoji。
    // 显式传入 config 的调用方应自行合并 DefaultCustomEmojiIcons(如果需要默认图标)。
    val effectiveConfig = config ?: RichTextConfig().copy(customEmojiIcons = DefaultCustomEmojiIcons)
    // 全局唯一浮动工具栏宿主:RichTextTextField 通过 LocalFloatingToolbarHost 上报选区。
    val floatingToolbarHost = remember { FloatingToolbarHostState() }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    CompositionLocalProvider(
        LocalRichTextConfig provides effectiveConfig,
        LocalFloatingToolbarHost provides floatingToolbarHost,
        // 屏蔽系统文本工具栏:
        // - 1.11+ 新机制([LocalTextContextMenuToolbarProvider] / [LocalTextContextMenuDropdownProvider]):
        //   选词/长按/双击/右键会调 `provider.showTextContextMenu(...)`,默认 Android 走系统
        //   ActionMode / iOS 走 EditMenu / Desktop 走 Compose Popup —— 全部用 no-op 屏蔽。
        // - 旧机制([LocalTextToolbar]):KMP 1.11.0 Desktop / iOS / js / wasmJs 仍走这里,作为 fallback。
        LocalTextContextMenuDropdownProvider provides EmptyTextContextMenuProvider,
        LocalTextContextMenuToolbarProvider provides EmptyTextContextMenuProvider,
        LocalTextToolbar provides EmptyTextToolbar,
    ) {
        Column(modifier = modifier) {
            RichTextTextField(
                state = state,
                modifier = Modifier.weight(1f),
                placeholder = placeholder,
                focusRequester = focusRequester,
            )
            RichTextEditorContainer(
                state = state,
                editorState = editorState,
                focusRequester = focusRequester,
            )
        }

        // 全局唯一浮动工具栏
        val activeToolbar = floatingToolbarHost.active
        if (activeToolbar != null) {
            val toolbarState = activeToolbar.state
            val richTextClipboard = remember(toolbarState, clipboard) {
                object {
                    suspend fun setText(text: AnnotatedString) =
                        clipboardSetText(clipboard, toolbarState, text)
                    suspend fun getText(): AnnotatedString? =
                        clipboardGetText(clipboard, toolbarState).getOrNull()
                }
            }
            FloatingToolbar(
                state = toolbarState,
                textFieldGlobalPosition = activeToolbar.position,
                textFieldHeightPx = activeToolbar.heightPx,
                anchor = activeToolbar.anchor,
                showToolbar = !activeToolbar.isDragging,
                onDismiss = { floatingToolbarHost.dismiss() },
                onCopy = {
                    val sel = toolbarState.textFieldValue.selection
                    if (!sel.collapsed) {
                        val selected = toolbarState.textFieldValue.annotatedString
                            .subSequence(sel.min, sel.max)
                        coroutineScope.launch { richTextClipboard.setText(selected) }
                    }
                },
                onCut = {
                    val sel = toolbarState.textFieldValue.selection
                    if (!sel.collapsed) {
                        val selected = toolbarState.textFieldValue.annotatedString
                            .subSequence(sel.min, sel.max)
                        coroutineScope.launch { richTextClipboard.setText(selected) }
                        toolbarState.deleteSelection()
                    }
                },
                onPaste = {
                    coroutineScope.launch {
                        val clipAnnotated = richTextClipboard.getText()
                        if (clipAnnotated != null && clipAnnotated.text.isNotEmpty()) {
                            val clipText = clipAnnotated.text
                            val hasRichStyles = clipAnnotated.spanStyles.isNotEmpty()
                            if (hasRichStyles) {
                                toolbarState.insertAnnotatedString(clipAnnotated)
                            } else {
                                toolbarState.insertText(clipText)
                            }
                        }
                    }
                },
                onSelectAll = { toolbarState.selectAll() }
            )
        }
    }
    RichTextEditorDialogs(
        state = state,
        editorState = editorState,
        focusRequester = focusRequester,
    )
}

/**
 * 工具栏 + 展开面板的组合容器(横线分隔)。
 *
 * 抽出 [RichTextEditor] 的下半部分,便于使用方在"工具栏 + 面板"作为整体,
 * 而 [RichTextTextField] 自由嵌入到任何其他 Composable 树中的场景。
 */
@Composable
internal fun RichTextEditorContainer(
    state: RichTextState,
    editorState: RichTextEditorState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardHeightDp = rememberKeyboardHeight()
    val isKeyboardVisible = keyboardHeightDp > 0.dp
    val config = LocalRichTextConfig.current
    val defaultPanelHeight = config.panel.defaultHeight
    if (editorState.rememberedKeyboardHeight < defaultPanelHeight) {
        editorState.rememberedKeyboardHeight = defaultPanelHeight
    }
    if (isKeyboardVisible && keyboardHeightDp > editorState.rememberedKeyboardHeight) {
        editorState.rememberedKeyboardHeight = keyboardHeightDp
    }
    var collapseKeyboardPanel by remember { mutableStateOf(false) }
    var keyboardReplacingPanel by remember { mutableStateOf(false) }
    val panelTargetHeight = when {
        collapseKeyboardPanel -> 0.dp
        keyboardReplacingPanel -> editorState.rememberedKeyboardHeight
        editorState.activeCategory != null -> editorState.rememberedKeyboardHeight
        keyboardHeightDp > 0.dp -> keyboardHeightDp
        else -> 0.dp
    }
    val animatedPanelHeight by animateDpAsState(
        targetValue = panelTargetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            collapseKeyboardPanel = false
            keyboardReplacingPanel = false
            editorState.clearCategory()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press &&
                            !state.textFieldValue.selection.collapsed
                        ) {
                            state.saveSelection()
                        }
                    }
                }
            },
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CompositionLocalProvider(LocalPanelHeightOrNull provides animatedPanelHeight) {
            RichTextToolbar(
                state = state,
                editorState = editorState,
                focusRequester = focusRequester,
                onPanelOpenRequested = {
                    collapseKeyboardPanel = false
                    keyboardReplacingPanel = false
                },
                onKeyboardCollapseRequested = { collapseKeyboardPanel = true },
                onKeyboardExpandRequested = {
                    collapseKeyboardPanel = false
                    keyboardReplacingPanel = editorState.activeCategory != null
                },
            )
            RichTextPanel(
                state = state,
                editorState = editorState,
                focusRequester = focusRequester,
            )
        }
    }
}

/**
 * 富文本工具栏 — 撤销/重做/表情/文字样式/链接/@提及/导出/键盘切换。
 *
 * 可独立使用:只需提供 [state] / [editorState] / [focusRequester] 即可渲染。
 * 工具栏按钮的展开/折叠状态由 [editorState.activeCategory] 承载,点击面板
 * 中具体动作会触发 [state] 上的命令;面板内容由 [RichTextPanel] 渲染。
 *
 * 键盘可见性从 CompositionLocal 读取(通过 [androidx.compose.ui.platform.LocalSoftwareKeyboardController]
 * + [rememberKeyboardHeight] 内部组合),点击 Emoji / TextStyle 按钮时如键盘仍可见会自动收起。
 */
@Composable
fun RichTextToolbar(
    state: RichTextState,
    editorState: RichTextEditorState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isKeyboardVisible: Boolean = (LocalSoftwareKeyboardController.current != null && rememberKeyboardHeight() > 0.dp),
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
    onPanelOpenRequested: () -> Unit = {},
    onKeyboardCollapseRequested: () -> Unit = {},
    onKeyboardExpandRequested: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        ) {
            ToolbarCategoryItem(
                icon = { Image(Lucide.Undo2, contentDescription = "撤销") },
                isActive = false,
                enabled = state.canUndo,
                onClick = { state.undo() },
            )
            ToolbarCategoryItem(
                icon = { Image(Lucide.Redo2, contentDescription = "重做") },
                isActive = false,
                enabled = state.canRedo,
                onClick = { state.redo() },
            )
            ToolbarCategoryItem(
                icon = { Image(Feather.Smile, contentDescription = null) },
                isActive = editorState.activeCategory == ToolCategory.EMOJI,
                onClick = {
                    val willOpen = editorState.activeCategory != ToolCategory.EMOJI
                    if (willOpen) {
                        onPanelOpenRequested()
                        if (isKeyboardVisible) keyboardController?.hide()
                    }
                    editorState.toggleCategory(ToolCategory.EMOJI)
                },
            )
            ToolbarCategoryItem(
                icon = { Image(Lucide.Type, contentDescription = null) },
                isActive = editorState.activeCategory == ToolCategory.TEXT_STYLE,
                onClick = {
                    val willOpen = editorState.activeCategory != ToolCategory.TEXT_STYLE
                    if (willOpen) {
                        onPanelOpenRequested()
                        if (isKeyboardVisible) keyboardController?.hide()
                    }
                    editorState.toggleCategory(ToolCategory.TEXT_STYLE)
                },
            )
            ToolbarCategoryItem(
                icon = { Image(Feather.Link, contentDescription = null) },
                isActive = false,
                onClick = { editorState.requestLinkDialog(isKeyboardVisible) },
            )
            ToolbarCategoryItem(
                icon = { Image(Feather.Hash, contentDescription = null) },
                isActive = false,
                onClick = {
                    state.restoreSavedSelection()
                    state.insertHyperlink("@赤红", "")
                },
            )
            ToolbarCategoryItem(
                icon = { Image(Feather.Tool, contentDescription = null) },
                isActive = false,
                onClick = { editorState.requestExportDialog(isKeyboardVisible) },
            )
        }
        ToolbarCategoryItem(
            icon = { Image(Lucide.Keyboard, contentDescription = "键盘") },
            isActive = false,
            onClick = {
                if (isKeyboardVisible) {
                    onKeyboardCollapseRequested()
                    keyboardController?.hide()
                } else {
                    onKeyboardExpandRequested()
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
        )
    }
}

/**
 * 内部 CompositionLocal — 由 [RichTextEditorContainer] 提供其计算出来的 animatedPanelHeight,
 * 供同一组合树下的 [RichTextPanel] 默认参数消费。独立使用 [RichTextPanel] 时为 null,
 * 此时 Panel 内部自行计算。
 */
private val LocalPanelHeightOrNull = compositionLocalOf<Dp?> { null }

/**
 * 富文本展开面板 — 根据 [RichTextEditorState.activeCategory] 渲染 EmojiPanel / TextStylePanel。
 *
 * 独立使用:工具栏点击后 activeCategory 改变即可触发此 Composable 显示对应内容。
 * 高度由 [animatedPanelHeight] 控制(动画 dp),使用方应将其与 [RichTextToolbar] 组合。
 * 不传 [animatedPanelHeight] 时,内部会基于 [editorState] + 键盘高度自动计算(开箱即用)。
 */
@Composable
fun RichTextPanel(
    state: RichTextState,
    editorState: RichTextEditorState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    animatedPanelHeight: Dp = LocalPanelHeightOrNull.current ?: animatePanelHeight(editorState),
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedPanelHeight),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Crossfade(
            targetState = editorState.activeCategory,
            animationSpec = tween(durationMillis = 200),
        ) { category ->
            when (category) {
                ToolCategory.EMOJI -> {
                    val emojiSizeSp = state.currentSpanStyle().fontSize.let {
                        if (it != TextUnit.Unspecified) it else 16.sp
                    }
                    EmojiPanel(
                        onEmojiSelected = { emoji ->
                            state.restoreSavedSelection()
                            state.insertText(emoji)
                            focusRequester.requestFocus()
                        },
                        onCustomEmojiSelected = { emojiId ->
                            state.restoreSavedSelection()
                            state.insertCustomEmoji(emojiId, emojiSizeSp)
                            focusRequester.requestFocus()
                        },
                    )
                }
                ToolCategory.TEXT_STYLE -> TextStylePanel(
                    state = state,
                    restoreSelection = { state.restoreSavedSelection() },
                    onActionCompleted = { focusRequester.requestFocus() },
                )
                else -> {}
            }
        }
    }
}

/**
 * 对话框层 — 链接对话框 + 导出 JSON 对话框。
 *
 * 独立使用:将其放在编辑器同级的 Box 中即可接收 [editorState] 触发的对话框事件。
 * 关闭链接对话框后会通过 [LaunchedEffect] 自动 refocus + showKeyboard。
 */
@Composable
fun RichTextEditorDialogs(
    state: RichTextState,
    editorState: RichTextEditorState,
    focusRequester: FocusRequester,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (editorState.showLinkDialog) {
        LinkDialog(
            state = state,
            restoreSelection = { state.restoreSavedSelection() },
            onDismiss = { editorState.dismissLinkDialog() },
            onConfirm = { editorState.dismissLinkDialog() },
        )
    }
    var linkDialogEverShown by remember { mutableStateOf(false) }
    if (editorState.showLinkDialog) {
        linkDialogEverShown = true
    }
    LaunchedEffect(editorState.showLinkDialog) {
        if (!editorState.showLinkDialog && linkDialogEverShown) {
            val shouldRestoreKeyboard = editorState.consumeKeyboardRestoreRequest()
            if (shouldRestoreKeyboard) {
                delay(200)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    if (editorState.showExportDialog) {
        val jsonContent = remember { state.toJson() }
        ExportJsonDialog(
            jsonContent = jsonContent,
            onDismiss = { editorState.dismissExportDialog() },
        )
    }
    var exportDialogEverShown by remember { mutableStateOf(false) }
    if (editorState.showExportDialog) {
        exportDialogEverShown = true
    }
    LaunchedEffect(editorState.showExportDialog) {
        if (!editorState.showExportDialog && exportDialogEverShown) {
            val shouldRestoreKeyboard = editorState.consumeKeyboardRestoreRequest()
            if (shouldRestoreKeyboard) {
                delay(200)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
}

/**
 * 内部 helper:监听 [editorState] 与键盘状态,计算面板的目标高度并应用弹簧动画。
 * 拆出便于 [RichTextPanel] 默认参数直接调用,使用方也可显式传 `animatedPanelHeight` 跳过此开销。
 */
@Composable
private fun animatePanelHeight(
    editorState: RichTextEditorState,
): androidx.compose.ui.unit.Dp {
    val keyboardHeightDp = rememberKeyboardHeight()
    val isKeyboardVisible = keyboardHeightDp > 0.dp
    val config = LocalRichTextConfig.current
    val defaultPanelHeight = config.panel.defaultHeight
    if (editorState.rememberedKeyboardHeight < defaultPanelHeight) {
        editorState.rememberedKeyboardHeight = defaultPanelHeight
    }
    if (isKeyboardVisible && keyboardHeightDp > editorState.rememberedKeyboardHeight) {
        editorState.rememberedKeyboardHeight = keyboardHeightDp
    }
    val panelTargetHeight = when {
        editorState.activeCategory != null -> editorState.rememberedKeyboardHeight
        isKeyboardVisible -> keyboardHeightDp
        else -> 0.dp
    }
    val animatedPanelHeight by animateDpAsState(
        targetValue = panelTargetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
    )
    return animatedPanelHeight
}
