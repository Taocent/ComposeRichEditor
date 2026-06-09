package com.taocent.simple.compose.component.richtext.core.internal.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.taocent.simple.compose.component.richtext.core.RichTextState
import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig
import com.taocent.simple.compose.component.richtext.core.internal.platform.rememberPopupWindowPositionOffset

@Composable
fun FloatingToolbar(
    state: RichTextState,
    textFieldGlobalPosition: IntOffset,
    textFieldHeightPx: Int,
    anchor: IntOffset,
    showToolbar: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tbConfig = LocalRichTextConfig.current.floatingToolbar
    val cursorPaddingPx = with(LocalDensity.current) { tbConfig.cursorPadding.roundToPx() }
    val scrollOffsetPx = (anchor.y - textFieldHeightPx + cursorPaddingPx).coerceAtLeast(0)

    // iOS 上 `positionInWindow()` 返回 UIWindow 坐标(包含 status bar),但
    // `Popup.calculatePosition` 期望 "without insets" 坐标系,所以要先减掉
    // statusBars.top;其他平台期望 no-op,详见 [rememberPopupWindowPositionOffset]。
    val popupPositionOffset = rememberPopupWindowPositionOffset()
    val anchorXInWindow = textFieldGlobalPosition.x + anchor.x - popupPositionOffset.x
    val anchorTopInWindow = textFieldGlobalPosition.y + anchor.y - scrollOffsetPx - popupPositionOffset.y

    val offsetProvider = rememberToolbarPositionProvider(
        anchorXInWindowPx = anchorXInWindow,
        anchorTopInWindowPx = anchorTopInWindow
    )

    // 工具栏最大宽度来自配置,不再与 textFieldWidthPx 挂钩 —
    // 表格单元格这种窄字段下原本会因 textFieldWidthPx/2 太小而装不下所有按钮。
    val maxToolbarWidthDp = tbConfig.maxWidth

    Box(modifier = modifier) {
        Popup(
            popupPositionProvider = offsetProvider,
            onDismissRequest = onDismiss
        ) {
            AnimatedVisibility(
                visible = showToolbar,
                enter = fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { -it / 2 }
                )
            ) {
                FloatingToolbarContent(
                    state = state,
                    maxToolbarWidthDp = maxToolbarWidthDp,
                    onCopy = onCopy,
                    onCut = onCut,
                    onPaste = onPaste,
                    onSelectAll = onSelectAll
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarContent(
    state: RichTextState,
    maxToolbarWidthDp: Dp,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tbConfig = LocalRichTextConfig.current.floatingToolbar
    val scrollState = rememberScrollState()
    val hasSelection = !state.textFieldValue.selection.collapsed
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .widthIn(max = maxToolbarWidthDp)
                .clip(RoundedCornerShape(tbConfig.shapeRadius))
                .background(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = RoundedCornerShape(tbConfig.shapeRadius)
                )
                .horizontalScroll(scrollState)
                .padding(horizontal = tbConfig.horizontalPadding, vertical = tbConfig.verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSelection) {
                FormatButton(
                    label = "B",
                    isActive = state.currentBold,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(fontWeight = FontWeight.Bold),
                    onClick = { state.toggleBold() }
                )

                FormatButton(
                    label = "I",
                    isActive = state.currentItalic,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(fontStyle = FontStyle.Italic),
                    onClick = { state.toggleItalic() }
                )

                FormatButton(
                    label = "U",
                    isActive = state.currentUnderline,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(textDecoration = TextDecoration.Underline),
                    onClick = { state.toggleUnderline() }
                )

                FormatButton(
                    label = "S",
                    isActive = state.currentStrikethrough,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(textDecoration = TextDecoration.LineThrough),
                    onClick = { state.toggleStrikethrough() }
                )

                FormatButton(
                    label = "x²",
                    isActive = state.currentSuperscript,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(fontSize = tbConfig.scriptFontSize),
                    onClick = { state.toggleSuperscript() }
                )

                FormatButton(
                    label = "x₂",
                    isActive = state.currentSubscript,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(fontSize = tbConfig.scriptFontSize),
                    onClick = { state.toggleSubscript() }
                )

                Spacer(
                    modifier = Modifier
                        .padding(horizontal = tbConfig.dividerSpacing)
                        .width(tbConfig.dividerWidth)
                        .height(tbConfig.dividerHeight)
                        .background(
                            MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f)
                        )
                )

                FormatButton(
                    label = "A",
                    isActive = false,
                    activeColor = MaterialTheme.colorScheme.inversePrimary,
                    textStyle = TextStyle(fontSize = tbConfig.scriptFontSize),
                    onClick = { state.clearFormatting() }
                )

                Spacer(
                    modifier = Modifier
                        .padding(horizontal = tbConfig.dividerSpacing)
                        .width(tbConfig.dividerWidth)
                        .height(tbConfig.dividerHeight)
                        .background(
                            MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.3f)
                        )
                )
            }

            FormatButton(
                label = "复制",
                isActive = false,
                activeColor = MaterialTheme.colorScheme.inversePrimary,
                onClick = onCopy
            )

            FormatButton(
                label = "剪切",
                isActive = false,
                activeColor = MaterialTheme.colorScheme.inversePrimary,
                onClick = onCut
            )

            FormatButton(
                label = "粘贴",
                isActive = false,
                activeColor = MaterialTheme.colorScheme.inversePrimary,
                onClick = onPaste
            )

            FormatButton(
                label = "全选",
                isActive = false,
                activeColor = MaterialTheme.colorScheme.inversePrimary,
                onClick = onSelectAll
            )
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    textStyle: TextStyle = TextStyle(),
    onClick: () -> Unit
) {
    val tbConfig = LocalRichTextConfig.current.floatingToolbar
    val contentColor = if (isActive) {
        activeColor
    } else {
        MaterialTheme.colorScheme.inverseOnSurface
    }

    Box(
        modifier = Modifier
            .size(tbConfig.buttonSize)
            .clip(RoundedCornerShape(tbConfig.buttonRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = tbConfig.buttonHPadding, vertical = tbConfig.buttonVPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.merge(textStyle),
            color = contentColor,
            fontSize = tbConfig.buttonFontSize
        )
    }
}

@Composable
private fun rememberToolbarPositionProvider(
    anchorXInWindowPx: Int,
    anchorTopInWindowPx: Int
): PopupPositionProvider {
    val tbConfig = LocalRichTextConfig.current.floatingToolbar
    val topPaddingPx = with(LocalDensity.current) { tbConfig.abovePadding.roundToPx() }
    return androidx.compose.runtime.remember(anchorXInWindowPx, anchorTopInWindowPx, topPaddingPx) {
        ToolbarPositionProvider(
            anchorXInWindowPx = anchorXInWindowPx,
            anchorTopInWindowPx = anchorTopInWindowPx,
            topPaddingPx = topPaddingPx
        )
    }
}

private class ToolbarPositionProvider(
    private val anchorXInWindowPx: Int,
    private val anchorTopInWindowPx: Int,
    private val topPaddingPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val popupWidth = popupContentSize.width
        val popupHeight = popupContentSize.height

        val x = (anchorXInWindowPx - popupWidth / 2)
            .coerceIn(0, (windowSize.width - popupWidth).coerceAtLeast(0))

        val y = (anchorTopInWindowPx - popupHeight - topPaddingPx)
            .coerceAtLeast(0)

        return IntOffset(x, y)
    }
}
