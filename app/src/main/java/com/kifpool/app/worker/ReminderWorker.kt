package com.kifpool.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kifpool.app.data.AppDatabase
import com.kifpool.app.logic.Jalali
import com.kifpool.app.logic.ReminderScheduler
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * چک روزانه یادآورها (حدود ۸:۳۰ صبح) + جبران اعلان‌های از‌دست‌رفته هنگام باز شدن برنامه.
 *
 * برای هر یادآور فعال:
 *  - اگر روز شمسیِ امروز == روز یادآور (با درنظرگرفتن کوتاه‌بودن ماه) باشد
 *  - و امروز هنوز اعلان داده نشده باشد
 * یک اعلان نمایش داده می‌شود. خطای تک‌تک یادآورها ایزوله است تا هرگز کل چک نتواند بشکند.
 */
class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            checkAndNotify(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {

        private const val DAILY_WORK = "kifpool_reminder_daily"
        private const val NOW_WORK = "kifpool_reminder_now"

        /** روز مؤثر یادآور در این ماه (روز ۳۱ در ماه ۳۰روزه => ۳۰) */
        fun effectiveDay(dayOfMonth: Int, today: LocalDate): Int {
            val j = Jalali.toJalali(today)
            val len = Jalali.monthLength(j.year, j.month)
            return dayOfMonth.coerceIn(1, len)
        }

        fun todayKey(today: LocalDate = LocalDate.now()): String {
            val j = Jalali.toJalali(today)
            return j.year.toString() + "-" + j.month + "-" + j.day
        }

        /** پیمایش یادآورهای فعال امروز و ارسال اعلان‌های رسیده. */
        suspend fun checkAndNotify(context: Context) {
            val dao = AppDatabase.get(context).reminderDao()
            val today = LocalDate.now()
            val j = Jalali.toJalali(today)
            val key = todayKey(today)
            for (reminder in dao.allEnabled()) {
                try {
                    val due = effectiveDay(reminder.dayOfMonth, today) == j.day
                    if (due && reminder.lastNotifiedKey != key) {
                        ReminderScheduler.notifyReminder(context, reminder)
                        dao.update(reminder.copy(lastNotifiedKey = key))
                    }
                } catch (e: Exception) {
                    // ایزوله‌سازی خطای هر یادآور
                }
            }
        }

        /** زمان‌بندی چک روزانه ۸:۳۰ (idempotent — با KEEP دوباره ساخته نمی‌شود). */
        fun scheduleDaily(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(8, 30)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, next))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                DAILY_WORK, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        /** چک فوری (مثلاً بعد از باز شدن برنامه یا ثبت یادآور جدید). */
        fun checkNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW_WORK, ExistingWorkPolicy.REPLACE, request
            )
        }
    }
}
