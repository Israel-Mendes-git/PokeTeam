package com.poketeam.app.ui.times

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import android.content.Intent
import com.poketeam.app.domain.Sugestao
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.domain.AnaliseTime
import com.poketeam.app.domain.LinhaDefesa
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Time
import com.poketeam.app.ui.pokedex.BarraStat
import com.poketeam.app.ui.pokedex.Sprite
import com.poketeam.app.ui.pokedex.SpriteAnimado
import com.poketeam.app.ui.tema.SeloTipo
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.bordaPixel
import com.poketeam.app.domain.Build
import com.poketeam.app.ui.builds.CartaoBuild
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.rememberModalBottomSheetState
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.CaixaPixel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

private val CorBoa: Color @Composable get() = LocalPaleta.current.bom

@Composable
fun EditorTimeScreen(
    vm: EditorTimeViewModel,
    nomePool: String,
    aoAdicionar: () -> Unit,
    aoVerFicha: (Pokemon) -> Unit,
    aoSair: () -> Unit,
) {
    val estado by vm.time.collectAsStateWithLifecycle()
    val sair = { vm.descartarSeVazio(); aoSair() }
    BackHandler(onBack = sair)

    val t = estado
    if (t == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Nome editado localmente para o cursor não pular a cada gravação.
    var nome by remember { mutableStateOf(t.time.nome) }
    LaunchedEffect(t.time.id) { nome = t.time.nome }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp, top = 4.dp)) {
            IconButton(onClick = sair) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            TextField(
                value = nome,
                onValueChange = { nome = it; vm.renomear(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            val contexto = LocalContext.current
            IconButton(
                onClick = {
                    vm.textoParaCompartilhar()?.let { texto ->
                        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
                        contexto.startActivity(Intent.createChooser(envio, "Compartilhar time"))
                    }
                },
                enabled = t.pokemons.isNotEmpty(),
            ) { Icon(Icons.Default.Share, "Compartilhar time") }
        }

        val podeDesfazer by vm.podeDesfazer.collectAsStateWithLifecycle()
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (podeDesfazer) "Time limpo" else "${t.pokemons.size} de ${Time.MAXIMO}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            if (podeDesfazer) {
                TextButton(onClick = { vm.desfazerLimpar() }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("DESFAZER")
                }
            } else if (t.pokemons.isNotEmpty()) {
                TextButton(onClick = { vm.limpar() }) {
                    Icon(Icons.Default.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("LIMPAR TIME")
                }
            }
        }

        val megas by vm.megasDoTime.collectAsStateWithLifecycle()
        var trocando by remember { mutableStateOf<Int?>(null) }
        trocando?.let { i -> t.pokemons.getOrNull(i)?.let { p -> PainelTroca(vm, i, p) { trocando = null } } }
        // 6 vagas em 3 colunas, dentro da caixa pixel
        CaixaPixel(Modifier.padding(12.dp).fillMaxWidth(), preenchimento = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until Time.MAXIMO).chunked(3).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    linha.forEach { i ->
                        val p = t.pokemons.getOrNull(i)
                        Box(Modifier.weight(1f)) {
                            if (p != null) VagaOcupada(
                                t.emBatalha[i], megas = megas[p.id].orEmpty(), megaAtiva = t.megaAtiva?.id,
                                aoVerFicha = { aoVerFicha(p) }, aoRemover = { vm.remover(i) }, aoMega = vm::alternarMega,
                                aoTrocar = { trocando = i },
                            )
                            else VagaVazia(ativa = i == t.pokemons.size, aoClicar = aoAdicionar)
                        }
                    }
                }
            }
        }
        }

        if (t.pokemons.isNotEmpty()) PontosFracos(t.analise)

        val logado by vm.logado.collectAsStateWithLifecycle()
        if (logado) {
            val compartilhado by vm.compartilhado.collectAsStateWithLifecycle()
            val erro by vm.erroCompartilhar.collectAsStateWithLifecycle()
            Row(
                Modifier.fillMaxWidth().clickable { vm.alternarCompartilhar() }.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Compartilhar com amigos", style = MaterialTheme.typography.bodyLarge)
                    Text(erro ?: if (compartilhado) "Seus amigos veem este time e as mudanças." else "Só você vê este time.",
                        color = if (erro != null) CorAlerta else LocalPaleta.current.sub, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = compartilhado, onCheckedChange = { vm.alternarCompartilhar() })
            }
        }

        BarraSorteio(vm, cheio = t.time.cheio, nomePool = nomePool)

        val sugestoes by vm.sugestoes.collectAsStateWithLifecycle()
        if (sugestoes.isNotEmpty()) FaixaSugestoes(sugestoes, temProblemas = t.analise.problemas.isNotEmpty(), aoEscolherSemToque = vm::adicionar)

        val buildsDoTime by vm.buildsDoTime.collectAsStateWithLifecycle()
        val padrao by vm.buildPadrao.collectAsStateWithLifecycle()
        if (t.pokemons.isNotEmpty()) BuildsDoTime(t, buildsDoTime, padrao, vm::escolherBuild)

        if (t.pokemons.isNotEmpty()) Detalhes(t.analise)
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraSorteio(vm: EditorTimeViewModel, cheio: Boolean, nomePool: String) {
    val sorteando by vm.sorteando.collectAsStateWithLifecycle()
    val aviso by vm.aviso.collectAsStateWithLifecycle()
    val regras by vm.regras.collectAsStateWithLifecycle()
    var mostrarRegras by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BotaoPixel(
                onClick = { vm.avisoVisto(); vm.recomendar() },
                enabled = !cheio && !sorteando,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(6.dp))
                Text(if (cheio) "COMPLETO" else "RECOMENDAR", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(6.dp))
            BotaoPixel(
                onClick = { vm.avisoVisto(); vm.sortear() },
                enabled = !cheio && !sorteando,
                modifier = Modifier.weight(1f),
                cor = LocalPaleta.current.painel,
                corTexto = LocalPaleta.current.texto,
            ) {
                Icon(Icons.Default.Casino, null)
                Spacer(Modifier.width(6.dp))
                Text("SORTEAR", style = MaterialTheme.typography.titleSmall)
            }
            BadgedBox(badge = { if (regras.ativas > 0) Badge { Text("${regras.ativas}") } }) {
                IconButton(onClick = { mostrarRegras = true }) { Icon(Icons.Default.Tune, "Configurar sortear e recomendar") }
            }
        }
        Text(
            if (cheio) "Tire alguém do time para trocar essa vaga."
            else "Recomendar escolhe o que fecha os pontos fracos; Sortear é na sorte. Ajuste os dois no botão ao lado. Pool: $nomePool.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        aviso?.let {
            Text(it, color = CorAlerta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
    }

    if (mostrarRegras) PainelConfigSorteio(vm) { mostrarRegras = false }
}

