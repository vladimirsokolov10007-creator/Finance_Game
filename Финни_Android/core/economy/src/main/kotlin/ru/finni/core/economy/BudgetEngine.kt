package ru.finni.core.economy

/** Результат проверки плана. */
sealed interface PlanCheck {
    data object Ok : PlanCheck
    data class OverBudget(val excess: Int) : PlanCheck
}

/** Движок бюджета: контроль плана и покупок. Чистый Kotlin, без Android. */
object BudgetEngine {

    /** План допустим, только если сумма направлений не превышает баланс. */
    fun checkPlan(plan: BudgetPlan, balance: Int): PlanCheck {
        val excess = plan.total - balance
        return if (excess > 0) PlanCheck.OverBudget(excess) else PlanCheck.Ok
    }

    fun remainder(plan: BudgetPlan, balance: Int): Int = balance - plan.total

    /**
     * Покупка при недостатке средств отклоняется (баланс не меняется).
     * Возвращает новый баланс или null при отказе.
     */
    fun tryPurchase(balance: Int, price: Int): Int? =
        if (price > balance) null else balance - price

    /** Отклонение факта от плана по направлению; допуск — 20% плана (мин. 2 монеты). */
    fun withinTolerance(plan: Int, fact: Int): Boolean {
        if (plan == 0 && fact == 0) return true
        val tolerance = maxOf(plan * 0.2, 2.0)
        return kotlin.math.abs(fact - plan) <= tolerance
    }

    /** Доля направлений, по которым факт соответствует плану (0..1). */
    fun adherence(plan: BudgetPlan, fact: BudgetFact): Float {
        val ok = listOf(
            withinTolerance(plan.mandatory, fact.mandatory),
            withinTolerance(plan.optional, fact.optional),
            withinTolerance(plan.savings, fact.savings),
        ).count { it }
        return ok / 3f
    }

    fun periodResult(plan: BudgetPlan, fact: BudgetFact): PeriodResult = PeriodResult(
        mandatoryCovered = fact.mandatory > 0,
        savingsMet = fact.savings >= plan.savings,
        adherence = adherence(plan, fact),
    )
}
