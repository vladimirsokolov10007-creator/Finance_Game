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
import ru.finni.app.ui.game.GameViewModel
import ru.finni.app.ui.game.ShopItemContent
import ru.finni.core.design.FinniButton
import ru.finni.core.design.FinniSecondaryButton

/** Экран 7 сценария: покупки с подтверждением и отказом при нехватке (ТЗ п. 2.5.6). */
@Composable
fun ShopScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return

    var showMandatory by rememberSaveable { mutableStateOf(true) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var declineLack by rememberSaveable { mutableStateOf(0) }
    val pendingItem = pending?.let { id -> viewModel.content.shop.firstOrNull { it.id == id } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Магазин", style = MaterialTheme.typography.titleLarge)
        Text(
            "Баланс: ${profile.balance} 🪙. Перед покупкой смотри цену, категорию и влияние на питомца.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = showMandatory, onClick = { showMandatory = true }, label = { Text("🍖 Обязательные") })
            FilterChip(selected = !showMandatory, onClick = { showMandatory = false }, label = { Text("🎾 Желаемые") })
        }
        Spacer(Modifier.height(8.dp))

        val items = viewModel.content.shop.filter { it.mandatory == showMandatory }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { item ->
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
                                (if (item.mandatory) "обязательное" else "желаемое") + " · ${item.price} 🪙",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item.hint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FinniButton(text = "Купить", onClick = {
                            pending = item.id
                            declineLack = maxOf(0, item.price - profile.balance)
                        })
                    }
                }
            }
        }
        FinniSecondaryButton(text = "Назад", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }

    // Диалог подтверждения / отказа
    pendingItem?.let { item ->
        val canBuy = item.price <= (viewModel.uiState.value.profile?.balance ?: 0)
        AlertDialog(
            onDismissRequest = { pending = null; declineLack = 0 },
            title = { Text(item.name) },
            text = {
                if (canBuy) {
                    Text(
                        "Цена: ${item.price} 🪙 (${if (item.mandatory) "обязательное" else "желаемое"})\n" +
                                "${item.hint}\n\nПосле покупки баланс уменьшится, а покупка сохранится в истории периода."
                    )
                } else {
                    Text(
                        "Не хватает $declineLack монет. Покупка при недостатке средств недопустима.\n\n" +
                                "Варианты: выполни задание и заработай, выбери товар подешевле " +
                                "или отложи покупку до следующего периода."
                    )
                }
            },
            confirmButton = {
                if (canBuy) {
                    TextButton(onClick = {
                        viewModel.buy(item)
                        pending = null
                    }) { Text("Купить") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null; declineLack = 0 }) { Text(if (canBuy) "Отмена" else "Понятно") }
            },
        )
    }
}
