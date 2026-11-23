package br.com.medflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores escolhidas para contraste mínimo 4.5:1 entre texto e fundo (WCAG AA).
private val Claro = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF00201C),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1B),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondaryContainer = Color(0xFF1F3B38),
    onSecondaryContainer = Color(0xFFD7F2EE),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE0E3E1),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE0E3E1),
)

@Composable
fun MedFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        content = content,
    )
}
