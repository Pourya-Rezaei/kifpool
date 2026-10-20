package com.kifpool.app.logic

import com.kifpool.app.data.RecurFreq
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * تست‌های موتور تراکنش‌های تکراری — تقویم شمسی، clamp روز، جبران عقب‌افتادگی.
 *
 * تاریخ‌های مرجع (بررسی‌شده با JalaliTest):
 *  1403/01/01 = 2024-03-20   |  1403 کبیسه است (اسفند ۳۰ روزه)
 *  1404/07/01 = 2025-09-23   |  1404 غیرکبیسه (اسفند ۲۹ روزه)
 *  مهر/آبان/آذر ۳۰ روزه‌اند؛ دی..شهریور طبق الگوی ۳۱/۳۰
 */
class RecurringEngineTest {

    private val monthly = RecurFreq.MONTHLY
    private val weekly = RecurFreq.WEEKLY
    private val yearly = RecurFreq.YEARLY

    // ---- ماهانه -------------------------------------------------------------

    @Test
    fun `monthly day 31 clamps to end of 30-day months`() {
        // بعد از ۱ مهر ۱۴۰۴ → سررسید روز ۳۱ مهر = clamp به ۳۰ مهر
        val after = Jalali.toGregorian(1404, 7, 1)
        val next = RecurringEngine.nextOccurrence(monthly, 31, 0, 1, after)!!
        assertEquals(Jalali.toGregorian(1404, 7, 30), next)
    }

    @Test
    fun `monthly day 31 in 31-day month stays 31`() {
        val after = Jalali.toGregorian(1404, 7, 30) // ۳۰ مهر
        val next = RecurringEngine.nextOccurrence(monthly, 31, 0, 1, after)!!
        // بعد از ۳۰ مهر → آذر ۳۰ روزه → ۳۰ آذر؟ نه: مهر تمام شد؛ سررسید بعدی ۳۱ مهر نیست؛
        // clamp مهر = ۳۰ که بعد از after نیست → ماه بعد = آبان (۳۰ روزه) → ۳۰ آبان
        assertEquals(Jalali.toGregorian(1404, 8, 30), next)
    }

    @Test
    fun `monthly rolls over year boundary`() {
        // بعد از ۵ اسفند ۱۴۰۴ → ۵ فروردین ۱۴۰۵
        val after = Jalali.toGregorian(1404, 12, 5)
        val next = RecurringEngine.nextOccurrence(monthly, 5, 0, 1, after)!!
        assertEquals(Jalali.toGregorian(1405, 1, 5), next)
    }

    @Test
    fun `monthly esfand 30 clamps to 29 in non-leap year`() {
        // after = ۳۰ بهمن ۱۴۰۴ (غیرکبیسه) → بهمن تمام شده؛ اسفند ۲۹روزه → ۲۹ اسفند
        val after = Jalali.toGregorian(1404, 11, 30)
        val next = RecurringEngine.nextOccurrence(monthly, 30, 0, 1, after)!!
        assertEquals(Jalali.toGregorian(1404, 12, 29), next)
    }

    @Test
    fun `monthly same-day anchor moves to next month`() {
        // اگر امروز دقیقاً روز ۱۰ باشد، سررسید بعدی باید ۱۰ ماه بعد باشد (سخت‌گیری)
        val after = Jalali.toGregorian(1404, 7, 10)
        val next = RecurringEngine.nextOccurrence(monthly, 10, 0, 1, after)!!
        assertEquals(Jalali.toGregorian(1404, 8, 10), next)
    }

    // ---- هفتگی --------------------------------------------------------------

    @Test
    fun `weekly finds coming Saturday`() {
        // 1404/07/05 شنبه است (weekdayIndex = ۰)
        val saturday = Jalali.toGregorian(1404, 7, 5)
        assertEquals(0, Jalali.weekdayIndex(saturday)) // شنبه = ۰
        val wednesday = saturday.plusDays(4) // چهارشنبهٔ همان هفته
        val next = RecurringEngine.nextOccurrence(weekly, 0, 0, 1, wednesday)!!
        assertEquals(saturday.plusDays(7), next) // شنبهٔ بعدی
    }

