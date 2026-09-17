package ru.finni.app.ui.plan

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.finni.app.ui.game.GameViewModel
import ru.finni.core.design.FullWidthFinniButton

/** Экран 5 сценария: план бюджета на период (3 направления, контроль суммы). */
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.profile ?: return
    val period = state.activePeriod

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("План бюджета", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (period != null) {
            // План подтверждён: показываем сравнение с фактом (ТЗ п. 2.5.5)
            Text("План на период ${period.index} подтверждён. Сравнение с фактом:")
            Spacer(Modifier.height(8.dp))
            PlanFactTable(
                planM = period.planMandatory, planO = period.planOptional, planS = period.planSavings,
                factM = period.factMandatory, factO = period.factOptional, factS = period.factSavings,
            )
            Text(
                "План нельзя изменить после подтверждения — но следующий период ты составишь ещё лучше!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            FullWidthFinniButton(text = "Назад", onClick = onBack)
            return@Column
        }

        Text("Распредели ${profile.balance} монет по трём направлениям. Сумма не должна превышать баланс.")
        Spacer(Modifier.height(12.dp))

        var m by rememberSaveable { mutableIntStateOf(0) }
        var o by rememberSaveable { mutableIntStateOf(0) }
        var s by rememberSaveable { mutableIntStateOf(0) }
        val sum = m + o + s
        val over = sum > profile.balance

        PlanSlider("🍖 Обязательные расходы", "Еда и уход для питомца", m, profile.balance) { m = it }
        PlanSlider("🎾 Необязательные расходы", "Игрушки и украшения", o, profile.balance) { o = it }
        PlanSlider("🏦 Накопления", "На финансовую цель", s, profile.balance) { s = it }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (over) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Итого плана: $sum из ${profile.balance} · Остаток: ${profile.balance - sum}" +
                        if (over) "\n⚠️ Сумма превышает бюджет — уменьши одно из направлений." else "",
                modifier = Modifier.padding(14.dp),
                color = if (over) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        FullWidthFinniButton(
            text = "Подтвердить план",
            enabled = !over,
            onClick = { viewModel.confirmPlan(m, o, s) },
        )
        Spacer(Modifier.height(8.dp))
        FullWidthFinniButton(text = "Назад", onClick = onBack)
    }
}

@Composable
private fun PlanSlider(
    title: String,
    hint: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("$value 🪙", style = MaterialTheme.typography.titleMedium)
        }
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..max.toFloat(),
            steps = max - 1,
        )
    }
}

@Composable
fun PlanFactTable(
    planM: Int, planO: Int, planS: Int,
    factM: Int, factO: Int, factS: Int,
) {
    fun within(plan: Int, fact: Int): Boolean {
        if (plan == 0 && fact == 0) return true
        return kotlin.math.abs(fact - plan) <= maxOf(plan * 0.2, 2.0)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Направление", modifier = Modifier.weight(1f))
                Text("План", textAlign = TextAlign.End, modifier = Modifier.weight(0.4f))
                Text("Факт", textAlign = TextAlign.End, modifier = Modifier.weight(0.4f))
                Text("✓", textAlign = TextAlign.End, modifier = Modifier.weight(0.3f))
            }
            PvfRow("🍖 Обязательные", planM, factM, within(planM, factM))
            PvfRow("🎾 Необязательные", planO, factO, within(planO, factO))
            PvfRow("🏦 Накопления", planS, factS, within(planS, factS))
        }
    }
}

@Composable
private fun PvfRow(name: String, plan: Int, fact: Int, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(name, modifier = Modifier.weight(1f))
        Text("$plan", textAlign = TextAlign.End, modifier = Modifier.weight(0.4f))
        Text("$fact", textAlign = TextAlign.End, modifier = Modifier.weight(0.4f))
        Text(
            if (ok) "✓" else "±${kotlin.math.abs(fact - plan)}",
            textAlign = TextAlign.End,
            color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(0.5f),
        )
    }
}
