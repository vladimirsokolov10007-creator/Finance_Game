package ru.finni.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.common.FinniEventToast
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

private val TOPIC_NAMES = mapOf(
    "BUDGET_PLANNING" to "Планирование бюджета",
    "SAVINGS" to "Накопления",
    "PURCHASES" to "Покупки и платежи",
)

/** Экран 6 сценария: задания по темам; из каждой категории — 1 задание в день (v0.5). */
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val progress by viewModel.taskProgress.collectAsState()

    FinniEventToast(viewModel)

    // v0.5: задания зависят от возраста — для 10–11 лет доступны и более сложные
    val ageGroup = state.profile?.ageGroup ?: 0
    val tasksByTopic = viewModel.content.tasks
        .filter { it.ageGroup <= ageGroup }
        .groupBy { it.topic }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Задания", style = MaterialTheme.typography.titleLarge)
        Text(
            "Игровые ситуации с выбором и последствиями. Из каждой категории можно выполнить только 1 задание в день." +
                    if (ageGroup == 1) " Возраст 10–11: добавлены задания посложнее." else "",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            tasksByTopic.forEach { (topic, tasks) ->
                val topicDone = viewModel.topicDoneToday(topic)
                item(key = "topic_$topic") {
                    Text(
                        "${TOPIC_NAMES[topic] ?: topic}" + if (topicDone) " · ✅ выполнено сегодня" else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (topicDone) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                items(tasks, key = { it.id }) { task ->
                    val doneBefore = progress[task.id]?.completed == true
                    val playable = !topicDone
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (doneBefore) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    (if (doneBefore) "✅ " else "") + task.title,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "награда ${task.reward} 🪙",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (playable) {
                                FinniButton(text = "Играть", onClick = { onPlay(task.id) })
                            } else {
                                Text("завтра", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.weight(1f))
            FinniSecondaryButton(text = "🔄 Начать заново", onClick = { viewModel.resetProfile(onRestart) }, modifier = Modifier.weight(1f))
        }
    }
}
