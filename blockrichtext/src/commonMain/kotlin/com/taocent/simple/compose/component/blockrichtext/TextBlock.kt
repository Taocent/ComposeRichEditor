package com.taocent.simple.compose.component.blockrichtext

import androidx.compose.ui.text.AnnotatedString

data class TextBlock(
    override val id: String = generateBlockId(),
    val content: AnnotatedString = AnnotatedString("")
) : DocumentBlock
