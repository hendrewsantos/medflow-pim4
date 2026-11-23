package br.com.medflow.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import br.com.medflow.app.R
import br.com.medflow.app.ui.theme.gradienteDoApp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Abertura do aplicativo (cerca de 3 s): o traço do batimento se desenha, a mão sobe, o conjunto dá uma batida e
 * o nome aparece. Toque em qualquer lugar para pular. Com "remover animações" do sistema vira só um esmaecer.
 */
@Composable
fun Abertura(onFim: () -> Unit) {
    val reduzido = movimentoReduzido()
    val traco = remember { Animatable(if (reduzido) 1f else 0f) }
    val mao = remember { Animatable(if (reduzido) 0f else 0f) }
    val batida = remember { Animatable(1f) }
    val nome = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            if (reduzido) {
                launch { mao.animateTo(1f, tween(600)) }
                launch { delay(300); nome.animateTo(1f, tween(600)) }
                delay(1800)
            } else {
                launch { traco.animateTo(1f, tween(1300, easing = LinearEasing)) }
                launch { delay(700); mao.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
                launch { delay(1500); batida.animateTo(1.07f, tween(180)); batida.animateTo(1f, tween(320)) }
                launch { delay(1700); nome.animateTo(1f, tween(500)) }
                delay(3000)
            }
        }
        onFim()
    }

    val tracoCor = Color(0xFF12A865)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradienteDoApp())
            .clickable(onClickLabel = "Pular abertura", onClick = onFim)
            .semantics { contentDescription = "MedFlow Solutions. Toque para pular." },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { scaleX = batida.value; scaleY = batida.value },
            ) {
                Canvas(modifier = Modifier.width(250.dp).height(70.dp).alpha(if (reduzido) mao.value else 1f)) {
                    val l = size.width
                    val a = size.height
                    val caminho = Path().apply {
                        moveTo(0f, a * 0.5f)
                        lineTo(l * 0.36f, a * 0.5f)
                        lineTo(l * 0.43f, a * 0.5f)
                        lineTo(l * 0.48f, a * 0.08f)
                        lineTo(l * 0.54f, a * 0.92f)
                        lineTo(l * 0.60f, a * 0.22f)
                        lineTo(l * 0.64f, a * 0.5f)
                        lineTo(l, a * 0.5f)
                    }
                    val medida = PathMeasure().apply { setPath(caminho, false) }
                    val parcial = Path()
                    medida.getSegment(0f, medida.length * traco.value, parcial, true)
                    drawPath(
                        parcial,
                        color = tracoCor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                Image(
                    painter = painterResource(R.drawable.mao_medflow),
                    contentDescription = null,
                    modifier = Modifier
                        .width(190.dp)
                        .graphicsLayer {
                            alpha = mao.value
                            translationY = (1f - mao.value) * 18.dp.toPx()
                        },
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp).alpha(nome.value),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0B2545))) { append("MedFlow ") }
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = Color(0xFF0B6A8F))) { append("Solutions") }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    "A dose certa, no horário certo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3D546B),
                )
            }
        }
    }
}
