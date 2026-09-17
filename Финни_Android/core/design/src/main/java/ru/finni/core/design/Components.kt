package ru.finni.core.design

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ТЗ п. 3.6: интерактивные элементы ≥48×48 dp.
private val MinTouch = 52.dp

@Composable
fun FinniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MinTouch),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text)
    }
}

@Composable
fun FinniSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MinTouch),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(text)
    }
}

@Composable
fun FullWidthFinniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FinniButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
    )
}
