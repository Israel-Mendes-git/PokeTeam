package com.poketeam.app.ui.amigos

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.poketeam.app.data.Treinador
import com.poketeam.app.ui.pokedex.Sprite
import com.poketeam.app.ui.tema.BotaoPixel
import com.poketeam.app.ui.tema.CaixaPixel
import com.poketeam.app.ui.tema.ConfirmarExclusao
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.SeloTipo
import com.poketeam.app.ui.tema.TituloPixel
import com.poketeam.app.ui.tema.bordaPixel

@Composable
fun FotoTreinador(t: Treinador?, tamanhoDp: Int) {
    val p = LocalPaleta.current
    Box(Modifier.size(tamanhoDp.dp).background(p.vaga).bordaPixel(p, grossura = 2.dp, dupla = false), contentAlignment = Alignment.Center) {
        if (t?.foto != null) {
            AsyncImage(model = t.foto, contentDescription = t.apelido, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(3.dp))
        } else {
            Icon(Icons.Default.Person, null, tint = p.sub, modifier = Modifier.size((tamanhoDp * 0.6f).dp))
        }
    }
}

@Composable
fun AmigosScreen(vm: AmigosViewModel, aoAbrirAmigo: (Int) -> Unit) {
    val eu by vm.eu.collectAsStateWithLifecycle()
    val ocupado by vm.ocupado.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    // Pedidos e amigos novos chegam do servidor: atualiza sempre que a aba aparece.
    LaunchedEffect(Unit) { vm.atualizar() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TituloPixel("Amigos", Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
        if (ocupado) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        mensagem?.let { m ->
            LaunchedEffect(m) { kotlinx.coroutines.delay(4000); vm.mensagemVista() }
            Text(m, color = LocalPaleta.current.destaque, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        }
        when {
            !vm.disponivel -> Text("Amigos não está configurado nesta versão do app.", color = LocalPaleta.current.sub,
                modifier = Modifier.padding(16.dp))
            eu == null -> Entrar(vm, ocupado)
            else -> Logado(vm, eu!!, aoAbrirAmigo)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Entrar(vm: AmigosViewModel, ocupado: Boolean) {
    val activity = LocalContext.current as Activity
    CaixaPixel(Modifier.padding(12.dp).fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Entre para adicionar amigos e ver os times que eles compartilham.", textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium)
            Text("O resto do app continua funcionando sem conta.", textAlign = TextAlign.Center,
                color = LocalPaleta.current.sub, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            BotaoPixel(onClick = { vm.entrarComGoogle(activity) }, enabled = !ocupado) {
                Text("ENTRAR COM GOOGLE", style = MaterialTheme.typography.titleSmall)
            }
            if (vm.modoTeste) {
                Text("Build de teste local:", color = LocalPaleta.current.sub, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ana", "Bia").forEach { n -> TextButton(onClick = { vm.entrarComoTeste(n) }) { Text("Entrar como $n") } }
                }
            }
        }
    }
}

@Composable
private fun Logado(vm: AmigosViewModel, eu: Treinador, aoAbrirAmigo: (Int) -> Unit) {
    val p = LocalPaleta.current
    val contexto = LocalContext.current
    val amigos by vm.amigos.collectAsStateWithLifecycle()
    var editarApelido by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmarApagar by remember { mutableStateOf(false) }
    var codigo by remember { mutableStateOf("") }
    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.mudarFoto(uri)
    }

    // Perfil
    CaixaPixel(Modifier.padding(12.dp).fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable { escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    FotoTreinador(eu, 72)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editarApelido = true }) {
                        Text(eu.apelido, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Edit, "Mudar apelido", tint = p.sub, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                    }
                    Text("Toque na foto para trocar", color = p.sub, style = MaterialTheme.typography.labelSmall)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Mais opções") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Sair") }, onClick = { menu = false; vm.sair() })
                        DropdownMenuItem(text = { Text("Apagar conta") }, onClick = { menu = false; confirmarApagar = true })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("SEU CÓDIGO", color = p.sub, style = MaterialTheme.typography.labelSmall)
                    Text(eu.codigo.orEmpty(), style = MaterialTheme.typography.headlineSmall, color = p.destaque)
                }
                IconButton(onClick = {
                    val texto = "Me adiciona no PokeTeam! Meu código é ${eu.codigo}"
                    contexto.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Mandar código"))
                }) { Icon(Icons.Default.Share, "Mandar código") }
            }
        }
    }

    // Adicionar por código
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = codigo, onValueChange = { codigo = it.uppercase().filter(Char::isLetterOrDigit).take(6) },
            singleLine = true, placeholder = { Text("Código do amigo") }, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        BotaoPixel(onClick = { vm.adicionar(codigo); codigo = "" }, enabled = codigo.length == 6) { Text("ADICIONAR") }
    }

    val lista = amigos
    if (lista != null) {
        if (lista.recebidos.isNotEmpty()) {
            Secao("PEDIDOS RECEBIDOS")
            lista.recebidos.forEach { t ->
                LinhaTreinador(t) {
                    TextButton(onClick = { vm.aceitar(t.id) }) { Text("Aceitar") }
                    IconButton(onClick = { vm.desfazer(t.id) }) { Icon(Icons.Default.Close, "Recusar") }
                }
            }
        }
        Secao("AMIGOS (${lista.amigos.size})")
        if (lista.amigos.isEmpty()) Text("Mande seu código para os amigos ou digite o deles acima.", color = p.sub,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
        lista.amigos.forEach { t -> LinhaTreinador(t, aoClicar = { aoAbrirAmigo(t.id) }) { Text("▸", color = p.acento) } }
        if (lista.enviados.isNotEmpty()) {
            Secao("AGUARDANDO RESPOSTA")
            lista.enviados.forEach { t ->
                LinhaTreinador(t) { TextButton(onClick = { vm.desfazer(t.id) }) { Text("Cancelar") } }
            }
        }
    }

    if (editarApelido) {
        var novo by remember { mutableStateOf(eu.apelido) }
        AlertDialog(
            onDismissRequest = { editarApelido = false },
            title = { Text("Apelido") },
            text = { OutlinedTextField(value = novo, onValueChange = { novo = it.take(20) }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.mudarApelido(novo); editarApelido = false }, enabled = novo.trim().length >= 2) { Text("Salvar") } },
            dismissButton = { TextButton(onClick = { editarApelido = false }) { Text("Cancelar") } },
        )
    }
    if (confirmarApagar) ConfirmarExclusao("Apagar sua conta?", "Some do servidor: perfil, amizades e times compartilhados. Seus times no celular ficam.",
        acao = "Apagar", aoConfirmar = vm::apagarConta) { confirmarApagar = false }
}

