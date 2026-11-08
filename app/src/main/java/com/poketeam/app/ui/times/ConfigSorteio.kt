package com.poketeam.app.ui.times

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.domain.ConfigRecomendar
import com.poketeam.app.domain.EstiloRecomendar
import com.poketeam.app.domain.FormatoSmogon
import com.poketeam.app.domain.RegrasSorteio
import com.poketeam.app.domain.Tipo
import com.poketeam.app.ui.tema.LocalPaleta

/** Painel de configuração: aba Sortear (regras) e aba Recomendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelConfigSorteio(vm: EditorTimeViewModel, abaInicial: Int = 0, aoFechar: () -> Unit) {
    val regras by vm.regras.collectAsStateWithLifecycle()
    val reco by vm.configRecomendar.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(abaInicial) }
    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        TabRow(selectedTabIndex = aba, containerColor = LocalPaleta.current.painel) {
            Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("SORTEAR") })
            Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("RECOMENDAR") })
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)
        ) {
            if (aba == 0) AbaSortear(regras, vm::mudarRegras, aoLigarMonotype = {
                // Sem formato escolhido, o monotype puxa as builds do Monotype do Smogon.
                if (reco.formato == null) vm.mudarRecomendar { it.copy(formato = FormatoSmogon.MONOTYPE.name) }
            }) else AbaRecomendar(reco, vm::mudarRecomendar)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbaSortear(r: RegrasSorteio, mudar: ((RegrasSorteio) -> RegrasSorteio) -> Unit, aoLigarMonotype: () -> Unit) {
    val p = LocalPaleta.current
    Titulo("Monotype")
    Text(
        if (r.monotipo == null) "Escolha um tipo e todo o time terá esse tipo (o segundo tipo é livre)."
        else "Todo membro tem ${r.tipoMono?.nome ?: ""}. O Recomendar usa o segundo tipo para cobrir as fraquezas.",
        color = p.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChipTexto("DESLIGADO", r.monotipo == null) { mudar { it.copy(monotipo = null) } }
        Tipo.entries.forEach { t ->
            ChipTipo(t, if (r.monotipo == t.chave) 1 else 0) {
                if (r.monotipo == null) aoLigarMonotype()
                mudar { it.copy(monotipo = if (it.monotipo == t.chave) null else t.chave) }
            }
        }
    }

    Titulo("Quem pode sair")
    Linha("Só evolução final", "Nada de Caterpie no time", r.soFinais) { mudar { it.copy(soFinais = !it.soFinais) } }
    Linha("Sem lendários e míticos", null, r.semLendarios) { mudar { it.copy(semLendarios = !it.semLendarios) } }
    Linha("Só favoritos", "Só entre os seus ★", r.soFavoritos) { mudar { it.copy(soFavoritos = !it.soFavoritos) } }

    Titulo("Total mínimo de stats")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0 to "Sem mínimo", 400 to "400+", 450 to "450+", 500 to "500+", 550 to "550+").forEach { (v, rotulo) ->
            FilterChip(selected = r.totalMinimo == v, onClick = { mudar { it.copy(totalMinimo = v) } }, label = { Text(rotulo) })
        }
    }

    Titulo("Tipos")
    Text("Toque para alternar: neutro → ✓ precisa ter → ✕ proibido", color = p.sub, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Tipo.entries.forEach { t ->
            val estado = when (t.chave) {
                in r.tiposObrigatorios -> 1
                in r.tiposProibidos -> 2
                else -> 0
            }
            ChipTipo(t, estado) {
                mudar { atual ->
                    when (estado) {
                        0 -> atual.copy(tiposObrigatorios = atual.tiposObrigatorios + t.chave)
                        1 -> atual.copy(tiposObrigatorios = atual.tiposObrigatorios - t.chave, tiposProibidos = atual.tiposProibidos + t.chave)
                        else -> atual.copy(tiposProibidos = atual.tiposProibidos - t.chave)
                    }
                }
            }
        }
    }

    Titulo("Montagem")
    Linha(
        "Sem tipo repetido",
        if (r.monotipo != null) "No monotype, vale para o segundo tipo" else "Cada tipo aparece em um membro só",
        r.semTipoRepetido,
    ) { mudar { it.copy(semTipoRepetido = !it.semTipoRepetido) } }
    Linha("Evitar fraquezas graves", "Testa várias combinações e fica com a mais equilibrada", r.evitarFraquezas) {
        mudar { it.copy(evitarFraquezas = !it.evitarFraquezas) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbaRecomendar(c: ConfigRecomendar, mudar: ((ConfigRecomendar) -> ConfigRecomendar) -> Unit) {
    val p = LocalPaleta.current
    Titulo("Estilo")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        EstiloRecomendar.entries.forEach { e ->
            FilterChip(selected = c.estilo == e, onClick = { mudar { it.copy(estilo = e) } }, label = { Text(e.rotulo) })
        }
    }
    Text(
        when (c.estilo) {
            EstiloRecomendar.CASUAL -> "Só cobre os pontos fracos do time; força no competitivo não conta."
            EstiloRecomendar.EQUILIBRADO -> "Cobre os pontos fracos e, no empate, prefere quem é forte no Smogon."
            EstiloRecomendar.COMPETITIVO -> "Puxa os pokémons mais fortes do Smogon, sem deixar fraquezas graves."
        },
        color = p.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp),
    )

    Titulo("Formato do Smogon")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(selected = c.formato == null, onClick = { mudar { it.copy(formato = null) } }, label = { Text("Qualquer") })
        FormatoSmogon.entries.forEach { f ->
            FilterChip(selected = c.formato == f.name, onClick = { mudar { it.copy(formato = f.name) } }, label = { Text(f.rotulo) })
        }
    }
    Text("Prefere quem tem build nesse formato; a build do formato vem primeiro no time.",
        color = p.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))

    Titulo("Outros")
    Linha("Preferir favoritos", "Seus ★ ganham vantagem, sem ser obrigatório", c.preferirFavoritos) {
        mudar { it.copy(preferirFavoritos = !it.preferirFavoritos) } }
    Linha("Usar as regras do sorteio", "Tipos, lendários, total mínimo, sem tipo repetido e só favoritos valem aqui também",
        c.usarRegrasDoSorteio) { mudar { it.copy(usarRegrasDoSorteio = !it.usarRegrasDoSorteio) } }
}

@Composable
private fun ChipTipo(t: Tipo, estado: Int, aoClicar: () -> Unit) {
    val p = LocalPaleta.current
    val cor = Color(t.cor)
    // Neutro: fundo tingido da cor do tipo e texto do tema, para os tipos escuros (Fantasma, Sombrio) continuarem legíveis.
    val fundo = when (estado) { 0 -> cor.copy(alpha = 0.28f); 1 -> cor; else -> p.alerta.copy(alpha = 0.25f) }
    Row(
        Modifier.background(fundo).clickable(onClick = aoClicar).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            (when (estado) { 1 -> "✓ "; 2 -> "✕ "; else -> "" }) + t.nome.uppercase(),
            color = when (estado) { 1 -> Color.White; 2 -> p.alerta; else -> p.texto },
            fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Titulo(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
}

@Composable
private fun Linha(titulo: String, descricao: String?, ligado: Boolean, habilitado: Boolean = true, alternar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = habilitado, onClick = alternar).padding(vertical = 6.dp)
            .alpha(if (habilitado) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (descricao != null) Text(descricao, style = MaterialTheme.typography.bodySmall, color = LocalPaleta.current.sub)
        }
        Switch(checked = ligado, onCheckedChange = { alternar() }, enabled = habilitado)
    }
}

@Composable
private fun ChipTexto(texto: String, marcado: Boolean, aoClicar: () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.background(if (marcado) p.texto else p.vaga).clickable(onClick = aoClicar).padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(texto, color = if (marcado) p.fundo else p.texto, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
