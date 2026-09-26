package ru.finni.app.ui.adult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.common.FinniEventToast
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton
import ru.finni.core.economy.AchievementEngine

/**
 * v0.6: раздел взрослого — статистика ребёнка и сброс прогресса (ТЗ п. 2.5.12).
 * Локально, без регистрации и сети; данные только на устройстве.
 */
@Composable
fun AdultScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onReset: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    val progress by viewModel.taskProgress.collectAsState()
    val closed by viewModel.closedPeriods.collectAsState()
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    FinniEventToast(viewModel)

    val characters = listOf("Девочка · худи", "Девочка · лапки", "Мальчик")
    val ages = listOf("7–9 лет", "10–11 лет")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Раздел взрослого", style = MaterialTheme.typography.titleLarge)
        Text(
            "Сводка об учебном прогрессе ребёнка. Данные хранятся только на этом устройстве, регистрация не требуется.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (profile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ребёнок: ${profile.childName} · ${ages.getOrElse(profile.ageGroup) { "7–9 лет" }}")
                    Text("Питомец: ${profile.petName} (${characters.getOrElse(profile.petBody) { "персонаж" }})")
                    Text("Недель завершено: ${closed.size} · сейчас неделя ${profile.periodIndex}")
                    Text("Баланс: ${profile.balance} 🪙 · накопления: ${profile.savings} 🪙")
                }
            }

            val done = progress.values.count { it.completed }
            val attempts = progress.values.sumOf { it.attempts }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Заданий выполнено: $done из ${viewModel.content.tasks.size}")
                    Text("Попыток в заданиях: $attempts")
                    Text("Достижений: ${state.achievements.size} из ${AchievementEngine.all.size}")
                    Text("Трофеев: ${state.trophies.size}")
                }
            }

            if (closed.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Дисциплина бюджета по неделям:", style = MaterialTheme.typography.titleMedium)
                        closed.sortedBy { it.index }.forEach { p ->
                            val mark = when {
                                p.mandatoryCovered && p.adherence >= 0.99f -> "✅ план выполнен"
                                p.mandatoryCovered -> "✓ улучшение куплено"
                                else -> "✗ улучшение не куплено"
                            }
                            Text("Неделя ${p.index}: $mark")
                        }
                        val avg = closed.map { it.adherence }.average().toFloat()
                        Text("Среднее попадание в план: ${(avg * 100).toInt()}%")
                    }
                }
            }

            // v0.7 (ТЗ п. 2.5.12): просветительские цели приложения
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Цели обучения", style = MaterialTheme.typography.titleMedium)
                    Text("• Понимать, что расходы не должны превышать доходы")
                    Text("• Различать обязательные и необязательные расходы")
                    Text("• Планировать покупки в условиях ограниченного бюджета")
                    Text("• Ставить цель и регулярно откладывать часть средств")
                    Text("• Оценивать собственные финансовые решения")
                }
            }

            // v0.7 (ТЗ п. 3.6): настройки доступности
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Анимация питомца", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Выключите, если анимация отвлекает или утомляет.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = !state.animOff,
                        onCheckedChange = { viewModel.setAnimationEnabled(it) },
                    )
                }
            }

            // v0.7 (ТЗ п. 2.5.13): сброс демонстрационного профиля к исходному состоянию
            if (state.demo) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Демонстрационный режим", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Тестовый профиль можно вернуть к исходному состоянию — сценарий показа будет как при первом запуске демо.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        FinniButton(
                            text = "🎬 Сбросить демо-профиль",
                            onClick = { viewModel.startDemoProfile() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Опасная зона", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Сброс удалит профиль, питомца, все покупки, достижения и историю. Действие необратимо.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FinniButton(
                        text = "🗑 Сбросить весь прогресс",
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            Text(
                "Профиль ещё не создан — статистики пока нет.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.weight(1f))
            FinniSecondaryButton(text = "🔄 Начать заново", onClick = { viewModel.resetProfile(onRestart) }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить весь прогресс?") },
            text = { Text("Профиль, питомец, покупки, достижения и история будут удалены безвозвратно.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    viewModel.resetProfile(onReset)
                }) { Text("Удалить всё") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Отмена") }
            },
        )
    }
}
