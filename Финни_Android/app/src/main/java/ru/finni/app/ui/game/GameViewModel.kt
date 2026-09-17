package ru.finni.app.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.finni.core.data.PeriodEntity
import ru.finni.core.data.ProfileEntity
import ru.finni.core.data.ProfileRepository
import ru.finni.core.data.TaskProgressEntity
import ru.finni.core.data.TransactionEntity
import ru.finni.core.economy.BudgetEngine
import ru.finni.core.economy.BudgetFact
import ru.finni.core.economy.BudgetPlan
import ru.finni.core.economy.PetEngine
import ru.finni.core.economy.PetStage
import ru.finni.core.economy.RewardEngine
import ru.finni.core.economy.SavingsEngine
import java.util.UUID
import javax.inject.Inject

data class GameUiState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val activePeriod: PeriodEntity? = null,
    val demo: Boolean = false,
    val dailyClaimed: Boolean = false,
    val periodOutcome: Pair<PeriodEntity, String>? = null, // итоги закрытого периода
    val event: String? = null,      // одноразовое сообщение (toast)
    val eventId: Long = 0,
)

private const val PREFS = "finni_prefs"
private const val KEY_DEMO = "demo"
private const val KEY_LAST_DAILY = "last_daily"

/** Центральный ViewModel игрового цикла. Один инстанс создаётся в FinniNavHost и передаётся всем экранам. */
@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProfileRepository,
    val content: ContentRepository,
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val taskProgress = MutableStateFlow<Map<String, TaskProgressEntity>>(emptyMap())
    val closedPeriods = MutableStateFlow<List<PeriodEntity>>(emptyList())

    init {
        viewModelScope.launch {
            val demo = prefs.getBoolean(KEY_DEMO, false)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val dailyClaimed = prefs.getString(KEY_LAST_DAILY, null) == today
            repository.observeProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        profile = profile,
                        demo = demo,
                        dailyClaimed = dailyClaimed && !demo,
                    )
                }
                if (profile != null) {
                    restorePeriod(profile.id)
                    transactions.value = repository.observeTransactions(profile.id).first()
                    taskProgress.value = repository.observeTaskProgress(profile.id)
                        .first().associateBy { it.taskId }
                    closedPeriods.value = repository.observePeriods(profile.id)
                        .first().filter { it.status == "CLOSED" }
                }
            }
        }
        // транзакции/прогресс — отдельные потоки
        viewModelScope.launch {
            val p = repository.observeProfile().first() ?: return@launch
            repository.observeTransactions(p.id).collect { transactions.value = it }
        }
        viewModelScope.launch {
            val p = repository.observeProfile().first() ?: return@launch
            repository.observeTaskProgress(p.id).collect { list ->
                taskProgress.value = list.associateBy { it.taskId }
            }
        }
        viewModelScope.launch {
            val p = repository.observeProfile().first() ?: return@launch
            repository.observePeriods(p.id).collect { list ->
                closedPeriods.value = list.filter { it.status == "CLOSED" }
                val active = list.firstOrNull { it.status == "ACTIVE" }
                if (_uiState.value.activePeriod?.id != active?.id) {
                    _uiState.update { it.copy(activePeriod = active) }
                }
            }
        }
    }

    private suspend fun restorePeriod(profileId: String) {
        // activePeriod подтянется потоком periods; ничего дополнительного не нужно
    }

    fun consumeEvent() = _uiState.update { it.copy(event = null) }

    private fun emitEvent(text: String) = _uiState.update {
        it.copy(event = text, eventId = it.eventId + 1)
    }

    /* ---------- доход ---------- */

    fun claimDaily() {
        val s = _uiState.value
        val profile = s.profile ?: return
        if (!s.demo && s.dailyClaimed) {
            emitEvent("Ежедневный доход уже получен. Загляни завтра!")
            return
        }
        viewModelScope.launch {
            val newBalance = profile.balance + RewardEngine.DAILY_INCOME
            repository.updateProfile(profile.copy(balance = newBalance))
            repository.addTransaction(
                profile.id, activePeriodId(), "INCOME_DAILY",
                RewardEngine.DAILY_INCOME, "Ежедневный доход",
            )
            prefs.edit().putString(KEY_LAST_DAILY, today()).apply()
            _uiState.update { it.copy(dailyClaimed = true) }
            emitEvent("+${RewardEngine.DAILY_INCOME} монет — ежедневный доход!")
        }
    }

    /* ---------- план бюджета ---------- */

    fun confirmPlan(mandatory: Int, optional: Int, savings: Int) {
        val profile = _uiState.value.profile ?: return
        val plan = BudgetPlan(mandatory, optional, savings)
        when (val check = BudgetEngine.checkPlan(plan, profile.balance)) {
            is ru.finni.core.economy.PlanCheck.OverBudget -> {
                emitEvent("Сумма плана превышает бюджет на ${check.excess} монет — уменьши одно из направлений.")
            }
            ru.finni.core.economy.PlanCheck.Ok -> viewModelScope.launch {
                val existing = _uiState.value.activePeriod
                if (existing != null) {
                    emitEvent("План уже подтверждён в этом периоде.")
                    return@launch
                }
                repository.upsertPeriod(
                    PeriodEntity(
                        id = UUID.randomUUID().toString(),
                        profileId = profile.id,
                        index = profile.periodIndex,
                        planMandatory = mandatory, planOptional = optional, planSavings = savings,
                        factMandatory = 0, factOptional = 0, factSavings = 0,
                        mandatoryCovered = false, savingsMet = false, adherence = 0f,
                        status = "ACTIVE",
                        startedAt = System.currentTimeMillis(),
                        closedAt = null,
                    )
                )
                emitEvent("План подтверждён! В конце периода сравним его с фактом.")
            }
        }
    }

    /* ---------- покупки ---------- */

    sealed interface PurchaseResult {
        data object Ok : PurchaseResult
        data class Declined(val lack: Int) : PurchaseResult
    }

    fun buy(item: ShopItemContent): PurchaseResult {
        val profile = _uiState.value.profile ?: return PurchaseResult.Declined(0)
        val newBalance = BudgetEngine.tryPurchase(profile.balance, item.price)
            ?: return PurchaseResult.Declined(item.price - profile.balance)
        viewModelScope.launch {
            val period = activePeriod()
            val cond = ru.finni.core.economy.PetCondition(profile.mood, profile.satiety)
            val newCond = PetEngine.applyEffect(cond, item.mood, item.satiety)
            repository.updateProfile(
                profile.copy(
                    balance = newBalance,
                    mood = newCond.mood,
                    satiety = newCond.satiety,
                )
            )
            period?.let {
                repository.upsertPeriod(
                    it.copy(
                        factMandatory = it.factMandatory + if (item.mandatory) item.price else 0,
                        factOptional = it.factOptional + if (!item.mandatory) item.price else 0,
                    )
                )
            }
            repository.addTransaction(
                profile.id, period?.id,
                if (item.mandatory) "PURCHASE_MANDATORY" else "PURCHASE_OPTIONAL",
                -item.price, item.name,
            )
        }
        return PurchaseResult.Ok
    }

    /* ---------- накопления ---------- */

    fun deposit(amount: Int = 10) {
        val profile = _uiState.value.profile ?: return
        val res = SavingsEngine.deposit(profile.balance, profile.savings, amount) ?: return
        viewModelScope.launch {
            repository.updateProfile(profile.copy(balance = res.first, savings = res.second))
            activePeriod()?.let {
                repository.upsertPeriod(it.copy(factSavings = it.factSavings + (res.second - profile.savings)))
            }
            repository.addTransaction(profile.id, activePeriodId(), "SAVINGS_IN", res.second - profile.savings, "В накопления")
        }
    }

    /** Предпросмотр снятия: показываем последствия до подтверждения (ТЗ п. 2.5.7). */
    fun previewWithdraw(amount: Int = 10): SavingsEngine.WithdrawPreview? {
        val profile = _uiState.value.profile ?: return null
        val goal = content.goals.firstOrNull { it.id == profile.goalId }
        return SavingsEngine.previewWithdraw(
            savings = profile.savings, balance = profile.balance, amount = amount,
            goalPrice = goal?.price,
            avgDeposit = avgDeposit(),
        )
    }

    fun confirmWithdraw(amount: Int = 10) {
        val profile = _uiState.value.profile ?: return
        val preview = previewWithdraw(amount) ?: return
        val (newBalance, newSavings) = SavingsEngine.confirmWithdraw(preview)
        viewModelScope.launch {
            repository.updateProfile(profile.copy(balance = newBalance, savings = newSavings))
            activePeriod()?.let {
                repository.upsertPeriod(it.copy(factSavings = maxOf(0, it.factSavings - amount)))
            }
            repository.addTransaction(profile.id, activePeriodId(), "SAVINGS_OUT", -amount, "Снято с накоплений")
            val goal = content.goals.firstOrNull { g -> g.id == profile.goalId }
            if (goal != null) {
                emitEvent("Снято $amount монет. До цели «${goal.name}» теперь ${preview.goalRemainder} монет.")
            }
        }
    }

    private fun avgDeposit(): Int {
        val closed = closedPeriods.value.takeLast(3)
        return if (closed.isEmpty()) 10 else (closed.sumOf { it.factSavings } / closed.size).coerceAtLeast(1)
    }

    fun selectGoal(goalId: String) {
        val profile = _uiState.value.profile ?: return
        viewModelScope.launch {
            repository.updateProfile(profile.copy(goalId = goalId))
            emitEvent("Цель выбрана! Пополняй накопления понемногу.")
        }
    }

    /* ---------- задания ---------- */

    fun applyTaskEffects(mood: Int, satiety: Int) {
        val profile = _uiState.value.profile ?: return
        val cond = ru.finni.core.economy.PetCondition(profile.mood, profile.satiety)
        val newCond = PetEngine.applyEffect(cond, mood, satiety)
        viewModelScope.launch {
            repository.updateProfile(profile.copy(mood = newCond.mood, satiety = newCond.satiety))
        }
    }

    fun completeTask(taskId: String, allCorrect: Boolean) {
        val profile = _uiState.value.profile ?: return
        val task = content.tasks.firstOrNull { it.id == taskId } ?: return
        val reward = RewardEngine.taskReward(task.reward, allCorrect)
        val prev = taskProgress.value[taskId]
        viewModelScope.launch {
            repository.updateProfile(profile.copy(balance = profile.balance + reward))
            repository.upsertTaskProgress(
                TaskProgressEntity(
                    taskId = taskId,
                    profileId = profile.id,
                    completed = true,
                    attempts = (prev?.attempts ?: 0) + 1,
                    lastCorrect = allCorrect,
                )
            )
            repository.addTransaction(profile.id, activePeriodId(), "INCOME_TASK", reward, "Задание: ${task.title}")
            emitEvent("Задание завершено! Награда +$reward монет${if (allCorrect) " (всё верно — бонус +2!)" else ""}.")
        }
    }

    /* ---------- итоги периода ---------- */

    fun closePeriod() {
        val profile = _uiState.value.profile ?: return
        val period = _uiState.value.activePeriod
        viewModelScope.launch {
            val plan = if (period != null)
                BudgetPlan(period.planMandatory, period.planOptional, period.planSavings)
            else BudgetPlan()
            val fact = if (period != null)
                BudgetFact(period.factMandatory, period.factOptional, period.factSavings)
            else BudgetFact()
            val result = BudgetEngine.periodResult(plan, fact)
            val outcome = PetEngine.closePeriod(
                ru.finni.core.economy.PetCondition(profile.mood, profile.satiety), result,
            )
            // стадия по среднему баллу закрытых периодов (включая текущий)
            val closedList = closedPeriods.value
            val allClosed = if (period != null)
                closedList + period.copy(
                    status = "CLOSED", closedAt = System.currentTimeMillis(),
                    mandatoryCovered = result.mandatoryCovered,
                    savingsMet = result.savingsMet,
                    adherence = result.adherence,
                )
            else closedList
            val avg = allClosed.map { p ->
                0.4f * (if (p.mandatoryCovered) 1f else 0f) + 0.3f * p.adherence + 0.3f * (if (p.savingsMet) 1f else 0f)
            }.average().toFloat()
            val currentStage = PetStage.entries.getOrElse(profile.petStage) { PetStage.BABY }
            val newStage = PetEngine.nextStage(currentStage, avg, allClosed.size)

            var message = outcome.message
            if (newStage.ordinal > currentStage.ordinal) {
                message += " 🎉 ${profile.petName} вырос: новая стадия «${newStage.title}»!"
            }

            period?.let {
                repository.upsertPeriod(
                    it.copy(
                        status = "CLOSED",
                        closedAt = System.currentTimeMillis(),
                        mandatoryCovered = result.mandatoryCovered,
                        savingsMet = result.savingsMet,
                        adherence = result.adherence,
                    )
                )
            }
            repository.updateProfile(
                profile.copy(
                    mood = outcome.condition.mood,
                    satiety = outcome.condition.satiety,
                    petStage = newStage.ordinal,
                    periodIndex = profile.periodIndex + 1,
                )
            )
            _uiState.update { it.copy(periodOutcome = (period ?: emptyPeriod(profile)) to message) }
        }
    }

    fun startNextPeriod() = _uiState.update { it.copy(periodOutcome = null) }

    private fun emptyPeriod(profile: ProfileEntity) = PeriodEntity(
        id = "", profileId = profile.id, index = profile.periodIndex,
        0, 0, 0, 0, 0, 0,
        mandatoryCovered = false, savingsMet = false, adherence = 0f,
        status = "CLOSED", startedAt = 0, closedAt = 0,
    )

    /* ---------- взрослый / демо / сброс ---------- */

    fun toggleDemo() {
        val newValue = !_uiState.value.demo
        prefs.edit().putBoolean(KEY_DEMO, newValue).apply()
        _uiState.update {
            it.copy(demo = newValue, dailyClaimed = false)
        }
    }

    fun resetProfile() {
        viewModelScope.launch {
            repository.resetProfile()
            prefs.edit().remove(KEY_LAST_DAILY).apply()
            _uiState.update { it.copy(activePeriod = null, periodOutcome = null) }
        }
    }

    /* ---------- утилиты ---------- */

    private suspend fun activePeriod(): PeriodEntity? = _uiState.value.activePeriod

    private fun activePeriodId(): String? = _uiState.value.activePeriod?.id

    private fun today() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())

    fun goalRemainder(): Pair<Int, Int?>? {
        val profile = _uiState.value.profile ?: return null
        val goal = content.goals.firstOrNull { it.id == profile.goalId } ?: return null
        val rest = maxOf(0, goal.price - profile.savings)
        val periods = SavingsEngine.periodsToGoal(goal.price, profile.savings, avgDeposit())
        return rest to periods
    }
}
