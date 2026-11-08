package com.poketeam.app.ui.pokedex

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Pool
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.bordaPixel
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import com.poketeam.app.domain.Tipo
import com.poketeam.app.ui.tema.SeloTipo

@Composable
fun PokedexScreen(
    vm: PokedexViewModel,
    aoAbrir: (Pokemon) -> Unit,
    /** Modo escolha: título no topo, botão voltar e pokémons já escolhidos marcados. */
    tituloEscolha: String? = null,
    marcados: Set<Int> = emptySet(),
    aoVoltar: () -> Unit = {},
    /** Se true, tocar num marcado também chama [aoAbrir] (tela de banir: toca de novo para desbanir). */
    marcadoClicavel: Boolean = false,
    /** Ícone e descrição dos marcados (time: check; banir: proibido). */
    iconeMarcado: ImageVector = Icons.Default.CheckCircle,
    descricaoMarcado: String = "Já está no time",
    /** Seletor de pool; some se [poolAtivo] for null. */
    pools: List<Pool> = emptyList(),
    poolAtivo: Pool? = null,
    aoEscolherPool: (String) -> Unit = {},
) {
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val lista by vm.resultado.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        if (tituloEscolha != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                Text(tituloEscolha, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = filtro.busca,
                onValueChange = vm::buscar,
                placeholder = { Text("Buscar nome ou número") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (filtro.busca.isNotEmpty()) IconButton(onClick = { vm.buscar("") }) {
                        Icon(Icons.Default.Close, "Limpar busca")
                    }
                },
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { mostrarFiltros = true }) {
                BadgedBox(badge = { if (filtro.ativos > 0) Badge { Text("${filtro.ativos}") } }) {
                    Icon(Icons.Default.FilterList, "Filtros")
                }
            }
        }

        if (poolAtivo != null) SeletorPool(pools, poolAtivo, aoEscolherPool)

        val resultado = lista
        if (resultado == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Text(
                if (resultado.size == 1) "1 pokémon" else "${resultado.size} pokémons",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(resultado, key = Pokemon::id) { p ->
                    val marcado = p.id in marcados
                    LinhaPokemon(p, marcado, iconeMarcado, descricaoMarcado, favorito = p.id in favoritos) { if (!marcado || marcadoClicavel) aoAbrir(p) }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
            }
        }
    }

    if (mostrarFiltros) {
        PainelFiltros(filtro, vm) { mostrarFiltros = false }
    }
}

@Composable
fun LinhaPokemon(p: Pokemon, marcado: Boolean, icone: ImageVector, descricao: String, favorito: Boolean = false, aoClicar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = aoClicar)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Sprite(p, 56)
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(p.nomeCompleto, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
                p.tipos.forEach { SeloTipo(it) }
            }
        }
        if (favorito) Text("★ ", color = LocalPaleta.current.destaque)
        if (marcado) Icon(icone, descricao, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("#${p.dex}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text("${p.total}", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Seletor do pool ativo, em caixa pixel. [aoGerenciar] adiciona "Gerenciar pools" no fim da lista. */
@Composable
fun SeletorPool(
    pools: List<Pool>,
    ativo: Pool,
    aoEscolher: (String) -> Unit,
    modifier: Modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
    aoGerenciar: (() -> Unit)? = null,
) {
    var aberto by remember { mutableStateOf(false) }
    val paleta = LocalPaleta.current
    Box(modifier) {
        Row(
            Modifier
                .background(paleta.painel)
                .bordaPixel(paleta, grossura = 2.dp, dupla = false)
                .clickable { aberto = true }
                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Tune, null, Modifier.size(16.dp), tint = paleta.sub)
            Text(" POOL: ${ativo.nome.uppercase()}", style = MaterialTheme.typography.labelLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp))
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            pools.forEach { p ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(p.nome, fontWeight = if (p.id == ativo.id) FontWeight.Bold else FontWeight.Normal)
                            Text(p.resumo(), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    leadingIcon = { RadioButton(selected = p.id == ativo.id, onClick = null) },
                    onClick = { aberto = false; aoEscolher(p.id) },
                )
            }
            if (aoGerenciar != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Gerenciar pools…", color = paleta.acento) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = paleta.acento) },
                    onClick = { aberto = false; aoGerenciar() },
                )
            }
        }
    }
}

@Composable
fun Sprite(p: Pokemon, tamanhoDp: Int) {
    AsyncImage(
        model = p.sprite,
        contentDescription = p.nomeCompleto,
        filterQuality = FilterQuality.None, // pixel art nítida ao ampliar
        modifier = Modifier
            .size(tamanhoDp.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PainelFiltros(filtro: FiltroPokedex, vm: PokedexViewModel, fechar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = fechar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = vm::limparFiltros) { Text("Limpar") }
            }
            Titulo("Tipo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Tipo.entries.forEach { t ->
                    val marcado = t in filtro.tipos
                    FilterChip(
                        selected = marcado,
                        onClick = { vm.alternarTipo(t) },
                        label = { Text(t.nome) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(t.cor),
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            Titulo("Geração")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..9).forEach { g ->
                    FilterChip(selected = g in filtro.geracoes, onClick = { vm.alternarGeracao(g) }, label = { Text("$g") })
                }
            }
            Titulo("Outros")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filtro.soFinais, onClick = vm::alternarFinais, label = { Text("Só evolução final") })
                FilterChip(selected = filtro.semLendarios, onClick = vm::alternarLendarios, label = { Text("Sem lendários/míticos") })
                FilterChip(selected = filtro.soFavoritos, onClick = vm::alternarFavoritos, label = { Text("★ Favoritos") })
            }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}
