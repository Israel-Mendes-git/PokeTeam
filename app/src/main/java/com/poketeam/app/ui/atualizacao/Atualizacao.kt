package com.poketeam.app.ui.atualizacao

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.data.Atualizador
import com.poketeam.app.data.EstadoAtualizacao
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.CaixaPixel
import com.poketeam.app.ui.tema.LocalPaleta
import kotlinx.coroutines.launch

/** Aviso de versão nova (aparece por cima de qualquer tela). */
@Composable
fun DialogoAtualizacao(atualizador: Atualizador) {
    val estado by atualizador.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    when (val e = estado) {
        is EstadoAtualizacao.Disponivel -> AlertDialog(
            onDismissRequest = atualizador::dispensar,
            title = { Text("Versão ${e.novidade.versao} disponível") },
            text = {
                Column {
                    Text("Você está na ${atualizador.versaoAtual}.", color = LocalPaleta.current.sub)
                    if (e.novidade.notas.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(e.novidade.notas, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
                    }
                    if (!atualizador.podeInstalar()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Na primeira vez o Android pede para liberar \"instalar apps desconhecidos\" para o PokeTeam. Libere e toque em Atualizar de novo.",
                            style = MaterialTheme.typography.bodySmall, color = LocalPaleta.current.sub)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { escopo.launch { atualizador.baixarEInstalar(e.novidade) } }) { Text("Atualizar") } },
            dismissButton = { TextButton(onClick = atualizador::dispensar) { Text("Depois") } },
        )
        is EstadoAtualizacao.Baixando -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Baixando ${e.novidade.versao}…") },
            text = { LinearProgressIndicator(progress = { e.progresso }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        else -> Unit
    }
}

/** Bloco dos Ajustes: versão atual e "Procurar atualização". */
@Composable
fun CaixaAtualizacao(atualizador: Atualizador, modifier: Modifier = Modifier) {
    val estado by atualizador.estado.collectAsStateWithLifecycle()
    val escopo = rememberCoroutineScope()
    val p = LocalPaleta.current
    CaixaPixel(modifier.fillMaxWidth()) {
        Column {
            Text("VERSÃO ${atualizador.versaoAtual}", style = MaterialTheme.typography.titleSmall)
            val linha = when (val e = estado) {
                EstadoAtualizacao.Checando -> "Procurando…"
                EstadoAtualizacao.EmDia -> "Você está na versão mais nova."
                is EstadoAtualizacao.Erro -> e.mensagem
                is EstadoAtualizacao.Disponivel -> "Versão ${e.novidade.versao} disponível."
                is EstadoAtualizacao.Baixando -> "Baixando…"
                EstadoAtualizacao.Parado ->
                    if (atualizador.configurado) "O app confere sozinho sempre que você abre."
                    else "Atualização automática desligada neste build."
            }
            Text(linha, style = MaterialTheme.typography.bodySmall,
                color = if (estado is EstadoAtualizacao.Erro) p.alerta else p.sub,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            BotaoPixel(
                onClick = { escopo.launch { atualizador.checar() } },
                enabled = atualizador.configurado && estado !is EstadoAtualizacao.Checando && estado !is EstadoAtualizacao.Baixando,
            ) { Text("PROCURAR ATUALIZAÇÃO", style = MaterialTheme.typography.titleSmall) }
        }
    }
}
