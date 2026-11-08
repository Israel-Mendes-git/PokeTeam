package com.poketeam.app.ui.times

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poketeam.app.domain.Time
import com.poketeam.app.ui.pokedex.Sprite
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.TituloPixel
import com.poketeam.app.ui.tema.bordaPixel
import com.poketeam.app.ui.tema.ConfirmarExclusao

/** Cor de alerta do tema ativo. */
val CorAlerta: Color @Composable get() = LocalPaleta.current.alerta

@Composable
fun MeusTimesScreen(vm: MeusTimesViewModel, nomePool: String, aoAbrir: (String) -> Unit) {
    val lista by vm.lista.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TituloPixel("Meus times", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp))
            val times = lista
            when {
                times == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                times.isEmpty() -> Text(
                    "Nenhum time ainda.\nMonte um em \"Novo time\" ou deixe o app\nsortear um do pool $nomePool.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    items(times, key = { it.time.id }) { t ->
                        CartaoTime(t, aoAbrir = { aoAbrir(t.time.id) },
                            aoFavoritar = { vm.alternarFavorito(t.time.id) },
                            aoDuplicar = { vm.duplicar(t.time.id) },
                            aoExcluir = { vm.excluir(t.time.id) })
                    }
                    item { Spacer(Modifier.height(90.dp)) } // espaço para os botões de baixo
                }
            }
        }
        // Os dois jeitos de começar um time, lado a lado no rodapé.
        val p = LocalPaleta.current
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(p.fundo).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BotaoPixel(onClick = { vm.novo { aoAbrir(it.id) } }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, null); Text(" NOVO TIME", style = MaterialTheme.typography.titleSmall)
            }
            BotaoPixel(onClick = { vm.novoSorteado { aoAbrir(it.id) } }, modifier = Modifier.weight(1f),
                cor = p.painel, corTexto = p.texto) {
                Icon(Icons.Default.Casino, null); Text(" SORTEAR", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CartaoTime(
    t: TimeResolvido,
    aoAbrir: () -> Unit,
    aoFavoritar: () -> Unit,
    aoDuplicar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) ConfirmarExclusao("Excluir \"${t.time.nome}\"?", "O time some de Meus times.", aoConfirmar = aoExcluir) { confirmar = false }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .bordaPixel(LocalPaleta.current)
            .clickable(onClick = aoAbrir)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.time.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            IconButton(onClick = aoFavoritar) {
                Icon(if (t.time.favorito) Icons.Default.Star else Icons.Default.StarBorder,
                    "Favorito", tint = if (t.time.favorito) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; aoDuplicar() })
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { menu = false; confirmar = true })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            t.pokemons.forEach { Sprite(it, 44) }
            repeat(Time.MAXIMO - t.pokemons.size) {
                Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f)))
            }
        }
        val problemas = t.analise.problemas
        if (problemas.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.Warning, null, tint = CorAlerta, modifier = Modifier.size(16.dp))
                Text(" Fraco a " + problemas.joinToString(", ") { it.tipo.nome },
                    style = MaterialTheme.typography.bodySmall, color = CorAlerta)
            }
        }
    }
}
