package com.kifpool.app.security

/**
 * مخزن کلید/مقدار برای اعتبارنامه‌ها — انتزاعی تا منطق امنیتی
 * بدون اندروید و کاملاً JVM-testable باشد (اصل Dependency Inversion).
 *
 * پیاده‌سازی‌ها:
 *  - [SharedPreferencesStore] برای اجرای واقعی روی دستگاه
 *  - InMemoryStore (در تست‌ها) برای اجرای سریع JVM
 */
interface CredentialStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

/** پیاده‌سازی واقعی روی SharedPreferences خصوصی برنامه (MODE_PRIVATE پیش‌فرض). */
class SharedPreferencesStore(private val prefs: android.content.SharedPreferences) : CredentialStore {

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
