# 参与贡献 ComposeRichEditor

[English](./CONTRIBUTING.md)

感谢你有兴趣参与 ComposeRichEditor。

ComposeRichEditor 是一个 Compose Multiplatform 富文本编辑器库。欢迎围绕编辑器正确性、IME 行为、选区行为、平台兼容性、测试、文档和示例改进提交贡献。

## 项目状态

项目正在准备第一个 alpha 版本。在 `1.0.0` 稳定版发布前，公开 API 仍可能发生变化。

## 开发环境

建议环境：

- macOS，用于完整验证 Android、iOS、Desktop 和 Web。
- 与当前 Gradle/Android 工具链兼容的 JDK。
- Android Studio 或 IntelliJ IDEA，并启用 Kotlin Multiplatform 和 Compose Multiplatform 支持。
- Xcode，用于 iOS 示例验证。

## 常用命令

运行核心 JVM 测试：

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

## Pull Request 要求

- 尽量保持改动聚焦且范围较小。
- 修复 bug 或修改行为时，请添加或更新测试。
- 修改公开用法时，请同步更新 README 或组件文档。
- 除非有明确说明，否则避免破坏公开 API。
- 不要提交本地构建产物、IDE 文件、凭据、签名密钥或生成报告。
- 不要在没有说明理由的情况下引入新依赖。

## 代码风格

- 遵循邻近文件已有的 Kotlin 和 Compose 风格。
- 优先保持清晰的状态归属和显式的平台特定行为。
- common 代码应保持平台无关；平台差异使用 source set 或 adapter 处理。
- 没有明确热点或可验证回归前，避免过度优化。

## 测试建议

编辑器行为改动建议考虑以下测试：

- 文本输入和 IME composition。
- 选区和光标移动。
- Span 样式和段落样式保留。
- 超链接、emoji、粘贴、撤销/重做和序列化流程。
- 如果改动涉及 `blockrichtext`，请覆盖块编辑器和表格行为。

## 报告 Bug

报告 bug 时请尽量包含：

- 平台和系统版本。
- Kotlin、Compose Multiplatform、库版本或 commit。
- 最小复现步骤。
- 期望行为和实际行为。
- 必要时提供截图、录屏或日志。

## 许可证

向 ComposeRichEditor 提交贡献即表示你同意你的贡献按照 Apache License 2.0 授权。
