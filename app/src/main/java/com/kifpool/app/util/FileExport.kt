package com.kifpool.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.kifpool.app.logic.Jalali
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * نوشتن فایل در پوشهٔ exports و باز کردن صفحهٔ اشتراک — یک‌جا برای همهٔ
 * خروجی‌ها (CSV و JSON). پیش‌تر همین منطق در Csv و BackupJson تکرار شده بود.
 *
 * نکتهٔ امنیتی: FileProvider فقط پوشهٔ exports را می‌بیند (file_paths.xml)
 * و URIها با FLAG_GRANT_READ_URI_PERMISSION و برای یک مقصد می‌روند.
 */
object FileExport {

    private const val PROVIDER_SUFFIX = ".fileprovider"

    /** نام فایل امن با تاریخ شمسی: kifpool_<y>_<m>.<ext> — بدون کاراکتر خطرناک. */
    fun exportFileName(prefix: String, ext: String, today: LocalDate = LocalDate.now()): String {
        val j = Jalali.toJalali(today)
        return String.format(
            Locale.US, "%s_%d_%d.%s", prefix, j.year, j.month, ext
        )
    }

    /** نوشتن متن در پوشهٔ exports (خارجی در صورت وجود، وگرنه حافظهٔ داخلی). */
    fun writeToExports(context: Context, fileName: String, content: String): File {
        val dir = context.getExternalFilesDir(EXPORT_DIR) ?: context.filesDir
        val file = File(dir, fileName)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /** باز کردن اشتراک‌گذاری فایل با اپ دیگر (در خطا Toast مسیر ذخیره). */
    fun share(context: Context, file: File, mime: String, subject: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context, context.packageName + PROVIDER_SUFFIX, file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context, "فایل ذخیره شد: " + file.name, android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    const val EXPORT_DIR = "exports"
}
