package ru.finni.core.economy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PetEngineTest {

    @Test
    fun `effect is clamped to zero hundred`() {
        val low = PetEngine.applyEffect(PetCondition(mood = 5, satiety = 5), moodDelta = -15, satietyDelta = -20)
        assertEquals(0, low.mood)
        assertEquals(0, low.satiety)
        val high = PetEngine.applyEffect(PetCondition(mood = 95, satiety = 95), moodDelta = 10, satietyDelta = 10)
        assertEquals(100, high.mood)
        assertEquals(100, high.satiety)
    }

    @Test
    fun `unclosed mandatory needs lower mood and satiety reversibly`() {
        val outcome = PetEngine.closePeriod(
            PetCondition(mood = 70, satiety = 70),
            PeriodResult(mandatoryCovered = false, savingsMet = false, adherence = 0f),
        )
        assertEquals(55, outcome.condition.mood)
        assertEquals(50, outcome.condition.satiety)
    }

    @Test
    fun `stage grows with average score and never falls`() {
        assertEquals(PetStage.FRIEND, PetEngine.nextStage(PetStage.BABY, 0.7f, periodsClosed = 3))
        assertEquals(PetStage.STAR, PetEngine.nextStage(PetStage.FRIEND, 0.85f, periodsClosed = 5))
        // не понижается
        assertEquals(PetStage.STAR, PetEngine.nextStage(PetStage.STAR, 0.2f, periodsClosed = 6))
        assertEquals(PetStage.FRIEND, PetEngine.nextStage(PetStage.FRIEND, 0.2f, periodsClosed = 9))
    }

    @Test
    fun `reward has bonus for all correct steps`() {
        assertEquals(5, RewardEngine.taskReward(5, allStepsCorrect = false))
        assertEquals(7, RewardEngine.taskReward(5, allStepsCorrect = true))
    }

    @Test
    fun `effect is clamped to raised maximums`() {
        val raised = PetCondition(mood = 145, satiety = 148, maxMood = 150, maxSatiety = 150)
        val result = PetEngine.applyEffect(raised, moodDelta = 10, satietyDelta = 10)
        assertEquals(150, result.mood)
        assertEquals(150, result.satiety)
    }

    @Test
    fun `game over only when a stat reaches zero`() {
        assertTrue(!PetEngine.isGameOver(PetCondition(mood = 10, satiety = 10)))
        assertTrue(PetEngine.isGameOver(PetCondition(mood = 0, satiety = 50)))
        assertTrue(PetEngine.isGameOver(PetCondition(mood = 50, satiety = 0)))
    }

    @Test
    fun `warning levels at thresholds`() {
        assertEquals(0, PetEngine.warningLevel(PetCondition(mood = 70, satiety = 70)))
        assertEquals(1, PetEngine.warningLevel(PetCondition(mood = 20, satiety = 70)))
        assertEquals(2, PetEngine.warningLevel(PetCondition(mood = 70, satiety = 10)))
        assertEquals(2, PetEngine.warningLevel(PetCondition(mood = 5, satiety = 70)))
    }

    @Test
    fun `close period clamps by raised maximums`() {
        val outcome = PetEngine.closePeriod(
            PetCondition(mood = 148, satiety = 145, maxMood = 150, maxSatiety = 150),
            PeriodResult(mandatoryCovered = true, savingsMet = false, adherence = 1f),
        )
        assertEquals(150, outcome.condition.mood) // 148 + 10 бонуса, кламп по 150
        assertEquals(145, outcome.condition.satiety)
    }

    @Test
    fun `raise caps exact values`() {
        val base = PetCondition(mood = 90, satiety = 80)
        val raised = PetEngine.raiseCaps(base, maxMoodBonus = 10, maxSatietyBonus = 20)
        assertEquals(110, raised.maxMood)
        assertEquals(120, raised.maxSatiety)
        assertEquals(100, raised.mood)
        assertEquals(100, raised.satiety)
        // повторный рост не выше капа
        val capped = PetEngine.raiseCaps(raised, maxMoodBonus = 100, maxSatietyBonus = 100)
        assertEquals(150, capped.maxMood)
        assertEquals(150, capped.maxSatiety)
    }
}
