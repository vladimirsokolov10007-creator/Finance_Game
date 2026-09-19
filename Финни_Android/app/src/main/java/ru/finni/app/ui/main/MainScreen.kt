package ru.finni.app.ui.main

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
import ru.finni.app.ui.common.FinniEventToast
import ru.finni.app.ui.common.PetAnimation
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.data.ProfileEntity
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton
import ru.finni.core.design.FinniTheme
import ru.finni.core.economy.PetEngine

/** Главный экран: питомец, баланс, накопления, состояние и вход в игровой цикл (ТЗ п. 2.5.3). */
@Composable
fun MainScreen(
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHistory: () -> Unit,
    onPeriodClosed: (gameOver: Boolean) -> Unit,
    viewModel: GameViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    FinniEventToast(viewModel)

    FinniTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val p = state.profile) {
                null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Загружаем профиль…", textAlign = TextAlign.Center)
                }
                else -> MainContent(
                    profile = p,
                    maxMood = state.maxMood,
                    maxSatiety = state.maxSatiety,
                    gameOver = PetEngine.isGameOver(
                        ru.finni.core.economy.PetCondition(p.mood, p.satiety, state.maxMood, state.maxSatiety),
                    ),
                    warningLevel = PetEngine.warningLevel(
                        ru.finni.core.economy.PetCondition(p.mood, p.satiety, state.maxMood, state.maxSatiety),
                    ),
                    onOpenPlan = onOpenPlan,
                    onOpenShop = onOpenShop,
                    onOpenGoals = onOpenGoals,
                    onOpenTasks = onOpenTasks,
                    onOpenHistory = onOpenHistory,
                    onClosePeriod = {
                        viewModel.closePeriod { gameOver -> onPeriodClosed(gameOver) }
                    },
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    profile: ProfileEntity,
    maxMood: Int,
    maxSatiety: Int,
    gameOver: Boolean,
    warningLevel: Int,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHistory: () -> Unit,
    onClosePeriod: () -> Unit,
) {
    val accessories = listOf("", "🎀", "👑", "🎖️", "🚴", "⛺")
    val stages = listOf("Малыш", "Друг", "Звезда")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Неделя ${profile.periodIndex}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // v0.5: анимированный питомец (видео) вместо эмодзи; персонаж из профиля
                PetAnimation(character = profile.petBody)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${profile.petName} · ${stages.getOrElse(profile.petStage) { "Малыш" }}" +
                            accessories.getOrElse(profile.petAccessory) { "" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                ConditionBar("Настроение", profile.mood, maxMood)
                ConditionBar("Сытость", profile.satiety, maxSatiety)
            }
        }
        Spacer(Modifier.height(12.dp))

        // v0.3: предупреждения о низких показателях
        when (warningLevel) {
            1 -> WarningCard(
                text = "⚠️ Финни грустит или голоден — показатели ниже 20! Загляни в магазин.",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
            2 -> WarningCard(
                text = "🆘 Финни совсем плохо — показатели ниже 10! Если завершить неделю сейчас, игра закончится.",
                containerColor = MaterialTheme.colorScheme.errorContainer,
            )
        }
        if (warningLevel > 0) Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BalanceChip("Баланс", "🪙 ${profile.balance}")
            BalanceChip("Накопления", "🏦 ${profile.savings}")
        }
        Spacer(Modifier.height(16.dp))

        if (gameOver) {
            // v0.3: игра окончена — цикл недоступен
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("💔 Игра окончена", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Показатели ${profile.petName} упали до нуля. Чтобы сыграть снова, начни игру заново — «Начать заново» есть на экране итогов.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // --- игровой цикл ---
            Text("Игровой цикл", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FinniButton(text = "📋 План бюджета", onClick = onOpenPlan, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FinniButton(text = "🛒 Покупки", onClick = onOpenShop, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FinniButton(text = "🏦 Накопления и цели", onClick = onOpenGoals, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FinniButton(text = "🎓 Задания", onClick = onOpenTasks, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FinniButton(text = "📜 Прогресс и справка", onClick = onOpenHistory, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            FinniSecondaryButton(
                text = "🏁 Завершить неделю",
                onClick = onClosePeriod,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WarningCard(text: String, containerColor: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ConditionBar(label: String, value: Int, max: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value/$max", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(2.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { value / max.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun BalanceChip(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
