package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Link
import com.composables.icons.feather.Smile
import com.composables.icons.feather.Table
import com.composables.icons.feather.Tool
import com.composables.icons.feather.Type
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Keyboard
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.Undo2
import com.taocent.simple.compose.component.richtext.core.internal.platform.rememberKeyboardHeight
import com.taocent.simple.compose.component.richtext.core.internal.dialog.ExportJsonDialog
import com.taocent.simple.compose.component.richtext.core.internal.dialog.LinkDialog
import com.taocent.simple.compose.component.blockrichtext.internal.dialog.TableInsertDialog
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
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardGetText
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.clipboardSetText
import com.taocent.simple.compose.component.blockrichtext.LocalRichTextConfig
import com.taocent.simple.compose.component.blockrichtext.RichTextConfig
import com.taocent.simple.compose.component.blockrichtext.internal.block.TextBlockItem
import com.taocent.simple.compose.component.blockrichtext.internal.block.renderer.TableRenderer
import com.taocent.simple.compose.component.richtext.core.internal.clipboard.toAnnotatedStringOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class BlockToolCategory {
    EMOJI,
    TEXT_STYLE,
    LINK
}

@Composable
fun BlockRichTextEditor(
    state: BlockState,
    modifier: Modifier = Modifier,
    config: RichTextConfig? = null,
) {
    // 当调用方未传入 config 时,自动注入 :blockrichtext 模块提供的 DefaultCustomEmojiIcons,
    // 使 EmojiPanel 与富文本中的 CustomEmojiCanvasOverlay 都能正常渲染默认 emoji。
    // 显式传入 config 的调用方应自行合并 DefaultCustomEmojiIcons(如果需要默认图标)。
    val effectiveConfig = config ?: RichTextConfig().copy(customEmojiIcons = DefaultCustomEmojiIcons)
    // 屏蔽系统文本工具栏,用我们自己的全局 FloatingToolbar 代替:
    // - 1.11+ 新机制([LocalTextContextMenuToolbarProvider] / [LocalTextContextMenuDropdownProvider]):
    //   选词/长按/双击/右键会调 `provider.showTextContextMenu(...)`,默认 Android 走系统
    //   ActionMode / iOS 走 EditMenu / Desktop 走 Compose Popup —— 全部用 no-op 屏蔽。
    // - 旧机制([LocalTextToolbar]):KMP 1.11.0 Desktop / iOS / js / wasmJs 仍走这里,作为 fallback。
    CompositionLocalProvider(
        LocalRichTextConfig provides effectiveConfig,
        LocalTextContextMenuDropdownProvider provides EmptyTextContextMenuProvider,
        LocalTextContextMenuToolbarProvider provides EmptyTextContextMenuProvider,
        LocalTextToolbar provides EmptyTextToolbar,
    ) {
        BlockRichTextEditorContent(state, modifier)
    }
}

