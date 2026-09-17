package ru.finni.app.ui.main

import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.data.ProfileEntity
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton
import ru.finni.core.design.FinniTheme

/** Главный экран: питомец, баланс, накопления, состояние и вход в игровой цикл (ТЗ п. 2.5.3). */
@Composable
fun MainScreen(
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAdult: () -> Unit,
    onPeriodClosed: () -> Unit,
    viewModel: GameViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // одноразовые события — toast
    var lastEventId by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    LaunchedEffect(state.eventId) {
        if (state.eventId != lastEventId) {
            lastEventId = state.eventId
            state.event?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

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
                    dailyClaimed = state.dailyClaimed,
                    demo = state.demo,
                    onOpenPlan = onOpenPlan,
                    onOpenShop = onOpenShop,
                    onOpenGoals = onOpenGoals,
                    onOpenTasks = onOpenTasks,
                    onOpenHistory = onOpenHistory,
                    onOpenAdult = onOpenAdult,
                    onClaimDaily = { viewModel.claimDaily() },
                    onClosePeriod = {
                        viewModel.closePeriod()
                        onPeriodClosed()
                    },
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    profile: ProfileEntity,
    dailyClaimed: Boolean,
    demo: Boolean,
    onOpenPlan: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAdult: () -> Unit,
    onClaimDaily: () -> Unit,
    onClosePeriod: () -> Unit,
) {
    val bodies = listOf("🐶", "🐱", "🐰")
    val accessories = listOf("", "🎀", "👑")
    val stages = listOf("Малыш", "Друг", "Звезда")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Период ${profile.periodIndex}", style = MaterialTheme.typography.titleLarge)
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
                Text(
                    text = bodies.getOrElse(profile.petBody) { "🐶" } +
                            accessories.getOrElse(profile.petAccessory) { "" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${profile.petName} · ${stages.getOrElse(profile.petStage) { "Малыш" }}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                ConditionBar("Настроение", profile.mood)
                ConditionBar("Сытость", profile.satiety)
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BalanceChip("Баланс", "🪙 ${profile.balance}")
            BalanceChip("Накопления", "🏦 ${profile.savings}")
        }
        Spacer(Modifier.height(16.dp))

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
            text = if (dailyClaimed) "Ежедневный доход получен ✓" else "💰 Получить ежедневный доход",
            onClick = onClaimDaily,
            enabled = !dailyClaimed,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        FinniSecondaryButton(
            text = "🏁 Завершить период",
            onClick = onClosePeriod,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FinniSecondaryButton(text = "🔒 Взрослый", onClick = onOpenAdult)
            if (demo) {
                Text(
                    "ДЕМО",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ConditionBar(label: String, value: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value/100", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(2.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { value / 100f },
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
