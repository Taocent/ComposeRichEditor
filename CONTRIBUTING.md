# Contributing to ComposeRichEditor

[中文文档](./CONTRIBUTING.zh-CN.md)

Thank you for your interest in contributing to ComposeRichEditor.

ComposeRichEditor is a Compose Multiplatform rich text editor library. Contributions are welcome, especially around editor correctness, IME behavior, selection behavior, platform compatibility, tests, documentation, and sample improvements.

## Project Status

The project is preparing for its first alpha release. Public APIs may still change before `1.0.0`.

## Development Setup

Requirements:

- macOS is recommended for full Android, iOS, Desktop, and Web validation.
- JDK compatible with the configured Gradle/Android toolchain.
- Android Studio or IntelliJ IDEA with Kotlin Multiplatform and Compose Multiplatform support.
- Xcode for iOS sample validation.

## Useful Commands

Run core JVM tests:

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

## Pull Request Guidelines

- Keep changes focused and small where possible.
- Add or update tests for bug fixes and behavior changes.
- Update README or component documentation when changing public usage.
- Avoid breaking public APIs unless the change is intentional and documented.
- Do not commit local build outputs, IDE files, credentials, signing keys, or generated reports.
- Do not introduce dependencies without explaining why they are needed.

## Code Style

- Follow the existing Kotlin and Compose style in nearby files.
- Prefer clear state ownership and explicit platform-specific behavior.
- Keep common code platform-neutral; use platform source sets or adapters for platform-specific behavior.
- Avoid over-optimizing before there is a clear hotspot or measured regression.

## Testing Expectations

For editor behavior changes, consider tests for:

- Text input and IME composition.
- Selection and cursor movement.
- Span and paragraph style preservation.
- Hyperlink, emoji, paste, undo/redo, and serialization flows.
- Block editor and table behavior if the change touches `blockrichtext`.

## Reporting Bugs

When reporting bugs, please include:

- Platform and OS version.
- Kotlin, Compose Multiplatform, and library version or commit.
- Minimal reproduction steps.
- Expected behavior and actual behavior.
- Screenshots, screen recordings, or logs when helpful.

## License

By contributing to ComposeRichEditor, you agree that your contributions are licensed under the Apache License 2.0.
