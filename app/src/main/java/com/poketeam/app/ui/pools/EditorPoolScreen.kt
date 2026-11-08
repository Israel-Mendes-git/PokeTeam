package com.poketeam.app.ui.pools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.ui.pokedex.Sprite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorPoolScreen(vm: EditorPoolViewModel, aoBanir: () -> Unit, aoSair: () -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val e = estado
    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val p = e.pool
    var nome by remember { mutableStateOf(p.nome) }
    LaunchedEffect(p.id) { nome = p.nome }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp, top = 4.dp)) {
            IconButton(onClick = aoSair) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            TextField(
                value = nome,
                onValueChange = { nome = it; vm.renomear(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                modifier = Modifier.weight(1f),
            )
        }
        Text("${e.total} pokémons neste pool", color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 8.dp))

        Secao("Gerações", if (p.geracoes.isEmpty()) "Nenhuma marcada = todas" else null)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            (1..9).forEach { g -> FilterChip(selected = g in p.geracoes, onClick = { vm.alternarGeracao(g) }, label = { Text("$g") }) }
        }

        Secao("Regras", null)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            FilterChip(selected = p.semLendarios, onClick = vm::alternarLendarios, label = { Text("Sem lendários") })
            FilterChip(selected = p.semMiticos, onClick = vm::alternarMiticos, label = { Text("Sem míticos") })
            FilterChip(selected = p.soFinais, onClick = vm::alternarFinais, label = { Text("Só evolução final") })
            FilterChip(selected = p.semBebes, onClick = vm::alternarBebes, label = { Text("Sem bebês") })
            FilterChip(selected = p.semFormas, onClick = vm::alternarFormas, label = { Text("Sem formas regionais") })
        }

        Secao("Banidos", if (e.banidos.isEmpty()) "Ninguém banido" else "${e.banidos.size} fora do pool · toque para desbanir")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            e.banidos.forEach { b ->
                InputChip(
                    selected = false,
                    onClick = { vm.desbanir(b.id) },
                    label = { Text(b.nomeCompleto) },
                    avatar = { Sprite(b, 28) },
                    trailingIcon = { Icon(Icons.Default.Close, "Desbanir", Modifier.width(16.dp)) },
                )
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = aoBanir) {
                Icon(Icons.Default.Block, null)
                Spacer(Modifier.width(8.dp))
                Text("Banir pokémons")
            }
            if (e.banidos.isNotEmpty()) TextButton(onClick = vm::limparBanidos) { Text("Limpar banidos") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Secao(titulo: String, dica: String?) {
    Column(Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (dica != null) Text(dica, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
