# 更新日志

[English](./CHANGELOG.md)

ComposeRichEditor 的所有重要变更都会记录在此文件中。

本文件格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。项目计划在稳定版 `1.0.0` 之后遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [0.1.0-alpha01] - 未发布

### 新增

- 初始开源项目身份：ComposeRichEditor。
- Apache-2.0 许可证、贡献指南和安全策略文档。
- README、贡献指南、安全策略和更新日志的中英文文档。
- 为 `io.github.taocent:compose-richtext-core`、`io.github.taocent:compose-richtext` 和 `io.github.taocent:compose-block-richtext` 添加 Maven 发布配置。
- 通过环境变量配置远程仓库和签名信息。
- 添加 GitHub Actions CI，用于 JVM 测试、common/JVM 编译、iOS 模拟器编译和 Maven Local 发布验证。

### 变更

- 将默认 Kotlin Multiplatform README 替换为面向库用户的说明文档。
- 将 Maven Group ID 统一为 `io.github.taocent`。
- 将 `CustomEmoji` 和 `DefaultCustomEmojis` 迁移到 `richtext-core` 的公开 API 包。
- 为块级编辑器 API 添加 `ExperimentalBlockRichTextApi` 标记，用于 alpha 阶段的实验性 API 管理。

### 已验证

- `:richtext-core:jvmTest`、`:richtext:jvmTest` 和 `:blockrichtext:jvmTest` 通过。
- `richtext-core`、`richtext` 和 `blockrichtext` 的 common/JVM 编译通过。
- `richtext-core`、`richtext` 和 `blockrichtext` 的 iOS Simulator Arm64 编译通过。
- 已通过 `publishToMavenLocal` 验证本地 Maven 发布。
