package com.kifpool.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست SecurityManager با مخزن درون‌حافظه‌ای — بدون اندروید، سریع و قطعی. */
class SecurityManagerTest {

    private class InMemoryStore : CredentialStore {
        val map = HashMap<String, Any?>()
        override fun getString(key: String): String? = map[key] as String?
        override fun putString(key: String, value: String) { map[key] = value }
        override fun getBoolean(key: String, default: Boolean): Boolean = map[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }

    private fun newManager(): Pair<SecurityManager, InMemoryStore> {
        val store = InMemoryStore()
        return SecurityManager(store) to store
    }

    // ---- PIN -----------------------------------------------------------------

    @Test
    fun `setPin enables lock and verify works`() {
        val (mgr, store) = newManager()
        assertFalse(mgr.lockEnabled)
        mgr.setPin("1234")
        assertTrue(mgr.lockEnabled)
        assertTrue(mgr.checkPin("1234"))
        assertFalse(mgr.checkPin("4321"))
        assertTrue("هش باید PBKDF2 باشد", (store.map[SecurityManager.KEY_PIN] as String).startsWith("pbkdf2-sha256$"))
    }

    @Test
    fun `invalid pins are rejected by setPin`() {
        val (mgr, store) = newManager()
        mgr.setPin("123")   // کوتاه
        assertFalse(mgr.lockEnabled)
        assertNull(store.map[SecurityManager.KEY_PIN])
        mgr.setPin("1234567") // بلند
        assertFalse(mgr.lockEnabled)
        mgr.setPin("12a4")  // غیر عددی
        assertFalse(mgr.lockEnabled)
    }

    @Test
    fun `checkPin with empty store is false`() {
        val (mgr, _) = newManager()
        assertFalse(mgr.checkPin("1234"))
    }

    @Test
    fun `checkPin sanitizes persian digits`() {
        val (mgr, _) = newManager()
        mgr.setPin("1379")
        assertTrue(mgr.checkPin("۱۳۷۹"))
    }

    @Test
    fun `legacy sha256 pin migrates transparently after first success`() {
        val store = InMemoryStore()
        // شبیه‌سازی کاربر قدیمی: هش SHA-256 خام «1379»
        val md = java.security.MessageDigest.getInstance("SHA-256")
            .digest("1379".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        store.putString(SecurityManager.KEY_PIN, md)
        store.putBoolean(SecurityManager.KEY_LOCK, true)

        val mgr = SecurityManager(store)
        assertTrue(mgr.checkPin("1379")) // ورود با رمز قدیمی
        val upgraded = store.map[SecurityManager.KEY_PIN] as String
        assertTrue("بعد از ورود موفق باید PBKDF2 شود: $upgraded", upgraded.startsWith("pbkdf2-sha256$"))
        // و رمز همان رمز قبلی است
        assertTrue(mgr.checkPin("1379"))
        assertFalse(mgr.checkPin("0000"))
    }

    // ---- سوال امنیتی -----------------------------------------------------------

    @Test
    fun `security question normalizes answer`() {
        val (mgr, _) = newManager()
        assertTrue(mgr.setSecurityQuestion("نام بهترین دوستت؟", "Ali  Reza "))
        assertEquals("نام بهترین دوستت؟", mgr.securityQuestion)
        assertTrue("حروف بزرگ/کوچک نباید مهم باشد", mgr.checkSecurityAnswer("ali reza"))
        assertTrue("فاصله‌های اضافی نباید مهم باشد", mgr.checkSecurityAnswer("  ALI    REZA "))
        assertFalse(mgr.checkSecurityAnswer("hassan"))
    }

    @Test
    fun `empty question or answer is rejected`() {
        val (mgr, store) = newManager()
        assertFalse(mgr.setSecurityQuestion("", "answer"))
        assertFalse(mgr.setSecurityQuestion("   ", "answer"))
        assertFalse(mgr.setSecurityQuestion("question", ""))
        assertFalse(mgr.setSecurityQuestion("question", "  "))
        assertNull(store.map[SecurityManager.KEY_SEC_Q])
    }

    @Test
    fun `security answer is stored hashed`() {
        val (mgr, store) = newManager()
        mgr.setSecurityQuestion("q", "secret answer")
        val stored = store.map[SecurityManager.KEY_SEC_A] as String
        assertTrue(stored.startsWith("pbkdf2-sha256$"))
        assertFalse("جواب نباید خام ذخیره شود", stored.contains("secret"))
    }

    @Test
    fun `checkSecurityAnswer without setup is false`() {
        val (mgr, _) = newManager()
        assertFalse(mgr.checkSecurityAnswer("anything"))
    }

    // ---- چرخهٔ عمر ------------------------------------------------------------

    @Test
    fun `disable removes only security keys and keeps other settings`() {
        val (mgr, store) = newManager()
        mgr.setPin("1234")
        mgr.setSecurityQuestion("q", "a")
        mgr.biometricEnabled = true

        store.putString("user_theme", "keep-me") // تنظیم خارج از دامنهٔ امنیت

        assertTrue(mgr.lockEnabled)
        mgr.disable()

        assertFalse(mgr.lockEnabled)
        assertFalse(mgr.biometricEnabled)
        assertEquals("", mgr.securityQuestion)
        assertFalse(mgr.checkPin("1234"))
        assertEquals("تنظیمات غیرامنیتی نباید پاک شوند", "keep-me", store.getString("user_theme"))
    }

    @Test
    fun `biometric flag persists in store`() {
        val (mgr, store) = newManager()
        assertFalse(mgr.biometricEnabled)
        mgr.biometricEnabled = true
        assertEquals(true, store.map[SecurityManager.KEY_BIO])
        assertTrue(mgr.biometricEnabled)
    }
}
