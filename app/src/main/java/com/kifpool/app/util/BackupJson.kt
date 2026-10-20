package com.kifpool.app.util

import android.content.Context
import android.net.Uri
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.DebtEntity
import com.kifpool.app.data.DebtType
import com.kifpool.app.data.GoalContributionEntity
import com.kifpool.app.data.RecurringTransactionEntity
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.data.SavingsGoalEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.data.TxType
import com.kifpool.app.logic.Jalali
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.time.LocalDate

/**
 * پشتیبان‌گیری و بازیابی کامل داده‌ها در قالب JSON (کاملاً آفلاین، بدون وابستگی خارجی).
 *
 * امنیت بازیابی (نسخهٔ ۱٫۶٫۱):
 *  - سقف حجم ورودی ([MAX_INPUT_BYTES]) — جلوگیری از حملهٔ DoS حافظه با فایل غول‌پیکر
 *  - سقف تعداد رکورد هر آرایه ([MAX_CATEGORIES]/[MAX_TRANSACTIONS]/…) — رد کل فایل در تجاوز
 *  - پالایش هر رکورد: مبلغ ≥ ۰، طول رشته‌ها محدود، مقادیر خارج از دامنه حذف/اصلاح
 *  - پشتیبان‌های قدیمی (بدون debts و budgetAmount) پذیرفته می‌شوند (سازگاری رو به عقب)
 */
object BackupJson {

    data class Backup(
        val categories: List<CategoryEntity>,
        val transactions: List<TransactionEntity>,
        val reminders: List<ReminderEntity>,
        val debts: List<DebtEntity> = emptyList(),
        val recurring: List<RecurringTransactionEntity> = emptyList(),
        val goals: List<SavingsGoalEntity> = emptyList(),
        val contributions: List<GoalContributionEntity> = emptyList()
    )

    // ---- سقف‌های امنیتی -------------------------------------------------------
    const val MAX_INPUT_BYTES = 8 * 1024 * 1024      // ۸ مگابایت
    const val MAX_CATEGORIES = 500
    const val MAX_TRANSACTIONS = 100_000
    const val MAX_REMINDERS = 500
    const val MAX_DEBTS = 5_000
    const val MAX_RECURRING = 500
    const val MAX_GOALS = 200
    const val MAX_CONTRIBUTIONS = 20_000

    const val MAX_NAME = 40
    const val MAX_PERSON = 60
    const val MAX_NOTE = 300
    const val MAX_TITLE = 60

    // ---- export ---------------------------------------------------------------

    fun build(
        txs: List<TransactionEntity>,
        cats: List<CategoryEntity>,
        reminders: List<ReminderEntity>,
        debts: List<DebtEntity> = emptyList(),
        recurring: List<RecurringTransactionEntity> = emptyList(),
        goals: List<SavingsGoalEntity> = emptyList(),
        contributions: List<GoalContributionEntity> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("app", "kifpool")
        root.put("backupVersion", 3)
        val j = Jalali.toJalali(LocalDate.now())
        root.put("exportedAt", Fmt.fa(j.year.toString() + "/" + j.month + "/" + j.day))

        val catsArr = JSONArray()
        for (c in cats) {
            catsArr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("type", c.type)
                    .put("icon", c.icon)
                    .put("color", c.color)
                    .put("isDefault", c.isDefault)
                    .put("budgetAmount", c.budgetAmount)
            )
        }

        val txsArr = JSONArray()
        for (t in txs) {
            txsArr.put(
                JSONObject()
                    .put("amount", t.amount)
                    .put("categoryId", t.categoryId)
                    .put("date", t.date)
                    .put("note", t.note)
            )
        }

        val remArr = JSONArray()
        for (r in reminders) {
            remArr.put(
                JSONObject()
                    .put("title", r.title)
                    .put("amount", r.amount)
                    .put("dayOfMonth", r.dayOfMonth)
                    .put("timeMinutes", r.timeMinutes)
                    .put("enabled", r.enabled)
            )
        }

        val debtArr = JSONArray()
        for (d in debts) {
            debtArr.put(
                JSONObject()
                    .put("person", d.person)
                    .put("amount", d.amount)
                    .put("type", d.type)
                    .put("note", d.note)
                    .put("createdDate", d.createdDate)
                    .put("dueDate", d.dueDate)
                    .put("settled", d.settled)
            )
        }

        root.put("categories", catsArr)
        root.put("transactions", txsArr)
        root.put("reminders", remArr)
        root.put("debts", debtArr)

