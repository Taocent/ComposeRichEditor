package com.taocent.simple.compose.component.richtext.core

/**
 * 富文本日志接口 — 内部操作(粘贴/序列化/剪贴板)出现可观察错误时统一上报。
 *
 * 默认实现 [NoOpLogger] 不做任何事(性能零开销);生产环境可注入 Crashlytics / Sentry / Logcat 适配器。
 * 通过 [RichTextConfig.logger] 配置,或运行时调用 [RichTextState.setLogger] 临时切换。
 */
interface RichTextLogger {

    /**
     * 上报一个错误。线程安全由实现方保证(实现内部建议加锁 / 使用线程安全队列)。
     * @param error 错误详情(类别 / 消息 / 异常 / 时间戳)
     * @param context 错误发生上下文(例如 "smartPaste"、"loadFromJson" 等调用方标识)
     */
    fun log(error: RichTextError, context: String? = null)

    companion object {
        /** 默认 NoOp 实例,线程安全可共享。 */
        val NoOp: RichTextLogger = NoOpLogger()
    }
}

/**
 * 默认 NoOp 实现 — 不做任何事,避免在生产环境意外输出日志。
 */
class NoOpLogger : RichTextLogger {
    override fun log(error: RichTextError, context: String?) {
        // 故意留空
    }
}

/**
 * 控制台输出实现 — 适合开发/调试阶段。生产环境建议替换为 Crashlytics / Sentry 适配器。
 *
 * 输出格式:`[RichTextError] <context> kind=<kind> message=<message> cause=<cause>`
 */
class ConsoleLogger(
    private val printStream: (String) -> Unit = ::defaultPrint,
    private val includeStackTrace: Boolean = false,
) : RichTextLogger {
    override fun log(error: RichTextError, context: String?) {
        val ctxPart = context?.let { " context=$it" } ?: ""
        val line = "[RichTextError]$ctxPart kind=${error.kind} message='${error.message}' cause=${error.cause}"
        printStream(line)
        if (includeStackTrace) {
            error.cause?.printStackTrace()
        }
    }

    private companion object {
        fun defaultPrint(line: String) {
            println(line)
        }
    }
}
