package ru.finni.app.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ru.finni.app.ui.game.GameViewModel

/** Показывает одноразовые события ViewModel (достижения, награды) в toast на любом экране. */
@Composable
fun FinniEventToast(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var lastEventId by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.eventId) {
        if (state.eventId != lastEventId) {
            lastEventId = state.eventId
            state.event?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
}
