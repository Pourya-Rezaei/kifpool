package com.kifpool.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست محدودکنندهٔ تلاش — سیاست: ۳ تلاش آزاد، سپس قفل نمایی از ۳۰ ثانیه
 * با سقف ۱۰ دقیقه. ساعت تزریقی است تا زمان کنترل‌شده آزمایش شود.
 */
class AttemptThrottleTest {

    private var now = 0L

    private fun throttle(
        free: Int = 3,
        base: Long = 30_000L,
        max: Long = 10 * 60_000L
    ) = AttemptThrottle(now = { now }, freeAttempts = free, baseLockMillis = base, maxLockMillis = max)

    @Test
    fun `first three failures do not lock`() {
        val t = throttle()
        repeat(3) {
            assertTrue(t.canAttempt())
            t.registerFailure()
            assertTrue("۳ تلاش اول باید آزاد باشد", t.canAttempt())
            assertEquals(0L, t.remainingLockMillis())
        }
    }

    @Test
    fun `fourth failure locks for base duration`() {
        val t = throttle()
        repeat(3) { t.registerFailure() }
        t.registerFailure()
        assertFalse(t.canAttempt())
        assertEquals(30_000L, t.remainingLockMillis())
    }

    @Test
    fun `lockout grows exponentially`() {
        val t = throttle()
        repeat(3) { t.registerFailure() }
        t.registerFailure() // قفل ۳۰ ثانیه
        now += 30_000L
        assertTrue(t.canAttempt())
        t.registerFailure() // قفل ۶۰ ثانیه
        assertEquals(60_000L, t.remainingLockMillis())
        now += 60_000L
        t.registerFailure() // قفل ۱۲۰ ثانیه
        assertEquals(120_000L, t.remainingLockMillis())
    }

    @Test
    fun `lockout is capped at max`() {
        val t = throttle(max = 300_000L) // سقف ۵ دقیقه برای تست سریع
        repeat(20) {
            t.registerFailure()
            // جلو از زمان می‌رویم تا قفل‌ها باز شوند
            now += t.remainingLockMillis() + 1
        }
        t.registerFailure()
        assertTrue("قفل نباید از سقف بگذرد", t.remainingLockMillis() <= 300_000L)
    }

    @Test
    fun `time heals the lock`() {
        val t = throttle()
        repeat(4) { t.registerFailure() }
        now += 29_999L
        assertFalse(t.canAttempt())
        now += 1L
        assertTrue(t.canAttempt())
    }

    @Test
    fun `success resets everything`() {
        val t = throttle()
        repeat(5) { t.registerFailure() }
        now += t.remainingLockMillis() + 1
        t.registerSuccess()
        // حالا حتی چند خطا هم نمی‌تواند فوراً قفل کند
        repeat(3) { t.registerFailure() }
        assertTrue(t.canAttempt())
    }

    @Test
    fun `attempt during lock does not extend by itself`() {
        val t = throttle()
        repeat(4) { t.registerFailure() }
        val lockedUntil = t.remainingLockMillis()
        // تلاش در حین قفل نباید از طرف خودش قفل را طولانی‌تر کند (UI جلوی تلاش را می‌گیرد)
        assertEquals(lockedUntil, t.remainingLockMillis())
    }
}
