package com.kifpool.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست‌های محاسبات پیشرفت اهداف پس‌انداز (نسخهٔ ۱٫۸).
 */
class GoalProgressTest {

    @Test
    fun `fraction is zero when target is zero`() {
        val p = GoalProgress(saved = 500_000, target = 0)
        assertEquals(0f, p.fraction)
        assertFalse(p.achieved)
    }

    @Test
    fun `fraction clamps at one when exceeded`() {
        val p = GoalProgress(saved = 12_000_000, target = 10_000_000)
        assertEquals(1f, p.fraction)
        assertEquals(100, p.percent)
        assertTrue(p.achieved)
    }

    @Test
    fun `fraction reflects partial progress`() {
        val p = GoalProgress(saved = 2_500_000, target = 10_000_000)
        assertEquals(0.25f, p.fraction, 0.0001f)
        assertEquals(25, p.percent)
        assertFalse(p.achieved)
    }

    @Test
    fun `remaining never goes negative`() {
        val p = GoalProgress(saved = 11_000_000, target = 10_000_000)
        assertEquals(0L, p.remaining)
    }

    @Test
    fun `remaining shows distance to target`() {
        val p = GoalProgress(saved = 3_000_000, target = 10_000_000)
        assertEquals(7_000_000L, p.remaining)
    }

    @Test
    fun `zero saved is valid starting point`() {
        val p = GoalProgress(saved = 0, target = 10_000_000)
        assertEquals(0f, p.fraction)
        assertEquals(0, p.percent)
        assertEquals(10_000_000L, p.remaining)
        assertFalse(p.achieved)
    }
}
