package com.kifpool.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** تناوب تراکنش تکراری. */
object RecurFreq {
    const val MONTHLY = 0   // هر ماه شمسی (روز مشخص با clamp به طول ماه)
    const val WEEKLY = 1    // هر هفته (روز هفته ایرانی: شنبه=۰ .. جمعه=۶)
    const val YEARLY = 2    // هر سال شمسی (ماه + روز شمسی)
}

/**
 * تراکنش تکراری (نسخهٔ ۱٫۸) — اجاره، قسط، اشتراک و هر پرداخت/درآمد دوره‌ای.
 *
 * دو حالت کارکرد:
 *  - autoAdd=true : در هر سررسید، تراکنش به‌صورت خودکار ثبت می‌شود (با جبران دوره‌های عقب‌افتاده)
 *  - autoAdd=false: فقط اعلان یادآوری در روز سررسید
 *
 * @param dayOfMonth  برای MONTHLY/YEARLY: روز ۱..۳۱ ماه شمسی (ماه کوتاه‌تر => آخرین روز ماه)
 * @param weekday     برای WEEKLY: شنبه=۰ .. جمعه=۶ (هم‌تراز با Jalali.weekdayIndex)
 * @param monthOfYear برای YEARLY: ماه شمسی ۱..۱۲
 * @param startDate   شروع اعتبار (epoch millis ابتدای روز شمسی)
 * @param lastRunDate آخرین سررسید پردازش‌شده (epoch millis؛ ۰ = هنوز پردازش نشده)
 */
@Entity(
    tableName = "recurring_transactions",
    indices = [Index("categoryId"), Index("enabled")]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,               // تومان
    val type: Int,                  // TxType.EXPENSE / TxType.INCOME
    val categoryId: Long,
    val frequency: Int,             // RecurFreq
    val dayOfMonth: Int = 1,        // 1..31
    val weekday: Int = 0,           // 0..6
    val monthOfYear: Int = 1,       // 1..12
    val autoAdd: Boolean = true,
    val note: String = "",
    val enabled: Boolean = true,
    val startDate: Long = 0,
    val lastRunDate: Long = 0
)

@Dao
interface RecurringDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY enabled DESC, id DESC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE enabled = 1")
    suspend fun allEnabled(): List<RecurringTransactionEntity>

    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM recurring_transactions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(rec: RecurringTransactionEntity): Long

    @Update
    suspend fun update(rec: RecurringTransactionEntity)

    @Delete
    suspend fun delete(rec: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll()
}