@Composable
private fun Secao(t: String) {
    Text(t, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun LinhaTreinador(t: Treinador, aoClicar: (() -> Unit)? = null, fim: @Composable () -> Unit) {
    val p = LocalPaleta.current
    Row(
        Modifier.padding(horizontal = 12.dp, vertical = 3.dp).fillMaxWidth().background(p.painel)
            .then(if (aoClicar != null) Modifier.clickable(onClick = aoClicar) else Modifier).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FotoTreinador(t, 44)
        Text(t.apelido, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 12.dp))
        fim()
    }
}

@Composable
fun AmigoScreen(vm: AmigoViewModel, aoVoltar: () -> Unit) {
    val tela by vm.tela.collectAsStateWithLifecycle()
    val mensagem by vm.mensagem.collectAsStateWithLifecycle()
    val p = LocalPaleta.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, end = 12.dp)) {
            IconButton(onClick = aoVoltar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            tela?.let {
                FotoTreinador(it.dados.treinador, 40)
                Text(it.dados.treinador.apelido, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 10.dp))
            }
        }
        mensagem?.let { m ->
            LaunchedEffect(m) { kotlinx.coroutines.delay(3000); vm.mensagemVista() }
            Text(m, color = p.destaque, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        val t = tela ?: return@Column
        val favs = t.dados.favoritos.mapNotNull { t.pokemons[it] }
        if (favs.isNotEmpty()) {
            Text("★ FAVORITOS", style = MaterialTheme.typography.titleSmall, color = p.destaque,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                favs.forEach { pk -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                    Sprite(pk, 52)
                    Text(pk.nome, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                } }
            }
        }
        Text("TIMES", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
        if (t.dados.times.isEmpty()) Text("Nenhum time compartilhado ainda.", color = p.sub, modifier = Modifier.padding(16.dp))
        t.dados.times.forEachIndexed { i, time ->
            CaixaPixel(Modifier.padding(12.dp).fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(time.nome, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.copiar(i) }) { Text("Copiar") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        time.membros.forEach { id ->
                            val pk = t.pokemons[if (time.mega != null && t.pokemons[time.mega]?.dex == t.pokemons[id]?.dex) time.mega else id]
                            if (pk != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Sprite(pk, 44)
                                if (pk.mega) Text("MEGA", color = p.destaque, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
