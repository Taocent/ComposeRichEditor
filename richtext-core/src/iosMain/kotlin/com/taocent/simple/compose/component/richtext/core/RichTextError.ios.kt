package com.taocent.simple.compose.component.richtext.core

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS 平台:返回 Unix epoch millis。
 */
internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
