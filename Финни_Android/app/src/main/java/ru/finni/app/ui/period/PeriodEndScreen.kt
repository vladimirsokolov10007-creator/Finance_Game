package ru.finni.app.ui.period

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import ru.finni.app.ui.plan.PlanFactTable
import ru.finni.core.design.FullWidthFinniButton

/** Экраны 9–10 сценария: обратная связь по неделе и рост питомца. */
@Composable
fun PeriodEndScreen(
    onNextPeriod: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val outcome = state.periodOutcome

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Итоги недели", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (outcome == null) {
            Text("Неделя ещё не завершена.")
            Spacer(Modifier.height(12.dp))
            FullWidthFinniButton(text = "Назад", onClick = onNextPeriod)
            return@Column
        }

        val (period, message) = outcome
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Text("Неделя ${period.index}: план vs факт", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PlanFactTable(
            planM = period.planMandatory, planO = period.planOptional, planS = period.planSavings,
            factM = period.factMandatory, factO = period.factOptional, factS = period.factSavings,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append(if (period.mandatoryCovered) "✓ Обязательные расходы закрыты"
                else "✗ Обязательные расходы не закрыты — исправь на новой неделе")
                append("\n")
                append(if (period.savingsMet) "✓ Накопления по плану или больше"
                else "✗ Накоплено меньше плана")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        FullWidthFinniButton(
            text = "Начать неделю ${state.profile?.periodIndex ?: 1}",
            onClick = {
                viewModel.startNextPeriod()
                onNextPeriod()
            },
        )
    }
}
