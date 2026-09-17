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
import ru.finni.core.economy.AchievementEngine
import ru.finni.core.economy.AchievementStats
import ru.finni.core.economy.BudgetEngine
import ru.finni.core.economy.BudgetFact
import ru.finni.core.economy.BudgetPlan
import ru.finni.core.economy.PetCondition
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
    val periodOutcome: Pair<PeriodEntity, String>? = null, // итоги закрытой недели
    val event: String? = null,      // одноразовое сообщение (toast)
    val eventId: Long = 0,
    // v0.3: максимумы показателей, купленные предметы, трофеи, достижения
    val maxMood: Int = 100,
    val maxSatiety: Int = 100,
    val purchasedItems: Set<String> = emptySet(),
    val trophies: Set<String> = emptySet(),
    val achievements: Set<String> = emptySet(),
)

private const val PREFS = "finni_prefs"
private const val KEY_MAX_MOOD = "max_mood"
private const val KEY_MAX_SATIETY = "max_satiety"
private const val KEY_PURCHASED = "purchased_items"
private const val KEY_TROPHIES = "trophies"
private const val KEY_ACHIEVEMENTS = "achievements"
private const val STAT_PURCHASES = "stat_purchases"
private const val STAT_MANDATORY = "stat_mandatory"
private const val STAT_PERFECT = "stat_perfect_plans"
private const val STAT_GOALS = "stat_goals"
private const val STAT_BEST_BALANCE = "stat_best_balance"

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
        // v0.3: восстановление прогресса из prefs (максимумы, покупки, трофеи, достижения)
        _uiState.update {
            it.copy(
                maxMood = prefs.getInt(KEY_MAX_MOOD, 100),
                maxSatiety = prefs.getInt(KEY_MAX_SATIETY, 100),
                purchasedItems = prefs.getStringSet(KEY_PURCHASED, emptySet()) ?: emptySet(),
                trophies = prefs.getStringSet(KEY_TROPHIES, emptySet()) ?: emptySet(),
                achievements = prefs.getStringSet(KEY_ACHIEVEMENTS, emptySet()) ?: emptySet(),
            )
        }
        viewModelScope.launch {
            repository.observeProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        profile = profile,
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
                    emitEvent("План уже подтверждён на этой неделе.")
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
                emitEvent("План подтверждён! В конце недели сравним его с фактом.")
            }
        }
    }

    /* ---------- покупки ---------- */

    sealed interface PurchaseResult {
        data object Ok : PurchaseResult
        data class Declined(val lack: Int) : PurchaseResult
        data object AlreadyOwned : PurchaseResult
    }

    fun buy(item: ShopItemContent): PurchaseResult {
        val s = _uiState.value
        val profile = s.profile ?: return PurchaseResult.Declined(0)
        // v0.3: предметы магазина одноразовые
        if (item.id in s.purchasedItems) return PurchaseResult.AlreadyOwned
        val newBalance = BudgetEngine.tryPurchase(profile.balance, item.price)
            ?: return PurchaseResult.Declined(item.price - profile.balance)
        viewModelScope.launch {
            val period = activePeriod()
            val cond = PetEngine.raiseCaps(
                PetCondition(profile.mood, profile.satiety, s.maxMood, s.maxSatiety),
                item.maxMoodBonus, item.maxSatietyBonus,
            )
            val newCond = PetEngine.applyEffect(cond, item.mood, item.satiety)
            repository.updateProfile(
                profile.copy(
                    balance = newBalance,
                    mood = newCond.mood,
                    satiety = newCond.satiety,
                )
            )
            val purchased = s.purchasedItems + item.id
            prefs.edit()
                .putInt(KEY_MAX_MOOD, newCond.maxMood)
                .putInt(KEY_MAX_SATIETY, newCond.maxSatiety)
                .putStringSet(KEY_PURCHASED, purchased)
                .putInt(STAT_PURCHASES, prefs.getInt(STAT_PURCHASES, 0) + 1)
                .apply()
            if (item.mandatory) {
                prefs.edit().putInt(STAT_MANDATORY, prefs.getInt(STAT_MANDATORY, 0) + 1).apply()
            }
            prefs.edit()
                .putInt(STAT_BEST_BALANCE, maxOf(prefs.getInt(STAT_BEST_BALANCE, 0), newBalance))
                .apply()
            _uiState.update {
                it.copy(
                    maxMood = newCond.maxMood,
                    maxSatiety = newCond.maxSatiety,
                    purchasedItems = purchased,
                )
            }
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
            checkAchievements()
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

    /** v0.3: исполнить мечту — списать накопления, выдать трофей и авто-экипировать аксессуар. */
    fun completeGoal(): Boolean {
        val s = _uiState.value
        val profile = s.profile ?: return false
        val goal = content.goals.firstOrNull { it.id == profile.goalId } ?: return false
        if (profile.savings < goal.price) return false
        if (goal.id in s.trophies) return false
        val goalIndex = content.goals.indexOf(goal).coerceAtLeast(0)
        viewModelScope.launch {
            val newTrophies = s.trophies + goal.id
            repository.updateProfile(
                profile.copy(
                    savings = profile.savings - goal.price,
                    petAccessory = 3 + goalIndex, // трофейный аксессуар
                )
            )
            prefs.edit()
                .putStringSet(KEY_TROPHIES, newTrophies)
                .putInt(STAT_GOALS, prefs.getInt(STAT_GOALS, 0) + 1)
                .apply()
            _uiState.update { it.copy(trophies = newTrophies) }
            repository.addTransaction(
                profile.id, activePeriodId(),
                "GOAL_COMPLETED", 0, "Цель исполнена: ${goal.name}",
            )
            emitEvent("🎉 Мечта сбылась! Трофей «${goal.name}» уже на питомце!")
            checkAchievements()
        }
        return true
    }

    /* ---------- задания ---------- */

    fun applyTaskEffects(mood: Int, satiety: Int) {
        val s = _uiState.value
        val profile = s.profile ?: return
        val cond = PetCondition(profile.mood, profile.satiety, s.maxMood, s.maxSatiety)
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
        val newBalance = profile.balance + reward
        viewModelScope.launch {
            repository.updateProfile(profile.copy(balance = newBalance))
            prefs.edit()
                .putInt(STAT_BEST_BALANCE, maxOf(prefs.getInt(STAT_BEST_BALANCE, 0), newBalance))
                .apply()
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
            checkAchievements()
        }
    }

    /* ---------- итоги недели ---------- */

    fun closePeriod(onFinished: (gameOver: Boolean) -> Unit = {}) {
        val s = _uiState.value
        val profile = s.profile ?: return
        val period = s.activePeriod
        viewModelScope.launch {
            val plan = if (period != null)
                BudgetPlan(period.planMandatory, period.planOptional, period.planSavings)
            else BudgetPlan()
            val fact = if (period != null)
                BudgetFact(period.factMandatory, period.factOptional, period.factSavings)
            else BudgetFact()
            val result = BudgetEngine.periodResult(plan, fact)
            val outcome = PetEngine.closePeriod(
                PetCondition(profile.mood, profile.satiety, s.maxMood, s.maxSatiety), result,
            )
            // v0.3: счётчик идеальных планов для достижения «Точный план»
            if (result.mandatoryCovered && result.adherence >= 0.99f) {
                prefs.edit().putInt(STAT_PERFECT, prefs.getInt(STAT_PERFECT, 0) + 1).apply()
            }
            // стадия по среднему баллу закрытых недель (включая текущую)
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

            val closedPeriod = period?.copy(
                status = "CLOSED",
                closedAt = System.currentTimeMillis(),
                mandatoryCovered = result.mandatoryCovered,
                savingsMet = result.savingsMet,
                adherence = result.adherence,
            )
            closedPeriod?.let { repository.upsertPeriod(it) }
            // v0.4: карманные деньги начисляются автоматически в начале каждой новой недели
            // (кроме первой — в первую неделю живём только на стартовые 40 монет)
            val newPeriodIndex = profile.periodIndex + 1
            val weeklyIncome = if (newPeriodIndex >= 2) RewardEngine.WEEKLY_INCOME else 0
            if (weeklyIncome > 0) {
                message += " Неделя $newPeriodIndex: +$weeklyIncome монет карманных денег! 🪙"
            }
            repository.updateProfile(
                profile.copy(
                    mood = outcome.condition.mood,
                    satiety = outcome.condition.satiety,
                    petStage = newStage.ordinal,
                    periodIndex = newPeriodIndex,
                    balance = profile.balance + weeklyIncome,
                )
            )
            if (weeklyIncome > 0) {
                repository.addTransaction(
                    profile.id, null, "INCOME_WEEKLY", weeklyIncome, "Карманные деньги новой недели",
                )
                prefs.edit()
                    .putInt(STAT_BEST_BALANCE, maxOf(prefs.getInt(STAT_BEST_BALANCE, 0), profile.balance + weeklyIncome))
                    .apply()
            }
            _uiState.update { it.copy(periodOutcome = (closedPeriod ?: emptyPeriod(profile)) to message) }
            checkAchievements()
            // v0.3: проигрыш, если хотя бы один показатель упал до 0
            onFinished(PetEngine.isGameOver(outcome.condition))
        }
    }

    fun startNextPeriod() = _uiState.update { it.copy(periodOutcome = null) }

    private fun emptyPeriod(profile: ProfileEntity) = PeriodEntity(
        id = "", profileId = profile.id, index = profile.periodIndex,
        0, 0, 0, 0, 0, 0,
        mandatoryCovered = false, savingsMet = false, adherence = 0f,
        status = "CLOSED", startedAt = 0, closedAt = 0,
    )

    /* ---------- утилиты ---------- */

    /** Проверка и разблокировка достижений; новые — toast'ом. */
    private fun checkAchievements() {
        val stats = AchievementStats(
            purchases = prefs.getInt(STAT_PURCHASES, 0),
            mandatoryPurchases = prefs.getInt(STAT_MANDATORY, 0),
            tasksCompleted = taskProgress.value.values.count { it.completed },
            periodsClosed = closedPeriods.value.size,
            perfectPlans = prefs.getInt(STAT_PERFECT, 0),
            goalsCompleted = prefs.getInt(STAT_GOALS, 0),
            trophies = _uiState.value.trophies.size,
            bestBalance = prefs.getInt(STAT_BEST_BALANCE, 0),
        )
        val already = _uiState.value.achievements
        val fresh = AchievementEngine.newlyUnlocked(stats, already)
        if (fresh.isEmpty()) return
        val newSet = already + fresh.map { it.id }
        prefs.edit().putStringSet(KEY_ACHIEVEMENTS, newSet).apply()
        _uiState.update { it.copy(achievements = newSet) }
        emitEvent(
            if (fresh.size == 1) "🏅 Достижение «${fresh[0].title}»!"
            else fresh.joinToString(prefix = "🏅 Достижения: ") { "«${it.title}»" }
        )
    }

    /** Полный сброс игры: удаляет профиль и весь прогресс (v0.3). */
    fun resetProfile(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.resetProfile()
            prefs.edit().clear().apply()
            _uiState.update {
                it.copy(
                    periodOutcome = null,
                    maxMood = 100,
                    maxSatiety = 100,
                    purchasedItems = emptySet(),
                    trophies = emptySet(),
                    achievements = emptySet(),
                )
            }
            transactions.value = emptyList()
            taskProgress.value = emptyMap()
            closedPeriods.value = emptyList()
            onDone()
        }
    }

    /** Текущее состояние питомца с учётом максимумов. */
    fun petCondition(): PetCondition? {
        val s = _uiState.value
        val profile = s.profile ?: return null
        return PetCondition(profile.mood, profile.satiety, s.maxMood, s.maxSatiety)
    }

    fun isGameOver(): Boolean = petCondition()?.let { PetEngine.isGameOver(it) } ?: false

    fun warningLevel(): Int = petCondition()?.let { PetEngine.warningLevel(it) } ?: 0

    /** Трофеи с эмодзи для отображения. */
    fun trophyList(): List<Pair<String, String>> {
        val goals = content.goals
        val emojis = listOf("🎖️", "🚴", "⛺")
        return _uiState.value.trophies.mapNotNull { goalId ->
            val idx = goals.indexOfFirst { it.id == goalId }
            if (idx >= 0) (emojis.getOrElse(idx) { "🏆" }) to goals[idx].name else null
        }
    }

    private suspend fun activePeriod(): PeriodEntity? = _uiState.value.activePeriod

    private fun activePeriodId(): String? = _uiState.value.activePeriod?.id

    fun goalRemainder(): Pair<Int, Int?>? {
        val profile = _uiState.value.profile ?: return null
        val goal = content.goals.firstOrNull { it.id == profile.goalId } ?: return null
        val rest = maxOf(0, goal.price - profile.savings)
        val periods = SavingsEngine.periodsToGoal(goal.price, profile.savings, avgDeposit())
        return rest to periods
    }
}
