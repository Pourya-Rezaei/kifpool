package com.kifpool.app.logic

import android.content.Context
import com.kifpool.app.data.AppDatabase
import com.kifpool.app.data.RecurFreq
import com.kifpool.app.data.RecurringTransactionEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.util.Fmt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * موتور تراکنش‌های تکراری — تمام محاسبات تاریخ خالص و قابل تست هستند (JVM).
 *
 * تقویم مرجع، شمسی است: «اجاره ۵ هر ماه» یعنی ۵ هر ماه شمسی، حتی اگر ماه ۲۹ یا ۳۰ روزه
 * باشد (در آن صورت آخرین روز ماه). پردازش خودکار با «جبران عقب‌افتادگی» انجام می‌شود:
 * اگر برنامه چند روز باز نشده باشد، همهٔ سررسیدهای گذشته تا امروز (با سقف [MAX_CATCH_UP])
 * یک‌جا ثبت می‌شوند تا آمار ماه‌ها دقیق بماند.
 */
object RecurringEngine {

    /** سقف جبران دوره‌های عقب‌افتاده برای هر تکرارشو (ضد حلقهٔ بی‌پایان) */
    const val MAX_CATCH_UP = 60

    /** روز مؤثر [day] در ماه شمسی (y,m) — روز بزرگ‌تر از طول ماه => آخرین روز ماه */
    fun effectiveJalaliDay(jy: Int, jm: Int, day: Int): Int =
        day.coerceIn(1, Jalali.monthLength(jy, jm))

    /**
     * نخستین سررسید اکیداً بعد از [after].
     * اگر امروز سررسید باشد و [after] هم امروز باشد، نتیجه سررسیدِ بعدی است (سخت‌گیرانه).
     */
    fun nextOccurrence(
        frequency: Int,
        dayOfMonth: Int,
        weekday: Int,
        monthOfYear: Int,
        after: LocalDate
    ): LocalDate? = when (frequency) {
        RecurFreq.WEEKLY -> {
            val delta = ((weekday - Jalali.weekdayIndex(after) + 7) % 7).toLong()
            after.plusDays(if (delta == 0L) 7L else delta)
        }

        RecurFreq.MONTHLY -> {
            val j = Jalali.toJalali(after)
            var y = j.year
            var m = j.month
            var candidate = Jalali.toGregorian(y, m, effectiveJalaliDay(y, m, dayOfMonth))
            if (!candidate.isAfter(after)) {
                val nk = Jalali.MonthKey(y, m).next()
                y = nk.year
                m = nk.month
                candidate = Jalali.toGregorian(y, m, effectiveJalaliDay(y, m, dayOfMonth))
            }
            candidate
        }

        RecurFreq.YEARLY -> {
            val jy = Jalali.toJalali(after).year
            var y = jy
            var candidate =
                Jalali.toGregorian(y, monthOfYear.coerceIn(1, 12), effectiveJalaliDay(y, monthOfYear.coerceIn(1, 12), dayOfMonth))
            if (!candidate.isAfter(after)) {
                y += 1
                candidate =
                    Jalali.toGregorian(y, monthOfYear.coerceIn(1, 12), effectiveJalaliDay(y, monthOfYear.coerceIn(1, 12), dayOfMonth))
            }
            candidate
        }

        else -> null
    }

    /**
     * همهٔ سررسیدها در بازهٔ (afterExclusive, today] — مرتب صعودی.
     * برای «آخرین بار پردازش‌شده = lastRunDate» و امروز استفاده می‌شود.
     */
    fun dueOccurrences(
        frequency: Int,
        dayOfMonth: Int,
        weekday: Int,
        monthOfYear: Int,
        afterExclusive: LocalDate,
        today: LocalDate,
        max: Int = MAX_CATCH_UP
    ): List<LocalDate> {
        val out = ArrayList<LocalDate>()
        var cursor = afterExclusive
        while (out.size < max) {
            val next = nextOccurrence(frequency, dayOfMonth, weekday, monthOfYear, cursor) ?: break
            if (next.isAfter(today)) break
            out.add(next)
            cursor = next
        }
        return out
    }

    // ---- پوشش مدل داده (کمک‌های UI و پردازش) --------------------------------

