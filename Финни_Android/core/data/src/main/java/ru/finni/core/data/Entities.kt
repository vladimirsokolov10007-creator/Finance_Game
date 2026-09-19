package ru.finni.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Локальный игровой профиль. Только на устройстве, без регистрации. */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val childName: String,
    val petName: String,
    /** Персонаж: 0 — Девочка · худи, 1 — Девочка · лапки, 2 — Мальчик. */
    val petBody: Int,
    val petColor: Int,
    val petAccessory: Int,
    /** Возрастная группа игрока: 0 — 7–9 лет, 1 — 10–11 лет (влияет на сложность заданий). */
    val ageGroup: Int,
    val balance: Int,
    val savings: Int,
    val goalId: String?,
    val petStage: Int,
    val mood: Int,
    val satiety: Int,
    val periodIndex: Int,
    /** Тестовый профиль для экспертной проверки (сбрасывается). */
    val isTestProfile: Boolean,
    val createdAt: Long,
)

/** Игровой период: план и факт по трём направлениям. */
@Entity(tableName = "periods")
data class PeriodEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val index: Int,
    val planMandatory: Int,
    val planOptional: Int,
    val planSavings: Int,
    val factMandatory: Int,
    val factOptional: Int,
    val factSavings: Int,
    val mandatoryCovered: Boolean,
    val savingsMet: Boolean,
    val adherence: Float,
    val status: String, // PLANNED / ACTIVE / CLOSED
    val startedAt: Long,
    val closedAt: Long?,
)

/** Каждое начисление и списание — с источником и суммой (объяснимость баланса). */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val periodId: String?,
    /** INCOME_START / INCOME_DAILY / INCOME_TASK / PURCHASE_MANDATORY / PURCHASE_OPTIONAL / SAVINGS_IN / SAVINGS_OUT */
    val type: String,
    val amount: Int,
    val title: String,
    val createdAt: Long,
)

/** Прогресс по учебным заданиям. */
@Entity(tableName = "task_progress")
data class TaskProgressEntity(
    @PrimaryKey val taskId: String,
    val profileId: String,
    val completed: Boolean,
    val attempts: Int,
    val lastCorrect: Boolean,
)
