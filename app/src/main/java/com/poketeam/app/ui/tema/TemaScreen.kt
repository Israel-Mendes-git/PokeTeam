package com.poketeam.app.ui.tema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp

/** Escolha do tema: cada cartão é uma miniatura desenhada com as cores do próprio tema. */
@Composable
fun TemaScreen(
    atual: String,
    aoEscolher: (String) -> Unit,
    aoVoltar: () -> Unit,
    /** Bloco extra no topo (versão e atualização). */
    topo: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, end = 16.dp)) {
            IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            TituloPixel("Ajustes")
        }
        Box(Modifier.padding(horizontal = 12.dp)) { topo() }
        Text("TEMA", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Paletas.todas, key = { it.id }) { p -> Miniatura(p, p.id == atual) { aoEscolher(p.id) } }
        }
    }
}

@Composable
private fun Miniatura(p: Paleta, escolhida: Boolean, aoClicar: () -> Unit) {
    val atual = LocalPaleta.current
    Column(
        Modifier
            .background(if (escolhida) atual.acento else androidx.compose.ui.graphics.Color.Transparent)
            .padding(4.dp)
            .clickable(onClick = aoClicar)
    ) {
        Column(Modifier.fillMaxWidth().background(p.fundo).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("▶ ${p.nome.uppercase()}", color = p.destaque, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Box(Modifier.fillMaxWidth().background(p.painel).bordaPixel(p, grossura = 2.dp).padding(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { i -> Box(Modifier.size(22.dp).background(if (i == 2) p.fundo else p.vaga)) }
                }
            }
            Text("Texto · detalhe", color = p.texto, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
            Box(Modifier.fillMaxWidth().background(p.bordaFundo)) {
                Text("SORTEAR", color = p.textoAcento, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.fillMaxWidth().padding(end = 3.dp, bottom = 3.dp).background(p.acento).padding(6.dp))
            }
        }
        if (escolhida) Text("✓ EM USO", color = atual.textoAcento, fontSize = 11.sp, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 6.dp, top = 3.dp, bottom = 1.dp))
        else Spacer(Modifier.height(4.dp))
    }
}
