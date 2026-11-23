package br.com.medflow.app.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Respeita a configuração do sistema "Remover animações" (escala de animação zero):
 * nesse caso as telas aparecem sem deslocamento e a abertura vira só um esmaecer.
 */
@Composable
fun movimentoReduzido(): Boolean {
    val contexto = LocalContext.current
    return remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Entrada suave de um item de lista: sobe alguns pixels enquanto aparece, em sequência pelo índice. */
@Composable
fun Modifier.entradaSuave(indice: Int = 0): Modifier {
    val reduzido = movimentoReduzido()
    val progresso = remember { Animatable(if (reduzido) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduzido) {
            delay(indice.coerceIn(0, 8) * 60L)
            progresso.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
        }
    }
    return this.graphicsLayer {
        alpha = progresso.value
        translationY = (1f - progresso.value) * 22.dp.toPx()
    }
}
