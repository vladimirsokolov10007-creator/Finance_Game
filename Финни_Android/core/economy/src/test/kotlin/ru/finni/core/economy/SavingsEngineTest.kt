package ru.finni.core.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavingsEngineTest {

    @Test
    fun `deposit moves coins within balance`() {
        val (balance, savings) = SavingsEngine.deposit(balance = 30, savings = 0, amount = 10)!!
        assertEquals(20, balance)
        assertEquals(10, savings)
    }

    @Test
    fun `deposit more than balance is clamped`() {
        val (balance, savings) = SavingsEngine.deposit(balance = 5, savings = 0, amount = 10)!!
        assertEquals(0, balance)
        assertEquals(5, savings)
    }

    @Test
    fun `withdraw preview shows goal consequences`() {
        val preview = SavingsEngine.previewWithdraw(
            savings = 30, balance = 20, amount = 15,
            goalPrice = 60, avgDeposit = 10,
        )!!
        assertEquals(15, preview.newSavings)
        assertEquals(35, preview.newBalance)
        assertEquals(45, preview.goalRemainder)
        assertEquals(5, preview.periodsToGoal)
        val (b, s) = SavingsEngine.confirmWithdraw(preview)
        assertEquals(35, b)
        assertEquals(15, s)
    }

    @Test
    fun `withdraw more than savings is declined`() {
        assertNull(SavingsEngine.previewWithdraw(savings = 5, balance = 0, amount = 10))
    }

    @Test
    fun `periods to goal uses average deposit`() {
        assertEquals(4, SavingsEngine.periodsToGoal(goalPrice = 60, savings = 20, avgDeposit = 10))
        assertNull(SavingsEngine.periodsToGoal(goalPrice = 60, savings = 20, avgDeposit = 0))
    }
}
