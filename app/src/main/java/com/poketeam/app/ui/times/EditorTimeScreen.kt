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
import com.poketeam.app.ui.tema.SeloTipo
import com.poketeam.app.ui.tema.LocalPaleta
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

        Text("${t.pokemons.size} de ${Time.MAXIMO}", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))

        // 6 vagas em 3 colunas, dentro da caixa pixel
        CaixaPixel(Modifier.padding(12.dp).fillMaxWidth(), preenchimento = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until Time.MAXIMO).chunked(3).forEach { linha ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    linha.forEach { i ->
                        val p = t.pokemons.getOrNull(i)
                        Box(Modifier.weight(1f)) {
                            if (p != null) VagaOcupada(p, aoVerFicha = { aoVerFicha(p) }, aoRemover = { vm.remover(i) })
                            else VagaVazia(ativa = i == t.pokemons.size, aoClicar = aoAdicionar)
                        }
                    }
                }
            }
        }
        }

        if (t.pokemons.isNotEmpty()) PontosFracos(t.analise)

        BarraSorteio(vm, cheio = t.time.cheio, nomePool = nomePool)

        val sugestoes by vm.sugestoes.collectAsStateWithLifecycle()
        if (sugestoes.isNotEmpty()) FaixaSugestoes(sugestoes, temProblemas = t.analise.problemas.isNotEmpty(), aoEscolher = vm::adicionar)

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
                onClick = { vm.avisoVisto(); vm.sortear() },
                enabled = !cheio && !sorteando,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Casino, null)
                Spacer(Modifier.width(8.dp))
                Text(if (cheio) "TIME COMPLETO" else "SORTEAR VAGAS", style = MaterialTheme.typography.titleSmall)
            }
            IconButton(onClick = { mostrarRegras = true }) { Icon(Icons.Default.Tune, "Regras do sorteio") }
        }
        Text(
            if (cheio) "Tire alguém do time para sortear de novo essa vaga."
            else "Sorteia do pool $nomePool e mantém quem já está no time.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        aviso?.let {
            Text(it, color = CorAlerta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }
    }

    if (mostrarRegras) {
        ModalBottomSheet(onDismissRequest = { mostrarRegras = false }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("Regras do sorteio", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                LinhaRegra("Só evolução final", "Nada de Caterpie no time sorteado", regras.soFinais) {
                    vm.mudarRegras { r -> r.copy(soFinais = !r.soFinais) } }
                LinhaRegra("Sem tipo repetido", "Cada tipo aparece em um membro só", regras.semTipoRepetido) {
                    vm.mudarRegras { r -> r.copy(semTipoRepetido = !r.semTipoRepetido) } }
                LinhaRegra("Evitar fraquezas graves", "Testa várias combinações e fica com a mais equilibrada", regras.evitarFraquezas) {
                    vm.mudarRegras { r -> r.copy(evitarFraquezas = !r.evitarFraquezas) } }
            }
        }
    }
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

@Composable
private fun FaixaSugestoes(sugestoes: List<Sugestao>, temProblemas: Boolean, aoEscolher: (Pokemon) -> Unit) {
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
private fun VagaOcupada(p: Pokemon, aoVerFicha: () -> Unit, aoRemover: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .height(144.dp)
            .background(LocalPaleta.current.vaga)
            .clickable { menu = true }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Sprite(p, 64)
        Text(p.nome, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (p.forma != null) Text(p.forma, style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
            p.tipos.forEach { SeloTipo(it, compacto = true) }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Ver ficha") }, onClick = { menu = false; aoVerFicha() })
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
