package ru.finni.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        PeriodEntity::class,
        TransactionEntity::class,
        TaskProgressEntity::class,
    ],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
