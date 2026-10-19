package com.kifpool.app.security

/**
 * سیاست رمز PIN — تنها مرجع حقیقت (Single Source of Truth) برای قواعد PIN.
 *
 * قبلاً قواعد در دو جا تکرار شده بود (LockScreen و PinSetupDialog)؛ اکنون
 * هر دو از همین کلاس استفاده می‌کنند — اصل DRY. اگر فردا رمز ۸ رقمی شود،
 * فقط این فایل تغییر می‌کند.
 */
object PinPolicy {

    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6

    /** فقط ارقام، با سقف [MAX_LENGTH]. ورودی‌های فارسی/عربی هم پذیرفته می‌شوند. */
    fun sanitize(raw: String): String {
        val digits = buildString {
            for (c in raw) {
                when (c) {
                    in '0'..'9' -> append(c)
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                }
            }
        }
        return if (digits.length > MAX_LENGTH) digits.substring(0, MAX_LENGTH) else digits
    }

    /** رمز معتبر = بین ۴ تا ۶ رقم. */
    fun isValid(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }
}
