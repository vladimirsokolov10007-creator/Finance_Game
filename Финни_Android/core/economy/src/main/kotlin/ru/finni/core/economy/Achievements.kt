package ru.finni.core.economy

/** Определение достижения. */
data class AchievementDef(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
)

/** Входные счётчики прогресса для проверки достижений. */
data class AchievementStats(
    val purchases: Int = 0,
    val mandatoryPurchases: Int = 0,
    val tasksCompleted: Int = 0,
    val periodsClosed: Int = 0,
    val perfectPlans: Int = 0,
    val goalsCompleted: Int = 0,
    val trophies: Int = 0,
    val bestBalance: Int = 0,
)

/** Каталог и движок достижений (v0.3). Чистая функция — тестируется без Android. */
object AchievementEngine {

    val all: List<AchievementDef> = listOf(
        AchievementDef("first_purchase", "🛒", "Первая покупка", "Купи первый предмет для Финни в магазине."),
        AchievementDef("caretaker", "🧼", "Заботливый хозяин", "Купи 3 обязательных предмета (корм и уход)."),
        AchievementDef("shopper", "🏪", "Знаток магазина", "Сделай 5 покупок в магазине."),
        AchievementDef("scholar", "🎓", "Юный экономист", "Выполни первое задание."),
        AchievementDef("planner", "📋", "Точный план", "Заверши неделю с идеальным выполнением плана."),
        AchievementDef("dream", "🌟", "Мечта сбылась", "Накопи и исполни первую цель."),
        AchievementDef("collector", "🏆", "Коллекционер", "Получи 2 трофея за исполненные цели."),
        AchievementDef("rich", "💰", "Богач", "Накопи на балансе 80 монет одновременно."),
    )

    private val checks: Map<String, (AchievementStats) -> Boolean> = mapOf(
        "first_purchase" to { it.purchases >= 1 },
        "caretaker" to { it.mandatoryPurchases >= 3 },
        "shopper" to { it.purchases >= 5 },
        "scholar" to { it.tasksCompleted >= 1 },
        "planner" to { it.perfectPlans >= 1 },
        "dream" to { it.goalsCompleted >= 1 },
        "collector" to { it.trophies >= 2 },
        "rich" to { it.bestBalance >= 80 },
    )

    /** Достижения, которые только что разблокировались (есть в проверках, но ещё нет в already). */
    fun newlyUnlocked(stats: AchievementStats, already: Set<String>): List<AchievementDef> =
        all.filter { def -> def.id !in already && (checks[def.id]?.invoke(stats) ?: false) }

    fun byId(id: String): AchievementDef? = all.firstOrNull { it.id == id }
}
