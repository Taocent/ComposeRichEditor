# ComposeRichEditor

[中文文档](./README.zh-CN.md)

ComposeRichEditor is a Compose Multiplatform rich text editor library for Android, iOS, Desktop, and Web. It provides a plain rich text editor, a block-based rich text editor, rich text formatting, custom emoji rendering, hyperlink support, table editing, smart paste, JSON serialization, and cross-platform editor UI components.

> Status: `0.1.0-alpha01` is planned as the first public alpha. APIs may still change before a stable release.

## Modules

| Module | Artifact | Status | Description |
|---|---|---|---|
| `richtext-core` | `io.github.taocent:compose-richtext-core` | Alpha | Core state, formatting, paragraph model, emoji, paste, serialization, platform adapters, and shared UI internals. |
| `richtext` | `io.github.taocent:compose-richtext` | Alpha | Ready-to-use rich text editor composables, toolbar, panels, and dialogs. |
| `blockrichtext` | `io.github.taocent:compose-block-richtext` | Alpha / Experimental | Block-based editor with text blocks, tables, block navigation, and shared floating toolbar behavior. |
| `shared`, `androidApp`, `desktopApp`, `webApp`, `iosApp` | Not published | Sample | Demo applications and integration samples for supported platforms. |

## Platform Support

| Platform | Status | Notes |
|---|---|---|
| Android | Supported | Main mobile target; IME, selection, toolbar, and clipboard paths are actively tested. |
| iOS | Supported | Uses platform-specific IME composition handling for stable Chinese/Japanese/Korean input. |
| Desktop JVM | Supported | Supports keyboard selection, toolbar behavior, and rich text editing. |
| Web | Experimental | Build target exists; browser input and clipboard behavior may vary. |

## Installation

The first public alpha will use the following coordinates after publishing:

```kotlin
dependencies {
    implementation("io.github.taocent:compose-richtext-core:0.1.0-alpha01")
    implementation("io.github.taocent:compose-richtext:0.1.0-alpha01")
    implementation("io.github.taocent:compose-block-richtext:0.1.0-alpha01")
}
```

For local development before publishing, include the modules directly from this repository:

```kotlin
dependencies {
    implementation(project(":richtext-core"))
    implementation(project(":richtext"))
    implementation(project(":blockrichtext"))
}
```

## Quick Start

### Plain Rich Text Editor

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
        placeholder = "Start writing..."
    )
}
```

### Block Rich Text Editor

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

The block editor APIs are marked with `ExperimentalBlockRichTextApi` in the alpha series. Opt in at the smallest practical scope, such as the screen or wrapper composable that owns `BlockState`.

## Features

- Rich text editing based on Compose Multiplatform `BasicTextField` and `AnnotatedString`.
- Inline styles: bold, italic, underline, superscript, subscript, text color, background color, and font size.
- Paragraph alignment and paragraph model synchronization.
- Hyperlink insertion, hyperlink styling, and link-aware selection behavior.
- Custom emoji insertion and rendering.
- Smart paste for plain text, Markdown-like content, HTML-like content, and rich JSON content.
- JSON serialization and restoration for rich text content.
- Undo and redo support.
- Floating toolbar, bottom toolbar, text style panel, emoji panel, and dialogs.
- Block editor with text blocks and table editing.
- Platform-specific handling for IME, clipboard, context menu, and keyboard behavior.

## Known Limitations

- The project is in alpha; public API names and package structure may change.
- `blockrichtext` is more experimental than the plain `richtext` editor and requires `ExperimentalBlockRichTextApi` opt-in.
- Web support is experimental and may not match Android, iOS, and Desktop behavior yet.
- Maven publishing is configured for local and remote repositories; coordinates above describe the intended first alpha artifacts.
- Some package names still use the original project namespace and may be revised before stable release.

## Development

Run core checks:

```bash
./gradlew :richtext-core:jvmTest
./gradlew :richtext:jvmTest
./gradlew :blockrichtext:jvmTest
```

Compile common and JVM targets:

```bash
./gradlew :richtext-core:compileKotlinMetadata :richtext:compileKotlinJvm :blockrichtext:compileKotlinJvm
```

Compile iOS simulator targets:

```bash
./gradlew :richtext-core:compileKotlinIosSimulatorArm64 :richtext:compileKotlinIosSimulatorArm64 :blockrichtext:compileKotlinIosSimulatorArm64
```

Run sample apps:

```bash
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Open `iosApp` in Xcode to run the iOS sample.

## Documentation

- [RichTextEditor component notes](./ComponentInfo/RichTextEditor.md)
- [Project information](./ProjectInfo.md)
- More open-source documentation will be added under `docs/` before the first public release.

## Versioning

ComposeRichEditor follows semantic versioning after the stable `1.0.0` release. Before `1.0.0`, minor versions and alpha releases may contain breaking API changes.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](./CONTRIBUTING.md) before opening an issue or pull request.

## Security

Please report security issues according to [SECURITY.md](./SECURITY.md).

## License

ComposeRichEditor is licensed under the [Apache License 2.0](./LICENSE).
