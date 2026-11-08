package com.poketeam.app.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.TabelaDeTipos
import com.poketeam.app.domain.Tipo
import com.poketeam.app.ui.tema.SeloTipo

@Composable
fun DetalheScreen(p: Pokemon, voltar: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        IconButton(onClick = voltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Sprite(p, 160)
            Text("#${p.dex}  ·  Geração ${p.geracao}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
            Text(p.nomeCompleto, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                p.tipos.forEach { SeloTipo(it) }
            }
            val marcas = listOfNotNull(
                "Lendário".takeIf { p.lendario }, "Mítico".takeIf { p.mitico },
                "Bebê".takeIf { p.bebe }, "Evolução final".takeIf { p.final && !p.lendario && !p.mitico },
            )
            if (marcas.isNotEmpty()) {
                Text(marcas.joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 6.dp))
            }
        }

        Secao("Stats base")
        p.stats.forEachIndexed { i, v -> BarraStat(Pokemon.NOMES_STATS[i], v) }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text("Total", Modifier.width(88.dp), fontWeight = FontWeight.Bold)
            Text("${p.total}", fontWeight = FontWeight.Bold)
        }

        Secao("Defesa contra tipos")
        val defesa = TabelaDeTipos.defesa(p.tipos)
        GrupoDefesa("Fraco 4×", defesa, 4.0)
        GrupoDefesa("Fraco 2×", defesa, 2.0)
        GrupoDefesa("Resiste ½×", defesa, 0.5)
        GrupoDefesa("Resiste ¼×", defesa, 0.25)
        GrupoDefesa("Imune", defesa, 0.0)
        Box(Modifier.height(24.dp))
    }
}

@Composable
private fun Secao(titulo: String) {
    Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
}

@Composable
fun BarraStat(nome: String, valor: Int) {
    val cor = when {
        valor < 60 -> Color(0xFFE5534B)
        valor < 90 -> Color(0xFFE8A33D)
        valor < 120 -> Color(0xFF7BC86C)
        else -> Color(0xFF3FB6A8)
    }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(nome, Modifier.width(88.dp), style = MaterialTheme.typography.bodySmall)
        Text("$valor", Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)) {
            Box(Modifier.fillMaxWidth((valor / 200f).coerceAtMost(1f)).height(8.dp).background(cor, RectangleShape))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrupoDefesa(rotulo: String, defesa: Map<Tipo, Double>, mult: Double) {
    val tipos = defesa.filterValues { it == mult }.keys
    if (tipos.isEmpty()) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) { tipos.forEach { SeloTipo(it) } }
    }
}
