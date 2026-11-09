package com.poketeam.app.ui.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.domain.AnaliseTime
import com.poketeam.app.domain.Draft
import com.poketeam.app.domain.ModoDraft
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.ui.pokedex.LinhaPokemon
import com.poketeam.app.ui.pokedex.Sprite
import com.poketeam.app.ui.pokedex.SpriteAnimado
import com.poketeam.app.ui.tema.SeloTipo
import com.poketeam.app.ui.times.CorAlerta
import com.poketeam.app.ui.tema.ConfirmarExclusao
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.TituloPixel

/** Uma cor por jogador, para saber de quem é a vez de longe. */
internal val CoresJogador = listOf(Color(0xFFFF5A4E), Color(0xFF4EA8FF), Color(0xFF7BC86C), Color(0xFFFFC94D))

@Composable
fun DraftScreen(vm: DraftViewModel, aoSalvar: () -> Unit, aoAbrirSala: (String) -> Unit) {
    val carga by vm.tela.collectAsStateWithLifecycle()
    when (val c = carga) {
        Carga.Carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        Carga.SemDraft -> Configuracao(vm, aoAbrirSala)
        is Carga.EmAndamento ->
            if (c.tela.draft.terminou) Resultado(c.tela, vm, aoSalvar) else Vez(c.tela, vm)
    }
}

