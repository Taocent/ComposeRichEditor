package com.taocent.simple.compose.component.richtext.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.taocent.simple.compose.component.richtext.core.internal.editor.TextEditor
import com.taocent.simple.compose.component.richtext.core.internal.emoji.CUSTOM_EMOJI_TAG
import com.taocent.simple.compose.component.richtext.core.internal.format.FormatManager
import com.taocent.simple.compose.component.richtext.core.internal.format.RichTextFormatState
import com.taocent.simple.compose.component.richtext.core.internal.hyperlink.HyperlinkManager
import com.taocent.simple.compose.component.richtext.core.internal.paragraph.ParagraphModelManager
import com.taocent.simple.compose.component.richtext.core.internal.paragraph.RichParagraphModel
import com.taocent.simple.compose.component.richtext.core.internal.selection.SelectionManager
import com.taocent.simple.compose.component.richtext.core.internal.serialization.RichTextSerializer
import com.taocent.simple.compose.component.richtext.core.document.DocumentModel
import com.taocent.simple.compose.component.richtext.core.document.DocumentModelMapper
import com.taocent.simple.compose.component.richtext.core.paste.PasteParser
import com.taocent.simple.compose.component.richtext.core.internal.serialization.SmartPaste
import com.taocent.simple.compose.component.richtext.core.internal.undo.UndoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val LinkColor = Color(0xFF1E88E5)
@Stable
class RichTextState(
    initialText: String = "",
    maxUndoHistory: Int = 100,
    undoMergeIntervalMs: Long = 500
) : RichTextFormatState {

    companion object {
        const val HYPERLINK_TAG = "URL"

        fun fromJson(json: String): AnnotatedString {
            return RichTextSerializer.fromJson(json)
        }
    }

    var textFieldValue by mutableStateOf(
        TextFieldValue(AnnotatedString(initialText))
    )
        private set

    override var currentBold by mutableStateOf(false)
    override var currentItalic by mutableStateOf(false)
    override var currentUnderline by mutableStateOf(false)
    override var currentStrikethrough by mutableStateOf(false)
    override var currentSuperscript by mutableStateOf(false)
    override var currentSubscript by mutableStateOf(false)
    override var currentColor by mutableStateOf(Color.Unspecified)
    override var currentBackground by mutableStateOf(Color.Unspecified)
    override var currentFontSize by mutableStateOf(TextUnit.Unspecified)
    override var currentTextAlign by mutableStateOf(TextAlign.Left)

    private val paragraphModelManager = ParagraphModelManager(textFieldValue.annotatedString)

    internal val paragraphModels: List<RichParagraphModel>
        get() = paragraphModelManager.models

    internal var justToggledStyle = false
    internal var skipNextUndoRecord = false
    private var copyBufferOriginal: AnnotatedString? = null
    private var copyBufferClipText: String? = null

    val hasSelection: Boolean
        get() = !textFieldValue.selection.collapsed

    val plainText: String
        get() = textFieldValue.annotatedString.text

    val documentModel: DocumentModel
        get() = DocumentModelMapper.fromAnnotatedString(textFieldValue.annotatedString)

    private val textEditor = TextEditor(this)
    private val formatManager = FormatManager(this)
    private val hyperlinkManager = HyperlinkManager(this)
    private val selectionManager = SelectionManager(this)
    private val undoManager = UndoManager(maxSize = maxUndoHistory, mergeIntervalMs = undoMergeIntervalMs)

    /**
     * 错误日志接收器 — 默认 [RichTextLogger.NoOp] 不输出。
     * 调用 [setLogger] 可运行时切换(例如 Debug 构建临时启用 [ConsoleLogger])。
     */
    var logger: RichTextLogger = RichTextLogger.NoOp
        private set

    private val _errors = MutableStateFlow<RichTextError?>(null)

    /**
     * 最近一次错误 — UI 可 collect 该 StateFlow 展示 snackbar / toast,处理完后调用 [acknowledgeError] 清空。
     * 仅保留最近一条;不会累积历史(历史应通过 [logger] 落地)。
     */
    val errors: StateFlow<RichTextError?> = _errors.asStateFlow()

    /**
     * 上报一个错误:写入 [errors] + 委托给 [logger]。所有 catch 块统一通过此入口收口。
     */
    internal fun reportError(error: RichTextError, context: String? = null) {
        _errors.value = error
        logger.log(error, context)
    }

    /**
     * 运行时切换错误日志接收器 — 例如 Debug 构建临时启用 [ConsoleLogger]。
     * 同时清空 [errors] 中残留的错误,避免在新 logger 上下文中误以为是新错误。
     */
    fun setLogger(newLogger: RichTextLogger) {
        logger = newLogger
        _errors.value = null
    }

    /**
     * UI 展示完错误后调用 — 显式确认已读,清空 [errors] 防止重复弹窗。
     */
    fun acknowledgeError() {
        _errors.value = null
    }

    /**
     * KeyCommand 注册表 — 默认注册 Bold / Italic / Underline / Undo / Redo 快捷键。
     * 使用方可 [addKeyCommand] / [removeKeyCommand] 扩展。
     */
    val keyCommands: KeyCommandRegistry = KeyCommandRegistry().also { registry ->
        // 修饰键采用 ctrl + meta 同时注册,Mac(⌘) 与 Win/Linux(Ctrl) 共享
        val boldCmd = KeyCommand(key = Key.B, ctrl = true, meta = true)
        val italicCmd = KeyCommand(key = Key.I, ctrl = true, meta = true)
        val underlineCmd = KeyCommand(key = Key.U, ctrl = true, meta = true)
        val undoCmd = KeyCommand(key = Key.Z, ctrl = true, meta = true)
        val redoCmd = KeyCommand(key = Key.Z, ctrl = true, meta = true, shift = true)
        val redoAltCmd = KeyCommand(key = Key.Y, ctrl = true, meta = true)  // Windows 习惯
        registry.register(boldCmd) { toggleBold(); true }
        registry.register(italicCmd) { toggleItalic(); true }
        registry.register(underlineCmd) { toggleUnderline(); true }
        registry.register(undoCmd) { undo(); true }
        registry.register(redoCmd) { redo(); true }
        registry.register(redoAltCmd) { redo(); true }
    }

    val canUndo: Boolean
        get() = undoManager.canUndo

    val canRedo: Boolean
        get() = undoManager.canRedo

    fun onValueChange(newValue: TextFieldValue) {
        textEditor.onValueChange(newValue)
    }

    fun onValueChange(newValue: TextFieldValue, supportsImeComposition: Boolean) {
        textEditor.onValueChange(newValue, supportsImeComposition)
    }

    override fun insertText(text: String) {
        textEditor.insertText(text)
    }

    fun insertTextWithEmojis(text: String, validEmojiIds: Set<String>, displaySize: TextUnit) {
        textEditor.insertTextWithEmojis(text, validEmojiIds, displaySize)
    }

    fun deleteSelection() {
        textEditor.deleteSelection()
    }

    fun selectAll() {
        val text = textFieldValue.text
        if (text.isNotEmpty()) {
            updateTextFieldValue(
                textFieldValue.copy(selection = TextRange(0, text.length))
            )
        }
    }

    override fun toggleBold() {
        formatManager.toggleBold()
    }

    override fun toggleItalic() {
        formatManager.toggleItalic()
    }

    override fun toggleUnderline() {
        formatManager.toggleUnderline()
    }

    override fun toggleStrikethrough() {
        formatManager.toggleStrikethrough()
    }

    override fun toggleSuperscript() {
        formatManager.toggleSuperscript()
    }

    override fun toggleSubscript() {
        formatManager.toggleSubscript()
    }

    override fun setColor(color: Color) {
        formatManager.setColor(color)
    }

    override fun setFontSize(size: TextUnit) {
        formatManager.setFontSize(size)
    }

    override fun setBackground(color: Color) {
        formatManager.setBackground(color)
    }

    override fun setTextAlign(align: TextAlign) {
        formatManager.setTextAlign(align)
    }

    override fun clearFormatting() {
        formatManager.clearFormatting()
    }

    override fun insertHyperlink(text: String, url: String) {
        hyperlinkManager.insertHyperlink(text, url)
    }

    fun format(block: RichTextFormat.() -> Unit) {
        RichTextFormat(this, formatManager).block()
    }

    override fun toJson(): String {
        return try {
            RichTextSerializer.toJson(textFieldValue.annotatedString)
        } catch (e: Throwable) {
            reportError(RichTextError.Serialize(e), "toJson")
            ""  // fallback:返回空 JSON 字符串,保证调用方不会 NPE
        }
    }

    /**
     * 从 JSON 字符串恢复内容。失败时返回 [PasteResult.Failure],调用方可决定是否回退到原内容。
     * 调用 [errors] StateFlow 也会收到错误,UI 可同时展示。
     */
    fun loadFromJson(json: String): PasteResult<Unit> = try {
        val annotated = RichTextSerializer.fromJson(json)
        textFieldValue = TextFieldValue(
            annotatedString = annotated,
            selection = TextRange(annotated.text.length)
        )
        paragraphModelManager.restoreFromAnnotated(annotated)
        justToggledStyle = false
        syncStyleFromSelection()
        PasteResult.success(Unit)
    } catch (e: Throwable) {
        val error = RichTextError.JsonParse(e)
        reportError(error, "loadFromJson")
        PasteResult.failure(error)
    }

    fun loadFromDocumentModel(document: DocumentModel) {
        val annotated = DocumentModelMapper.toAnnotatedString(document)
        restoreTextFieldValue(
            TextFieldValue(
                annotatedString = annotated,
                selection = TextRange(annotated.length)
            )
        )
        justToggledStyle = false
        syncStyleFromSelection()
    }

    fun getCopyText(): String {
        return getCopyAnnotatedString().text
    }

    fun copyToBuffer(): AnnotatedString {
        val sel = textFieldValue.selection
        val start = sel.min
        val end = sel.max
        copyBufferOriginal = textFieldValue.annotatedString.subSequence(start, end)
        val clipAnnotated = getCopyAnnotatedString()
        copyBufferClipText = clipAnnotated.text
        return clipAnnotated
    }

    fun setCopyBufferFromAnnotated(annotated: AnnotatedString): AnnotatedString {
        val clipAnnotated = convertEmojiToIdFormat(annotated)
        copyBufferOriginal = annotated
        copyBufferClipText = clipAnnotated.text
        return clipAnnotated
    }

    fun resolveClipboard(clipboardAnnotated: AnnotatedString?): AnnotatedString? {
        val clipText = copyBufferClipText
        val original = copyBufferOriginal
        if (clipText != null && original != null
            && clipboardAnnotated != null && clipboardAnnotated.text == clipText
        ) {
            return original
        }
        return clipboardAnnotated
    }

    fun getCopyAnnotatedString(): AnnotatedString {
        val sel = textFieldValue.selection
        if (sel.collapsed) return AnnotatedString("")
        val annotated = textFieldValue.annotatedString
        val start = sel.min
        val end = sel.max
        val emojis = annotated.getStringAnnotations(CUSTOM_EMOJI_TAG, start, end)
            .sortedBy { it.start }
        if (emojis.isEmpty()) {
            return annotated.subSequence(start, end)
        }
        val posMap = mutableMapOf<Int, Int>()
        val sb = StringBuilder()
        var oldPos = start
        var newPos = 0
        for (emoji in emojis) {
            while (oldPos < emoji.start) {
                posMap[oldPos] = newPos
                sb.append(annotated.text[oldPos])
                newPos++
                oldPos++
            }
            posMap[oldPos] = newPos
            val tag = "[${emoji.item}]"
            sb.append(tag)
            newPos += tag.length
            oldPos++
        }
        while (oldPos < end) {
            posMap[oldPos] = newPos
            sb.append(annotated.text[oldPos])
            newPos++
            oldPos++
        }
        posMap[end] = newPos
        val newText = sb.toString()
        val builder = AnnotatedString.Builder(newText.length)
        builder.append(newText)
        for (span in annotated.spanStyles) {
            val s = maxOf(span.start, start)
            val e = minOf(span.end, end)
            if (s < e) {
                val ns = posMap[s] ?: continue
                val ne = posMap[e] ?: continue
                if (ns < ne && ne <= newText.length) {
                    builder.addStyle(span.item, ns, ne)
                }
            }
        }
        for (paragraph in annotated.paragraphStyles) {
            val s = maxOf(paragraph.start, start)
            val e = minOf(paragraph.end, end)
            if (s <= e) {
                val ns = posMap[s] ?: continue
                val ne = posMap[e] ?: continue
                if (ns <= ne && ne <= newText.length) {
                    builder.addStyle(paragraph.item, ns, ne)
                }
            }
        }
        for (ann in annotated.getStringAnnotations(start, end)) {
            if (ann.tag == CUSTOM_EMOJI_TAG) continue
            val ns = posMap[ann.start] ?: continue
            val ne = posMap[ann.end] ?: continue
            if (ns < ne && ne <= newText.length) {
                builder.addStringAnnotation(ann.tag, ann.item, ns, ne)
            }
        }
        return builder.toAnnotatedString()
    }

    private fun convertEmojiToIdFormat(annotated: AnnotatedString): AnnotatedString {
        val emojis = annotated.getStringAnnotations(CUSTOM_EMOJI_TAG, 0, annotated.length)
            .sortedBy { it.start }
        if (emojis.isEmpty()) return annotated
        val posMap = mutableMapOf<Int, Int>()
        val sb = StringBuilder()
        var oldPos = 0
        var newPos = 0
        for (emoji in emojis) {
            while (oldPos < emoji.start) {
                posMap[oldPos] = newPos
                sb.append(annotated.text[oldPos])
                newPos++
                oldPos++
            }
            posMap[oldPos] = newPos
            val tag = "[${emoji.item}]"
            sb.append(tag)
            newPos += tag.length
            oldPos++
        }
        while (oldPos < annotated.text.length) {
            posMap[oldPos] = newPos
            sb.append(annotated.text[oldPos])
            newPos++
            oldPos++
        }
        posMap[annotated.text.length] = newPos
        val newText = sb.toString()
        val builder = AnnotatedString.Builder(newText.length)
        builder.append(newText)
        for (span in annotated.spanStyles) {
            val ns = posMap[span.start] ?: continue
            val ne = posMap[span.end] ?: continue
            if (ns < ne && ne <= newText.length) {
                builder.addStyle(span.item, ns, ne)
            }
        }
        for (paragraph in annotated.paragraphStyles) {
            val ns = posMap[paragraph.start] ?: continue
            val ne = posMap[paragraph.end] ?: continue
            if (ns <= ne && ne <= newText.length) {
                builder.addStyle(paragraph.item, ns, ne)
            }
        }
        for (ann in annotated.getStringAnnotations(0, annotated.length)) {
            if (ann.tag == CUSTOM_EMOJI_TAG) continue
            val ns = posMap[ann.start] ?: continue
            val ne = posMap[ann.end] ?: continue
            if (ns < ne && ne <= newText.length) {
                builder.addStringAnnotation(ann.tag, ann.item, ns, ne)
            }
        }
        return builder.toAnnotatedString()
    }

    fun insertAnnotatedString(annotated: AnnotatedString) {
        textEditor.insertAnnotatedString(annotated)
    }

    override fun insertCustomEmoji(emojiId: String, displaySize: TextUnit) {
        textEditor.insertCustomEmoji(emojiId, displaySize)
    }

    /**
     * 智能粘贴 — 自动检测 JSON / HTML / Markdown 格式并解析,失败时降级为纯文本。
     *
     * @return [PasteResult.Success] 时 value 为已插入的内容长度;[PasteResult.Failure] 仅在初始化阶段异常时出现。
     * 单段子解析失败不会中断整体粘贴(降级为纯文本),仅通过 [errors] / [logger] 记录。
     */
    fun smartPaste(text: String): PasteResult<Int> {
        val result = SmartPaste.detectAndParse(text, ::reportError)
        insertAnnotatedString(result)
        return PasteResult.success(result.length)
    }

    fun smartPaste(text: String, parsers: List<PasteParser>): PasteResult<Int> {
        val result = SmartPaste.detectAndParseFragment(
            text = text,
            errorReporter = ::reportError,
            extraParsers = parsers
        )
        insertAnnotatedString(result.annotatedString)
        return PasteResult.success(result.annotatedString.length)
    }

    /**
     * 智能粘贴(可指定启用的格式)— 同 [smartPaste] 但允许调用方禁用某些格式检测。
     */
    fun smartPaste(
        text: String,
        jsonEnabled: Boolean,
        htmlEnabled: Boolean,
        markdownEnabled: Boolean,
        parsers: List<PasteParser> = emptyList(),
    ): PasteResult<Int> {
        val result = SmartPaste.detectAndParse(
            text, ::reportError, jsonEnabled, htmlEnabled, markdownEnabled, parsers,
        )
        insertAnnotatedString(result)
        return PasteResult.success(result.length)
    }

    override fun undo() {
        val previousValue = undoManager.undo(textFieldValue) ?: return
        textFieldValue = previousValue
        paragraphModelManager.restoreFromAnnotated(previousValue.annotatedString)
        justToggledStyle = false
        syncStyleFromSelection()
    }

    override fun redo() {
        val nextValue = undoManager.redo(textFieldValue) ?: return
        textFieldValue = nextValue
        paragraphModelManager.restoreFromAnnotated(nextValue.annotatedString)
        justToggledStyle = false
        syncStyleFromSelection()
    }

    fun isInsideHyperlink(position: Int): Boolean {
        return hyperlinkManager.isInsideHyperlink(position)
    }

    fun getHyperlinkAtPosition(position: Int): TextRange? {
        return hyperlinkManager.getHyperlinkAtPosition(position)
    }

    fun restoreTextFieldValue(value: TextFieldValue) {
        textFieldValue = value
        paragraphModelManager.restoreFromAnnotated(value.annotatedString)
    }

    private var savedSelection: TextFieldValue? = null

    override fun saveSelection() {
        savedSelection = textFieldValue
    }

    override fun restoreSavedSelection() {
        savedSelection?.let { textFieldValue = it }
        savedSelection = null
    }

    override fun currentSpanStyle(): SpanStyle {
        val decorations = mutableListOf<TextDecoration>()
        if (currentUnderline) decorations.add(TextDecoration.Underline)
        if (currentStrikethrough) decorations.add(TextDecoration.LineThrough)
        return SpanStyle(
            fontWeight = if (currentBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (currentItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (decorations.isEmpty()) TextDecoration.None else TextDecoration.combine(decorations),
            baselineShift = when {
                currentSuperscript -> BaselineShift.Superscript
                currentSubscript -> BaselineShift.Subscript
                else -> BaselineShift.None
            },
            color = if (currentColor == Color.Unspecified) Color.Unspecified else currentColor,
            background = if (currentBackground == Color.Unspecified) Color.Unspecified else currentBackground,
            fontSize = if (currentFontSize == TextUnit.Unspecified) TextUnit.Unspecified else currentFontSize
        )
    }

    internal fun recordUndoSnapshot(oldValue: TextFieldValue, newValue: TextFieldValue) {
        undoManager.record(oldValue, newValue)
    }

    internal fun updateTextFieldValue(newValue: TextFieldValue) {
        val oldValue = textFieldValue
        val isComposing = newValue.composition != null
        if (skipNextUndoRecord) {
            skipNextUndoRecord = false
        } else if (!isComposing && oldValue.annotatedString != newValue.annotatedString) {
            undoManager.record(oldValue, newValue)
        }
        textFieldValue = newValue
    }

    internal fun paragraphIndexAt(offset: Int): Int {
        return paragraphModelManager.paragraphIndexAt(plainText, offset)
    }

    internal fun setParagraphTextAligns(selection: TextRange, align: TextAlign) {
        val annotated = paragraphModelManager.setTextAlign(textFieldValue.annotatedString, selection, align)
        updateTextFieldValue(textFieldValue.copy(annotatedString = annotated))
    }

    internal fun textAlignForSelection(selection: TextRange = textFieldValue.selection): TextAlign {
        return paragraphModelManager.textAlignForSelection(textFieldValue.annotatedString, selection)
    }

    internal fun syncParagraphModelsAfterTextChange(
        oldText: String,
        newAnnotated: AnnotatedString,
        editStart: Int,
        removedCount: Int,
        insertedText: String,
        fallbackTextAlign: TextAlign? = null
    ): AnnotatedString {
        return paragraphModelManager.syncAfterTextChange(
            oldText = oldText,
            newAnnotated = newAnnotated,
            editStart = editStart,
            removedCount = removedCount,
            insertedText = insertedText,
            fallbackTextAlign = fallbackTextAlign
        )
    }

    internal fun restoreParagraphModels(models: List<RichParagraphModel>) {
        paragraphModelManager.restore(models)
    }

    internal fun syncStyleFromSelection() {
        selectionManager.syncStyleFromSelection()
    }

    internal fun snapCursorSelection(
        annotated: AnnotatedString,
        oldSelection: TextRange,
        newSelection: TextRange
    ): TextRange {
        return hyperlinkManager.snapCursorSelection(annotated, oldSelection, newSelection)
    }

    internal fun snapCursorOutOfHyperlinks(
        oldSelection: TextRange,
        newValue: TextFieldValue
    ): TextFieldValue {
        return hyperlinkManager.snapCursorOutOfHyperlinks(oldSelection, newValue)
    }

    internal fun isSelectionFullyInHyperlink(): Boolean {
        return hyperlinkManager.isSelectionFullyInHyperlink()
    }

    internal fun isSelectionAllBold(): Boolean {
        return selectionManager.isSelectionAllBold()
    }

    internal fun isSelectionAllItalic(): Boolean {
        return selectionManager.isSelectionAllItalic()
    }

    internal fun isSelectionAllUnderlined(): Boolean {
        return selectionManager.isSelectionAllUnderlined()
    }

    internal fun isSelectionAllStrikethrough(): Boolean {
        return selectionManager.isSelectionAllStrikethrough()
    }

    internal fun isSelectionAllSuperscript(): Boolean {
        return selectionManager.isSelectionAllSuperscript()
    }

    internal fun isSelectionAllSubscript(): Boolean {
        return selectionManager.isSelectionAllSubscript()
    }

    /**
     * 注册一个快捷键 — 便捷方法。命令已存在则覆盖。
     * @return true 表示覆盖了已有命令,false 表示新增。
     */
    fun addKeyCommand(command: KeyCommand, action: () -> Boolean): Boolean {
        return keyCommands.register(command) { action() }
    }

    /**
     * 取消注册一个快捷键。
     * @return 是否实际移除了绑定。
     */
    fun removeKeyCommand(command: KeyCommand): Boolean {
        return keyCommands.unregister(command)
    }
}
