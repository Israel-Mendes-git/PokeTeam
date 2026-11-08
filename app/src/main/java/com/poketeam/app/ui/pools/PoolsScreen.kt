package com.poketeam.app.ui.pools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.poketeam.app.ui.tema.TituloPixel
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.bordaPixel
import com.poketeam.app.ui.tema.ConfirmarExclusao

@Composable
fun PoolsScreen(vm: PoolsViewModel, aoVoltar: () -> Unit, aoEditar: (String) -> Unit) {
    val lista by vm.lista.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                TituloPixel("Pools")
            }
            Text("O pool ativo define quais pokémons aparecem na Pokédex e na escolha do time.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            val pools = lista
            if (pools == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                    items(pools, key = { it.pool.id }) { item ->
                        CartaoPool(item,
                            aoAtivar = { vm.ativar(item.pool.id) },
                            aoEditar = { aoEditar(item.pool.id) },
                            aoDuplicar = { vm.duplicar(item.pool.id) },
                            aoExcluir = { vm.excluir(item.pool.id) })
                    }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
        val elencos by vm.elencos.collectAsStateWithLifecycle()
        var escolher by remember { mutableStateOf(false) }
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            ExtendedFloatingActionButton(
                onClick = { if (elencos.isEmpty()) vm.novo { aoEditar(it.id) } else escolher = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Novo pool") },
            )
            DropdownMenu(expanded = escolher, onDismissRequest = { escolher = false }) {
                DropdownMenuItem(
                    text = { Column { Text("Pool vazio"); Text("Todos os pokémons, você define as regras",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { escolher = false; vm.novo { aoEditar(it.id) } },
                )
                elencos.forEach { (e, total) ->
                    DropdownMenuItem(
                        text = { Column { Text(e.nome); Text("Elenco do modpack · $total pokémons",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                        onClick = { escolher = false; vm.novoDeElenco(e) { aoEditar(it.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoPool(
    item: PoolComContagem,
    aoAtivar: () -> Unit,
    aoEditar: () -> Unit,
    aoDuplicar: () -> Unit,
    aoExcluir: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmar by remember { mutableStateOf(false) }
    if (confirmar) ConfirmarExclusao("Excluir o pool \"${item.pool.nome}\"?",
        "As regras e os banidos dele se perdem. Os times montados não mudam.", aoConfirmar = aoExcluir) { confirmar = false }
    val borda = if (item.ativo) MaterialTheme.colorScheme.primary else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .bordaPixel(LocalPaleta.current.let { if (item.ativo) it.copy(borda = it.acento) else it })
            .clickable(onClick = aoAtivar)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = item.ativo, onClick = aoAtivar)
        Column(Modifier.weight(1f)) {
            Text(item.pool.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.pool.resumo(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${item.total} pokémons", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp))
        }
        if (!item.pool.fixo) {
            IconButton(onClick = aoEditar) { Icon(Icons.Default.Edit, "Editar") }
        }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Duplicar") }, onClick = { menu = false; aoDuplicar() })
                if (!item.pool.fixo) {
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { menu = false; confirmar = true })
                }
            }
        }
    }
}
