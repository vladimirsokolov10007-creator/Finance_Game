package ru.finni.app.ui.shop

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import ru.finni.app.ui.game.ShopItemContent
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

/** Экран 7 сценария: магазин — продукты (восполняют) и улучшения (максимум, по порядку). */
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onVictory: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return

    FinniEventToast(viewModel)

    var showProducts by rememberSaveable { mutableStateOf(true) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var declineLack by rememberSaveable { mutableStateOf(0) }
    val pendingItem = pending?.let { id -> viewModel.content.shop.firstOrNull { it.id == id } }

    val improvements = viewModel.improvements()
    val nextImprovement = improvements.firstOrNull { it.id !in state.purchasedItems }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Магазин", style = MaterialTheme.typography.titleLarge)
        Text(
            "Баланс: ${profile.balance} 🪙. Продукты восполняют сытость и настроение. " +
                    "Улучшения поднимают максимум и покупаются по порядку.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = showProducts, onClick = { showProducts = true }, label = { Text("🍖 Продукты") })
            FilterChip(selected = !showProducts, onClick = { showProducts = false }, label = { Text("🔧 Улучшения") })
        }
        Spacer(Modifier.height(8.dp))

        val items = viewModel.content.shop.filter { it.mandatory == !showProducts }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { item ->
                val owned = item.id in state.purchasedItems
                val locked = item.mandatory && !owned && nextImprovement != null && item.id != nextImprovement.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                (if (item.mandatory) "улучшение ${item.keyOrder} из ${improvements.size}" else "продукт") +
                                        " · ${item.price} 🪙",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item.hint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (locked) {
                                Text(
                                    "🔒 Сначала купи: ${nextImprovement!!.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        when {
                            owned -> FinniSecondaryButton(text = "✓ Куплено", enabled = false, onClick = {})
                            locked -> FinniSecondaryButton(text = "🔒", enabled = false, onClick = {})
                            else -> FinniButton(text = "Купить", onClick = {
                                pending = item.id
                                declineLack = maxOf(0, item.price - profile.balance)
                            })
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

    // Диалог подтверждения / отказа
    pendingItem?.let { item ->
        val canBuy = item.price <= (viewModel.uiState.value.profile?.balance ?: 0)
        AlertDialog(
            onDismissRequest = { pending = null; declineLack = 0 },
            title = { Text(item.name) },
            text = {
                if (canBuy) {
                    val effectText = buildString {
                        if (item.mandatory) {
                            if (item.maxMoodBonus > 0) append("Максимум настроения +${item.maxMoodBonus} навсегда.\n")
                            if (item.maxSatietyBonus > 0) append("Максимум сытости +${item.maxSatietyBonus} навсегда.\n")
                            append("Покупается один раз.\n")
                        } else {
                            if (item.mood > 0) append("Настроение +${item.mood}.\n")
                            if (item.satiety > 0) append("Сытость +${item.satiety}.\n")
                            append("Можно покупать сколько угодно раз.\n")
                        }
                    }
                    Text(
                        "Цена: ${item.price} 🪙 (${if (item.mandatory) "улучшение" else "продукт"})\n" +
                                effectText + item.hint
                    )
                } else {
                    Text(
                        "Не хватает $declineLack монет. Покупка при недостатке средств недопустима.\n\n" +
                                "Варианты: выполни задание и заработай, выбери товар подешевле " +
                                "или отложи покупку до следующей недели."
                    )
                }
            },
            confirmButton = {
                if (canBuy) {
                    TextButton(onClick = {
                        val result = viewModel.buy(item)
                        pending = null
                        // v0.5: куплено последнее улучшение — экран победы
                        if (result is GameViewModel.PurchaseResult.Ok && result.victory) {
                            onVictory()
                        }
                    }) { Text("Купить") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null; declineLack = 0 }) { Text(if (canBuy) "Отмена" else "Понятно") }
            },
        )
    }
}
