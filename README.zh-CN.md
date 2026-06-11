# ComposeRichEditor

[![Maven Central](https://img.shields.io/maven-central/v/io.github.taocent/compose-richtext.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.taocent/compose-richtext)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](./LICENSE)

[English](./README.md)

ComposeRichEditor 是一个面向 Android、iOS、Desktop 和 Web 的 Compose Multiplatform 富文本编辑器库。它提供普通富文本编辑器、块级富文本编辑器、富文本格式化、自定义 emoji 渲染、超链接、表格编辑、智能粘贴、JSON 序列化和跨平台编辑器 UI 组件。

> 当前状态：`0.1.0-alpha01` 已发布到 Maven Central。在稳定版发布前，API 仍可能调整。

## 模块

| 模块 | Artifact | 状态 | 说明 |
|---|---|---|---|
| `richtext-core` | `io.github.taocent:compose-richtext-core` | Alpha | 核心状态、格式化、段落模型、emoji、粘贴、序列化、平台适配和共享 UI 内部能力。 |
| `richtext` | `io.github.taocent:compose-richtext` | Alpha | 可直接使用的富文本编辑器 Composable、工具栏、面板和弹窗。 |
| `blockrichtext` | `io.github.taocent:compose-block-richtext` | Alpha / Experimental | 块级编辑器，包含文本块、表格、块导航和全局浮动工具栏行为。 |
| `shared`、`androidApp`、`desktopApp`、`webApp`、`iosApp` | 不发布 | 示例 | 支持平台的 Demo 应用和集成示例。 |

## 平台支持

| 平台 | 状态 | 说明 |
|---|---|---|
| Android | 支持 | 主要移动端目标；IME、选区、工具栏和剪贴板路径正在持续测试。 |
| iOS | 支持 | 针对中文/日文/韩文等输入法使用平台特定 IME composition 处理。 |
| Desktop JVM | 支持 | 支持键盘选区、工具栏行为和富文本编辑。 |
| Web | 实验性 | 已有构建目标；浏览器输入和剪贴板行为可能存在差异。 |

## 安装

第一个公开 alpha 已发布到 Maven Central：

```kotlin
dependencies {
    implementation("io.github.taocent:compose-richtext-core:0.1.0-alpha01")
    implementation("io.github.taocent:compose-richtext:0.1.0-alpha01")
    implementation("io.github.taocent:compose-block-richtext:0.1.0-alpha01")
}
```

本地开发可以直接依赖仓库模块：

```kotlin
dependencies {
    implementation(project(":richtext-core"))
    implementation(project(":richtext"))
    implementation(project(":blockrichtext"))
}
```

## 快速开始

### 普通富文本编辑器

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.taocent.simple.compose.component.richtext.RichTextEditor
import com.taocent.simple.compose.component.richtext.rememberRichTextState

@Composable
fun EditorScreen() {
    val state = rememberRichTextState()

    RichTextEditor(
        state = state,
        modifier = Modifier.fillMaxSize(),
        placeholder = "开始输入..."
    )
}
```

### 块级富文本编辑器

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.taocent.simple.compose.component.blockrichtext.BlockRichTextEditor
import com.taocent.simple.compose.component.blockrichtext.ExperimentalBlockRichTextApi
import com.taocent.simple.compose.component.blockrichtext.rememberBlockState

@OptIn(ExperimentalBlockRichTextApi::class)
@Composable
fun BlockEditorScreen() {
    val state = rememberBlockState()

    BlockRichTextEditor(
        state = state,
        modifier = Modifier.fillMaxSize()
    )
}
```

块级编辑器 API 在 alpha 阶段标记为 `ExperimentalBlockRichTextApi`。建议在拥有 `BlockState` 的页面或封装 Composable 上以尽量小的作用域 opt-in。

## 功能

- 基于 Compose Multiplatform `BasicTextField` 和 `AnnotatedString` 的富文本编辑。
- 行内样式：加粗、斜体、下划线、上标、下标、文字颜色、背景色和字号。
- 段落对齐和段落模型同步。
- 超链接插入、超链接样式和链接感知选区行为。
- 自定义 emoji 插入和渲染。
- 智能粘贴，支持纯文本、类 Markdown 内容、类 HTML 内容和富文本 JSON 内容。
- 富文本内容 JSON 序列化和恢复。
- 撤销和重做。
- 浮动工具栏、底部工具栏、文字样式面板、emoji 面板和弹窗。
- 块级编辑器，支持文本块和表格编辑。
- 针对 IME、剪贴板、上下文菜单和键盘行为的平台特定处理。

## 已知限制

- 项目仍处于 alpha 阶段，公开 API 名称和包结构可能调整。
- `blockrichtext` 比普通 `richtext` 编辑器更加实验性，需要显式 opt-in `ExperimentalBlockRichTextApi`。
- Web 支持仍为实验性，行为可能还无法完全对齐 Android、iOS 和 Desktop。
- Maven 发布已配置本地和远程仓库，文档中的坐标是计划中的首个 alpha artifact。
- 部分包名仍使用原始项目命名空间，稳定版前可能调整。

## 开发

运行核心测试：

```bash
./gradlew :richtext-core:jvmTest
./gradlew :richtext:jvmTest
./gradlew :blockrichtext:jvmTest
```

编译 common 和 JVM 目标：

```bash
./gradlew :richtext-core:compileKotlinMetadata :richtext:compileKotlinJvm :blockrichtext:compileKotlinJvm
```

编译 iOS 模拟器目标：

```bash
./gradlew :richtext-core:compileKotlinIosSimulatorArm64 :richtext:compileKotlinIosSimulatorArm64 :blockrichtext:compileKotlinIosSimulatorArm64
```

运行示例应用：

```bash
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

iOS 示例请用 Xcode 打开 `iosApp` 运行。

## 文档

- [RichTextEditor 组件说明](./ComponentInfo/RichTextEditor.md)
- [项目信息](./ProjectInfo.md)
- 首个公开版本前会继续在 `docs/` 下补充更多开源文档。

## 版本策略

ComposeRichEditor 在稳定版 `1.0.0` 之后遵循语义化版本。`1.0.0` 之前，minor 版本和 alpha 版本可能包含破坏性 API 变更。

## 贡献

欢迎参与贡献。提交 issue 或 pull request 前，请阅读 [CONTRIBUTING.zh-CN.md](./CONTRIBUTING.zh-CN.md)。

## 安全

安全问题请按 [SECURITY.zh-CN.md](./SECURITY.zh-CN.md) 说明进行报告。

## 许可证

ComposeRichEditor 使用 [Apache License 2.0](./LICENSE) 许可证。