// ---------- Configuração ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Configuracao(vm: DraftViewModel, aoAbrirSala: (String) -> Unit) {
    val c by vm.config.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TituloPixel("Draft")
        Text("Cada um escolhe na sua vez, e quem pega primeiro leva.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (vm.onlineDisponivel) SecaoOnline(vm, aoAbrirSala)

        Titulo("Regras")
        RegrasDraft(c, vm)

        Titulo("No mesmo celular")
        Text("Passem o celular de mão em mão.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        c.jogadores.forEachIndexed { i, nome ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Box(Modifier.size(14.dp).background(CoresJogador[i]))
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(value = nome, onValueChange = { vm.nomear(i, it) }, singleLine = true, modifier = Modifier.weight(1f))
            }
        }
        Row {
            TextButton(onClick = vm::adicionarJogador, enabled = c.jogadores.size < Draft.MAX_JOGADORES) {
                Icon(Icons.Default.Add, null); Text(" Jogador")
            }
            TextButton(onClick = vm::removerJogador, enabled = c.jogadores.size > Draft.MIN_JOGADORES) {
                Icon(Icons.Default.Remove, null); Text(" Jogador")
            }
        }

        BotaoPixel(onClick = vm::iniciar, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Começar draft aqui", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegrasDraft(c: com.poketeam.app.domain.ConfigDraft, vm: DraftViewModel) {
    Column {
        Subtitulo("Pokémons por jogador")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..6).forEach { n -> FilterChip(selected = c.porJogador == n, onClick = { vm.mudarPorJogador(n) }, label = { Text("$n") }) }
        }

        Subtitulo("Ordem")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = c.serpente, onClick = { if (!c.serpente) vm.alternarSerpente() }, label = { Text("Serpente (1-2, 2-1)") })
            FilterChip(selected = !c.serpente, onClick = { if (c.serpente) vm.alternarSerpente() }, label = { Text("Fixa (1-2, 1-2)") })
        }

        Subtitulo("Como escolher")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = c.modo == ModoDraft.OPCOES, onClick = { vm.mudarModo(ModoDraft.OPCOES) }, label = { Text("3 opções sorteadas") })
            FilterChip(selected = c.modo == ModoDraft.LIVRE, onClick = { vm.mudarModo(ModoDraft.LIVRE) }, label = { Text("Livre (busca)") })
        }
        Text(
            if (c.modo == ModoDraft.OPCOES) "Mais rápido: cada vez aparecem 3 pokémons, dá para pedir outras ou buscar."
            else "Cada um busca o que quiser no pool.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        FilterChip(selected = c.soFinais, onClick = vm::alternarFinais, label = { Text("Só evolução final") },
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SecaoOnline(vm: DraftViewModel, aoAbrirSala: (String) -> Unit) {
    val logado by vm.logado.collectAsStateWithLifecycle()
    val salas by vm.salas.collectAsStateWithLifecycle()
    val erro by vm.erroOnline.collectAsStateWithLifecycle()
    val ocupado by vm.ocupadoOnline.collectAsStateWithLifecycle()
    LaunchedEffect(logado) { if (logado) vm.atualizarSalas() }

    Titulo("Online, cada um no seu celular")
    if (!logado) {
        Text("Entre com sua conta na aba Amigos para jogar online.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var codigo by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotaoPixel(onClick = { vm.criarSala(aoAbrirSala) }, enabled = !ocupado, modifier = Modifier.weight(1f)) {
            Text("Criar sala", style = MaterialTheme.typography.titleSmall)
        }
        OutlinedTextField(
            value = codigo, onValueChange = { codigo = com.poketeam.app.data.DraftOnline.normalizarCodigo(it).take(8) },
            singleLine = true, placeholder = { Text("Código") }, shape = RectangleShape, modifier = Modifier.width(110.dp),
        )
        TextButton(onClick = { vm.entrarNaSala(codigo, aoAbrirSala) }, enabled = codigo.length >= 4 && !ocupado) { Text("Entrar") }
    }
    if (erro != null) Text(erro!!, color = CorAlerta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    val s = salas
    if (s != null && s.abertas.isNotEmpty()) {
        Subtitulo("Salas de amigos esperando gente")
        s.abertas.forEach { r -> LinhaSala(r, "Entrar") { vm.entrarNaSala(r.codigo, aoAbrirSala) } }
    }
    if (s != null && s.minhas.isNotEmpty()) {
        Subtitulo("Suas salas")
        s.minhas.forEach { r -> LinhaSala(r, "Abrir") { aoAbrirSala(r.codigo) } }
    }
    Text("A sala usa as regras abaixo e o pool ativo.", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun LinhaSala(r: com.poketeam.app.data.ResumoSala, acao: String, aoClicar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = aoClicar).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(r.jogadores.joinToString(", "), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Sala ${r.codigo} · " + when (r.estado) { "espera" -> "esperando"; "fim" -> "terminou"; else -> "em andamento" } +
                " · ${r.porJogador} por jogador", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(acao.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Subtitulo(t: String) {
    Text(t, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
}

// ---------- Vez de alguém ----------

@Composable
private fun Vez(t: TelaDraft, vm: DraftViewModel) {
    val d = t.draft
    val vez = d.vez ?: return
    val cor = CoresJogador[vez]
    // "Escolher da lista" no modo opções vale só para esta vez.
    var usarLista by remember(d.escolhas.size) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) ConfirmarExclusao("Encerrar o draft?", "As escolhas feitas até agora se perdem.", acao = "Encerrar",
        aoConfirmar = vm::encerrar) { confirmar = false }
    val minhas = d.escolhasDe(vez).size

    Column(Modifier.fillMaxSize()) {
        // Cabeçalho da vez
        Row(
            Modifier.fillMaxWidth().padding(12.dp).background(cor.copy(alpha = .18f), RectangleShape)
                .border(2.dp, cor, RectangleShape).padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Vez de ${d.jogadores[vez]}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = cor)
                Text("Escolha ${minhas + 1} de ${d.porJogador} · ${d.escolhas.size + 1}ª do draft · ${t.totalDisponiveis} disponíveis",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = vm::desfazer, enabled = d.escolhas.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Desfazer última escolha") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Encerrar draft") }, onClick = { menu = false; confirmar = true })
                }
            }
        }

        ResumoEscolhas(t, destaque = vez)

        if (d.modo == ModoDraft.OPCOES && !usarLista) {
            Opcoes(t.opcoes, cor, vm::escolher)
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = vm::outrasOpcoes, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Outras opções")
                }
                OutlinedButton(onClick = { usarLista = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, null); Spacer(Modifier.width(6.dp)); Text("Buscar")
                }
            }
        } else {
            ListaBusca(t, vm, voltarParaOpcoes = if (d.modo == ModoDraft.OPCOES) ({ usarLista = false }) else null)
        }
    }
}

