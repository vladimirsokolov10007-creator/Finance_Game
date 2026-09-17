package ru.finni.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles LIMIT 1")
    fun observeProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods WHERE profileId = :profileId ORDER BY `index`")
    fun observePeriods(profileId: String): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods WHERE profileId = :profileId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActivePeriod(profileId: String): PeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(period: PeriodEntity)

    @Query("DELETE FROM periods WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE profileId = :profileId ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(profileId: String, limit: Int = 30): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}

@Dao
interface TaskProgressDao {
    @Query("SELECT * FROM task_progress WHERE profileId = :profileId")
    fun observeProgress(profileId: String): Flow<List<TaskProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: TaskProgressEntity)

    @Query("DELETE FROM task_progress WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
