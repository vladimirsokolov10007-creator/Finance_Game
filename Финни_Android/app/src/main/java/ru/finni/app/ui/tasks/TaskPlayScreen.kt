package ru.finni.app.ui.tasks

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

/** Прохождение задания: шаги, варианты, объяснение независимо от правильности. */
@Composable
fun TaskPlayScreen(
    taskId: String,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val task = viewModel.content.tasks.firstOrNull { it.id == taskId } ?: run {
        Text("Задание не найдено"); return
    }

    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    var correctCount by rememberSaveable { mutableIntStateOf(0) }
    var appliedEffects by rememberSaveable { mutableStateOf(false) }

    val step = task.steps[stepIndex]
    val picked = pickedId?.let { id -> step.options.firstOrNull { it.id == id } }
    val isLast = stepIndex == task.steps.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(task.title, style = MaterialTheme.typography.titleLarge)
        Text(
            "Шаг ${stepIndex + 1} из ${task.steps.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(task.story, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(step.text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            step.options.forEach { option ->
                val pickedThis = pickedId == option.id
                val showCorrect = pickedId != null && option.correct
                val containerColor = when {
                    pickedThis && option.correct -> MaterialTheme.colorScheme.secondaryContainer
                    pickedThis -> MaterialTheme.colorScheme.errorContainer
                    showCorrect -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                Card(
                    onClick = {
                        if (pickedId == null) {
                            pickedId = option.id
                            if (option.correct) correctCount++
                            // эффект на питомца применяем один раз за шаг
                            if (!appliedEffects) {
                                viewModel.applyTaskEffects(option.mood, option.satiety)
                                appliedEffects = true
                            }
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(option.text, modifier = Modifier.padding(14.dp))
                }
            }
        }

        picked?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (it.correct) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    it.explanation,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            FinniButton(
                text = if (isLast) "Забрать награду +${task.reward} 🪙" else "Дальше",
                onClick = {
                    if (isLast) {
                        viewModel.completeTask(task.id, correctCount == task.steps.size)
                        onBack()
                    } else {
                        stepIndex++
                        pickedId = null
                        appliedEffects = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinniSecondaryButton(text = "К списку заданий", onClick = onBack, modifier = Modifier.weight(1f))
            FinniSecondaryButton(text = "🔄 Начать заново", onClick = { viewModel.resetProfile(onRestart) }, modifier = Modifier.weight(1f))
        }
    }
}
