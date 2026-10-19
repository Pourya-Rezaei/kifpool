package com.kifpool.app.security

/**
 * مدیریت یکپارچهٔ اعتبارنامه‌ها (PIN + سوال امنیتی + قفل) — تنها مرجع حقیقت.
 *
 * پیش از این، منطق هش/مقایسه/ذخیره داخل WalletViewModel پراکنده بود؛ اکنون:
 *  - هش امن PBKDF2 با salt (به‌جای SHA-256 بدون salt)
 *  - مقایسهٔ زمان‌ثابت (مقاوم به timing attack)
 *  - مهاجرت شفاف هش‌های قدیمی پس از اولین ورود موفق
 *  - نرمال‌سازی جواب سوال امنیتی (فاصله‌ها + حروف بزرگ/کوچک)
 *  - [disable] فقط کلیدهای خودش را پاک می‌کند (بدون آسیب به تنظیمات دیگر)
 *
 * کلاس کاملاً JVM-testable است: ساعت، هَش‌کننده و مخزن همگی تزریقی‌اند.
 */
class SecurityManager(
    private val store: CredentialStore,
    private val hasher: PasswordHasher = Pbkdf2Hasher
) {

    // ---- وضعیت قفل -----------------------------------------------------------

    var lockEnabled: Boolean
        get() = store.getBoolean(KEY_LOCK, false)
        private set(value) = store.putBoolean(KEY_LOCK, value)

    var biometricEnabled: Boolean
        get() = store.getBoolean(KEY_BIO, false)
        set(value) = store.putBoolean(KEY_BIO, value)

    /** متن سوال امنیتی (نمایش آن برای کاربر مشکلی ندارد؛ فقط جواب راز است). */
    var securityQuestion: String
        get() = store.getString(KEY_SEC_Q) ?: ""
        private set(value) = store.putString(KEY_SEC_Q, value)

    private val hasPin: Boolean get() = store.getString(KEY_PIN) != null

    // ---- PIN -----------------------------------------------------------------

    /** تعیین PIN جدید و فعال‌شدن قفل. ورودی نامعتبر نادیده گرفته می‌شود. */
    fun setPin(pin: String) {
        if (!PinPolicy.isValid(pin)) return
        store.putString(KEY_PIN, hasher.hash(pin))
        lockEnabled = true
    }

    /**
     * بررسی PIN. در صورت موفقیت روی هش قدیمی (SHA-256 نسخه‌های قبل)،
     * به‌صورت شفاف با PBKDF2 ارتقا می‌یابد.
     */
    fun checkPin(pin: String): Boolean {
        val stored = store.getString(KEY_PIN) ?: return false
        val candidate = PinPolicy.sanitize(pin)
        if (!PinPolicy.isValid(candidate)) return false
        val ok = hasher.verify(candidate, stored)
        if (ok && hasher.needsUpgrade(stored)) {
            store.putString(KEY_PIN, hasher.hash(candidate))
        }
        return ok
    }

    // ---- سوال امنیتی -----------------------------------------------------------

    /** تنظیم سوال/جواب امنیتی؛ در موفقیت true. جواب نرمال‌سازی و هش می‌شود. */
    fun setSecurityQuestion(question: String, answer: String): Boolean {
        val q = normalizeQuestion(question)
        val a = normalizeAnswer(answer)
        if (q.isEmpty() || a.isEmpty()) return false
        securityQuestion = q
        store.putString(KEY_SEC_A, hasher.hash(a))
        return true
    }

    /** بررسی جواب سوال امنیتی (با همان نرمال‌سازی هنگام ثبت). */
    fun checkSecurityAnswer(answer: String): Boolean {
        val stored = store.getString(KEY_SEC_A) ?: return false
        val candidate = normalizeAnswer(answer)
        if (candidate.isEmpty()) return false
        val ok = hasher.verify(candidate, stored)
        if (ok && hasher.needsUpgrade(stored)) {
            store.putString(KEY_SEC_A, hasher.hash(candidate))
        }
        return ok
    }

    // ---- چرخهٔ عمر قفل ---------------------------------------------------------

    /**
     * برداشتن کامل قفل (فراموشی رمز با جواب درست، یا خاموش‌کردن از تنظیمات).
     * فقط کلیدهای امنیتی پاک می‌شوند — تم ظاهری، بودجه و هدف دست‌نخورده می‌مانند.
     */
    fun disable() {
        lockEnabled = false
        store.putBoolean(KEY_LOCK, false)
        store.putBoolean(KEY_BIO, false)
        store.remove(KEY_PIN)
        store.remove(KEY_SEC_Q)
        store.remove(KEY_SEC_A)
    }

    // ---- نرمال‌سازی ------------------------------------------------------------

    private fun normalizeQuestion(raw: String): String =
        raw.trim().replace(WHITESPACE, " ").take(MAX_QUESTION_LENGTH)

    /**
     * جواب‌ها: حذف فاصله‌های اضافی + یکسان‌سازی حروف بزرگ/کوچک
     * تا «Ali  Reza» و «ali reza» هر دو درست باشند.
     */
    private fun normalizeAnswer(raw: String): String =
        raw.trim().replace(WHITESPACE, " ").lowercase().take(MAX_ANSWER_LENGTH)

    companion object {
        const val MAX_QUESTION_LENGTH = 60
        const val MAX_ANSWER_LENGTH = 40

        const val KEY_LOCK = "lock_enabled"
        const val KEY_PIN = "lock_pin_hash"
        const val KEY_BIO = "biometric_enabled"
        const val KEY_SEC_Q = "security_question"
        const val KEY_SEC_A = "security_answer_hash"

        private val WHITESPACE = Regex("\\s+")
    }
}
