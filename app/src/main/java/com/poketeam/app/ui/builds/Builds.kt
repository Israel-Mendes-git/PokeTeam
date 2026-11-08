package com.poketeam.app.ui.builds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poketeam.app.domain.Build
import com.poketeam.app.domain.Natureza
import com.poketeam.app.domain.Tipo
import com.poketeam.app.ui.tema.CaixaPixel
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.SeloTipo
import com.poketeam.app.ui.tema.TituloPixel

/** Tipos do Smogon vêm em inglês ("Fire"); a chave da PokéAPI é a mesma em minúsculas. */
private fun tipoDoSmogon(nome: String): Tipo? = runCatching { Tipo.daChave(nome.lowercase()) }.getOrNull()

/** Uma build do Smogon: golpes, item, habilidade, nature (com o efeito) e EVs. */
@Composable
fun CartaoBuild(b: Build, modifier: Modifier = Modifier, escolhida: Boolean = false, aoClicar: (() -> Unit)? = null) {
    val p = LocalPaleta.current
    val base = if (aoClicar != null) modifier.clickable(onClick = aoClicar) else modifier
    CaixaPixel(base.fillMaxWidth(), cor = if (escolhida) p.vaga else null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(b.nome.uppercase(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(if (escolhida) "✓ ${b.formato}" else b.formato, color = if (escolhida) p.acento else p.sub,
                    style = MaterialTheme.typography.labelMedium)
            }
            b.golpes.forEach { opcoes ->
                Text("• " + opcoes.joinToString(" / "), style = MaterialTheme.typography.bodyMedium)
            }
            Linha("Item", b.item.joinToString(" / "))
            Linha("Habilidade", b.habilidade.joinToString(" / "))
            if (b.nature.isNotEmpty()) {
                Linha("Nature", b.nature.joinToString(" / ") { n -> Natureza.porNome(n)?.let { "$n (${it.efeito})" } ?: n })
            }
            Linha("EVs", b.evsTexto)
            val teras = b.tera.mapNotNull(::tipoDoSmogon)
            if (teras.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tera", color = p.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(84.dp))
                teras.forEach { SeloTipo(it, compacto = true) }
            }
        }
    }
}

@Composable
private fun Linha(rotulo: String, valor: String) {
    if (valor.isBlank()) return
    Row {
        Text(rotulo, color = LocalPaleta.current.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(84.dp))
        Text(valor, style = MaterialTheme.typography.bodySmall)
    }
}

/** Tabela das 25 natures com o stat que sobe e o que desce. */
@Composable
fun NaturesScreen(aoVoltar: () -> Unit) {
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            TituloPixel("Natures")
        }
        Text("Cada nature sobe um stat em 10% e desce outro em 10%. As neutras não mudam nada.",
            color = p.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(Modifier.padding(horizontal = 12.dp)) {
            items(Natureza.todas.sortedBy { it.nome }, key = { it.nome }) { n ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp).background(p.painel).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(n.nome, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(n.efeito, color = if (n.neutra) p.sub else p.texto)
                }
            }
        }
    }
}
