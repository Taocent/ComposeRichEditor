package com.taocent.simple.compose.component.richtext.core

/**
 * 富文本操作的封装结果 — 替代 [Boolean] 表示成功/失败,失败时携带 [RichTextError] 详情。
 *
 * 使用方在 [smartPaste] / [loadFromJson] / clipboard 读写等可能失败的入口拿到该结果,
 * 决定是否提示用户、是否重试、是否回滚到 fallback 内容。
 */
sealed class PasteResult<out T> {

    /** 操作成功 — [value] 携带结果数据(可能为 null,例如剪贴板无内容时) */
    data class Success<T>(val value: T) : PasteResult<T>()

    /** 操作失败 — [error] 携带分类好的 [RichTextError] */
    data class Failure(val error: RichTextError) : PasteResult<Nothing>()

    /** 是否成功 */
    val isSuccess: Boolean get() = this is Success

    /** 是否失败 */
    val isFailure: Boolean get() = this is Failure

    /**
     * 成功时返回 [value],失败时返回 [default]。
     * 用于简洁的取值场景:`val text = result.getOrNull() ?: ""`
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrNull(): T? = if (this is Success) value as T? else null

    /**
     * 成功时执行 [onSuccess],失败时执行 [onFailure]。
     * 用于链式处理,避免大量 if/else。
     */
    inline fun fold(
        onSuccess: (T) -> Unit,
        onFailure: (RichTextError) -> Unit,
    ) {
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }
    }

    companion object {
        /** 工厂:成功 */
        fun <T> success(value: T): PasteResult<T> = Success(value)

        /** 工厂:失败 */
        fun <T> failure(error: RichTextError): PasteResult<T> = Failure(error)

        /**
         * 工厂:从 [block] 执行结果构造,捕获 [Throwable] 并包装为 [RichTextError]。
         * 用于在 try-catch 包裹的场景中简洁上报:
         * ```
         * val result = PasteResult.runCatching({ doSomething() }, ::JsonParse)
         * ```
         */
        inline fun <T> runCatching(
            errorFactory: (Throwable) -> RichTextError,
            block: () -> T,
        ): PasteResult<T> = try {
            Success(block())
        } catch (e: Throwable) {
            Failure(errorFactory(e))
        }
    }
}
