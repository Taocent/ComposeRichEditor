package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

actual fun AnnotatedString.toClipEntry(): ClipEntry =
    ClipEntry(ClipData.newPlainText("plain text", text))

actual suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString? {
    val item = if (clipData.itemCount > 0) clipData.getItemAt(0) else null
    val text = item?.text?.toString().orEmpty()
    return if (text.isEmpty()) null else AnnotatedString(text)
}