@Composable
private fun BlockRichTextEditorContent(
    state: BlockState,
    modifier: Modifier
) {
    val focusRequesterMap = remember { mutableMapOf<String, FocusRequester>() }
    // 焦点管理:
    // - 只在 `focusRequestVersion > 0` 时才调用 `requestFocus()`。
    //   0 是初始值(从未自增),对应 BlockState 初始化时设置 focusedBlockId 而不弹焦点的场景;
    //   自增只发生在用户显式触发的操作中(键盘导航 navGoUp/Down、undo/redo 恢复 Text 光标等)。
    // - 不再监听 `focusedBlockId`,避免因 LazyColumn 滚动/重组让某个 block 首次进入组合时
    //   被动触发 `requestFocus()`,抢走表格单元格焦点并在移动端自动弹出软键盘。
    // - 首次聚焦、点击聚焦等"用户主动操作"已由 TextBlock 内部的 onFocusChanged 自行处理,
    //   不需要由顶层再次强制 requestFocus;只有显式发出的 focus 请求才走顶层。
    val focusRequestVersion = state.focusRequestVersion
    val focusedFocusRequester = focusRequesterMap.getOrPut(state.focusedBlockId) {
        FocusRequester()
    }
    LaunchedEffect(focusRequestVersion) {
        if (focusRequestVersion > 0) {
            focusedFocusRequester.requestFocus()
        }
    }
    fun restoreFocus() {
        val cellRequester = state.focusedTableCellFocusRequester
        if (cellRequester != null) {
            cellRequester.requestFocus()
        } else {
            focusedFocusRequester.requestFocus()
        }
    }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var activeCategory by remember { mutableStateOf<BlockToolCategory?>(null) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showTableInsertDialog by remember { mutableStateOf(false) }
    var restoreKeyboardAfterDialog by remember { mutableStateOf(false) }
    val keyboardHeightDp = rememberKeyboardHeight()
    val isKeyboardVisible = keyboardHeightDp > 0.dp
    val config = LocalRichTextConfig.current
    val defaultPanelHeight = config.panel.defaultHeight
    var rememberedKeyboardHeight by remember { mutableStateOf(defaultPanelHeight) }
    if (isKeyboardVisible && keyboardHeightDp > rememberedKeyboardHeight) {
        rememberedKeyboardHeight = keyboardHeightDp
    }
    var collapseKeyboardPanel by remember { mutableStateOf(false) }
    var keyboardReplacingPanel by remember { mutableStateOf(false) }
    val panelTargetHeight = when {
        collapseKeyboardPanel -> 0.dp
        keyboardReplacingPanel -> rememberedKeyboardHeight
        activeCategory != null -> rememberedKeyboardHeight
        keyboardHeightDp > 0.dp -> keyboardHeightDp
        else -> 0.dp
    }
    val animatedPanelHeight by animateDpAsState(
        targetValue = panelTargetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            collapseKeyboardPanel = false
            keyboardReplacingPanel = false
            activeCategory = null
        }
    }

    // 全局浮动工具栏宿主:每块 / 每格的 RichTextTextField 通过 LocalFloatingToolbarHost
    // 上报选区状态,顶层只渲染一个 FloatingToolbar。
    val floatingToolbarHost = remember { FloatingToolbarHostState() }

    CompositionLocalProvider(LocalFloatingToolbarHost provides floatingToolbarHost) {
        Column(modifier = modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.V) {
                coroutineScope.launch {
                    val entry = clipboard.getClipEntry() ?: return@launch
                    val clipText = entry.toAnnotatedStringOrNull()?.text
                    if (!clipText.isNullOrEmpty()) {
                        state.pasteJson(clipText)
                    }
                }
                return@onPreviewKeyEvent true
            }
        if (event.type == KeyEventType.KeyDown && state.navSelectedTableId != null) {
            when (event.key) {
                Key.DirectionDown -> {
                    state.navGoDown()
                    return@onPreviewKeyEvent true
                }
                Key.DirectionUp -> {
                    state.navGoUp()
                    return@onPreviewKeyEvent true
                }
                Key.Enter -> {
                    return@onPreviewKeyEvent state.navInsertBeforeTable()
                }
                Key.Backspace, Key.Delete -> {
                    return@onPreviewKeyEvent state.navDeleteFromAbove()
                }
            }
        }
        if (event.type == KeyEventType.KeyDown && state.selectedTableBlockId != null) {
            if (event.key == Key.Backspace) {
                state.deleteSelectedTable()
                state.navRestoreIfPending()
                return@onPreviewKeyEvent true
            }
        }
        false
    }) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(state.blocks, key = { it.id }) { block ->
            // 块级插入/删除平滑动画:
            // - fade in / fade out:220ms FastOutSlowInEasing,与项目"工具栏/面板 220ms"约束一致
            // - placement(块上下位移):无回弹 spring,跟 `animatedPanelHeight` 的
            //   `spring(DampingRatioNoBouncy, StiffnessMediumLow)` 风格统一
            // - 关键前提:每个 block 的 `key = { it.id }` 必须稳定,否则
            //   LazyColumn 会把"修改"误判为"先删除旧再插入新",无法播 placement 动画
            // - 不影响焦点:`focusRequestVersion` 自增时 `LaunchedEffect` 调用
            //   `requestFocus()`,目标 block 即使在 fade in 中,FocusRequester
            //   已在 composable 内 remember,内部会 await 目标 view 出现
            val itemModifier = Modifier.animateItem(
                fadeInSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                placementSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold,
                ),
                fadeOutSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
            when (block) {
                is TextBlock -> {
                    val blockFocusRequester = focusRequesterMap.getOrPut(block.id) {
                        FocusRequester()
                    }
                    TextBlockItem(
                        block = block,
                        blockState = state,
                        focusRequester = blockFocusRequester,
                        modifier = itemModifier,
                    )
                }
                is TableBlock -> {
                    TableRenderer(
                        block = block,
                        blockState = state,
                        modifier = itemModifier.fillMaxWidth(),
                    )
                }
            }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                state.saveSelection()
                            }
                        }
                    }
                }
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                ) {
                    ToolbarCategoryItem(
                        icon = { Image(Lucide.Undo2, contentDescription = "撤销") },
                        isActive = false,
                        enabled = state.canUndo,
                        onClick = { state.undo() }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Lucide.Redo2, contentDescription = "重做") },
                        isActive = false,
                        enabled = state.canRedo,
                        onClick = { state.redo() }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Feather.Smile, contentDescription = null) },
                        isActive = activeCategory == BlockToolCategory.EMOJI,
                        onClick = {
                            if (activeCategory == BlockToolCategory.EMOJI) {
                                activeCategory = null
                            } else {
                                collapseKeyboardPanel = false
                                keyboardReplacingPanel = false
                                if (isKeyboardVisible) keyboardController?.hide()
                                activeCategory = BlockToolCategory.EMOJI
                            }
                        }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Feather.Type, contentDescription = null) },
                        isActive = activeCategory == BlockToolCategory.TEXT_STYLE,
                        onClick = {
                            if (activeCategory == BlockToolCategory.TEXT_STYLE) {
                                activeCategory = null
                            } else {
                                collapseKeyboardPanel = false
                                keyboardReplacingPanel = false
                                if (isKeyboardVisible) keyboardController?.hide()
                                activeCategory = BlockToolCategory.TEXT_STYLE
                            }
                        }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Feather.Link, contentDescription = null) },
                        isActive = false,
                        onClick = {
                            restoreKeyboardAfterDialog = isKeyboardVisible
                            showLinkDialog = true
                        }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Feather.Table, contentDescription = null) },
                        isActive = false,
                        onClick = {
                            restoreKeyboardAfterDialog = isKeyboardVisible
                            showTableInsertDialog = true
                        }
                    )
                    ToolbarCategoryItem(
                        icon = { Image(Feather.Tool, contentDescription = null) },
                        isActive = false,
                        onClick = {
                            restoreKeyboardAfterDialog = isKeyboardVisible
                            showExportDialog = true
                        }
                    )
                }
                ToolbarCategoryItem(
                    icon = { Image(Lucide.Keyboard, contentDescription = "键盘") },
                    isActive = false,
                    onClick = {
                        if (isKeyboardVisible) {
                            collapseKeyboardPanel = true
                            keyboardController?.hide()
                        } else {
                            collapseKeyboardPanel = false
                            keyboardReplacingPanel = activeCategory != null
                            restoreFocus()
                            keyboardController?.show()
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedPanelHeight),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp
            ) {
                Crossfade(
                    targetState = activeCategory,
                    animationSpec = tween(durationMillis = config.panel.crossfadeAnimationMs)
                ) { category ->
                    when (category) {
                        BlockToolCategory.EMOJI -> {
                            val emojiSizeSp = state.currentSpanStyle().fontSize.let {
                                if (it != TextUnit.Unspecified) it else 16.sp
                            }
                            EmojiPanel(
                                onEmojiSelected = { emoji ->
                                    state.restoreSavedSelection()
                                    state.insertText(emoji)
                                    restoreFocus()
                                },
                                onCustomEmojiSelected = { emojiId ->
                                    state.restoreSavedSelection()
                                    state.insertCustomEmoji(emojiId, emojiSizeSp)
                                    restoreFocus()
                                }
                            )
                        }
                        BlockToolCategory.TEXT_STYLE -> TextStylePanel(
                            state = state,
                            restoreSelection = { state.restoreSavedSelection() },
                            onActionCompleted = { restoreFocus() }
                        )
                        else -> {}
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        LinkDialog(
            state = state,
            restoreSelection = { state.restoreSavedSelection() },
            onDismiss = { showLinkDialog = false },
            onConfirm = { showLinkDialog = false }
        )
    }

    var linkDialogEverShown by remember { mutableStateOf(false) }
    if (showLinkDialog) {
        linkDialogEverShown = true
    }

    LaunchedEffect(showLinkDialog) {
        if (!showLinkDialog && linkDialogEverShown) {
            val shouldRestoreKeyboard = restoreKeyboardAfterDialog
            restoreKeyboardAfterDialog = false
            if (shouldRestoreKeyboard) {
                delay(config.panel.linkDialogFocusRestoreDelayMs)
                restoreFocus()
                keyboardController?.show()
            }
        }
    }

    if (showExportDialog) {
        val jsonContent = remember { state.toJson() }
        ExportJsonDialog(
            jsonContent = jsonContent,
            onDismiss = { showExportDialog = false }
        )
    }

    var exportDialogEverShown by remember { mutableStateOf(false) }
    if (showExportDialog) {
        exportDialogEverShown = true
    }

    LaunchedEffect(showExportDialog) {
        if (!showExportDialog && exportDialogEverShown) {
            val shouldRestoreKeyboard = restoreKeyboardAfterDialog
            restoreKeyboardAfterDialog = false
            if (shouldRestoreKeyboard) {
                delay(config.panel.linkDialogFocusRestoreDelayMs)
                restoreFocus()
                keyboardController?.show()
            }
        }
    }

    if (showTableInsertDialog) {
        TableInsertDialog(
            onDismiss = {
                showTableInsertDialog = false
            },
            onConfirm = { rows, columns ->
                state.insertTable(rows, columns)
                showTableInsertDialog = false
            }
        )
    }

    var tableDialogEverShown by remember { mutableStateOf(false) }
    if (showTableInsertDialog) {
        tableDialogEverShown = true
    }

    LaunchedEffect(showTableInsertDialog) {
        if (!showTableInsertDialog && tableDialogEverShown) {
            val shouldRestoreKeyboard = restoreKeyboardAfterDialog
            restoreKeyboardAfterDialog = false
            if (shouldRestoreKeyboard) {
                delay(config.panel.linkDialogFocusRestoreDelayMs)
                restoreFocus()
                keyboardController?.show()
            }
        }
    }

    // 全局唯一浮动工具栏:只在有活动 RichTextTextField 上报选区时渲染。
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
                            val validIds = config.customEmojis.map { it.id }.toSet()
                            val displaySize = toolbarState.currentFontSize
                                .takeIf { it != TextUnit.Unspecified }
                                ?: 16.sp
                            val hasEmojiPattern = Regex("\\[([a-zA-Z0-9_]+)]")
                                .findAll(clipText)
                                .any { it.groupValues[1] in validIds }
                            if (hasEmojiPattern) {
                                toolbarState.insertTextWithEmojis(clipText, validIds, displaySize)
                            } else {
                                val anyEnabled = config.smartPasteJsonEnabled ||
                                        config.smartPasteHtmlEnabled ||
                                        config.smartPasteMarkdownEnabled
                                if (anyEnabled) {
                                    toolbarState.smartPaste(
                                        clipText,
                                        jsonEnabled = config.smartPasteJsonEnabled,
                                        htmlEnabled = config.smartPasteHtmlEnabled,
                                        markdownEnabled = config.smartPasteMarkdownEnabled,
                                        parsers = config.pasteParsers
                                    )
                                } else {
                                    toolbarState.insertText(clipText)
                                }
                            }
                        }
                    }
                }
            },
            onSelectAll = { toolbarState.selectAll() }
        )
    }
    }  // 闭合 CompositionLocalProvider
}

@Composable
fun rememberBlockState(
    maxUndoHistory: Int = 100,
    undoMergeIntervalMs: Long = 500
): BlockState {
    return remember {
        BlockState(
            maxUndoHistory = maxUndoHistory,
            undoMergeIntervalMs = undoMergeIntervalMs
        )
    }
}
