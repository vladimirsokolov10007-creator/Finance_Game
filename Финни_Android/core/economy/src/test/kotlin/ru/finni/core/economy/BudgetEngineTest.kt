package ru.finni.core.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetEngineTest {

    @Test
    fun `plan within balance is accepted`() {
        assertEquals(BudgetEngine.checkPlan(BudgetPlan(15, 15, 10), 40), PlanCheck.Ok)
        assertEquals(BudgetEngine.remainder(BudgetPlan(15, 15, 10), 40), 0)
    }

    @Test
    fun `plan over balance is rejected with excess`() {
        val check = BudgetEngine.checkPlan(BudgetPlan(30, 20, 0), 40)
        assertIs<PlanCheck.OverBudget>(check)
        assertEquals(10, check.excess)
    }

    @Test
    fun `purchase with insufficient funds is declined`() {
        assertNull(BudgetEngine.tryPurchase(balance = 15, price = 20))
        assertEquals(10, BudgetEngine.tryPurchase(balance = 30, price = 20))
    }

    @Test
    fun `adherence counts directions within twenty percent`() {
        val plan = BudgetPlan(mandatory = 10, optional = 10, savings = 10)
        val fact = BudgetFact(mandatory = 11, optional = 5, savings = 10)
        assertEquals(2 / 3f, BudgetEngine.adherence(plan, fact))
    }

    @Test
    fun `period score blends mandatory adherence and savings`() {
        val perfect = BudgetEngine.periodResult(
            BudgetPlan(10, 0, 10),
            BudgetFact(10, 0, 10),
        )
        assertTrue(perfect.mandatoryCovered && perfect.savingsMet)
        // 0.4f + 0.3f + 0.3f в float даёт 1.0000001 — проверяем с допуском
        assertTrue(kotlin.math.abs(perfect.score - 1f) < 1e-4f, "score=${perfect.score}")

        val bad = BudgetEngine.periodResult(BudgetPlan(10, 0, 10), BudgetFact(0, 0, 0))
        // ничего не куплено: только пустое направление optional попало в допуск → 0.1.
        // «Безопасная ошибка»: неудачный период снижает балл, но не обнуляет прогресс.
        assertTrue(kotlin.math.abs(bad.score - 0.1f) < 1e-4f, "score=${bad.score}")
    }
}