@Composable
private fun LinhaRegra(titulo: String, descricao: String, ligado: Boolean, alternar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = alternar).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(descricao, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = ligado, onCheckedChange = { alternar() })
    }
}

/** Build escolhida de cada membro (a primeira do Smogon por padrão); tocar abre as outras. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildsDoTime(t: TimeResolvido, builds: Map<Int, List<Build>>, padrao: Map<Int, Int>, aoEscolher: (Int, Int) -> Unit) {
    val indice = { id: Int -> t.time.builds[id] ?: padrao[id] ?: 0 }
    var aberto by remember { mutableStateOf<Pokemon?>(null) }
    val paleta = LocalPaleta.current
    Text("BUILDS DO TIME", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp))
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        t.pokemons.forEach { p ->
            val lista = builds[p.id].orEmpty()
            val b = lista.getOrNull(indice(p.id))
            Row(
                Modifier.fillMaxWidth().background(paleta.painel).clickable(enabled = lista.isNotEmpty()) { aberto = p }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sprite(p, 44)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(p.nome, style = MaterialTheme.typography.titleSmall)
                    if (b == null) Text("Sem build no Smogon", color = paleta.sub, style = MaterialTheme.typography.bodySmall)
                    else {
                        Text("${b.nome} · ${b.formato}", color = paleta.sub, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(b.golpes.joinToString(" · ") { it.first() }, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                if (lista.size > 1) Text("${lista.size} ▸", color = paleta.acento, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    aberto?.let { p ->
        val lista = builds[p.id].orEmpty()
        ModalBottomSheet(onDismissRequest = { aberto = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("BUILDS DE ${p.nome.uppercase()}", style = MaterialTheme.typography.titleMedium)
                lista.forEachIndexed { i, b ->
                    CartaoBuild(b, escolhida = i == indice(p.id)) { aoEscolher(p.id, i); aberto = null }
                }
            }
        }
    }
}

@Composable
private fun FaixaSugestoes(sugestoes: List<Sugestao>, temProblemas: Boolean, aoEscolherSemToque: (Pokemon) -> Unit) {
    val toque = LocalHapticFeedback.current
    val aoEscolher: (Pokemon) -> Unit = { toque.performHapticFeedback(HapticFeedbackType.LongPress); aoEscolherSemToque(it) }
    Text(
        if (temProblemas) "Quem fecha os pontos fracos" else "Quem combina com esse time",
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp),
    )
    Text("Do pool ativo · toque para pôr no time", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sugestoes.forEach { s ->
            val p = s.pokemon
            Column(
                Modifier.width(140.dp).background(MaterialTheme.colorScheme.surface, RectangleShape)
                    .clickable { aoEscolher(p) }.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Sprite(p, 56)
                Text(p.nome, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (p.forma != null) Text(p.forma, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                    p.tipos.forEach { SeloTipo(it, compacto = true) }
                }
                if (s.resolve.isNotEmpty()) Text("Fecha " + s.resolve.joinToString(", ") { it.nome },
                    style = MaterialTheme.typography.labelSmall, color = CorBoa, textAlign = TextAlign.Center)
                if (s.cria.isNotEmpty()) Text("Abre " + s.cria.joinToString(", ") { it.nome },
                    style = MaterialTheme.typography.labelSmall, color = CorAlerta, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun VagaOcupada(
    p: Pokemon,
    megas: List<Pokemon>,
    megaAtiva: Int?,
    aoVerFicha: () -> Unit,
    aoRemover: () -> Unit,
    aoMega: (Int) -> Unit,
    aoTrocar: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val paleta = LocalPaleta.current
    Column(
        Modifier
            .fillMaxWidth()
            .height(144.dp)
            .background(LocalPaleta.current.vaga)
            .then(if (p.mega) Modifier.bordaPixel(paleta.copy(borda = paleta.destaque), grossura = 2.dp, dupla = false) else Modifier)
            .clickable { menu = true }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteAnimado(p, 64, pulaAoEntrar = true)
        Text(p.nome, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (p.forma != null) Text(p.forma.uppercase().takeIf { p.mega } ?: p.forma, style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = if (p.mega) paleta.destaque else MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            p.tipos.forEach { SeloTipo(it, compacto = true) }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Trocar por…") }, onClick = { menu = false; aoTrocar() })
            DropdownMenuItem(text = { Text("Ver ficha") }, onClick = { menu = false; aoVerFicha() })
            megas.forEach { m ->
                val ativa = m.id == megaAtiva
                DropdownMenuItem(
                    text = { Text(if (ativa) "Voltar ao normal" else "Mega evoluir" + (m.forma?.removePrefix("Mega")?.trim()?.takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: "")) },
                    onClick = { menu = false; aoMega(m.id) },
                )
            }
            DropdownMenuItem(text = { Text("Tirar do time") }, onClick = { menu = false; aoRemover() })
        }
    }
}

@Composable
private fun VagaVazia(ativa: Boolean, aoClicar: () -> Unit) {
    val cor = if (ativa) LocalPaleta.current.acento else LocalPaleta.current.sub
    Box(
        Modifier
            .fillMaxWidth()
            .height(144.dp)
            .drawBehind {
                // Borda tracejada em degraus, para a vaga vazia parecer "encaixe".
                val g = 2.dp.toPx()
                drawRect(cor, topLeft = Offset(g / 2, g / 2), size = Size(size.width - g, size.height - g),
                    style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
            }
            .clickable(onClick = aoClicar),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Add, "Adicionar pokémon", tint = cor)
    }
}

/** O que mais importa da análise: as fraquezas graves, numa caixa logo abaixo das vagas. */
@Composable
private fun PontosFracos(a: AnaliseTime) {
    CaixaPixel(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp).fillMaxWidth(), preenchimento = PaddingValues(12.dp)) {
        Column {
            if (a.problemas.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = CorBoa)
                    Text("  NENHUMA FRAQUEZA GRAVE", color = CorBoa, style = MaterialTheme.typography.titleSmall)
                }
            } else {
                Text("PONTOS FRACOS", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 6.dp))
                a.problemas.forEach { l ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                        Text("! ", color = CorAlerta, fontWeight = FontWeight.Bold)
                        SeloTipo(l.tipo)
                        val seguram = l.resistentes + l.imunes
                        Text("  ${l.fracos} fracos, " + if (seguram == 1) "1 segura" else "$seguram seguram", color = CorAlerta,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** Tabela de defesa, STAB e média de stats: recolhida por padrão para não poluir a tela. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Detalhes(a: AnaliseTime) {
    var aberto by rememberSaveable { mutableStateOf(false) }
    Text(
        if (aberto) "▲ ESCONDER DETALHES" else "▼ VER DETALHES (DEFESA, STAB, STATS)",
        color = LocalPaleta.current.sub, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.fillMaxWidth().clickable { aberto = !aberto }.padding(horizontal = 16.dp, vertical = 16.dp),
    )
    if (!aberto) return

    Secao("Defesa do time contra cada tipo")
    Text("▼ fracos   ▲ resistem ou são imunes", style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
    // Duas colunas de 9 tipos
    val linhas = a.defesa.sortedBy { it.tipo.ordinal }
    Row(Modifier.padding(horizontal = 16.dp)) {
        linhas.chunked(9).forEach { col ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                col.forEach { LinhaTipoDefesa(it) }
            }
        }
    }

    if (a.semCoberturaStab.isNotEmpty()) {
        Secao("Nenhum STAB super efetivo contra")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { a.semCoberturaStab.forEach { SeloTipo(it) } }
    }

    Secao("Média de stats")
    a.mediaStats.forEachIndexed { i, v -> BarraStat(Pokemon.NOMES_STATS[i], v) }
}

@Composable
private fun LinhaTipoDefesa(l: LinhaDefesa) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(92.dp)) { SeloTipo(l.tipo) }
        Text(if (l.fracos > 0) "▼${l.fracos}" else "", color = if (l.problema) CorAlerta else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (l.problema) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.bodySmall)
        val seguram = l.resistentes + l.imunes
        Text(if (seguram > 0) "▲$seguram" else "", color = CorBoa, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
}

/** Substitutos para um membro: toque e ele entra na mesma vaga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PainelTroca(vm: EditorTimeViewModel, indice: Int, sai: Pokemon, aoFechar: () -> Unit) {
    val opcoes by androidx.compose.runtime.produceState<com.poketeam.app.domain.Substitutos?>(null, indice, sai.id) {
        value = vm.substitutos(indice)
    }
    val toque = LocalHapticFeedback.current
    val escolher = { p: Pokemon -> toque.performHapticFeedback(HapticFeedbackType.LongPress); vm.trocar(indice, p); aoFechar() }
    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sprite(sai, 40)
                Text("  TROCAR ${sai.nome.uppercase()}", style = MaterialTheme.typography.titleSmall)
            }
            val o = opcoes
            when {
                o == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                o.parecidos.isEmpty() && o.paraOTime.isEmpty() ->
                    Text("Nenhum candidato com as regras atuais.", color = LocalPaleta.current.sub, modifier = Modifier.padding(12.dp))
                else -> {
                    if (o.parecidos.isNotEmpty()) {
                        TituloTroca("Parecidos (tipo em comum)")
                        o.parecidos.forEach { LinhaTroca(it, escolher) }
                    }
                    if (o.paraOTime.isNotEmpty()) {
                        TituloTroca("Melhores para o time")
                        o.paraOTime.forEach { LinhaTroca(it, escolher) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloTroca(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPaleta.current.sub,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
}

@Composable
private fun LinhaTroca(s: Sugestao, aoEscolher: (Pokemon) -> Unit) {
    val p = s.pokemon
    Row(Modifier.fillMaxWidth().clickable { aoEscolher(p) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Sprite(p, 48)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(p.nomeCompleto, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                p.tipos.forEach { SeloTipo(it, compacto = true) }
                Text("  Total ${p.total}", style = MaterialTheme.typography.labelSmall, color = LocalPaleta.current.sub)
            }
            if (s.resolve.isNotEmpty()) Text("Fecha: " + s.resolve.joinToString { it.nome },
                style = MaterialTheme.typography.labelSmall, color = CorBoa)
            if (s.cria.isNotEmpty()) Text("Deixa fraco a: " + s.cria.joinToString { it.nome },
                style = MaterialTheme.typography.labelSmall, color = CorAlerta)
        }
    }
}
