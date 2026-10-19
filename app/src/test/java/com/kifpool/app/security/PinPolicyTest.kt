package com.kifpool.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست سیاست PIN — تک مرجع قواعد برای LockScreen و PinSetupDialog. */
class PinPolicyTest {

    @Test
    fun `length boundaries are enforced`() {
        assertFalse(PinPolicy.isValid("123"))        // ۳ رقم — کوتاه
        assertTrue(PinPolicy.isValid("1234"))        // ۴ رقم — حداقل
        assertTrue(PinPolicy.isValid("12345"))       // ۵ رقم
        assertTrue(PinPolicy.isValid("123456"))      // ۶ رقم — حداکثر
        assertFalse(PinPolicy.isValid("1234567"))    // ۷ رقم — بلند
    }

    @Test
    fun `non digits are invalid`() {
        assertFalse(PinPolicy.isValid("12a4"))
        assertFalse(PinPolicy.isValid("12 4"))
        assertFalse(PinPolicy.isValid("----"))
        assertFalse(PinPolicy.isValid(""))
    }

    @Test
    fun `sanitize keeps digits only and caps length`() {
        assertEquals("123456", PinPolicy.sanitize("123456"))
        assertEquals("1234", PinPolicy.sanitize("۱۲۳۴"))      // فارسی
        assertEquals("1234", PinPolicy.sanitize("١٢٣٤"))      // عربی
        assertEquals("123456", PinPolicy.sanitize("1234567890")) // سقف ۶ رقم
        assertEquals("", PinPolicy.sanitize("abc"))
        assertEquals("1379", PinPolicy.sanitize("a1b3c7d9e"))
    }

    @Test
    fun `sanitize output always valid when long enough`() {
        val cleaned = PinPolicy.sanitize("۱۳۷۹")
        assertTrue(PinPolicy.isValid(cleaned))
    }
}
