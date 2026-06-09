package com.taocent.simple.compose.component.blockrichtext

sealed interface DocumentBlock {
    val id: String
}

private var blockIdCounter = 0L

internal fun generateBlockId(): String {
    return "block_${++blockIdCounter}"
}
