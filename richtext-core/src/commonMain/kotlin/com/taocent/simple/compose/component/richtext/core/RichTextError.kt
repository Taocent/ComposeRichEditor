package com.taocent.simple.compose.component.richtext.core

/**
 * 富文本错误类别 — 用 sealed class 描述可观察的失败场景,供 UI 展示 snackbar / 调试日志使用。
 *
 * 错误通过 [RichTextState.errors] 暴露最近一条,调用方可观察 StateFlow 显示提示或上埋点。
 * 通过 [RichTextLogger] 注入日志接收器,生产环境可对接 Crashlytics / Sentry。
 */
sealed class RichTextError(
    /** 错误类别(供 i18n / 错误码使用) */
    val kind: Kind,
    /** 用户可读消息(已本地化或默认英文) */
    val message: String,
    /** 原始异常(可能为 null,例如剪贴板无权限但未抛异常) */
    val cause: Throwable? = null,
    /** 时间戳(epoch millis),便于 UI 排序或去重 */
    val timestamp: Long = currentTimeMillis(),
) {
    /** 错误分类 — 与具体异常解耦,UI 可按类别做不同处理(剪贴板错误提示重试、JSON 错误提示源数据格式) */
    enum class Kind {
        /** JSON 反序列化失败(粘贴板来源是损坏的 RichText JSON) */
        JSON_PARSE,
        /** HTML 反序列化失败(粘贴板来源是损坏的 HTML) */
        HTML_PARSE,
        /** Markdown 反序列化失败(粘贴板来源是损坏的 Markdown) */
        MARKDOWN_PARSE,
        /** 剪贴板读取失败(权限拒绝 / OS 限制) */
        CLIPBOARD_READ,
        /** 剪贴板写入失败(权限拒绝 / 容量超限) */
        CLIPBOARD_WRITE,
        /** 序列化失败(AnnotatedString → JSON 内部错误) */
        SERIALIZE,
    }

    /** 复制富文本(写入剪贴板)时的失败 */
    class ClipboardWrite(cause: Throwable? = null) : RichTextError(
        Kind.CLIPBOARD_WRITE,
        "复制到剪贴板失败",
        cause,
    )

    /** 粘贴富文本(读取剪贴板)时的失败 */
    class ClipboardRead(cause: Throwable? = null) : RichTextError(
        Kind.CLIPBOARD_READ,
        "从剪贴板读取失败",
        cause,
    )

    /** SmartPaste: 文本像 JSON 但解析失败 */
    class JsonParse(cause: Throwable? = null) : RichTextError(
        Kind.JSON_PARSE,
        "粘贴内容 JSON 解析失败",
        cause,
    )

    /** SmartPaste: 文本像 HTML 但解析失败 */
    class HtmlParse(cause: Throwable? = null) : RichTextError(
        Kind.HTML_PARSE,
        "粘贴内容 HTML 解析失败",
        cause,
    )

    /** SmartPaste: 文本像 Markdown 但解析失败 */
    class MarkdownParse(cause: Throwable? = null) : RichTextError(
        Kind.MARKDOWN_PARSE,
        "粘贴内容 Markdown 解析失败",
        cause,
    )

    /** AnnotatedString → JSON 序列化失败 */
    class Serialize(cause: Throwable? = null) : RichTextError(
        Kind.SERIALIZE,
        "富文本序列化失败",
        cause,
    )

    override fun toString(): String = "RichTextError(kind=$kind, message='$message', cause=$cause)"
}

/**
 * 提供跨平台的 epoch millis — JVM/Wasm/iOS 各自实现。
 * 测试时可注入固定时间戳验证排序/去重。
 */
internal expect fun currentTimeMillis(): Long
