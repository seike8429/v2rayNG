package com.spidervpn.app.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the duration formatter used by the SpiderVPN home screen timer.
 */
class SpiderHomeScreenTest {

    @Test
    fun `zero elapsed formats as 00-00-00`() {
        assertEquals("00:00:00", formatDuration(0))
    }

    @Test
    fun `negative elapsed clamps to zero`() {
        assertEquals("00:00:00", formatDuration(-5_000))
    }

    @Test
    fun `seconds and minutes format correctly`() {
        assertEquals("00:01:05", formatDuration(65_000))
        assertEquals("00:59:59", formatDuration(3_599_000))
    }

    @Test
    fun `hours roll over correctly`() {
        assertEquals("01:00:00", formatDuration(3_600_000))
        assertEquals("11:15:04", formatDuration(40_504_000))
    }

    @Test
    fun `sub-second values truncate`() {
        assertEquals("00:00:01", formatDuration(1_999))
    }
}
