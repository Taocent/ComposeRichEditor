# RichTextEditor 富文本编辑器

不依赖 Markdown 的跨平台富文本编辑组件，基于 Compose Multiplatform 的 `BasicTextField` 和 `AnnotatedString` 实现。

## 组件属性 (Properties)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `state` | `RichTextState` | — | 富文本状态管理器（必传） |
| `modifier` | `Modifier` | `Modifier` | 应用于根布局的修饰符 |
| `placeholder` | `String` | `"请输入文本..."` | 输入框为空时显示的占位文本 |

## RichTextState 方法 (Methods)

| 方法 | 参数 | 说明 |
|------|------|------|
| `onValueChange` | `TextFieldValue` | 处理文本输入变化，自动应用当前格式 |
| `toggleBold` | — | 切换选中/Body加粗状态 |
| `toggleItalic` | — | 切换选中/Body斜体状态 |
| `toggleUnderline` | — | 切换选中/Body下划线状态 |
| `toggleSuperscript` | — | 切换选中/Body上标状态（与下标互斥） |
| `toggleSubscript` | — | 切换选中/Body下标状态（与上标互斥） |
| `setColor` | `Color` | 设置当前/SEL颜色，传入 `Color.Unspecified` 恢复默认 |
| `setBackground` | `Color` | 设置当前/SEL背景高亮色，传入 `Color.Unspecified` 恢复默认 |
| `setFontSize` | `TextUnit` | 设置当前/SEL字号，传入 `TextUnit.Unspecified` 恢复默认 |
| `clearFormatting` | — | 清除选中区域所有格式 |
| `currentSpanStyle` | — | 返回当前生效的 SpanStyle |

## RichTextState 字段 (Fields)

| 字段 | 类型 | 说明 |
|------|------|------|
| `textFieldValue` | `TextFieldValue` | 当前文本字段值（含 AnnotatedString 和选区） |
| `currentBold` | `Boolean` | 光标/选中位置是否加粗 |
| `currentItalic` | `Boolean` | 光标/选中位置是否斜体 |
| `currentUnderline` | `Boolean` | 光标/选中位置是否有下划线 |
| `currentSuperscript` | `Boolean` | 光标/选中位置是否为上标 |
| `currentSubscript` | `Boolean` | 光标/选中位置是否为下标 |
| `currentColor` | `Color` | 光标/选中位置的文字颜色 |
| `currentBackground` | `Color` | 光标/选中位置的背景高亮色 |
| `currentFontSize` | `TextUnit` | 光标/选中位置的字体大小 |
| `hasSelection` | `Boolean` | 当前是否有文本被选中 |
| `plainText` | `String` | 纯文本内容 |

## 使用示例 (Usage Examples)

### 基本用法

```kotlin
@Composable
fun MyRichTextScreen() {
    val state = rememberRichTextState()

    RichTextEditor(
        state = state,
        modifier = Modifier.fillMaxSize(),
        placeholder = "请输入内容..."
    )
}
```

### 带初始文本

```kotlin
val state = rememberRichTextState(initialText = "Hello World")
```

### 自定义格式化操作

```kotlin
Button(onClick = { state.toggleBold() }) {
    Text("B")
}
Button(onClick = { state.toggleSuperscript() }) {
    Text("x²")
}
```

## 预设值常量

### PresetColors

8 种预设颜色：黑、红、绿、蓝、橙、紫、青、棕

### PresetFontSizes

7 种预设字号：12sp、14sp、16sp、18sp、20sp、24sp、30sp

### PresetBackgroundColors

6 种预设背景高亮色：黄、绿、青、粉、橙、灰（均为半透明色）

## 已完成功能 (Completed Features)

- [x] 文本内容输入与编辑
- [x] 光标定位与文本选中
- [x] 加粗（Bold）格式化
- [x] 斜体（Italic）格式化
- [x] 下划线（Underline）格式化
- [x] 上标（Superscript）格式化 — `BaselineShift.Superscript`
- [x] 下标（Subscript）格式化 — `BaselineShift.Subscript`
- [x] 上标与下标互斥（开启一个自动关闭另一个）
- [x] 文字颜色设置（8 种预设颜色）
- [x] 文本背景高亮色设置（6 种预设半透明色）
- [x] 字体大小设置（7 种预设字号）
- [x] 清除格式功能
- [x] 选中文本应用格式
- [x] 混合样式选区 toggle 统一操作（先判断全体再设置）
- [x] 格式状态同步（工具栏高亮当前格式）
- [x] 新输入文本自动继承当前格式
- [x] 格式可视化工具栏
- [x] 点击工具栏按钮时保持光标/选区（PointerEventPass.Initial 选区快照）
- [x] Android、iOS、Desktop、Web 跨平台支持

## 技术方案

- **文本存储**：`AnnotatedString` + `SpanStyle` 实现富文本结构
- **输入处理**：通过 `BasicTextField` + `TextFieldValue` 管理编辑状态
- **变更检测**：前后缀对比算法检测新增文本，自动应用当前格式
- **格式应用**：`AnnotatedString.Builder.addStyle()` 为选区应用 SpanStyle
- **零依赖**：仅使用 Compose Multiplatform 内置 API，无 Markdown 或第三方富文本库
- **选区保持**：工具栏按钮点击时，通过 `pointerInput` + `PointerEventPass.Initial` 在焦点丢失前捕获选区快照，点击后先恢复选区再应用格式
