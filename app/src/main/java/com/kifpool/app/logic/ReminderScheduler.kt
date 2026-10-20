package com.kifpool.app.logic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kifpool.app.R
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.MainActivity
import com.kifpool.app.util.Fmt

/**
 * ساخت کانال اعلان و نمایش یادآورها.
 *
 * نکته مهم (رفع کرش):
 * - PendingIntent همیشه FLAG_IMMUTABLE دارد (الزامی اندروید ۱۲+).
 * - فلگ SINGLE_TOP تا با launchMode=singleTask اکتیویتی دوباره ساخته نشود
 *   و از طریق onNewIntent صفحه یادآورها باز شود.
 * - روی اندروید ۱۳+ قبل از notify دسترسی POST_NOTIFICATIONS چک می‌شود
 *   تا در نبود اجازه، هیچ کرشی رخ ندهد.
 */
object ReminderScheduler {

    const val CHANNEL_ID = "kifpool_reminders"
    const val EXTRA_OPEN_REMINDERS = "extra_open_reminders"
    const val EXTRA_OPEN_RECURRING = "extra_open_recurring"
    private const val CHANNEL_NAME = "یادآورهای پرداخت"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری پرداخت‌های ماهانه مثل اجاره، قبض و قسط"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** آیا الان می‌شود اعلان نشان داد؟ (اندروید ۱۳+: فقط با اجازه کاربر) */
    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** نمایش یادآور کلاسیک — روی متد عمومی [notifyPayment] سوار شده (DRY). */
    fun notifyReminder(context: Context, reminder: ReminderEntity) {
        val body = buildString {
            append("یادآوری: ")
            append(reminder.title)
            if (reminder.amount > 0) {
                append(" — مبلغ ")
                append(Fmt.money(reminder.amount))
                append(" تومان")
            }
        }
        notifyPayment(
            context = context,
            notificationId = reminder.id.toInt(),
            title = "🔔 " + reminder.title,
            body = body,
            openRecurring = false,
            deepLinkRequestId = reminder.id.toInt()
        )
    }

    /** یادآور سررسید تراکنش تکراری — با دبیلینک به صفحهٔ تکرارشوها. */
    fun notifyRecurring(context: Context, rec: com.kifpool.app.data.RecurringTransactionEntity) {
        val body = buildString {
            append("سررسید رسیده: ")
            append(rec.title)
            append(" — ")
            append(Fmt.money(rec.amount))
            append(" تومان")
            if (!rec.autoAdd) append(" (فقط یادآوری)")
        }
        notifyPayment(
            context = context,
            notificationId = (rec.id + 100_000).toInt(), // فاصله از آیدی یادآورها
            title = "🔁 " + rec.title,
            body = body,
            openRecurring = true,
            deepLinkRequestId = rec.id.toInt()
        )
    }

    /**
     * هستهٔ مشترک ساخت اعلان (DRY):
     * همهٔ اعلان‌ها از این مسیر می‌گذرند تا فلگ‌های امن و چک دسترسی یک‌جا باشند.
     */
    private fun notifyPayment(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        openRecurring: Boolean,
        deepLinkRequestId: Int
    ) {
        try {
            ensureChannel(context)
            if (!canNotify(context)) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (openRecurring) {
                    putExtra(EXTRA_OPEN_RECURRING, true)
                } else {
                    putExtra(EXTRA_OPEN_REMINDERS, true)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                deepLinkRequestId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            // هیچ‌وقت به‌خاطر اعلان، برنامه نباید کرش کند
        }
    }
}
