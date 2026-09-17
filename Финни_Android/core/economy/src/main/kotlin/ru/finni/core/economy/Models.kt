package ru.finni.core.economy

/** Направления плана/факта бюджета. */
enum class BudgetDirection { MANDATORY, OPTIONAL, SAVINGS }

/** План бюджета на игровой период (в монетах). */
data class BudgetPlan(
    val mandatory: Int = 0,
    val optional: Int = 0,
    val savings: Int = 0,
) {
    val total: Int get() = mandatory + optional + savings
}

/** Фактическое исполнение бюджета за период. */
data class BudgetFact(
    val mandatory: Int = 0,
    val optional: Int = 0,
    val savings: Int = 0,
) {
    val total: Int get() = mandatory + optional + savings
}

/** Состояние питомца: обратимые показатели 0..100. */
data class PetCondition(
    val mood: Int = 70,
    val satiety: Int = 70,
) {
    init {
        require(mood in 0..100 && satiety in 0..100) { "Показатели состояния должны быть в 0..100" }
    }
}

/** Стадии развития питомца (не понижаются). */
enum class PetStage(val title: String) {
    BABY("Малыш"),
    FRIEND("Друг"),
    STAR("Звезда"),
}

/** Итог одного игрового периода для расчёта развития. */
data class PeriodResult(
    val mandatoryCovered: Boolean,
    val savingsMet: Boolean,
    /** Доля направлений, где отклонение факта от плана в пределах 20%. */
    val adherence: Float,
) {
    /** Взвешенный балл периода: 0.4·обязательные + 0.3·соответствие плану + 0.3·накопления. */
    val score: Float
        get() = 0.4f * (if (mandatoryCovered) 1f else 0f) +
                0.3f * adherence +
                0.3f * (if (savingsMet) 1f else 0f)
}
