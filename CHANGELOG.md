# Changelog

[中文文档](./CHANGELOG.zh-CN.md)

All notable changes to ComposeRichEditor will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project intends to follow [Semantic Versioning](https://semver.org/) after the stable `1.0.0` release.

## [0.1.0-alpha01] - Unreleased

### Added

- Initial open-source project identity: ComposeRichEditor.
- Apache-2.0 license and contribution/security documentation.
- English and Chinese documentation for README, contribution guide, security policy, and changelog.
- Maven publishing configuration for `io.github.taocent:compose-richtext-core`, `io.github.taocent:compose-richtext`, and `io.github.taocent:compose-block-richtext`.
- Remote repository and signing configuration through environment variables.
- GitHub Actions CI for JVM tests, common/JVM compilation, iOS simulator compilation, and Maven local publication verification.

### Changed

- Replaced the default Kotlin Multiplatform README with library-oriented documentation.
- Standardized Maven group ID to `io.github.taocent`.
- Moved `CustomEmoji` and `DefaultCustomEmojis` to the public `richtext-core` API package.
- Marked block editor APIs with `ExperimentalBlockRichTextApi` for the alpha series.

### Verified

- `:richtext-core:jvmTest`, `:richtext:jvmTest`, and `:blockrichtext:jvmTest` pass.
- Common/JVM compilation passes for `richtext-core`, `richtext`, and `blockrichtext`.
- iOS Simulator Arm64 compilation passes for `richtext-core`, `richtext`, and `blockrichtext`.
- Local Maven publication has been verified with `publishToMavenLocal`.
