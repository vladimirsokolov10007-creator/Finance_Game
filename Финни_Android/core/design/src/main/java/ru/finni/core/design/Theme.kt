package ru.finni.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ТЗ п. 3.6: основной текст ≥16 sp, крупные элементы.
val FinniTypography = Typography(
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFFF8C42),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1C7),
    onPrimaryContainer = Color(0xFF5A2D00),
    secondary = Color(0xFF4CAF7D),
    onSecondary = Color.White,
    tertiary = Color(0xFF4A90D9),
    background = Color(0xFFFDF6EC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3E9DA),
    error = Color(0xFFE05A5A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB27D),
    secondary = Color(0xFF7FD6AC),
    tertiary = Color(0xFF9CC7F5),
    background = Color(0xFF211D19),
    surface = Color(0xFF2C2721),
    surfaceVariant = Color(0xFF3A332B),
    error = Color(0xFFF08A8A),
)

private val FinniShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FinniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = FinniTypography,
        shapes = FinniShapes,
        content = content,
    )
}
