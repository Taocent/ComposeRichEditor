package com.taocent.simple.compose.component.richtext.core.internal.toolbar

import androidx.compose.ui.unit.IntOffset
import com.taocent.simple.compose.component.richtext.core.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class FloatingToolbarHostStateTest {

    private fun newState(): RichTextState = RichTextState()

    @Test
    fun reportActiveStoresInfo() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        val info = FloatingToolbarActiveInfo(
            state = s1,
            position = IntOffset(10, 20),
            widthPx = 100,
            heightPx = 40,
            anchor = IntOffset(1, 2),
            isDragging = false,
        )
        host.reportActive(info)
        assertSame(info, host.active)
    }

    @Test
    fun reportActiveReplacesPreviousInfo() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        val s2 = newState()
        host.reportActive(
            FloatingToolbarActiveInfo(s1, IntOffset(0, 0), 0, 0, IntOffset.Zero, false)
        )
        host.reportActive(
            FloatingToolbarActiveInfo(s2, IntOffset(5, 5), 10, 10, IntOffset(1, 1), false)
        )
        assertNotNull(host.active)
        assertSame(s2, host.active!!.state)
    }

    @Test
    fun clearActiveRemovesMatchingState() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        val s2 = newState()
        host.reportActive(
            FloatingToolbarActiveInfo(s1, IntOffset(0, 0), 0, 0, IntOffset.Zero, false)
        )
        // 不同 state 调用 clearActive 不应影响 host
        host.clearActive(s2)
        assertNotNull(host.active)
        assertSame(s1, host.active!!.state)
        // 相同 state 调用 clearActive 应清空 host
        host.clearActive(s1)
        assertNull(host.active)
    }

    @Test
    fun updateActivePositionUpdatesOnlyMatchingState() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        val s2 = newState()
        host.reportActive(
            FloatingToolbarActiveInfo(s1, IntOffset(0, 0), 0, 0, IntOffset(3, 4), false)
        )
        // 不同 state 的位置更新被忽略
        host.updateActivePosition(s2, IntOffset(99, 99), 99, 99, IntOffset(9, 9))
        assertEquals(IntOffset(0, 0), host.active!!.position)
        // 同 state 的位置更新生效
        host.updateActivePosition(s1, IntOffset(20, 30), 200, 300, IntOffset(5, 6))
        val updated = host.active!!
        assertEquals(IntOffset(20, 30), updated.position)
        assertEquals(200, updated.widthPx)
        assertEquals(300, updated.heightPx)
        assertEquals(IntOffset(5, 6), updated.anchor)
        assertSame(s1, updated.state)
    }

    @Test
    fun updateActivePositionIsNoOpWhenActiveIsNull() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        // active 为 null 时不抛异常
        host.updateActivePosition(s1, IntOffset(0, 0), 0, 0, IntOffset.Zero)
        assertNull(host.active)
    }

    @Test
    fun dismissClearsActive() {
        val host = FloatingToolbarHostState()
        val s1 = newState()
        host.reportActive(
            FloatingToolbarActiveInfo(s1, IntOffset(0, 0), 0, 0, IntOffset.Zero, false)
        )
        host.dismiss()
        assertNull(host.active)
    }
}
