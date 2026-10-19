package com.kifpool.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست هَش‌کنندهٔ PBKDF2 — امنیت PIN و سوال امنیتی به این کلاس وابسته است. */
class Pbkdf2HasherTest {

    private val hasher = Pbkdf2Hasher

    @Test
    fun `hash verify round trip works`() {
        val stored = hasher.hash("1234")
        assertTrue(hasher.verify("1234", stored))
        assertFalse(hasher.verify("1235", stored))
        assertFalse(hasher.verify("", stored))
    }

    @Test
    fun `stored format contains algorithm and iterations`() {
        val stored = hasher.hash("secret")
        assertTrue("format: $stored", stored.startsWith("pbkdf2-sha256$${Pbkdf2Hasher.ITERATIONS}$"))
        assertEquals(4, stored.split('$').size)
    }

    @Test
    fun `every hash uses a unique salt`() {
        val a = hasher.hash("same-secret")
        val b = hasher.hash("same-secret")
        assertNotEquals("salt باید یکتا باشد", a, b)
        assertTrue(hasher.verify("same-secret", a))
        assertTrue(hasher.verify("same-secret", b))
    }

    @Test
    fun `legacy sha256 hash is accepted for migration`() {
        // هش SHA-256 خام «۱۳۷۹» — دقیقاً مثل نسخه‌های ≤ ۱٫۶٫۰ که در ViewModel ساخته می‌شد
        val storedLegacy = java.security.MessageDigest.getInstance("SHA-256")
            .digest("1379".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals(64, storedLegacy.length)
        assertTrue("PIN قدیمی باید پذیرفته شود", hasher.verify("1379", storedLegacy))
        assertFalse(hasher.verify("0000", storedLegacy))
        // و باید ارتقا لازم باشد تا پس از ورود موفق به PBKDF2 مهاجرت کند
        assertTrue(hasher.needsUpgrade(storedLegacy))
    }

    @Test
    fun `modern hash does not need upgrade`() {
        assertFalse(hasher.needsUpgrade(hasher.hash("9999")))
    }

    @Test
    fun `malformed stored values verify to false without crashing`() {
        assertFalse(hasher.verify("1234", ""))
        assertFalse(hasher.verify("1234", "garbage"))
        assertFalse(hasher.verify("1234", "pbkdf2-sha256" + '$' + "abc" + '$' + "xx" + '$' + "yy"))
        assertFalse(hasher.verify("1234", "pbkdf2-sha256" + '$' + "120000" + '$' + "only-salt"))
        assertFalse(hasher.verify("1234", "pbkdf2-sha256" + '$' + "99999999999999999999" + '$' + "aa" + '$' + "bb"))
    }

    @Test
    fun `unicode secrets are supported`() {
        val secret = "رمز-فارسی۱۲۳"
        val stored = hasher.hash(secret)
        assertTrue(hasher.verify(secret, stored))
        assertFalse(hasher.verify("رمز-فارسی۱۲۴", stored))
    }

    @Test
    fun `hash is slower than trivial compare (sanity for brute force cost)`() {
        // تست sanity: PBKDF2 با ۱۲۰ هزار تکرار باید حداقل چند میلی‌ثانیه هزینه داشته باشد
        val start = System.nanoTime()
        hasher.hash("0000")
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("هش خیلی سریع بود ($elapsedMs ms) — پارامترها ضعیف‌اند", elapsedMs >= 5)
    }
}
