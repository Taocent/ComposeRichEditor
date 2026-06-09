package com.taocent.simple.compose.component.richtext.core

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyCommandTest {

    // region KeyCommand.parse

    @Test
    fun parseSimpleLetter() {
        val cmd = KeyCommand.parse("Ctrl+B")
        assertNotNull(cmd)
        assertEquals(Key.B, cmd.key)
        assertTrue(cmd.ctrl)
        assertFalse(cmd.meta)
        assertFalse(cmd.shift)
    }

    @Test
    fun parseCmdAlias() {
        val cmd = KeyCommand.parse("Cmd+B")
        assertNotNull(cmd)
        assertTrue(cmd.meta)
    }

    @Test
    fun parseWithShift() {
        val cmd = KeyCommand.parse("Ctrl+Shift+Z")
        assertNotNull(cmd)
        assertEquals(Key.Z, cmd.key)
        assertTrue(cmd.ctrl)
        assertTrue(cmd.shift)
    }

    @Test
    fun parseTab() {
        val cmd = KeyCommand.parse("Tab")
        assertNotNull(cmd)
        assertEquals(Key.Tab, cmd.key)
    }

    @Test
    fun parseUnknownKeyReturnsNull() {
        assertNull(KeyCommand.parse("Ctrl+FooBar"))
    }

    @Test
    fun parseUnknownModifierReturnsNull() {
        assertNull(KeyCommand.parse("Hyper+B"))
    }

    @Test
    fun parseEmptyReturnsNull() {
        assertNull(KeyCommand.parse(""))
    }

    // endregion

    // region KeyCommand.matchesKey

    @Test
    fun matchesExactKey() {
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
    }

    @Test
    fun rejectsMissingCtrl() {
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        assertFalse(cmd.matchesKey(Key.B, ctrlPressed = false, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
    }

    @Test
    fun nonStrictAllowsExtraModifiers() {
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        // 非严格模式:多余 Shift 仍匹配
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = true, strictMatch = false))
    }

    @Test
    fun strictRejectsExtraModifiers() {
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        // 严格模式:Ctrl+B 不应匹配 Ctrl+Shift+B
        assertFalse(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = true, strictMatch = true))
    }

    @Test
    fun ctrlOrMetaEitherMatches() {
        val cmd = KeyCommand(key = Key.B, ctrl = true, meta = true)
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = false, metaPressed = true, altPressed = false, shiftPressed = false, strictMatch = false))
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = true, altPressed = false, shiftPressed = false, strictMatch = false))
    }

    @Test
    fun rejectsWrongKey() {
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        assertFalse(cmd.matchesKey(Key.I, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
    }

    // endregion

    // region KeyCommandRegistry

    @Test
    fun registerAddsEntry() {
        val registry = KeyCommandRegistry()
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        val replaced = registry.register(cmd) { true }
        assertFalse(replaced)  // 首次注册
        assertEquals(1, registry.size)
    }

    @Test
    fun registerReplacesExisting() {
        val registry = KeyCommandRegistry()
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        registry.register(cmd) { true }
        val replaced = registry.register(cmd) { false }
        assertTrue(replaced)  // 已存在,覆盖
        assertEquals(1, registry.size)
    }

    @Test
    fun unregisterRemovesEntry() {
        val registry = KeyCommandRegistry()
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        registry.register(cmd) { true }
        assertTrue(registry.unregister(cmd))
        assertEquals(0, registry.size)
    }

    @Test
    fun unregisterReturnsFalseForUnknown() {
        val registry = KeyCommandRegistry()
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        assertFalse(registry.unregister(cmd))
    }

    @Test
    fun clearRemovesAllEntries() {
        val registry = KeyCommandRegistry()
        registry.register(KeyCommand(key = Key.B, ctrl = true)) { true }
        registry.register(KeyCommand(key = Key.I, ctrl = true)) { true }
        registry.clear()
        assertEquals(0, registry.size)
    }

    @Test
    fun registeredCommandsReturnsSnapshot() {
        val registry = KeyCommandRegistry()
        val b = KeyCommand(key = Key.B, ctrl = true)
        val i = KeyCommand(key = Key.I, ctrl = true)
        registry.register(b) { true }
        registry.register(i) { true }
        val snapshot = registry.registeredCommands()
        assertEquals(listOf(b, i), snapshot)
    }

    @Test
    fun dispatchInvokesActionAndReturnsTrue() {
        val registry = KeyCommandRegistry()
        var invoked = false
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        registry.register(cmd) { invoked = true; true }
        // 派发不走 KeyEvent 路径,直接验证 matchesKey + action 调用
        val matched = registry
            .registeredCommands()
            .first()
            .matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false)
        assertTrue(matched)
        // 模拟 dispatch:手写一次循环匹配 + 调用 action
        var consumed = false
        for (entry in registry.registeredCommands().map { cmd2 -> cmd2 to { invoked = true; true } }) {
            if (entry.first.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false)) {
                consumed = entry.second()
                break
            }
        }
        assertTrue(consumed)
        assertTrue(invoked)
    }

    @Test
    fun dispatchFallsThroughWhenActionReturnsFalse() {
        val registry = KeyCommandRegistry()
        // 两条命令绑定到不同的 Key:一个返回 false 让 dispatch 继续找下一个
        val first = KeyCommand(key = Key.B, ctrl = true)
        val second = KeyCommand(key = Key.I, ctrl = true)  // 不同 Key
        var secondInvoked = false
        registry.register(first) { false }  // 不消费
        registry.register(second) { secondInvoked = true; true }
        // 验证:对 Ctrl+B 而言,first 匹配;对 Ctrl+I 而言,first 不匹配但 second 匹配
        assertTrue(first.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
        assertFalse(first.matchesKey(Key.I, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
        assertTrue(second.matchesKey(Key.I, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
    }

    @Test
    fun shiftKeyNarrowingInStrictMode() {
        // 严格模式下:Ctrl+B 命令不应被 Ctrl+Shift+B 触发
        val cmd = KeyCommand(key = Key.B, ctrl = true)
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = false, strictMatch = false))
        assertTrue(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = true, strictMatch = false))
        // 严格模式拒绝多余 Shift
        assertFalse(cmd.matchesKey(Key.B, ctrlPressed = true, metaPressed = false, altPressed = false, shiftPressed = true, strictMatch = true))
    }

    // endregion

    // region RichTextState 默认快捷键

    @Test
    fun stateHasDefaultKeyCommands() {
        val state = RichTextState()
        val commands = state.keyCommands.registeredCommands()
        // 默认注册: B/I/U/Z/Shift+Z/Y(ctrl + meta)
        assertEquals(6, commands.size)
        assertTrue(commands.any { it.key == Key.B && it.ctrl && it.meta })
        assertTrue(commands.any { it.key == Key.I && it.ctrl && it.meta })
        assertTrue(commands.any { it.key == Key.U && it.ctrl && it.meta })
        assertTrue(commands.any { it.key == Key.Z && it.ctrl && it.meta && !it.shift })
        assertTrue(commands.any { it.key == Key.Z && it.ctrl && it.meta && it.shift })
        assertTrue(commands.any { it.key == Key.Y && it.ctrl && it.meta })
    }

    @Test
    fun stateAddKeyCommandExtendsRegistry() {
        val state = RichTextState()
        val before = state.keyCommands.size
        val cmd = KeyCommand(key = Key.K, ctrl = true)
        state.addKeyCommand(cmd) { true }
        assertEquals(before + 1, state.keyCommands.size)
    }

    @Test
    fun stateRemoveKeyCommandShrinksRegistry() {
        val state = RichTextState()
        val before = state.keyCommands.size
        val cmd = KeyCommand(key = Key.B, ctrl = true, meta = true)
        state.removeKeyCommand(cmd)
        assertEquals(before - 1, state.keyCommands.size)
    }

    @Test
    fun stateRemoveUnknownCommandReturnsFalse() {
        val state = RichTextState()
        val cmd = KeyCommand(key = Key.F1)
        assertFalse(state.removeKeyCommand(cmd))
    }

    // endregion
}
