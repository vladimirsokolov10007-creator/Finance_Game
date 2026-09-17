package ru.finni.core.economy

/** Движок накоплений и финансовых целей. */
object SavingsEngine {

    /** Пополнение накоплений в пределах доступного баланса. */
    fun deposit(balance: Int, savings: Int, amount: Int): Pair<Int, Int>? {
        if (amount <= 0) return null
        val real = minOf(amount, balance)
        if (real <= 0) return null
        return (balance - real) to (savings + real)
    }

    /** Снятие с накоплений только с подтверждением; до подтверждения показываем последствия. */
    data class WithdrawPreview(
        val newSavings: Int,
        val newBalance: Int,
        val goalRemainder: Int?,
        val periodsToGoal: Int?,
    )

    fun previewWithdraw(
        savings: Int, balance: Int, amount: Int,
        goalPrice: Int? = null, avgDeposit: Int? = null,
    ): WithdrawPreview? {
        if (amount <= 0 || amount > savings) return null
        val newSavings = savings - amount
        val remainder = goalPrice?.let { maxOf(0, it - newSavings) }
        val periods = if (goalPrice != null && avgDeposit != null && avgDeposit > 0)
            remainder?.let { kotlin.math.ceil(it.toDouble() / avgDeposit).toInt() }
        else null
        return WithdrawPreview(
            newSavings = newSavings,
            newBalance = balance + amount,
            goalRemainder = remainder,
            periodsToGoal = periods,
        )
    }

    fun confirmWithdraw(preview: WithdrawPreview): Pair<Int, Int> =
        preview.newBalance to preview.newSavings

    /** Срок цели в периодах по среднему пополнению. */
    fun periodsToGoal(goalPrice: Int, savings: Int, avgDeposit: Int): Int? {
        if (avgDeposit <= 0) return null
        val rest = maxOf(0, goalPrice - savings)
        return kotlin.math.ceil(rest.toDouble() / avgDeposit).toInt()
    }
}
