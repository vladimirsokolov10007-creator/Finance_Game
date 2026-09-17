package ru.finni.core.economy

/**
 * Движок состояния и развития питомца.
 * Ошибки пользователя обратимы: нет «смерти», прогресс стадии не понижается.
 */
object PetEngine {

    private const val PENALTY_MOOD = 15
    private const val PENALTY_SATIETY = 20
    private const val BONUS_MOOD_PLAN_OK = 10

    /** Применить эффект покупки/задания к состоянию (кламп 0..100). */
    fun applyEffect(condition: PetCondition, moodDelta: Int, satietyDelta: Int): PetCondition =
        PetCondition(
            mood = (condition.mood + moodDelta).coerceIn(0, 100),
            satiety = (condition.satiety + satietyDelta).coerceIn(0, 100),
        )

    /** Пересчёт состояния по итогам периода + текст обратной связи для ребёнка. */
    data class PeriodOutcome(val condition: PetCondition, val message: String)

    fun closePeriod(condition: PetCondition, result: PeriodResult): PeriodOutcome {
        return if (!result.mandatoryCovered) {
            PeriodOutcome(
                condition = PetCondition(
                    mood = (condition.mood - PENALTY_MOOD).coerceIn(0, 100),
                    satiety = (condition.satiety - PENALTY_SATIETY).coerceIn(0, 100),
                ),
                message = "Обязательные покупки не закрыты — Финни голодный и грустный. " +
                        "В новом периоде сначала купи корм!",
            )
        } else if (result.adherence >= 0.99f) {
            PeriodOutcome(
                condition = PetCondition(
                    mood = (condition.mood + BONUS_MOOD_PLAN_OK).coerceIn(0, 100),
                    satiety = condition.satiety,
                ),
                message = "План выполнен точно! Финни гордится тобой.",
            )
        } else {
            PeriodOutcome(condition, "Период завершён. Сравни план и факт — и улучши следующий период!")
        }
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
