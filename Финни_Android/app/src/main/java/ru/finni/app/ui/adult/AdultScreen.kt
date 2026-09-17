package ru.finni.app.ui.adult

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

private val TOPIC_NAMES = mapOf(
    "BUDGET_PLANNING" to "Планирование бюджета",
    "SAVINGS" to "Накопления",
    "PURCHASES" to "Покупки и платежи",
)

/** Экран 12: раздел для взрослого — барьер «удерживай 3 секунды», прогресс, демо-режим, сброс. */
@Composable
fun AdultScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val progress by viewModel.taskProgress.collectAsState()
    val closed by viewModel.closedPeriods.collectAsState()
    var unlocked by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val profile = state.profile

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Раздел для взрослого", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (!unlocked) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text("🔒", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Этот раздел для родителя. Удерживай кнопку 3 секунды, чтобы войти.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown()
                                    val start = System.currentTimeMillis()
                                    val job = scope.launch {
                                        while (System.currentTimeMillis() - start < 3000) {
                                            holdProgress =
                                                ((System.currentTimeMillis() - start) / 3000f).coerceIn(0f, 1f)
                                            if (holdProgress >= 1f) {
                                                unlocked = true
                                                break
                                            }
                                            delay(50)
                                        }
                                    }
                                    waitForUpOrCancellation()
                                    job.cancel()
                                    holdProgress = 0f
                                }
                            },
                    ) {
                        Text(if (holdProgress > 0f) "Удерживай… ${(holdProgress * 100).toInt()}%" else "УДЕРЖИВАЙ, ЧТОБЫ ВОЙТИ")
                    }
                    if (holdProgress > 0f) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { holdProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        // --- содержимое раздела ---
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Цель приложения", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Сформировать у ребёнка 7–11 лет базовые навыки управления деньгами: " +
                            "планировать бюджет, различать обязательные и необязательные расходы, копить на цель. " +
                            "Приложение не использует реальные деньги, рекламу и не собирает персональные данные.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Прогресс без оценок", style = MaterialTheme.typography.titleMedium)
                val doneTopics = viewModel.content.tasks
                    .filter { progress[it.id]?.completed == true }
                    .map { it.topic }.distinct()
                Text("Пройденные темы: ${doneTopics.joinToString { TOPIC_NAMES[it] ?: it }.ifBlank { "пока нет" }}")
                Text("Заданий выполнено: ${progress.values.count { it.completed }} из ${viewModel.content.tasks.size}")
                Text("Периодов завершено: ${closed.size}")
                Text(
                    "Ребёнок: ${profile?.childName ?: "—"} · Питомец: ${profile?.petName ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Демонстрационный режим", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Для экспертной проверки: ежедневный доход по кнопке без ожидания, все задания доступны сразу.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                FinniButton(
                    text = if (state.demo) "Демо-режим включён — отключить" else "Включить демо-режим",
                    onClick = { viewModel.toggleDemo() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("Управление профилем", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Сброс вернёт игру к исходному состоянию: начальный баланс, первая стадия, чистая история.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinniButton(
                        text = "Сбросить тестовый профиль",
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Сбросить профиль?") },
            text = { Text("Прогресс ребёнка будет удалён без возможности восстановления. Действие доступно без обращения к разработчику.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetProfile()
                    showResetConfirm = false
                    unlocked = false
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Отмена") }
            },
        )
    }
}
