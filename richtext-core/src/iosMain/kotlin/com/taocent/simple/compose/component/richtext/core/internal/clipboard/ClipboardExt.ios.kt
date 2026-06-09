package com.taocent.simple.compose.component.richtext.core.internal.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalComposeUiApi::class)
actual fun AnnotatedString.toClipEntry(): ClipEntry =
    ClipEntry.withPlainText(text)

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun ClipEntry.toAnnotatedStringOrNull(): AnnotatedString? =
    getPlainText()?.takeIf { it.isNotEmpty() }?.let { AnnotatedString(it) }
