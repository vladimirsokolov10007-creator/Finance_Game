package ru.finni.core.data

import java.util.UUID

/** Репозиторий локального профиля: источник истины для UI. */
class ProfileRepository(private val db: FinniDatabase) {

    fun observeProfile() = db.profileDao().observeProfile()
    fun observePeriods(profileId: String) = db.periodDao().observePeriods(profileId)
    fun observeTransactions(profileId: String) = db.transactionDao().observeRecent(profileId)
    fun observeTaskProgress(profileId: String) = db.taskProgressDao().observeProgress(profileId)

    suspend fun createProfile(
        childName: String,
        petName: String,
        petBody: Int,
        petColor: Int,
        petAccessory: Int,
        ageGroup: Int,
        startBalance: Int,
    ): ProfileEntity {
        val profile = ProfileEntity(
            id = UUID.randomUUID().toString(),
            childName = childName,
            petName = petName,
            petBody = petBody,
            petColor = petColor,
            petAccessory = petAccessory,
            ageGroup = ageGroup,
            balance = startBalance,
            savings = 0,
            goalId = null,
            petStage = 0,
            mood = 70,
            satiety = 70,
            periodIndex = 1,
            isTestProfile = true,
            createdAt = System.currentTimeMillis(),
        )
        db.profileDao().upsert(profile)
        db.transactionDao().insert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                profileId = profile.id,
                periodId = null,
                type = "INCOME_START",
                amount = startBalance,
                title = "Стартовый бюджет",
                createdAt = System.currentTimeMillis(),
            )
        )
        return profile
    }

    suspend fun updateProfile(profile: ProfileEntity) = db.profileDao().upsert(profile)

    suspend fun upsertPeriod(period: PeriodEntity) = db.periodDao().upsert(period)

    suspend fun upsertTaskProgress(progress: TaskProgressEntity) = db.taskProgressDao().upsert(progress)

    suspend fun addTransaction(
        profileId: String,
        periodId: String?,
        type: String,
        amount: Int,
        title: String,
    ) {
        db.transactionDao().insert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                periodId = periodId,
                type = type,
                amount = amount,
                title = title,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /** Сброс тестового профиля: удаляет профиль и весь прогресс (доступно взрослому). */
    suspend fun resetProfile() {
        val profile = db.profileDao().getProfile() ?: return
        db.periodDao().deleteForProfile(profile.id)
        db.transactionDao().deleteForProfile(profile.id)
        db.taskProgressDao().deleteForProfile(profile.id)
        db.profileDao().deleteAll()
    }
}
