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
import ru.finni.app.ui.common.FinniEventToast
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniSecondaryButton
import ru.finni.core.economy.AchievementEngine

/** Экран 11: история, учебный прогресс, справочник терминов (ТЗ п. 2.5.11). */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onAdult: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return
    val transactions by viewModel.transactions.collectAsState()
    val progress by viewModel.taskProgress.collectAsState()
    val closed by viewModel.closedPeriods.collectAsState()
    val stages = listOf("Малыш", "Друг", "Звезда")
    val goal = viewModel.content.goals.firstOrNull { it.id == profile.goalId }

    FinniEventToast(viewModel)

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
                    Text("Недель завершено: ${closed.size}")
                    Text("Заданий выполнено: ${progress.values.count { it.completed }} из ${viewModel.content.tasks.size}")
                    Text("Цель: ${goal?.name ?: "не выбрана"}${goal?.let { " — накоплено ${profile.savings} из ${it.price}" } ?: ""}")
                }
            }
            Spacer(Modifier.height(6.dp))
            FinniSecondaryButton(
                text = "👪 Раздел взрослого",
                onClick = onAdult,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // v0.3: достижения
        item {
            Text(
                "Достижения: ${state.achievements.size} из ${AchievementEngine.all.size}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(AchievementEngine.all, key = { "ach_" + it.id }) { def ->
            val unlocked = def.id in state.achievements
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "${def.emoji} ${def.title}" + if (unlocked) " ✓" else " 🔒",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        def.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // v0.3: трофеи за исполненные цели
        item {
            Text("Трофеи", style = MaterialTheme.typography.titleMedium)
        }
        val trophies = viewModel.trophyList()
        if (trophies.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        "Пока нет трофеев. Накопи на цель и исполни мечту — трофей появится на питомце!",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(trophies, key = { "tr_" + it.second }) { (emoji, name) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        "$emoji $name",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.weight(1f))
                FinniSecondaryButton(text = "🔄 Начать заново", onClick = { viewModel.resetProfile(onRestart) }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
