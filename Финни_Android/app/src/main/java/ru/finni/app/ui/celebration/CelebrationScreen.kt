package ru.finni.app.ui.celebration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniTheme

/** v0.3: праздничный экран исполненной цели — символическая награда за накопления. */
@Composable
fun CelebrationScreen(
    goalId: String,
    onDone: () -> Unit,
    viewModel: GameViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
    val goal = viewModel.content.goals.firstOrNull { it.id == goalId }
    val goalIndex = viewModel.content.goals.indexOfFirst { it.id == goalId }.coerceAtLeast(0)
    val trophyEmoji = listOf("🎖️", "🚴", "⛺").getOrElse(goalIndex) { "🏆" }
    val petName = profile?.petName ?: "Финни"

    FinniTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎉", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("Мечта сбылась!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ты копил(а) несколько недель — и вот результат:",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(trophyEmoji, style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(goal?.name ?: "Цель", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Трофей уже на $petName — посмотри на главном экране!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Накопления на это потрачены, но достижение «Мечта сбылась» останется с тобой навсегда.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                FinniButton(text = "Продолжить игру", onClick = onDone, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