    /** نقطهٔ شروع پیمایش: آخرین روزِ پردازش‌شده، یا یک روز قبل از شروع اعتبار */
    private fun scanFrom(rec: RecurringTransactionEntity): LocalDate {
        val start = if (rec.startDate > 0) Jalali.dateOf(rec.startDate) else LocalDate.now()
        val lastRun = if (rec.lastRunDate > 0) Jalali.dateOf(rec.lastRunDate) else null
        return if (lastRun != null && lastRun.isAfter(start)) lastRun else start.minusDays(1)
    }

    /** نخستین سررسیدِ در انتظار پردازش (حتی امروز) — null یعنی چیزی در انتظار نیست */
    fun pendingDate(rec: RecurringTransactionEntity, today: LocalDate = LocalDate.now()): LocalDate? =
        dueOccurrences(
            rec.frequency, rec.dayOfMonth, rec.weekday, rec.monthOfYear,
            scanFrom(rec), today, max = 1
        ).firstOrNull()

    /** نخستین سررسیدِ آینده (بعد از امروز) برای نمایش در UI — null یعنی تعریف‌نشده */
    fun upcomingDate(rec: RecurringTransactionEntity, today: LocalDate = LocalDate.now()): LocalDate? =
        nextOccurrence(rec.frequency, rec.dayOfMonth, rec.weekday, rec.monthOfYear, today)

    /** برچسب تناوب برای کارت‌ها: «ماهانه، ۵ هر ماه» / «هفتگی، شنبه‌ها» / «سالانه، ۱ فروردین» */
    fun frequencyLabel(rec: RecurringTransactionEntity): String = when (rec.frequency) {
        RecurFreq.MONTHLY -> "ماهانه، " + Fmt.fa(rec.dayOfMonth) + " هر ماه"
        RecurFreq.WEEKLY -> "هفتگی، " + WEEKDAY_NAMES[rec.weekday.coerceIn(0, 6)] + "ها"
        RecurFreq.YEARLY -> "سالانه، " + Fmt.fa(rec.dayOfMonth) + " " +
            Jalali.MONTH_NAMES[rec.monthOfYear.coerceIn(1, 12) - 1]
        else -> ""
    }

    val WEEKDAY_NAMES = listOf("شنبه", "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

    // ---- پردازش خودکار (از ReminderWorker فراخوانی می‌شود) -------------------

    /** جلوگیری از پردازش هم‌زمان موازی (دو ورکر/نخ) => ثبت دوبرابر تراکنش */
    private val processMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * پردازش همهٔ تکرارشوهای فعال:
     *  - autoAdd: ثبت تراکنش برای هر سررسید عقب‌افتاده/امروز + پیشبرد lastRunDate
     *  - فقط یادآور: اعلان تنها وقتی آخرین سررسیدِ عقب‌افتاده، امروز است
     *
     * خطای هر رکورد ایزوله است تا یک آیتم خراب، بقیه را متوقف نکند.
     */
    suspend fun processDue(context: Context, today: LocalDate = LocalDate.now()) =
        processMutex.withLock {
            val db = AppDatabase.get(context)
            val dao = db.recurringDao()
            val txDao = db.transactionDao()
            val scheduler = com.kifpool.app.logic.ReminderScheduler

            for (rec in dao.allEnabled()) {
                try {
                    val due = dueOccurrences(
                        rec.frequency, rec.dayOfMonth, rec.weekday, rec.monthOfYear,
                        scanFrom(rec), today
                    )
                    if (due.isEmpty()) continue

                    if (rec.autoAdd) {
                        for (day in due) {
                            txDao.insert(
                                TransactionEntity(
                                    amount = rec.amount,
                                    categoryId = rec.categoryId,
                                    date = Jalali.startOfDayMillis(day),
                                    note = rec.note
                                )
                            )
                        }
                    } else if (due.last() == today) {
                        scheduler.notifyRecurring(context, rec)
                    }
                    dao.update(rec.copy(lastRunDate = Jalali.startOfDayMillis(due.last())))
                } catch (e: Exception) {
                    // ایزوله‌سازی خطای هر تکرارشو
                }
            }
        }
}
