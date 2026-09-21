package ru.finni.app.ui.onboarding

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FullWidthFinniButton

/** Экран 1 сквозного сценария: знакомство с целью игры и тремя типами решений. */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    onDemo: () -> Unit,
    gameViewModel: GameViewModel,
) {
    val gameState by gameViewModel.uiState.collectAsState()
    var demoStarted by remember { mutableStateOf(false) }
    // профиль уже есть — пропускаем онбординг (восстановление сессии);
    // после запуска демо навигацией управляет onDemo, а не этот эффект
    LaunchedEffect(gameState.profile) {
        if (gameState.profile != null && !demoStarted) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🐶", style = MaterialTheme.typography.titleLarge)
        Text("Питомец Финни", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Научись управлять деньгами, заботясь о питомце. Всего три типа решений:",
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        DecisionCard(emoji = "🍖", title = "Потратить на обязательное",
            text = "Еда и уход. Без них Финни грустит — покупаем в первую очередь.")
        DecisionCard(emoji = "🎾", title = "Потратить на желаемое",
            text = "Игрушки и украшения. Приятно, но может подождать.")
        DecisionCard(emoji = "🏦", title = "Отложить",
            text = "Копи на большую цель понемногу каждую неделю.")

        Spacer(Modifier.height(20.dp))
        FullWidthFinniButton(text = "Понятно, начнём!", onClick = onDone)
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = {
            demoStarted = true
            gameViewModel.startDemoProfile(onDone = onDemo)
        }) {
            Text(
                "🎬 Демо-режим: показ для коллег",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Гостевой режим: без имени, телефона и e-mail. Прогресс хранится только на этом устройстве.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DecisionCard(emoji: String, title: String, text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("$emoji  $title", style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
