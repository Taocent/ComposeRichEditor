package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

/**
 * 富文本快捷键描述 — 不可变数据类,封装 [Key] + 4 个修饰键状态。
 *
 * 使用方可通过 [parse] 从字符串创建(如 "Ctrl+B"、"Ctrl+Shift+Z")。
 * [ctrl] 与 [meta] 视为"主修饰键"的两种平台表示,任一为 true 即要求事件中 Ctrl/Meta 至少一个按下,
 * 便于 Mac(⌘) 与 Win/Linux(Ctrl) 共享同一快捷键。
 */
data class KeyCommand(
    val key: Key,
    val ctrl: Boolean = false,
    val meta: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
) {
    /**
     * 判断 [event] 是否匹配本命令。修饰键为 true 时要求按下,未声明的修饰键不要求"必须未按"(避免组合冲突)。
     * 注意:这里**不**做"未声明的修饰键必须未按"的严格检查,允许 Ctrl+Shift+B 也匹配 Ctrl+B 命令。
     * 严格匹配可通过 [strictMatch] 启用。
     */
    fun matches(event: KeyEvent, strictMatch: Boolean = false): Boolean =
        matchesKey(event.key, event.isCtrlPressed, event.isMetaPressed, event.isAltPressed, event.isShiftPressed, strictMatch)

    /**
     * 内部匹配入口:直接接收 Key 与 4 个修饰键状态,用于测试或非 [KeyEvent] 场景。
     * 公共 API 应使用 [matches]。
     *
     * 修饰键语义:
     * - [ctrl] 与 [meta] 中任一为 true,要求事件中 Ctrl 或 Meta 至少一个按下(跨平台共享主修饰键)。
     * - [ctrl] 与 [meta] 均为 false,要求事件中 Ctrl 与 Meta 都未按下(避免 Ctrl+B 误触发纯 B)。
     * - [alt] / [shift] 独立判定:声明为 true 则要求事件中也按下。
     * - [strictMatch] 为 true 时,未声明的修饰键必须未按(更严格的"字面匹配")。
     */
    internal fun matchesKey(
        eventKey: Key,
        ctrlPressed: Boolean,
        metaPressed: Boolean,
        altPressed: Boolean,
        shiftPressed: Boolean,
        strictMatch: Boolean,
    ): Boolean {
        if (eventKey != key) return false
        // 主修饰键:Ctrl/Meta 至少一个(若声明了)
        if (ctrl || meta) {
            if (!ctrlPressed && !metaPressed) return false
        } else if (ctrlPressed || metaPressed) {
            return false
        }
        if (alt && !altPressed) return false
        if (shift && !shiftPressed) return false
        if (strictMatch) {
            if (!alt && altPressed) return false
            if (!shift && shiftPressed) return false
        }
        return true
    }

    companion object {
        /**
         * 从字符串解析 KeyCommand — 支持 "Ctrl+B" / "Cmd+B" / "Ctrl+Shift+Z" / "Alt+Delete" 等。
         * 不识别的 Key 名称或修饰键返回 null(调用方应日志告警而非抛异常)。
         */
        fun parse(spec: String): KeyCommand? {
            val parts = spec.split("+").map { it.trim() }
            if (parts.isEmpty()) return null
            val keyStr = parts.last()
            val key = parseKey(keyStr) ?: return null
            var ctrl = false
            var meta = false
            var alt = false
            var shift = false
            for (mod in parts.dropLast(1)) {
                when (mod.lowercase()) {
                    "ctrl", "control" -> ctrl = true
                    "cmd", "command", "meta" -> meta = true
                    "alt", "option" -> alt = true
                    "shift" -> shift = true
                    else -> return null  // 未知修饰键
                }
            }
            return KeyCommand(key = key, ctrl = ctrl, meta = meta, alt = alt, shift = shift)
        }

        private fun parseKey(name: String): Key? = when (name.uppercase()) {
            "A" -> Key.A
            "B" -> Key.B
            "C" -> Key.C
            "D" -> Key.D
            "E" -> Key.E
            "F" -> Key.F
            "G" -> Key.G
            "H" -> Key.H
            "I" -> Key.I
            "J" -> Key.J
            "K" -> Key.K
            "L" -> Key.L
            "M" -> Key.M
            "N" -> Key.N
            "O" -> Key.O
            "P" -> Key.P
            "Q" -> Key.Q
            "R" -> Key.R
            "S" -> Key.S
            "T" -> Key.T
            "U" -> Key.U
            "V" -> Key.V
            "W" -> Key.W
            "X" -> Key.X
            "Y" -> Key.Y
            "Z" -> Key.Z
            "TAB" -> Key.Tab
            "ENTER" -> Key.Enter
            "ESCAPE", "ESC" -> Key.Escape
            "DELETE", "DEL" -> Key.Delete
            "BACKSPACE" -> Key.Backspace
            "SPACE", "SPACEBAR" -> Key.Spacebar
            "ARROW_LEFT", "LEFT" -> Key.DirectionLeft
            "ARROW_RIGHT", "RIGHT" -> Key.DirectionRight
            "ARROW_UP", "UP" -> Key.DirectionUp
            "ARROW_DOWN", "DOWN" -> Key.DirectionDown
            else -> null
        }
    }

    override fun toString(): String {
        val parts = mutableListOf<String>()
        if (ctrl) parts += "Ctrl"
        if (meta) parts += "Meta"
        if (alt) parts += "Alt"
        if (shift) parts += "Shift"
        parts += key.toString().substringAfterLast('.')
        return parts.joinToString("+")
    }
}
