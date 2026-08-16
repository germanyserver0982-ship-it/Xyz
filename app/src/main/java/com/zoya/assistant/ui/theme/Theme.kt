package com.zoya.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ZoyaColorScheme = lightColorScheme(
    primary = ZoyaViolet,
    secondary = ZoyaPink,
    tertiary = ZoyaCyan,
    background = ZoyaBackground,
    surface = ZoyaSurface,
    onBackground = ZoyaTextPrimary,
    onSurface = ZoyaTextPrimary
)

@Composable
fun ZoyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ZoyaColorScheme,
        typography = MaterialTheme.typography.copy(
            headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp)
        ),
        content = content
    )
}
