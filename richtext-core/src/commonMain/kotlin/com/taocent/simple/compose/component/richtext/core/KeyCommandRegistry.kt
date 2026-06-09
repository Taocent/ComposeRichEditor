package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

/**
 * KeyCommand 注册表 — 持有 [KeyCommand] → action 的映射,提供注册、取消、派发能力。
 *
 * action 接收 [KeyEvent] 并返回 `Boolean`:
 * - `true`:消费事件,不再向其它处理者传递
 * - `false`:不消费,允许 [RichTextTextField] 等上层做默认处理(例如剪贴板 Ctrl+C/X/V)
 *
 * 派发按注册顺序遍历,先注册先匹配;支持 [register] 时自动移除旧绑定(避免重复注册覆盖)。
 */
class KeyCommandRegistry {

    /** (KeyCommand, action) 对 — 使用 List 而非 Map,保证派发顺序可控。 */
    private data class Entry(val command: KeyCommand, val action: (KeyEvent) -> Boolean)

    private val entries: MutableList<Entry> = mutableListOf()

    /** 当前注册的快捷键数量。 */
    val size: Int get() = entries.size

    /**
     * 注册一个快捷键。如果 [command] 已存在,新 action 覆盖旧 action 并返回 true(便于 idempotent 重新注册)。
     */
    fun register(command: KeyCommand, action: (KeyEvent) -> Boolean): Boolean {
        val existing = entries.indexOfFirst { it.command == command }
        return if (existing >= 0) {
            entries[existing] = Entry(command, action)
            true
        } else {
            entries += Entry(command, action)
            false
        }
    }

    /**
     * 取消注册一个快捷键。返回是否实际移除了绑定。
     */
    fun unregister(command: KeyCommand): Boolean {
        val removed = entries.removeAll { it.command == command }
        return removed
    }

    /**
     * 派发 [event] 给已注册的快捷键。返回是否被任意 action 消费。
     *
     * 仅处理 [KeyEventType.KeyDown](避免 KeyUp 重复触发)。
     */
    fun dispatch(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        for (entry in entries) {
            if (entry.command.matches(event) && entry.action(event)) {
                return true
            }
        }
        return false
    }

    /** 清除所有已注册的快捷键 — 主要用于测试或彻底重置。 */
    fun clear() {
        entries.clear()
    }

    /** 返回当前已注册快捷键的不可变快照。 */
    fun registeredCommands(): List<KeyCommand> = entries.map { it.command }
}
