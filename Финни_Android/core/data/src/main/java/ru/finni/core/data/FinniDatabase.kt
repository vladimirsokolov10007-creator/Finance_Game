package ru.finni.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 1 → 2: добавлена колонка profiles.ageGroup (возрастная группа игрока). */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE profiles ADD COLUMN ageGroup INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        PeriodEntity::class,
        TransactionEntity::class,
        TaskProgressEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FinniDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun periodDao(): PeriodDao
    abstract fun transactionDao(): TransactionDao
    abstract fun taskProgressDao(): TaskProgressDao

    companion object {
        @Volatile private var instance: FinniDatabase? = null

        fun get(context: Context): FinniDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinniDatabase::class.java,
                    "finni.db",
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
            }
    }
}
