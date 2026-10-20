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

/**
 * هدف پس‌انداز (نسخهٔ ۱٫۸) — «سفر شمال»، «خرید گوشی» و ...
 *
 * مبلغ پس‌اندازشده مستقیماً ذخیره نمی‌شود؛ از جمع واریزی‌ها/برداشت‌های
 * [GoalContributionEntity] محاسبه می‌شود تا هیچ‌وقت از تراکنش‌های واقعی عقب نیفتد.
 *
 * @param targetDate epoch millis تاریخ هدف؛ ۰ = بدون تاریخ (برای نسخه‌های بعدی)
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Long,         // تومان
    val icon: String = "savings",   // کلید آیکون، see Presets.CATEGORY_ICONS
    val color: String = "#2DD4BF",  // hex like "#2DD4BF"
    val archived: Boolean = false,
    val createdDate: Long = 0
)

/** واریز (+) یا برداشت (−) از یک هدف پس‌انداز. */
@Entity(
    tableName = "goal_contributions",
    indices = [Index("goalId")]
)
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,               // + واریز / − برداشت (همیشه مخالف صفر)
    val date: Long,                 // epoch millis
    val note: String = ""
)

/** محاسبات پیشرفت هدف — خالص و قابل تست (الگوی WeekGoal). */
data class GoalProgress(val saved: Long, val target: Long) {
    val fraction: Float
        get() = if (target > 0) (saved.toFloat() / target).coerceIn(0f, 1f) else 0f
    val percent: Int
        get() = (fraction * 100).toInt()
    val remaining: Long
        get() = (target - saved).coerceAtLeast(0)
    val achieved: Boolean
        get() = target > 0 && saved >= target
}

@Dao
interface GoalDao {

    @Query("SELECT * FROM savings_goals WHERE archived = 0 ORDER BY id DESC")
    fun observeActive(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY id DESC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun count(): Int

    @Insert
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}

@Dao
interface GoalContributionDao {

    @Query("SELECT * FROM goal_contributions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<GoalContributionEntity>>

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY date DESC")
    suspend fun forGoal(goalId: Long): List<GoalContributionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM goal_contributions WHERE goalId = :goalId")
    suspend fun savedForGoal(goalId: Long): Long

    @Insert
    suspend fun insert(contribution: GoalContributionEntity): Long

    @Delete
    suspend fun delete(contribution: GoalContributionEntity)

    @Query("DELETE FROM goal_contributions WHERE goalId = :goalId")
    suspend fun deleteForGoal(goalId: Long)

    @Query("DELETE FROM goal_contributions")
    suspend fun deleteAll()
}
