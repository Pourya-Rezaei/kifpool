package com.kifpool.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * تست تقویم شمسی — قلب الگوریتمی برنامه.
 * تاریخ‌های مرجع با منابع مستقل (timeanddate / التقویم رسمی) تطبیق داده شده‌اند.
 */
class JalaliTest {

    private val tehran: ZoneId = ZoneId.of("Asia/Tehran")

    @Test
    fun `known dates convert correctly`() {
        // نوروز ۱۴۰۳ = ۲۰۲۴-۰۳-۲۰
        assertEquals(Jalali.Date(1403, 1, 1), Jalali.toJalali(LocalDate.of(2024, 3, 20)))
        // نوروز ۱۴۰۴ = ۲۰۲۵-۰۳-۲۱ (سال قبلش کبیسه بود)
        assertEquals(Jalali.Date(1404, 1, 1), Jalali.toJalali(LocalDate.of(2025, 3, 21)))
        // نوروز ۱۴۰۲ = ۲۰۲۳-۰۳-۲۱
        assertEquals(Jalali.Date(1402, 1, 1), Jalali.toJalali(LocalDate.of(2023, 3, 21)))
        // آخر سال ۱۴۰۲ = ۲۰۲۴-۰۳-۱۹
        assertEquals(Jalali.Date(1402, 12, 29), Jalali.toJalali(LocalDate.of(2024, 3, 19)))
        // ۲۲ بهمن ۱۴۰۲ = ۱۱ فوریه ۲۰۲۴ (سالگرد انقلاب)
        assertEquals(Jalali.Date(1402, 11, 22), Jalali.toJalali(LocalDate.of(2024, 2, 11)))
        // ۱ مرداد ۱۴۰۳ = ۲۲ ژوئیه ۲۰۲۴
        assertEquals(Jalali.Date(1403, 5, 1), Jalali.toJalali(LocalDate.of(2024, 7, 22)))
    }

    @Test
    fun `gregorian round trip is stable for 3000 days`() {
        var date = LocalDate.of(2020, 1, 1)
        for (i in 0 until 3000) {
            val j = Jalali.toJalali(date)
            val back = Jalali.toGregorian(j.year, j.month, j.day)
            assertEquals("round trip failed at $date", date, back)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `leap years have 30 day esfand`() {
        // ۱۴۰۳ کبیسه است (کبیسه‌های چرخهٔ ۳۳ساله: ۱۳۷۰، ۱۳۷۵، ۱۳۷۹، ۱۳۸۳، ۱۳۸۷، ۱۳۹۱، ۱۳۹۵، ۱۳۹۹، ۱۴۰۳)
        assertTrue(Jalali.isLeap(1403))
        assertEquals(30, Jalali.monthLength(1403, 12))
        // ۱۴۰۲ و ۱۴۰۴ کبیسه نیستند
        assertEquals(29, Jalali.monthLength(1402, 12))
        assertEquals(29, Jalali.monthLength(1404, 12))
    }

    @Test
    fun `month lengths follow the 31-30-29 pattern`() {
        for (m in 1..6) assertEquals(31, Jalali.monthLength(1403, m))
        for (m in 7..11) assertEquals(30, Jalali.monthLength(1403, m))
    }

    @Test
    fun `invalid jalali year is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Jalali.toGregorian(4000, 1, 1) // خارج از محدودهٔ جدول breaks (تا ۳۱۷۷)
        }
    }

    @Test
    fun `weekday index follows saturday-based week`() {
        // ۲۰۲۴-۰۳-۱۶ شنبه بود
        assertEquals(0, Jalali.weekdayIndex(LocalDate.of(2024, 3, 16)))
        assertEquals(6, Jalali.weekdayIndex(LocalDate.of(2024, 3, 22))) // جمعه
        // هر شنبه، شروع هفتهٔ خودش است
        val saturday = LocalDate.of(2025, 10, 18)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(saturday, Jalali.weekStart(saturday))
        // چهارشنبهٔ همان هفته به شنبهٔ ابتدای هفته برمی‌گردد
        assertEquals(saturday, Jalali.weekStart(saturday.plusDays(4)))
    }

    @Test
    fun `month key navigation crosses year boundaries`() {
        val dec = Jalali.MonthKey(1403, 12)
        assertEquals(Jalali.MonthKey(1404, 1), dec.next())
        assertEquals(Jalali.MonthKey(1403, 11), dec.previous())
        val jan = Jalali.MonthKey(1404, 1)
        assertEquals(Jalali.MonthKey(1403, 12), jan.previous())
        assertEquals(Jalali.MonthKey(1404, 2), jan.next())
    }

    @Test
    fun `month boundaries align with day one`() {
        val key = Jalali.MonthKey(1403, 3) // خرداد
        val start = Jalali.toJalali(
            Jalali.startOfDayMillis(Jalali.toGregorian(key.year, key.month, 1), tehran)
                .let { java.time.Instant.ofEpochMilli(it).atZone(tehran).toLocalDate() }
        )
        assertEquals(1, start.day)
        assertEquals(3, start.month)
    }
}
