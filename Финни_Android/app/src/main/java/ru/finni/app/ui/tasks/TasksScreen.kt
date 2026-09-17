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
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

private val TOPIC_NAMES = mapOf(
    "BUDGET_PLANNING" to "Планирование бюджета",
    "SAVINGS" to "Накопления",
    "PURCHASES" to "Покупки и платежи",
)

/** Экран 6 сценария: список заданий по 3 темам (ТЗ п. 2.5.8). */
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val progress by viewModel.taskProgress.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Задания", style = MaterialTheme.typography.titleLarge)
        Text(
            "Игровые ситуации с выбором и последствиями. Объяснение — при любом ответе.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(viewModel.content.tasks, key = { it.id }) { task ->
                val done = progress[task.id]?.completed == true
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                (if (done) "✅ " else "") + task.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${TOPIC_NAMES[task.topic] ?: task.topic} · награда ${task.reward} 🪙",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!done) {
                            FinniButton(text = "Играть", onClick = { onPlay(task.id) })
                        } else {
                            Text("готово", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
        FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}
