package ru.finni.app.ui.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.common.PetAnimation
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FullWidthFinniButton

/** Экраны 2–3 сценария: гостевой профиль + настройка питомца (≥9 комбинаций). */
@Composable
fun CreatePetScreen(
    onCreated: () -> Unit,
    viewModel: CreatePetViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val gameState by gameViewModel.uiState.collectAsState()

    LaunchedEffect(state.created) {
        if (state.created) onCreated()
    }
    // профиль уже существует — не показываем создание повторно
    LaunchedEffect(gameState.profile) {
        if (gameState.profile != null && !state.created) onCreated()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Создай питомца", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

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
                PetPreview(state)
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.childName,
            onValueChange = viewModel::onChildNameChange,
            label = { Text("Твоё игровое имя") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.petName,
            onValueChange = viewModel::onPetNameChange,
            label = { Text("Имя питомца") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        Text("Кто твой питомец?", style = MaterialTheme.typography.titleMedium)
        PickerRow(
            items = listOf("Девочка · худи", "Девочка · лапки", "Мальчик"),
            selected = state.body,
            onSelect = viewModel::onBodyChange,
        )
        Text("Сколько тебе лет?", style = MaterialTheme.typography.titleMedium)
        PickerRow(
            items = listOf("7–9 лет", "10–11 лет"),
            selected = state.ageGroup,
            onSelect = viewModel::onAgeGroupChange,
        )
        Text(
            "Персонаж выбирается навсегда. Возраст влияет на задания: для 10–11 лет они чуть сложнее.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        FullWidthFinniButton(
            text = "Готово, знакомиться!",
            onClick = viewModel::createProfile,
            enabled = state.canCreate,
        )
    }
}

@Composable
private fun PetPreview(state: CreatePetUiState) {
    // v0.5: живое превью — анимация выбранного персонажа
    PetAnimation(character = state.body)
    Text(
        text = state.petName.ifBlank { "Твой питомец" },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun PickerRow(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(label) },
            )
        }
    }
}
