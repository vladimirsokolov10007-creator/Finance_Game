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
}
