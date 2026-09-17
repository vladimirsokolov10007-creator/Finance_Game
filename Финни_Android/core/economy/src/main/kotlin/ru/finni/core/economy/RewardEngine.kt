package ru.finni.core.economy

/** Начисления игровой валюты. */
object RewardEngine {
    const val START_BALANCE = 40
    const val DAILY_INCOME = 20
    const val ALL_CORRECT_BONUS = 2

    fun taskReward(baseReward: Int, allStepsCorrect: Boolean): Int =
        baseReward + if (allStepsCorrect) ALL_CORRECT_BONUS else 0
}
