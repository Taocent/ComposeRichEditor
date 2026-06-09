# SimpleComposeComponent

Kotlin Multiplatform 项目，基于 Compose Multiplatform 构建跨平台可复用 UI 组件库。

## 项目信息

| 属性 | 值 |
|------|-----|
| 项目名称 | SimpleComposeComponent |
| 项目类型 | Compose Multiplatform 组件库 |
| 支持平台 | Android、iOS、Desktop (JVM)、Web (JS/Wasm) |
| 命名空间 | com.taocent.simple.compose.component |
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose / Compose Multiplatform |

## 项目结构

```
SimpleComposeComponent/
├── ProjectInfo.md              # 项目信息与进度文档
├── ComponentInfo/              # 组件文档目录
│   └── ComponentName.md        # 各组件独立文档
├── TestScreens/                # 测试界面代码目录
│   └── TestComponentName.kt    # 各组件测试界面
├── shared/                     # 共享代码模块 (commonMain)
│   └── src/
│       ├── commonMain/         # 跨平台公共代码
│       ├── androidMain/        # Android 平台实现
│       ├── iosMain/            # iOS 平台实现
│       ├── jvmMain/            # Desktop JVM 平台实现
│       ├── jsMain/             # JS 平台实现
│       └── wasmJsMain/         # Wasm JS 平台实现
├── androidApp/                 # Android 应用入口
├── desktopApp/                 # Desktop 应用入口
├── iosApp/                     # iOS 应用入口
├── webApp/                     # Web 应用入口
├── build.gradle.kts            # 根构建脚本
├── settings.gradle.kts         # 项目设置
└── gradle/                     # Gradle 配置
```

## 组件列表

| 序号 | 组件名称 | 状态 | 文档 | 测试界面 |
|------|----------|------|------|----------|
| 1 | RichTextEditor | ✅ 已完成 | [RichTextEditor.md](ComponentInfo/RichTextEditor.md) | [TestRichTextEditor.kt](shared/src/commonMain/kotlin/com/taocent/simple/compose/component/testscreens/TestRichTextEditor.kt) |

## 开发进度

- [x] 项目初始化与多平台结构搭建
- [x] 主页导航功能实现
- [x] RichTextEditor 富文本编辑器组件开发
- [ ] 更多组件开发（进行中）

## 运行说明

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`
- Web (Wasm): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- Web (JS): `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS: 在 Xcode 中打开 iosApp/ 目录运行