        // ---- نسخهٔ ۱٫۸: تکرارشوها + اهداف پس‌انداز ----------------------------
        val recArr = JSONArray()
        for (r in recurring) {
            recArr.put(
                JSONObject()
                    .put("title", r.title)
                    .put("amount", r.amount)
                    .put("type", r.type)
                    .put("categoryId", r.categoryId)
                    .put("frequency", r.frequency)
                    .put("dayOfMonth", r.dayOfMonth)
                    .put("weekday", r.weekday)
                    .put("monthOfYear", r.monthOfYear)
                    .put("autoAdd", r.autoAdd)
                    .put("note", r.note)
                    .put("enabled", r.enabled)
                    .put("startDate", r.startDate)
                    .put("lastRunDate", r.lastRunDate)
            )
        }
        val goalArr = JSONArray()
        for (g in goals) {
            goalArr.put(
                JSONObject()
                    .put("id", g.id)
                    .put("title", g.title)
                    .put("targetAmount", g.targetAmount)
                    .put("icon", g.icon)
                    .put("color", g.color)
                    .put("archived", g.archived)
                    .put("createdDate", g.createdDate)
            )
        }
        val contribArr = JSONArray()
        for (c in contributions) {
            contribArr.put(
                JSONObject()
                    .put("goalId", c.goalId)
                    .put("amount", c.amount)
                    .put("date", c.date)
                    .put("note", c.note)
            )
        }
        root.put("recurring", recArr)
        root.put("goals", goalArr)
        root.put("contributions", contribArr)
        return root.toString(2)
    }

    fun write(context: Context, content: String): File {
        val name = FileExport.exportFileName(prefix = "kifpool_backup", ext = "json")
        return FileExport.writeToExports(context, name, content)
    }

    fun share(context: Context, file: File) = FileExport.share(
        context, file,
        mime = "application/json",
        subject = "پشتیبان کامل کیف پول من",
        chooserTitle = "ارسال فایل پشتیبان"
    )

    // ---- import ---------------------------------------------------------------

    /** خواندن متن فایل پشتیبان از Uri — با سقف حجم؛ فایل بزرگ‌تر => null. */
    fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            readBounded(stream, MAX_INPUT_BYTES)
        }
    } catch (e: Exception) {
        null
    }

    /** خواندن با سقف بایت؛ اگر فایل بزرگ‌تر باشد null (تا حافظه پر نشود). */
    private fun readBounded(stream: InputStream, maxBytes: Int): String? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val n = stream.read(buffer)
            if (n == -1) break
            total += n
            if (total > maxBytes) return null
            out.write(buffer, 0, n)
        }
        return out.toString("UTF-8")
    }

    /** تجزیه و اعتبارسنجی فایل پشتیبان؛ در خطا null برمی‌گرداند. */
    fun parse(text: String): Backup? {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "kifpool") return null

            val catsArr = root.optJSONArray("categories") ?: JSONArray()
            if (catsArr.length() > MAX_CATEGORIES) return null
            val cats = mutableListOf<CategoryEntity>()
            for (i in 0 until catsArr.length()) {
                val o = catsArr.getJSONObject(i)
                val name = bounded(o.optString("name"), MAX_NAME)
                if (name.isEmpty()) continue // رکورد بی‌نام معتبر نیست
                cats.add(
                    CategoryEntity(
                        id = o.optLong("id", 0L),
                        name = name,
                        type = if (o.optInt("type", 0) == TxType.INCOME) TxType.INCOME else TxType.EXPENSE,
                        icon = o.optString("icon", "wallet"),
                        color = o.optString("color", "#F97316"),
                        isDefault = o.optBoolean("isDefault", false),
                        budgetAmount = o.optLong("budgetAmount", 0L).coerceAtLeast(0L)
                    )
                )
            }

            val txsArr = root.optJSONArray("transactions") ?: JSONArray()
            if (txsArr.length() > MAX_TRANSACTIONS) return null
            val txs = mutableListOf<TransactionEntity>()
            for (i in 0 until txsArr.length()) {
                val o = txsArr.getJSONObject(i)
                val amount = o.optLong("amount", 0L)
                if (amount <= 0L) continue // مبلغ نامعتبر
                txs.add(
                    TransactionEntity(
                        amount = amount,
                        categoryId = o.optLong("categoryId", 0L).coerceAtLeast(0L),
                        date = o.optLong("date", 0L).coerceAtLeast(0L),
                        note = bounded(o.optString("note", ""), MAX_NOTE)
                    )
                )
            }

            val remArr = root.optJSONArray("reminders") ?: JSONArray()
            if (remArr.length() > MAX_REMINDERS) return null
            val rems = mutableListOf<ReminderEntity>()
            for (i in 0 until remArr.length()) {
                val o = remArr.getJSONObject(i)
                val title = bounded(o.optString("title"), MAX_TITLE)
                if (title.isEmpty()) continue
                rems.add(
                    ReminderEntity(
                        title = title,
                        amount = o.optLong("amount", 0L).coerceAtLeast(0L),
                        dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                        timeMinutes = o.optInt("timeMinutes", 8 * 60 + 30).coerceIn(0, 24 * 60 - 1),
                        enabled = o.optBoolean("enabled", true)
                    )
                )
            }

            val debtArr = root.optJSONArray("debts") ?: JSONArray()
            if (debtArr.length() > MAX_DEBTS) return null
            val debts = mutableListOf<DebtEntity>()
            for (i in 0 until debtArr.length()) {
                val o = debtArr.getJSONObject(i)
                val person = bounded(o.optString("person"), MAX_PERSON)
                val amount = o.optLong("amount", 0L)
                if (person.isEmpty() || amount <= 0L) continue
                debts.add(
                    DebtEntity(
                        person = person,
                        amount = amount,
                        type = if (o.optInt("type", DebtType.DEBT) == DebtType.CREDIT) DebtType.CREDIT else DebtType.DEBT,
                        note = bounded(o.optString("note", ""), MAX_NOTE),
                        createdDate = o.optLong("createdDate", 0L).coerceAtLeast(0L),
                        dueDate = o.optLong("dueDate", 0L).coerceAtLeast(0L),
                        settled = o.optBoolean("settled", false)
                    )
                )
            }

            val (parsedGoals, parsedContribs) = parseGoals(root)
            Backup(cats, txs, rems, debts, parseRecurring(root), parsedGoals, parsedContribs)
        } catch (e: Exception) {
            null
        }
    }

    /** پارس تکرارشوها — فایل‌های قدیمی بدون این آرایه، لیست خالی می‌گیرند. */
    private fun parseRecurring(root: JSONObject): List<RecurringTransactionEntity> {
        val arr = root.optJSONArray("recurring") ?: return emptyList()
        if (arr.length() > MAX_RECURRING) return emptyList()
        val out = mutableListOf<RecurringTransactionEntity>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.getJSONObject(i)
                val title = bounded(o.optString("title"), MAX_TITLE)
                val amount = o.optLong("amount", 0L)
                if (title.isEmpty() || amount <= 0L) continue
                out.add(
                    RecurringTransactionEntity(
                        title = title,
                        amount = amount,
                        type = if (o.optInt("type", 0) == TxType.INCOME) TxType.INCOME else TxType.EXPENSE,
                        categoryId = o.optLong("categoryId", 0L).coerceAtLeast(0L),
                        frequency = o.optInt("frequency", 0).coerceIn(0, 2),
                        dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                        weekday = o.optInt("weekday", 0).coerceIn(0, 6),
                        monthOfYear = o.optInt("monthOfYear", 1).coerceIn(1, 12),
                        autoAdd = o.optBoolean("autoAdd", true),
                        note = bounded(o.optString("note", ""), MAX_NOTE),
                        enabled = o.optBoolean("enabled", true),
                        startDate = o.optLong("startDate", 0L).coerceAtLeast(0L),
                        lastRunDate = o.optLong("lastRunDate", 0L).coerceAtLeast(0L)
                    )
                )
            } catch (e: Exception) {
                // رکورد خراب رد می‌شود
            }
        }
        return out
    }

    /** پارس اهداف + واریزی‌ها — فایل‌های قدیمی، لیست خالی می‌گیرند. */
    private fun parseGoals(root: JSONObject): Pair<List<SavingsGoalEntity>, List<GoalContributionEntity>> {
        val goalArr = root.optJSONArray("goals") ?: JSONArray()
        if (goalArr.length() > MAX_GOALS) return emptyList<SavingsGoalEntity>() to emptyList()
        val goals = mutableListOf<SavingsGoalEntity>()
        for (i in 0 until goalArr.length()) {
            try {
                val o = goalArr.getJSONObject(i)
                val title = bounded(o.optString("title"), MAX_TITLE)
                val target = o.optLong("targetAmount", 0L)
                if (title.isEmpty() || target <= 0L) continue
                goals.add(
                    SavingsGoalEntity(
                        id = o.optLong("id", 0L),
                        title = title,
                        targetAmount = target,
                        icon = o.optString("icon", "savings"),
                        color = o.optString("color", "#2DD4BF"),
                        archived = o.optBoolean("archived", false),
                        createdDate = o.optLong("createdDate", 0L).coerceAtLeast(0L)
                    )
                )
            } catch (e: Exception) {
                // رکورد خراب رد می‌شود
            }
        }
        val contribArr = root.optJSONArray("contributions") ?: JSONArray()
        if (contribArr.length() > MAX_CONTRIBUTIONS) return goals to emptyList()
        val contribs = mutableListOf<GoalContributionEntity>()
        for (i in 0 until contribArr.length()) {
            try {
                val o = contribArr.getJSONObject(i)
                val amount = o.optLong("amount", 0L)
                if (amount == 0L) continue
                contribs.add(
                    GoalContributionEntity(
                        goalId = o.optLong("goalId", 0L),
                        amount = amount,
                        date = o.optLong("date", 0L).coerceAtLeast(0L),
                        note = bounded(o.optString("note", ""), MAX_NOTE)
                    )
                )
            } catch (e: Exception) {
                // رکورد خراب رد می‌شود
            }
        }
        return goals to contribs
    }

    /** کوتاه‌سازی رشته به طول مجاز + حذف نویسه‌های کنترلی. */
    private fun bounded(raw: String, max: Int): String =
        raw.filter { !it.isISOControl() || it == '\n' }.trim().let {
            if (it.length > max) it.substring(0, max) else it
        }
}
