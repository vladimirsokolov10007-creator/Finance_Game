package ru.finni.core.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementsTest {

    private val empty = AchievementStats()
    private val allStats = AchievementStats(
        purchases = 5, mandatoryPurchases = 3, tasksCompleted = 1,
        periodsClosed = 2, perfectPlans = 1, goalsCompleted = 2,
        trophies = 2, bestBalance = 80,
    )

    @Test
    fun `empty stats unlock nothing`() {
        assertTrue(AchievementEngine.newlyUnlocked(empty, already = emptySet()).isEmpty())
    }

    @Test
    fun `all achievements unlock with rich stats`() {
        assertEquals(8, AchievementEngine.newlyUnlocked(allStats, already = emptySet()).size)
    }

    @Test
    fun `already unlocked are not repeated`() {
        val unlocked = AchievementEngine.newlyUnlocked(allStats, already = emptySet()).map { it.id }.toSet()
        assertTrue(AchievementEngine.newlyUnlocked(allStats, already = unlocked).isEmpty())
    }

    @Test
    fun `single thresholds trigger exactly expected ids`() {
        assertEquals(
            listOf("first_purchase"),
            AchievementEngine.newlyUnlocked(AchievementStats(purchases = 1), emptySet()).map { it.id },
        )
        assertEquals(
            listOf("rich"),
            AchievementEngine.newlyUnlocked(AchievementStats(bestBalance = 80), emptySet()).map { it.id },
        )
        assertEquals(
            listOf("dream", "collector"),
            AchievementEngine.newlyUnlocked(AchievementStats(goalsCompleted = 1, trophies = 2), emptySet()).map { it.id },
        )
    }
}
