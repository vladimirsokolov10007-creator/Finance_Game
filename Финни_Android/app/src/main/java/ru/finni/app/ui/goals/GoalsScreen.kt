package ru.finni.app.ui.goals

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.common.FinniEventToast
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

/** Экран 8 сценария: цели, накопления, снятие только с подтверждением (ТЗ п. 2.5.7). */
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onCelebration: (goalId: String) -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return
    var showWithdrawDialog by rememberSaveable { mutableStateOf(false) }

    FinniEventToast(viewModel)

    val goal = viewModel.content.goals.firstOrNull { it.id == profile.goalId }
    val remainder = goal?.let { maxOf(0, it.price - profile.savings) }
    val goalReady = goal != null && profile.savings >= goal.price && goal.id !in state.trophies

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Цели и накопления", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Накоплено: ${profile.savings} 🪙", style = MaterialTheme.typography.titleMedium)
                    Text(goal?.name ?: "Цель не выбрана", style = MaterialTheme.typography.titleMedium)
                }
                if (goal != null) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (profile.savings.toFloat() / goal.price).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    val periods = viewModel.goalRemainder()?.second
                    Text(
                        "Цена: ${goal.price} · осталось собрать: $remainder" +
                                (periods?.let { " · примерно $it недель" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinniButton(
                        text = "Положить 10 🪙",
                        enabled = profile.balance >= 10,
                        onClick = { viewModel.deposit(10) },
                        modifier = Modifier.weight(1f),
                    )
                    FinniSecondaryButton(
                        text = "Снять 10 🪙",
                        enabled = profile.savings >= 10,
                        onClick = { showWithdrawDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                }
                // v0.3: награда за крупную накопленную цель
                if (goalReady) {
                    Spacer(Modifier.height(8.dp))
                    FinniButton(
                        text = "🎉 Исполнить мечту!",
                        onClick = {
                            if (viewModel.completeGoal()) onCelebration(goal!!.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(viewModel.content.goals, key = { it.id }) { g ->
                val selected = profile.goalId == g.id
                Card(
                    onClick = { if (!selected) viewModel.selectGoal(g.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium)
                            Text("${g.price} 🪙", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            g.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selected) {
                            Text("✓ Текущая цель", color = MaterialTheme.colorScheme.secondary)
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

    // Подтверждение снятия: показываем последствия ДО подтверждения
    if (showWithdrawDialog) {
        val preview = viewModel.previewWithdraw(10)
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Снять 10 🪙 с накоплений?") },
            text = {
                if (preview == null) {
                    Text("Недостаточно средств на накоплениях.")
                } else {
                    Text(
                        "Накопления уменьшатся до ${preview.newSavings} 🪙.\n" +
                                (preview.goalRemainder?.let {
                                    "До цели «${goal?.name}» останется собрать $it 🪙" +
                                            (preview.periodsToGoal?.let { p -> " (~$p недель)" } ?: "") + "."
                                } ?: "") +
                                "\n\nПодтверди, если это осознанное решение."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = preview != null,
                    onClick = { viewModel.confirmWithdraw(10); showWithdrawDialog = false },
                ) { Text("Снять") }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("Отмена") }
            },
        )
    }
}
