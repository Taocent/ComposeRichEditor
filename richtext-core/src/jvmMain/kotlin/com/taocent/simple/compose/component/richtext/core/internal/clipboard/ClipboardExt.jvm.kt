package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(): ClipEntry =
    ClipEntry(StringSelection(text))

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString? {
    val transferable = asAwtTransferable ?: return null
    return try {
        val raw = transferable.getTransferData(DataFlavor.stringFlavor) as? String
        raw?.takeIf { it.isNotEmpty() }?.let { AnnotatedString(it) }
    } catch (_: UnsupportedFlavorException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: java.io.IOException) {
        null
    }
}
