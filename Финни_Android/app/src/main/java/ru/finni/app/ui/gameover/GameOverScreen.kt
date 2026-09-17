package ru.finni.app.ui.gameover

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
import ru.finni.core.design.FinniSecondaryButton
import ru.finni.core.design.FinniTheme

/** v0.3: экран проигрыша — показатели питомца упали до нуля. Перезапуск или выход в главное меню. */
@Composable
fun GameOverScreen(
    onRestart: () -> Unit,
    onHome: () -> Unit,
    viewModel: GameViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile
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
                Text("💔", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(8.dp))
                Text("Игра окончена", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = "$petName совсем ослаб: настроение или сытость упали до нуля. " +
                                "В настоящей жизни вовремя купленный корм и забота не дают такому случиться — " +
                                "в новой игре планируй бюджет вместе с питомцем!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (profile != null) {
                    Text(
                        "Недель пройдено: ${profile.periodIndex - 1} · " +
                                "Достижений: ${state.achievements.size} · Трофеев: ${state.trophies.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                FinniButton(
                    text = "🔄 Начать заново",
                    onClick = { viewModel.resetProfile(onRestart) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                FinniSecondaryButton(
                    text = "🏠 На главный экран",
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
