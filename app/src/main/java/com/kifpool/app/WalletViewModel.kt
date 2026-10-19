package com.kifpool.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kifpool.app.data.AppData
import com.kifpool.app.data.AppDatabase
import com.kifpool.app.data.CategoryEntity
import com.kifpool.app.data.DebtEntity
import com.kifpool.app.data.DebtType
import com.kifpool.app.data.ReminderEntity
import com.kifpool.app.data.TransactionEntity
import com.kifpool.app.logic.Jalali
import com.kifpool.app.util.BackupJson
import com.kifpool.app.util.Presets
import com.kifpool.app.util.SampleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

class WalletViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val txDao = db.transactionDao()
    private val catDao = db.categoryDao()
    private val remDao = db.reminderDao()
    private val debtDao = db.debtDao()
    private val prefs = app.getSharedPreferences("kifpool_prefs", Context.MODE_PRIVATE)

    // ---- settings ---------------------------------------------------------
    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK, true))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(prefs.getLong(KEY_BUDGET, 12_000_000L))
    val monthlyBudget: StateFlow<Long> = _monthlyBudget.asStateFlow()

    private val _weeklyGoal = MutableStateFlow(prefs.getLong(KEY_WEEKLY, 2_500_000L))
    val weeklyGoal: StateFlow<Long> = _weeklyGoal.asStateFlow()

    // ---- selected Jalali month --------------------------------------------
    private val _month = MutableStateFlow(Jalali.currentMonthKey())
    val selectedMonth: StateFlow<Jalali.MonthKey> = _month.asStateFlow()

    // ---- reminders (یادآور پرداخت) --------------------------------------------
    val reminders: StateFlow<List<ReminderEntity>> = remDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveReminder(id: Long?, title: String, amount: Long, dayOfMonth: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val day = dayOfMonth.coerceIn(1, 31)
            if (id == null) {
                remDao.insert(ReminderEntity(title = title.trim(), amount = amount, dayOfMonth = day))
            } else {
                val existing = remDao.observeAll().first().find { it.id == id } ?: return@launch
                remDao.update(existing.copy(title = title.trim(), amount = amount, dayOfMonth = day))
            }
        }

    fun setReminderEnabled(reminder: ReminderEntity, enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            remDao.update(reminder.copy(enabled = enabled))
        }

    fun deleteReminder(reminder: ReminderEntity) =
        viewModelScope.launch(Dispatchers.IO) { remDao.delete(reminder) }

    // ---- دفتر طلب و بدهی (نسخهٔ ۱٫۶) ----------------------------------------
    val debts: StateFlow<List<DebtEntity>> = debtDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveDebt(
        id: Long?,
        person: String,
        amount: Long,
        type: Int,
        note: String,
        dueDate: Long
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (person.isBlank() || amount <= 0) return@launch
        if (id == null) {
            debtDao.insert(
                DebtEntity(
                    person = person.trim(),
                    amount = amount,
                    type = type,
                    note = note.trim(),
                    createdDate = System.currentTimeMillis(),
                    dueDate = dueDate,
                    settled = false
                )
            )
        } else {
            val existing = debtDao.observeAll().first().find { it.id == id } ?: return@launch
            debtDao.update(
                existing.copy(
                    person = person.trim(),
                    amount = amount,
                    type = type,
                    note = note.trim(),
                    dueDate = dueDate
                )
            )
        }
    }

    fun setDebtSettled(debt: DebtEntity, settled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) { debtDao.update(debt.copy(settled = settled)) }

    fun deleteDebt(debt: DebtEntity) =
        viewModelScope.launch(Dispatchers.IO) { debtDao.delete(debt) }

    /** ساخت یادآور ماهانه از روی یک بدهی (سررسید روز ماه یا امروز). */
    fun createReminderForDebt(debt: DebtEntity, dayOfMonth: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val title = if (debt.type == DebtType.CREDIT) "طلب از " + debt.person
                        else "بدهی به " + debt.person
            remDao.insert(
                ReminderEntity(
                    title = title,
                    amount = debt.amount,
                    dayOfMonth = dayOfMonth.coerceIn(1, 31)
                )
            )
        }

    // ---- lock (قفل PIN + اثر انگشت) -------------------------------------------
    private val _lockEnabled = MutableStateFlow(prefs.getBoolean(KEY_LOCK, false))
    val lockEnabled: StateFlow<Boolean> = _lockEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIO, false))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, sha256(pin)).apply()
        _lockEnabled.value = true
        prefs.edit().putBoolean(KEY_LOCK, true).apply()
    }

    fun checkPin(pin: String): Boolean =
        prefs.getString(KEY_PIN, null) == sha256(pin)

    fun removeLock() {
        _lockEnabled.value = false
        _biometricEnabled.value = false
        prefs.edit().clear()
            .putBoolean(KEY_DARK, _darkMode.value)
            .putLong(KEY_BUDGET, _monthlyBudget.value)
            .putLong(KEY_WEEKLY, _weeklyGoal.value)
            .apply()
    }

    fun setBiometricEnabled(v: Boolean) {
        _biometricEnabled.value = v
        prefs.edit().putBoolean(KEY_BIO, v).apply()
    }

    // ---- سوال امنیتی (بازیابی PIN — نسخهٔ ۱٫۶) ---------------------------------
    private val _securityQuestion = MutableStateFlow(prefs.getString(KEY_SEC_Q, "") ?: "")
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    fun setSecurityQuestion(question: String, answer: String) {
        val q = question.trim()
        val a = answer.trim()
        if (q.isEmpty() || a.isEmpty()) return
        prefs.edit().putString(KEY_SEC_Q, q).putString(KEY_SEC_A, sha256(a)).apply()
        _securityQuestion.value = q
    }

    fun checkSecurityAnswer(answer: String): Boolean {
        val stored = prefs.getString(KEY_SEC_A, null) ?: return false
        return stored == sha256(answer.trim())
    }

    /** برداشتن قفل پس از پاسخ درست سوال امنیتی — کاربر باید رمز جدید بگذارد. */
    fun recoverLock() {
        removeLock()
    }

    private fun sha256(text: String): String = try {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        digest.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        text
    }

    // ---- JSON backup (پشتیبان‌گیری) ---------------------------------------------
    suspend fun fullBackup(): BackupJson.Backup = withContext(Dispatchers.IO) {
        val txs = txDao.observeAll().first()
        val cats = catDao.observeAll().first()
        val rems = remDao.observeAll().first()
        val debts = debtDao.observeAll().first()
        BackupJson.Backup(cats, txs, rems, debts)
    }

    suspend fun writeBackupFile(context: Context, backup: BackupJson.Backup): File =
        withContext(Dispatchers.IO) {
            BackupJson.write(context, BackupJson.build(backup.transactions, backup.categories, backup.reminders, backup.debts))
        }

    /** بازیابی کامل داده‌ها از فایل پشتیبان. در موفقیت true برمی‌گرداند. */
    suspend fun restoreBackup(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val text = BackupJson.read(context, uri) ?: return@withContext false
        val backup = BackupJson.parse(text) ?: return@withContext false
        txDao.deleteAll()
        catDao.deleteAll()
        remDao.deleteAll()
        debtDao.deleteAll()
        val idMap = mutableMapOf<Long, Long>()
        for (cat in backup.categories) {
            val newId = catDao.insert(cat.copy(id = 0))
            idMap[cat.id] = newId
        }
        for (tx in backup.transactions) {
            txDao.insert(tx.copy(id = 0, categoryId = idMap[tx.categoryId] ?: 0L))
        }
        for (rem in backup.reminders) {
            if (rem.title.isNotBlank()) remDao.insert(rem.copy(id = 0))
        }
        for (debt in backup.debts) {
            if (debt.person.isNotBlank()) debtDao.insert(debt.copy(id = 0))
        }
        true
    }

    // ---- database streams ---------------------------------------------------
    private val allTx = txDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = catDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val data: StateFlow<AppData?> = combine(
        allTx, categories, _month, _monthlyBudget, _weeklyGoal
    ) { txs, cats, mk, budget, goal ->
        AppData(txs, cats, mk, budget, goal, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (catDao.count() == 0) seed()
        }
    }

    private suspend fun seed() {
        val ids = Presets.DEFAULT_CATEGORIES
            .map { it.name to catDao.insert(it) }
            .toMap()
        val samples = SampleData.build(LocalDate.now()) { name -> ids[name] ?: 0L }
        samples.forEach { txDao.insert(it) }
    }

    fun reloadSample() = viewModelScope.launch(Dispatchers.IO) {
        txDao.deleteAll()
        catDao.deleteAll()
        seed()
    }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) {
        txDao.deleteAll()
        catDao.deleteAll()
    }

    // ---- month navigation ---------------------------------------------------
    fun selectMonth(key: Jalali.MonthKey) { _month.value = key }
    fun prevMonth() { _month.value = _month.value.previous() }
    fun nextMonth() { _month.value = _month.value.next() }

    // ---- transactions -------------------------------------------------------
    fun saveTransaction(id: Long?, amount: Long, categoryId: Long, dateMillis: Long, note: String) =
        viewModelScope.launch(Dispatchers.IO) {
            if (id == null) {
                txDao.insert(
                    TransactionEntity(amount = amount, categoryId = categoryId, date = dateMillis, note = note)
                )
            } else {
                val existing = txDao.byId(id) ?: return@launch
                txDao.update(
                    existing.copy(amount = amount, categoryId = categoryId, date = dateMillis, note = note)
                )
            }
        }

    fun deleteTransaction(tx: TransactionEntity) =
        viewModelScope.launch(Dispatchers.IO) { txDao.delete(tx) }

    suspend fun getTransaction(id: Long): TransactionEntity? =
        withContext(Dispatchers.IO) { txDao.byId(id) }

    // ---- categories -----------------------------------------------------------
    fun addCategory(name: String, type: Int, icon: String, color: String) =
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isBlank()) return@launch
            catDao.insert(
                CategoryEntity(name = name.trim(), type = type, icon = icon, color = color)
            )
        }

    /** تعیین بودجهٔ ماهانه برای یک دسته (۰ = بدون بودجه). */
    fun setCategoryBudget(category: CategoryEntity, budget: Long) =
        viewModelScope.launch(Dispatchers.IO) {
            catDao.update(category.copy(budgetAmount = budget.coerceAtLeast(0)))
        }

    /** Returns false when the category still has transactions and cannot be removed. */
    suspend fun deleteCategory(cat: CategoryEntity): Boolean = withContext(Dispatchers.IO) {
        if (txDao.countForCategory(cat.id) > 0) {
            false
        } else {
            catDao.delete(cat)
            true
        }
    }

    // ---- settings persistence ---------------------------------------------
    fun setDarkMode(v: Boolean) {
        _darkMode.value = v
        prefs.edit().putBoolean(KEY_DARK, v).apply()
    }

    fun setBudget(v: Long) {
        _monthlyBudget.value = v
        prefs.edit().putLong(KEY_BUDGET, v).apply()
    }

    fun setWeeklyGoal(v: Long) {
        _weeklyGoal.value = v
        prefs.edit().putLong(KEY_WEEKLY, v).apply()
    }

    // ---- CSV snapshot --------------------------------------------------------
    suspend fun snapshot(): Pair<List<TransactionEntity>, List<CategoryEntity>> =
        withContext(Dispatchers.IO) {
            txDao.observeAll().first() to catDao.observeAll().first()
        }

    companion object {
        private const val KEY_DARK = "dark_mode"
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_WEEKLY = "weekly_goal"
        private const val KEY_LOCK = "lock_enabled"
        private const val KEY_PIN = "lock_pin_hash"
        private const val KEY_BIO = "biometric_enabled"
        private const val KEY_SEC_Q = "security_question"
        private const val KEY_SEC_A = "security_answer_hash"
    }
}
