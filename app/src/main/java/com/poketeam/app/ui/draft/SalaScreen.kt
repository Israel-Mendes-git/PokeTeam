package com.poketeam.app.ui.draft

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.poketeam.app.domain.AnaliseTime
import com.poketeam.app.domain.ModoDraft
import com.poketeam.app.ui.pokedex.Sprite
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.ConfirmarExclusao
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.TituloPixel
import com.poketeam.app.ui.times.CorAlerta

@Composable
fun SalaScreen(vm: SalaViewModel, aoSair: () -> Unit, aoSalvar: () -> Unit) {
    val ciclo = LocalLifecycleOwner.current.lifecycle
    // Só consulta o servidor com a tela visível.
    LaunchedEffect(Unit) { ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.acompanhar() } }

    val t by vm.tela.collectAsStateWithLifecycle()
    val erro by vm.erro.collectAsStateWithLifecycle()
    val encerrada by vm.encerrada.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, end = 12.dp)) {
            IconButton(onClick = aoSair) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            Text("Sala ${vm.codigo}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        if (erro != null) Text(erro!!, color = CorAlerta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))

        val tela = t
        when {
            encerrada -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Essa sala foi encerrada.", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = aoSair) { Text("Voltar") }
            }
            tela == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            tela.sala.emEspera -> Espera(tela, vm, aoSair)
            tela.sala.terminou -> Fim(tela, vm, aoSair, aoSalvar)
            else -> Andamento(tela, vm, aoSair)
        }
    }
}

@Composable
private fun Espera(t: TelaSala, vm: SalaViewModel, aoSair: () -> Unit) {
    val s = t.sala
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val enviando by vm.enviando.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("CÓDIGO DA SALA", style = MaterialTheme.typography.labelMedium, color = p.sub)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.codigo, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = p.destaque, letterSpacing = 6.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val texto = "Entra no meu draft do PokeTeam! Aba Draft → Entrar com código: ${s.codigo}"
                contexto.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Convidar"))
            }) { Icon(Icons.Default.Share, "Convidar") }
        }
        Text("Seus amigos veem a sala na aba Draft, ou entram com o código.", style = MaterialTheme.typography.bodySmall, color = p.sub)

        Text("JOGADORES (${s.jogadores.size}/4)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
        s.jogadores.forEachIndexed { i, j ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(Modifier.size(14.dp).background(CoresJogador[i]))
                Text("  ${j.apelido}" + (if (j.id == s.dono) " · anfitrião" else "") + (if (i == t.eu) " (você)" else ""),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text("REGRAS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
        Text(
            listOf(
                "${s.porJogador} por jogador",
                if (s.serpente) "ordem serpente" else "ordem fixa",
                if (s.modo == ModoDraft.OPCOES) "3 opções sorteadas" else "livre",
                if (s.soFinais) "só evolução final" else null,
                "pool: ${s.pool.nome}",
            ).filterNotNull().joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = p.sub,
        )

        Spacer(Modifier.height(24.dp))
        if (t.souDono) {
            BotaoPixel(onClick = vm::iniciar, enabled = s.jogadores.size >= 2 && !enviando, modifier = Modifier.fillMaxWidth()) {
                Text(if (s.jogadores.size < 2) "Esperando amigos…" else "Começar draft", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Esperando ${s.jogadores.firstOrNull { it.id == s.dono }?.apelido ?: "o anfitrião"} começar…")
            }
        }
        TextButton(onClick = { vm.sair(aoSair) }, modifier = Modifier.padding(top = 8.dp)) {
            Text(if (t.souDono) "Encerrar sala" else "Sair da sala")
        }
    }
}

@Composable
private fun Andamento(t: TelaSala, vm: SalaViewModel, aoSair: () -> Unit) {
    val d = t.tela.draft
    val vez = d.vez ?: return
    val cor = CoresJogador[vez]
    val enviando by vm.enviando.collectAsStateWithLifecycle()
    val busca by vm.busca.collectAsStateWithLifecycle()
    val toque = LocalHapticFeedback.current
    LaunchedEffect(t.minhaVez, d.escolhas.size) { if (t.minhaVez) toque.performHapticFeedback(HapticFeedbackType.LongPress) }
    var usarLista by remember(d.escolhas.size) { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) ConfirmarExclusao("Encerrar a sala?", "O draft acaba para todo mundo.", acao = "Encerrar",
        aoConfirmar = { vm.sair(aoSair) }) { confirmar = false }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp).background(cor.copy(alpha = .18f), RectangleShape)
                .border(2.dp, cor, RectangleShape).padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(if (t.minhaVez) "Sua vez!" else "Vez de ${d.jogadores[vez]}",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = cor)
                Text("${d.escolhas.size + 1}ª escolha de ${d.total} · ${t.tela.totalDisponiveis} disponíveis",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (t.souDono) {
                IconButton(onClick = vm::desfazer, enabled = d.escolhas.isNotEmpty() && !enviando) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Desfazer última escolha")
                }
                TextButton(onClick = { confirmar = true }) { Text("Encerrar") }
            }
        }

        ResumoEscolhas(t.tela, destaque = vez)

        if (!t.minhaVez) {
            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("  Esperando ${d.jogadores[vez]} escolher…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }
        if (d.modo == ModoDraft.OPCOES && !usarLista) {
            Opcoes(t.tela.opcoes, cor) { if (!enviando) vm.escolher(it) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::outrasOpcoes, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Outras opções")
                }
                OutlinedButton(onClick = { usarLista = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp)); Text("Buscar")
                }
            }
        } else {
            ListaBusca(t.tela.lista, busca, vm::buscar, aoEscolher = { if (!enviando) vm.escolher(it) },
                voltarParaOpcoes = if (d.modo == ModoDraft.OPCOES) ({ usarLista = false }) else null)
        }
    }
}

@Composable
private fun Fim(t: TelaSala, vm: SalaViewModel, aoSair: () -> Unit, aoSalvar: () -> Unit) {
    val d = t.tela.draft
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TituloPixel("Draft encerrado!", Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp))
        d.jogadores.forEachIndexed { j, nome ->
            val time = t.tela.escolhasPorJogador[j]
            val problemas = AnaliseTime.de(time).problemas
            Column(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .border(1.5.dp, CoresJogador[j], RectangleShape).padding(12.dp)
            ) {
                Text(nome + if (j == t.eu) " (você)" else "", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = CoresJogador[j])
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    time.forEach { Sprite(it, 48) }
                }
                Text(time.joinToString(", ") { it.nomeCompleto }, style = MaterialTheme.typography.bodySmall)
                if (problemas.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Warning, null, tint = CorAlerta, modifier = Modifier.size(16.dp))
                    Text(" Fraco a " + problemas.joinToString(", ") { it.tipo.nome }, style = MaterialTheme.typography.bodySmall, color = CorAlerta)
                }
            }
        }
        if (t.eu != null) BotaoPixel(onClick = { vm.salvar(todos = false, depois = aoSalvar) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Salvar meu time", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { vm.salvar(todos = true, depois = aoSalvar) }, modifier = Modifier.fillMaxWidth()) {
            Text("Salvar os times de todo mundo")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (t.souDono) TextButton(onClick = vm::desfazer) { Text("Desfazer última escolha") }
            TextButton(onClick = { vm.sair(aoSair) }) { Text(if (t.souDono) "Encerrar sala" else "Sair") }
        }
        Text("Os times vão para Meus times como \"Draft · nome\".", textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    }
}
