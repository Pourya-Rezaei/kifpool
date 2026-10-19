package com.kifpool.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** تست فرمت‌دهنده‌ها: اعداد فارسی، جداکنندهٔ هزارگان، ورودی مبلغ زنده. */
class FmtTest {

    @Test
    fun `latin digits convert to persian`() {
        assertEquals("۰۱۲۳۴۵۶۷۸۹", Fmt.fa("0123456789"))
        assertEquals("۱۲۵", Fmt.fa(125))
        assertEquals("۱۲۵", Fmt.fa(125L))
    }

    @Test
    fun `money groups by three with persian separator`() {
        assertEquals("۱٬۲۵۰٬۰۰۰", Fmt.money(1_250_000L))
        assertEquals("۰", Fmt.money(0L))
        assertEquals("۹۹۹", Fmt.money(999L))
        assertEquals("۱٬۰۰۰", Fmt.money(1_000L))
    }

    @Test
    fun `money latin keeps ascii digits for csv`() {
        assertEquals("1,250,000", Fmt.moneyLatin(1_250_000L))
    }

    @Test
    fun `digitsOnly normalizes persian and arabic digits`() {
        assertEquals("123456", Fmt.digitsOnly("۱۲۳۴۵۶"))
        assertEquals("123456", Fmt.digitsOnly("١٢٣٤٥٦")) // ارقام عربی
        assertEquals("123456", Fmt.digitsOnly("123456"))
        assertEquals("1250000", Fmt.digitsOnly("۱٬۲۵۰٬۰۰۰")) // جداکننده حذف می‌شود
        assertEquals("", Fmt.digitsOnly("abc"))
    }

    @Test
    fun `amountInput live formats without leading zeros`() {
        assertEquals("۱٬۲۵۰٬۰۰۰", Fmt.amountInput("1250000"))
        assertEquals("", Fmt.amountInput(""))
        assertEquals("", Fmt.amountInput("000"))
        assertEquals("۱۰", Fmt.amountInput("0010"))
    }

    @Test
    fun `amountInput caps at 15 digits`() {
        val huge = Fmt.amountInput("12345678901234567890") // ۲۰ رقم ورودی
        assertEquals(123_456_789_012_345L, Fmt.parseAmount(huge)) // فقط ۱۵ رقم نگه داشته می‌شود
    }

    @Test
    fun `parseAmount round trips with money`() {
        val original = 98_765_432L
        val display = Fmt.money(original)
        assertEquals(original, Fmt.parseAmount(display))
    }

    @Test
    fun `parseAmount of garbage is zero`() {
        assertEquals(0L, Fmt.parseAmount(""))
        assertEquals(0L, Fmt.parseAmount("---"))
    }

    @Test
    fun `percent shows persian percent sign`() {
        assertEquals("۸۰٪", Fmt.percent(0.8f))
        assertEquals("۱۰۰٪", Fmt.percent(1f))
    }
}
