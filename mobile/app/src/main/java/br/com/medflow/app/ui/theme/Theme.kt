package br.com.medflow.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Mesma paleta do site: azul-petróleo e verde-água sobre neutros claros; textos com contraste mínimo 4.5:1 (WCAG AA).
private val Claro = lightColorScheme(
    primary = Color(0xFF0B6A8F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9EAF4),
    onPrimaryContainer = Color(0xFF0B2545),
    secondaryContainer = Color(0xFFE3EDF5),
    onSecondaryContainer = Color(0xFF0B2545),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color.Transparent,       // quem aparece é o degradê do app
    onBackground = Color(0xFF0E1F33),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1F33),
    surfaceVariant = Color(0xFFE3EDF5),
    onSurfaceVariant = Color(0xFF3D546B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF7D93A6),
    outlineVariant = Color(0xFFC3D0D7),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF7FC8E6),
    onPrimary = Color(0xFF00212F),
    primaryContainer = Color(0xFF0B3A52),
    onPrimaryContainer = Color(0xFFD9EAF4),
    secondaryContainer = Color(0xFF1F3B38),
    onSecondaryContainer = Color(0xFFD7F2EE),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color.Transparent,
    onBackground = Color(0xFFE5EDF2),
    surface = Color(0xFF121F2A),
    onSurface = Color(0xFFE5EDF2),
    surfaceVariant = Color(0xFF192A37),
    onSurfaceVariant = Color(0xFFA8B9C5),
    surfaceContainerLowest = Color(0xFF121F2A),
    surfaceContainerLow = Color(0xFF121F2A),
    surfaceContainer = Color(0xFF121F2A),
    surfaceContainerHigh = Color(0xFF192A37),
    surfaceContainerHighest = Color(0xFF192A37),
    outline = Color(0xFF6C8394),
    outlineVariant = Color(0xFF273B4A),
)

/** Degradê do app, como no site: topo verde-água, meio azul, fim branco (no escuro, tons profundos). */
@Composable
fun gradienteDoApp(): Brush =
    if (isSystemInDarkTheme()) {
        Brush.verticalGradient(listOf(Color(0xFF0F3039), Color(0xFF0E2330), Color(0xFF0B141C)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD6F0ED), Color(0xFFD5E6F5), Color(0xFFFFFFFF)))
    }

@Composable
fun MedFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro) {
        Box(modifier = Modifier.fillMaxSize().background(gradienteDoApp())) {
            content()
        }
    }
}
