package com.taocent.simple.compose.component.richtext

import com.taocent.simple.compose.component.richtext.core.LocalRichTextConfig as CoreLocalRichTextConfig

/**
 * Local CompositionLocal for [RichTextConfig] — re-export from :richtext-core
 * 以保持 :richtext 模块原有的导入路径(`com.taocent.simple.compose.component.richtext.LocalRichTextConfig`)。
 */
val LocalRichTextConfig: androidx.compose.runtime.ProvidableCompositionLocal<RichTextConfig>
    get() = CoreLocalRichTextConfig
