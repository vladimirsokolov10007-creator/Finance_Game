package ru.finni.core.economy

/** Начисления игровой валюты. */
object RewardEngine {
    const val START_BALANCE = 40

    /** Карманные деньги: начисляются автоматически в начале каждой новой недели (со 2-й). */
    const val WEEKLY_INCOME = 20
    const val ALL_CORRECT_BONUS = 2

    fun taskReward(baseReward: Int, allStepsCorrect: Boolean): Int =
        baseReward + if (allStepsCorrect) ALL_CORRECT_BONUS else 0
}
