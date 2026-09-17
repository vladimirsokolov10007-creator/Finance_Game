package ru.finni.app.ui.game

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Модели учебного контента (JSON из assets, отделён от кода — ТЗ п. 2.5.14). */
data class TaskOption(
    val id: String,
    val text: String,
    val explanation: String,
    val correct: Boolean,
    val mood: Int,
    val satiety: Int,
)

data class TaskStep(val text: String, val options: List<TaskOption>)

data class TaskContent(
    val id: String,
    val topic: String,
    val title: String,
    val story: String,
    val steps: List<TaskStep>,
    val reward: Int,
)

data class ShopItemContent(
    val id: String,
    val name: String,
    val mandatory: Boolean,
    val price: Int,
    val mood: Int,
    val satiety: Int,
    val hint: String,
)

data class GoalContent(val id: String, val name: String, val price: Int, val description: String)

data class GlossaryTerm(val term: String, val explanation: String)

/** Загрузчик контента из assets/content/ — файлы .json (org.json, без внешних зависимостей). */
@Singleton
class ContentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val tasks: List<TaskContent> by lazy { loadTasks() }
    val shop: List<ShopItemContent> by lazy { loadShop() }
    val goals: List<GoalContent> by lazy { loadGoals() }
    val glossary: List<GlossaryTerm> by lazy { loadGlossary() }

    private fun readAsset(name: String): String =
        context.assets.open("content/$name").bufferedReader().use { it.readText() }

    private fun loadTasks(): List<TaskContent> {
        val arr = org.json.JSONArray(readAsset("tasks.json"))
        return (0 until arr.length()).map { i ->
            val t = arr.getJSONObject(i)
            val stepsArr = t.getJSONArray("scenario")
            val steps = (0 until stepsArr.length()).map { si ->
                val s = stepsArr.getJSONObject(si)
                val optArr = s.getJSONArray("options")
                val options = (0 until optArr.length()).map { oi ->
                    val o = optArr.getJSONObject(oi)
                    val effect: JSONObject = o.optJSONObject("effect") ?: JSONObject()
                    TaskOption(
                        id = o.getString("id"),
                        text = o.getString("text"),
                        explanation = o.getString("explanation"),
                        correct = o.getBoolean("correct"),
                        mood = effect.optInt("mood", 0),
                        satiety = effect.optInt("satiety", 0),
                    )
                }
                TaskStep(s.getString("text"), options)
            }
            TaskContent(
                id = t.getString("id"),
                topic = t.getString("topic"),
                title = t.getString("title"),
                story = t.getString("story"),
                steps = steps,
                reward = t.getInt("reward"),
            )
        }
    }

    private fun loadShop(): List<ShopItemContent> {
        val arr = org.json.JSONArray(readAsset("shop.json"))
        return (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            val effect: JSONObject = s.optJSONObject("effect") ?: JSONObject()
            ShopItemContent(
                id = s.getString("id"),
                name = s.getString("name"),
                mandatory = s.getString("category") == "MANDATORY",
                price = s.getInt("price"),
                mood = effect.optInt("mood", 0),
                satiety = effect.optInt("satiety", 0),
                hint = s.getString("influenceHint"),
            )
        }
    }

    private fun loadGoals(): List<GoalContent> {
        val arr = org.json.JSONArray(readAsset("goals.json"))
        return (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            GoalContent(g.getString("id"), g.getString("name"), g.getInt("price"), g.getString("description"))
        }
    }

    private fun loadGlossary(): List<GlossaryTerm> {
        val arr = org.json.JSONArray(readAsset("glossary.json"))
        return (0 until arr.length()).map { i ->
            val g = arr.getJSONObject(i)
            GlossaryTerm(g.getString("term"), g.getString("explanation"))
        }
    }
}
