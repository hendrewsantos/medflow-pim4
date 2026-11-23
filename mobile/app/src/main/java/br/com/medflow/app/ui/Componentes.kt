package br.com.medflow.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Alvos de toque com no mínimo 48dp de altura. */
private val AlturaMinima = 48.dp

@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    descricao: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AlturaMinima)
            .then(if (descricao != null) Modifier.semantics { contentDescription = descricao } else Modifier),
    ) {
        Text(texto)
    }
}

@Composable
fun BotaoSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    descricao: String? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AlturaMinima)
            .then(if (descricao != null) Modifier.semantics { contentDescription = descricao } else Modifier),
    ) {
        Text(texto)
    }
}

@Composable
fun BotaoTexto(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, descricao: String? = null) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AlturaMinima)
            .then(if (descricao != null) Modifier.semantics { contentDescription = descricao } else Modifier),
    ) {
        Text(texto)
    }
}

/** Mensagem anunciada pelo leitor de tela quando aparece (liveRegion). */
@Composable
fun CartaoMensagem(
    texto: String,
    modifier: Modifier = Modifier,
    erro: Boolean = false,
    aoFechar: (() -> Unit)? = null,
) {
    val cores = if (erro) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
    Card(
        colors = cores,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(texto, modifier = Modifier.padding(16.dp))
        if (aoFechar != null) {
            BotaoTexto("Entendi", aoFechar, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
    }
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.semantics { heading() },
    )
}
