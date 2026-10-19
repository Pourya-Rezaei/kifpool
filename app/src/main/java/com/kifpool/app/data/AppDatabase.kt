package com.kifpool.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, ReminderEntity::class, DebtEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 -> v2: افزودن جدول یادآورها بدون دست‌زدن به داده‌های کاربر. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS reminders (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, " +
                        "dayOfMonth INTEGER NOT NULL, " +
                        "timeMinutes INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "lastNotifiedKey TEXT NOT NULL)"
                )
            }
        }

        /** v2 -> v3: ستون بودجهٔ دسته‌بندی + جدول دفتر طلب و بدهی (نسخهٔ ۱٫۶). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE categories ADD COLUMN budgetAmount INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS debts (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "person TEXT NOT NULL, " +
                        "amount INTEGER NOT NULL, " +
                        "type INTEGER NOT NULL, " +
                        "note TEXT NOT NULL, " +
                        "createdDate INTEGER NOT NULL, " +
                        "dueDate INTEGER NOT NULL, " +
                        "settled INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kifpool.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
