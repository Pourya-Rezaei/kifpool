package com.kifpool.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * یادآور پرداخت ماهانه (اجاره، قبض، قسط و ...).
 *
 * @param dayOfMonth   روز شمسی از ماه (۱..۳۱). اگر ماه کوتاه‌تر باشد، آخرین روز ماه.
 * @param timeMinutes  دقیقه از نیمه‌شب برای اعلان (فعلاً چک روزانه ۸:۳۰ انجام می‌شود).
 * @param lastNotifiedKey  کلید تاریخ شمسی آخرین اعلان مثل "1404-7-5" برای جلوگیری از تکرار.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long = 0,           // 0 = بدون مبلغ
    val dayOfMonth: Int,            // 1..31
    val timeMinutes: Int = 8 * 60 + 30,
    val enabled: Boolean = true,
    val lastNotifiedKey: String = ""
)

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY dayOfMonth ASC, id ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun allEnabled(): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun count(): Int

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
