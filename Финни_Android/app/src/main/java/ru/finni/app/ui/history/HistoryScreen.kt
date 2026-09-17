package ru.finni.app.ui.history

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniSecondaryButton

/** Экран 11: история, учебный прогресс, справочник терминов (ТЗ п. 2.5.11). */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return
    val transactions by viewModel.transactions.collectAsState()
    val progress by viewModel.taskProgress.collectAsState()
    val closed by viewModel.closedPeriods.collectAsState()
    val stages = listOf("Малыш", "Друг", "Звезда")
    val goal = viewModel.content.goals.firstOrNull { it.id == profile.goalId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Прогресс", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Питомец: ${profile.petName} · стадия «${stages.getOrElse(profile.petStage) { "Малыш" }}»")
                    Text("Периодов завершено: ${closed.size}")
                    Text("Заданий выполнено: ${progress.values.count { it.completed }} из ${viewModel.content.tasks.size}")
                    Text("Цель: ${goal?.name ?: "не выбрана"}${goal?.let { " — накоплено ${profile.savings} из ${it.price}" } ?: ""}")
                }
            }
        }

        item {
            Text("История начислений и трат", style = MaterialTheme.typography.titleMedium)
        }
        items(transactions.take(20), key = { it.id }) { tx ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        "${tx.amount} 🪙 — ${tx.title}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            Text("Справочник: что значат слова", style = MaterialTheme.typography.titleMedium)
        }
        items(viewModel.content.glossary, key = { it.term }) { g ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(12.dp)) {
                    Text(g.term, style = MaterialTheme.typography.titleMedium)
                    Text(
                        g.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
    }
}