@Composable
internal fun ResumoEscolhas(t: TelaDraft, destaque: Int?) {
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        t.draft.jogadores.forEachIndexed { j, nome ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(CoresJogador[j]))
                Text(" $nome", style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (j == destaque) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(96.dp))
                t.escolhasPorJogador[j].forEach { Box(Modifier.padding(end = 2.dp)) { Sprite(it, 34) } }
                repeat(t.draft.porJogador - t.escolhasPorJogador[j].size) {
                    Box(Modifier.padding(end = 2.dp).size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f)))
                }
            }
        }
    }
}

@Composable
internal fun Opcoes(opcoes: List<Pokemon>, cor: Color, escolherSemToque: (Pokemon) -> Unit) {
    val toque = LocalHapticFeedback.current
    val escolher: (Pokemon) -> Unit = { toque.performHapticFeedback(HapticFeedbackType.LongPress); escolherSemToque(it) }
    if (opcoes.isEmpty()) {
        Text("Não sobrou ninguém no pool para sortear. Use \"Buscar\" ou encerre o draft.",
            color = CorAlerta, modifier = Modifier.padding(16.dp))
        return
    }
    Text("Toque para escolher", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp))
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { p ->
            Column(
                Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .border(1.5.dp, cor.copy(alpha = .5f), RectangleShape)
                    .clickable { escolher(p) }.padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpriteAnimado(p, 88)
                Text(p.nome, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                if (p.forma != null) Text(p.forma, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
                    p.tipos.forEach { SeloTipo(it, compacto = true) }
                }
                Text("Total ${p.total}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ListaBusca(t: TelaDraft, vm: DraftViewModel, voltarParaOpcoes: (() -> Unit)?) {
    val busca by vm.busca.collectAsStateWithLifecycle()
    ListaBusca(t.lista, busca, vm::buscar, vm::escolher, voltarParaOpcoes)
}

@Composable
internal fun ListaBusca(
    lista: List<Pokemon>,
    busca: String,
    aoBuscar: (String) -> Unit,
    aoEscolher: (Pokemon) -> Unit,
    voltarParaOpcoes: (() -> Unit)?,
) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = busca, onValueChange = aoBuscar, singleLine = true,
            placeholder = { Text("Buscar no pool") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (busca.isNotEmpty()) IconButton(onClick = { aoBuscar("") }) { Icon(Icons.Default.Close, "Limpar") } },
            shape = RectangleShape, modifier = Modifier.weight(1f),
        )
        if (voltarParaOpcoes != null) IconButton(onClick = { aoBuscar(""); voltarParaOpcoes() }) { Icon(Icons.Default.List, "Voltar às opções") }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(lista, key = Pokemon::id) { p -> LinhaPokemon(p, false, Icons.Default.CheckCircle, "") { aoEscolher(p) } }
    }
}

// ---------- Resultado ----------

@Composable
private fun Resultado(t: TelaDraft, vm: DraftViewModel, aoSalvar: () -> Unit) {
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) ConfirmarExclusao("Descartar o draft?", "Os times escolhidos não vão para Meus times.", acao = "Descartar",
        aoConfirmar = vm::encerrar) { confirmar = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        TituloPixel("Draft encerrado!", Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp))
        t.draft.jogadores.forEachIndexed { j, nome ->
            val time = t.escolhasPorJogador[j]
            val problemas = AnaliseTime.de(time).problemas
            Column(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .border(1.5.dp, CoresJogador[j], RectangleShape).padding(12.dp)
            ) {
                Text(nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CoresJogador[j])
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
        BotaoPixel(onClick = { vm.salvarComoTimes(aoSalvar) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Salvar em Meus times", style = MaterialTheme.typography.titleMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = vm::desfazer) { Text("Desfazer última escolha") }
            TextButton(onClick = { confirmar = true }) { Text("Descartar") }
        }
        Text("Salvar cria um time \"Draft · nome\" para cada jogador.", textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Titulo(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
}
