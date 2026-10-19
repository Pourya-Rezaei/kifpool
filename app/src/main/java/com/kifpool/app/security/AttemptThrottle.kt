package com.kifpool.app.security

/**
 * محدودکنندهٔ تلاش‌های ناموفق (ضد حملهٔ Brute Force) — منطق خالص و تست‌پذیر.
 *
 * سیاست:
 *  - [freeAttempts] تلاش اول آزاد است (تجربهٔ کاربری بدون مزاحمت)
 *  - پس از آن، هر خطا قفل‌کردن نمایی دارد: ۳۰ ثانیه، ۱ دقیقه، ۲ دقیقه، …
 *  - سقف قفل [maxLockMillis] (پیش‌فرض ۱۰ دقیقه) تا کاربر واقعی گیر نکند
 *  - ورود موفق، شمارنده را صفر می‌کند
 *
 * ساعت به‌صورت تابع تزریق می‌شود تا در تست‌ها بتوان زمان را کنترل کرد.
 */
class AttemptThrottle(
    private val now: () -> Long = System::currentTimeMillis,
    private val freeAttempts: Int = DEFAULT_FREE_ATTEMPTS,
    private val baseLockMillis: Long = DEFAULT_BASE_LOCK_MILLIS,
    private val maxLockMillis: Long = DEFAULT_MAX_LOCK_MILLIS
) {

    private var failures = 0
    private var lockedUntil = 0L

    /** آیا اکنون اجازهٔ یک تلاش جدید هست؟ */
    fun canAttempt(): Boolean = now() >= lockedUntil

    /** میلی‌ثانیهٔ باقی‌مانده تا آزاد شدن (۰ = آزاد). */
    fun remainingLockMillis(): Long = (lockedUntil - now()).coerceAtLeast(0L)

    /** یک تلاش ناموفق را ثبت می‌کند؛ در صورت لزوم، قفل نمایی برمی‌گرداند. */
    fun registerFailure() {
        failures++
        if (failures > freeAttempts) {
            val exponent = failures - freeAttempts - 1L
            var lock = baseLockMillis
            repeat(exponent.toInt().coerceAtMost(64)) {
                lock = (lock * 2).coerceAtMost(maxLockMillis)
            }
            lockedUntil = now() + lock
        }
    }

    /** تلاش موفق — ریست کامل. */
    fun registerSuccess() {
        failures = 0
        lockedUntil = 0L
    }

    companion object {
        const val DEFAULT_FREE_ATTEMPTS = 3
        const val DEFAULT_BASE_LOCK_MILLIS = 30_000L      // ۳۰ ثانیه
        const val DEFAULT_MAX_LOCK_MILLIS = 10 * 60_000L  // ۱۰ دقیقه
    }
}