    @Test
    fun `weekly on same day moves one week forward`() {
        val saturday = Jalali.toGregorian(1404, 7, 5)
        val next = RecurringEngine.nextOccurrence(weekly, 0, 0, 1, saturday)!!
        assertEquals(saturday.plusDays(7), next)
    }

    // ---- سالانه -------------------------------------------------------------

    @Test
    fun `yearly esfand 30 handles leap vs non-leap`() {
        // بعد از نوروز ۱۴۰۳ (کبیسه) → ۳۰ اسفند ۱۴۰۳
        val after = Jalali.toGregorian(1403, 1, 1)
        val next = RecurringEngine.nextOccurrence(yearly, 30, 0, 12, after)!!
        assertEquals(Jalali.toGregorian(1403, 12, 30), next)

        // بعد از ۳۰ اسفند ۱۴۰۳ → سال ۱۴۰۴ غیرکبیسه → ۲۹ اسفند
        val next2 = RecurringEngine.nextOccurrence(yearly, 30, 0, 12, next)!!
        assertEquals(Jalali.toGregorian(1404, 12, 29), next2)
    }

    // ---- جبران عقب‌افتادگی ----------------------------------------------------

    @Test
    fun `due occurrences catches up multiple missed months`() {
        // lastRun = ۱ فروردین، امروز = ۱ خرداد → باید ۱ اردیبهشت و ۱ خرداد را بگیرد
        val lastRun = Jalali.toGregorian(1404, 1, 1)
        val today = Jalali.toGregorian(1404, 3, 1)
        val due = RecurringEngine.dueOccurrences(monthly, 1, 0, 1, lastRun, today)
        assertEquals(
            listOf(
                Jalali.toGregorian(1404, 2, 1),
                Jalali.toGregorian(1404, 3, 1)
            ),
            due
        )
    }

    @Test
    fun `due occurrences empty when nothing due`() {
        val lastRun = Jalali.toGregorian(1404, 7, 10)
        val today = Jalali.toGregorian(1404, 7, 20)
        val due = RecurringEngine.dueOccurrences(monthly, 1, 0, 1, lastRun, today)
        assertTrue(due.isEmpty())
    }

    @Test
    fun `due occurrences respects catch-up cap`() {
        // lastRun خیلی قدیمی → حداکثر MAX_CATCH_UP سررسید
        val lastRun = Jalali.toGregorian(1390, 1, 1)
        val today = Jalali.toGregorian(1404, 7, 1)
        val due = RecurringEngine.dueOccurrences(monthly, 1, 0, 1, lastRun, today)
        assertEquals(RecurringEngine.MAX_CATCH_UP, due.size)
    }

    @Test
    fun `due occurrences includes today`() {
        val today = Jalali.toGregorian(1404, 7, 10)
        val lastRun = Jalali.toGregorian(1404, 6, 10)
        val due = RecurringEngine.dueOccurrences(monthly, 10, 0, 1, lastRun, today)
        assertEquals(listOf(today), due)
    }

    // ---- کمکی‌های UI ----------------------------------------------------------

    @Test
    fun `upcoming date is strictly after today`() {
        val rec = com.kifpool.app.data.RecurringTransactionEntity(
            id = 1, title = "اجاره", amount = 5_000_000,
            type = com.kifpool.app.data.TxType.EXPENSE, categoryId = 1,
            frequency = monthly, dayOfMonth = 5,
            lastRunDate = 0, startDate = 0
        )
        val today = Jalali.toGregorian(1404, 7, 10)
        val next = RecurringEngine.upcomingDate(rec, today)!!
        assertEquals(Jalali.toGregorian(1404, 8, 5), next)
        assertTrue(next.isAfter(today))
    }

    @Test
    fun `unknown frequency returns null safely`() {
        val after = Jalali.toGregorian(1404, 7, 1)
        assertNull(RecurringEngine.nextOccurrence(frequency = 99, dayOfMonth = 1, weekday = 0, monthOfYear = 1, after = after))
    }

    @Test
    fun `frequency label formats monthly`() {
        val rec = com.kifpool.app.data.RecurringTransactionEntity(
            id = 1, title = "قسط", amount = 2_000_000,
            type = 0, categoryId = 1, frequency = monthly, dayOfMonth = 15
        )
        assertTrue(RecurringEngine.frequencyLabel(rec).contains("۱۵"))
    }
}
