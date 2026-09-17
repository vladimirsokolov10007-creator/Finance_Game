package ru.finni.core.economy

/**
 * Движок состояния и развития питомца.
 * Ошибки пользователя обратимы: нет «смерти», прогресс стадии не понижается.
 */
object PetEngine {

    private const val PENALTY_MOOD = 15
    private const val PENALTY_SATIETY = 20
    private const val BONUS_MOOD_PLAN_OK = 10

    /** Потолок максимумов показателей (предметы магазина, v0.3). */
    const val MAX_CAP = 150

    /** Применить эффект покупки/задания к состоянию (кламп 0..максимум). */
    fun applyEffect(condition: PetCondition, moodDelta: Int, satietyDelta: Int): PetCondition =
        PetCondition(
            mood = (condition.mood + moodDelta).coerceIn(0, condition.maxMood),
            satiety = (condition.satiety + satietyDelta).coerceIn(0, condition.maxSatiety),
            maxMood = condition.maxMood,
            maxSatiety = condition.maxSatiety,
        )

    /** Пересчёт состояния по итогам периода + текст обратной связи для ребёнка. */
    data class PeriodOutcome(val condition: PetCondition, val message: String)

    fun closePeriod(condition: PetCondition, result: PeriodResult): PeriodOutcome {
        return if (!result.mandatoryCovered) {
            PeriodOutcome(
                condition = PetCondition(
                    mood = (condition.mood - PENALTY_MOOD).coerceIn(0, condition.maxMood),
                    satiety = (condition.satiety - PENALTY_SATIETY).coerceIn(0, condition.maxSatiety),
                    maxMood = condition.maxMood,
                    maxSatiety = condition.maxSatiety,
                ),
                message = "Обязательные покупки не закрыты — Финни голодный и грустный. " +
                        "На новой неделе сначала купи корм!",
            )
        } else if (result.adherence >= 0.99f) {
            PeriodOutcome(
                condition = PetCondition(
                    mood = (condition.mood + BONUS_MOOD_PLAN_OK).coerceIn(0, condition.maxMood),
                    satiety = condition.satiety,
                    maxMood = condition.maxMood,
                    maxSatiety = condition.maxSatiety,
                ),
                message = "План выполнен точно! Финни гордится тобой.",
            )
        } else {
            PeriodOutcome(condition, "Неделя завершена. Сравни план и факт — и улучши следующую неделю!")
        }
    }

    /* ---------- v0.3: предупреждения, проигрыш, рост максимумов ---------- */

    /** Игра окончена, если хотя бы один показатель упал до 0. */
    fun isGameOver(condition: PetCondition): Boolean =
        condition.mood <= 0 || condition.satiety <= 0

    /** Уровень тревоги: 0 — ок, 1 — жёлтое предупреждение (≤20), 2 — красное (≤10). */
    fun warningLevel(condition: PetCondition): Int {
        val lowest = minOf(condition.mood, condition.satiety)
        return when {
            lowest <= 10 -> 2
            lowest <= 20 -> 1
            else -> 0
        }
    }

    /** Предмет повышает максимум настроения/сытости (кап 150); текущее значение растёт вместе с максимумом. */
    fun raiseCaps(condition: PetCondition, maxMoodBonus: Int, maxSatietyBonus: Int): PetCondition {
        val newMaxMood = (condition.maxMood + maxMoodBonus).coerceAtMost(MAX_CAP)
        val newMaxSatiety = (condition.maxSatiety + maxSatietyBonus).coerceAtMost(MAX_CAP)
        return PetCondition(
            mood = (condition.mood + maxMoodBonus).coerceAtMost(newMaxMood),
            satiety = (condition.satiety + maxSatietyBonus).coerceAtMost(newMaxSatiety),
            maxMood = newMaxMood,
            maxSatiety = newMaxSatiety,
        )
    }

    /** Развитие по среднему баллу периодов. Стадия никогда не понижается. */
    fun nextStage(current: PetStage, averageScore: Float, periodsClosed: Int): PetStage {
        val target = when {
            averageScore >= 0.8f && periodsClosed >= 5 -> PetStage.STAR
            averageScore >= 0.6f -> PetStage.FRIEND
            else -> PetStage.BABY
        }
        return if (target.ordinal > current.ordinal) target else current
    }
}
