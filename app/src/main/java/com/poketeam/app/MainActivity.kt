package com.poketeam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Style
import com.poketeam.app.ui.draft.DraftScreen
import com.poketeam.app.ui.draft.DraftViewModel
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Pool
import com.poketeam.app.ui.pools.EditorPoolScreen
import com.poketeam.app.ui.pools.EditorPoolViewModel
import com.poketeam.app.ui.pools.PoolsScreen
import com.poketeam.app.ui.pools.PoolsViewModel
import kotlinx.coroutines.flow.map
import com.poketeam.app.ui.pokedex.DetalheScreen
import com.poketeam.app.ui.pokedex.PokedexScreen
import com.poketeam.app.ui.pokedex.PokedexViewModel
import com.poketeam.app.ui.tema.PokeTeamTema
import com.poketeam.app.ui.tema.LocalPaleta
import com.poketeam.app.ui.tema.Paletas
import com.poketeam.app.ui.tema.TemaScreen
import com.poketeam.app.ui.builds.NaturesScreen
import com.poketeam.app.ui.pokedex.SeletorPool
import com.poketeam.app.ui.pokedex.LocalAnimados
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.People
import com.poketeam.app.ui.amigos.AmigosScreen
import com.poketeam.app.ui.amigos.AmigosViewModel
import com.poketeam.app.ui.amigos.AmigoScreen
import com.poketeam.app.ui.amigos.AmigoViewModel
import com.poketeam.app.ui.atualizacao.CaixaAtualizacao
import com.poketeam.app.ui.atualizacao.DialogoAtualizacao
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.poketeam.app.ui.times.EditorTimeScreen
import com.poketeam.app.ui.times.EditorTimeViewModel
import com.poketeam.app.ui.times.MeusTimesScreen
import com.poketeam.app.ui.times.MeusTimesViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class Aba(val rota: String, val rotulo: String, val icone: ImageVector) {
    TIMES("times", "Times", Icons.Default.Groups),
    DRAFT("draft", "Draft", Icons.Default.Style),
    POKEDEX("pokedex", "Pokédex", Icons.Default.Search),
    AMIGOS("amigos", "Amigos", Icons.Default.People),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PokeTeamApp
        val pokedex = app.pokedex
        val times = app.times
        val pools = app.pools
        // Regra do pool ativo, usada na Pokédex e na escolha do time.
        val regraAtiva = pools.ativo.map { pool -> { p: Pokemon -> pool.permite(p) } }

        setContent {
            val prefs by app.prefs.valor.collectAsStateWithLifecycle()
            val paleta = Paletas.porId(prefs?.tema)
            // Ícones da barra de status claros ou escuros conforme o tema.
            LaunchedEffect(paleta.claro) {
                val t = android.graphics.Color.TRANSPARENT
                val estilo = if (paleta.claro) SystemBarStyle.light(t, t) else SystemBarStyle.dark(t)
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }
            // Procura versão nova ao abrir o app e sempre que ele volta para a tela.
            val escopoAtualizacao = rememberCoroutineScope()
            LifecycleEventEffect(Lifecycle.Event.ON_START) { escopoAtualizacao.launch { app.atualizador.checarSeFaltar() } }
            PokeTeamTema(paleta) {
              CompositionLocalProvider(LocalAnimados provides app.animados) {
                DialogoAtualizacao(app.atualizador)
                val nav = rememberNavController()
                val rotaAtual = nav.currentBackStackEntryAsState().value?.destination?.route
                val escopo = rememberCoroutineScope()
                val estadoPools by pools.estado.collectAsStateWithLifecycle()
                val poolAtivo = estadoPools?.ativo ?: Pool.TUDO
                val listaPools = estadoPools?.todos.orEmpty()
                val escolherPool: (String) -> Unit = { id -> escopo.launch { pools.ativar(id) } }

                val naAba = Aba.entries.any { it.rota == rotaAtual }
                Scaffold(
                    topBar = {
                        if (naAba) BarraTopo(listaPools, poolAtivo, escolherPool,
                            aoGerenciar = { nav.navigate("pools") }, aoTema = { nav.navigate("tema") })
                    },
                    bottomBar = {
                        if (Aba.entries.any { it.rota == rotaAtual }) {
                            NavigationBar {
                                Aba.entries.forEach { aba ->
                                    NavigationBarItem(
                                        selected = rotaAtual == aba.rota,
                                        onClick = { nav.irParaAba(aba.rota) },
                                        icon = { Icon(aba.icone, null) },
                                        label = { Text(aba.rotulo) },
                                    )
                                }
                            }
                        }
                    },
                ) { espaco ->
                    NavHost(
                        nav,
                        startDestination = Aba.TIMES.rota,
                        modifier = Modifier.padding(espaco).consumeWindowInsets(espaco),
                    ) {
                        composable(Aba.TIMES.rota) {
                            val vm: MeusTimesViewModel = viewModel(factory = viewModelFactory {
                                initializer { MeusTimesViewModel(times, pokedex, app.sorteador, app.conta) }
                            })
                            MeusTimesScreen(vm, poolAtivo.nome) { id -> nav.navigate("time/$id") }
                        }
                        composable(Aba.POKEDEX.rota) {
                            val vm: PokedexViewModel = viewModel(factory = viewModelFactory {
                                initializer { PokedexViewModel(pokedex, regraAtiva, app.prefs.favoritos) }
                            })
                            // O pool fica na barra de topo; aqui não repete o seletor.
                            PokedexScreen(vm, aoAbrir = { nav.navigate("pokemon/${it.id}") })
                        }
                        composable("pokemon/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                            val p by produceState<Pokemon?>(null, id) { value = pokedex.porId(id) }
                            val bs by produceState(emptyList<com.poketeam.app.domain.Build>(), id) { value = app.builds.de(id) }
                            val megas by produceState(emptyList<Pokemon>(), p) { value = p?.let { pokedex.megasDe(it.dex) }.orEmpty() }
                            val favoritos by app.prefs.favoritos.collectAsStateWithLifecycle()
                            p?.let {
                                DetalheScreen(it, voltar = { nav.popBackStack() }, builds = bs, favorito = id in favoritos,
                                    aoFavoritar = { app.prefs.alternarFavorito(id) }, aoNatures = { nav.navigate("natures") }, megas = megas)
                            }
                        }
                        composable("time/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id").orEmpty()
                            val vm: EditorTimeViewModel = viewModel(factory = viewModelFactory {
                                initializer { EditorTimeViewModel(id, times, pokedex, app.sorteador, pools, app.builds, app.conta) }
                            })
                            EditorTimeScreen(
                                vm,
                                nomePool = poolAtivo.nome,
                                aoAdicionar = { nav.navigate("escolher/$id") },
                                aoVerFicha = { nav.navigate("pokemon/${it.id}") },
                                aoSair = { nav.popBackStack() },
                            )
                        }
                        composable("escolher/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id").orEmpty()
                            val vm: PokedexViewModel = viewModel(factory = viewModelFactory {
                                initializer { PokedexViewModel(pokedex, regraAtiva, app.prefs.favoritos, favoritosPrimeiro = true) }
                            })
                            val lista by times.todos.collectAsStateWithLifecycle()
                            val time = lista?.firstOrNull { it.id == id }
                            PokedexScreen(
                                vm,
                                aoAbrir = { p ->
                                    escopo.launch {
                                        times.adicionarMembro(id, p.id)
                                        nav.popBackStack()
                                    }
                                },
                                tituloEscolha = "Escolher para ${time?.nome ?: "o time"}",
                                marcados = time?.membros?.toSet().orEmpty(),
                                aoVoltar = { nav.popBackStack() },
                                pools = listaPools, poolAtivo = poolAtivo, aoEscolherPool = escolherPool,
                            )
                        }
                        composable(Aba.DRAFT.rota) {
                            val vm: DraftViewModel = viewModel(factory = viewModelFactory {
                                initializer { DraftViewModel(app.drafts, pokedex, pools, app.prefs) }
                            })
                            DraftScreen(vm, aoSalvar = { nav.irParaAba(Aba.TIMES.rota) })
                        }
                        composable("pools") {
                            val vm: PoolsViewModel = viewModel(factory = viewModelFactory {
                                initializer { PoolsViewModel(pools, pokedex, app.elencos) }
                            })
                            PoolsScreen(vm, aoVoltar = { nav.popBackStack() }) { id -> nav.navigate("pool/$id") }
                        }
                        composable("natures") { NaturesScreen { nav.popBackStack() } }
                        composable(Aba.AMIGOS.rota) {
                            val vm: AmigosViewModel = viewModel(factory = viewModelFactory {
                                initializer { AmigosViewModel(app.conta, times) }
                            })
                            AmigosScreen(vm) { id -> nav.navigate("amigo/$id") }
                        }
                        composable("amigo/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id")?.toIntOrNull() ?: 0
                            val vm: AmigoViewModel = viewModel(factory = viewModelFactory {
                                initializer { AmigoViewModel(id, app.conta, times, pokedex) }
                            })
                            AmigoScreen(vm) { nav.popBackStack() }
                        }
                        composable("tema") {
                            TemaScreen(paleta.id, aoEscolher = app.prefs::mudarTema, aoVoltar = { nav.popBackStack() }) {
                                CaixaAtualizacao(app.atualizador)
                            }
                        }
                        composable("pool/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id").orEmpty()
                            val vm: EditorPoolViewModel = viewModel(factory = viewModelFactory {
                                initializer { EditorPoolViewModel(id, pools, pokedex) }
                            })
                            EditorPoolScreen(vm, aoBanir = { nav.navigate("banir/$id") }, aoSair = { nav.popBackStack() })
                        }
                        composable("banir/{id}") { entrada ->
                            val id = entrada.arguments?.getString("id").orEmpty()
                            // Mostra as regras do pool sem os banidos, para dar para desbanir tocando de novo.
                            val regra = pools.estado.map { e ->
                                val pool = e?.todos?.firstOrNull { it.id == id } ?: Pool.TUDO
                                ({ p: Pokemon -> pool.permiteSemBanidos(p) })
                            }
                            val vm: PokedexViewModel = viewModel(factory = viewModelFactory {
                                initializer { PokedexViewModel(pokedex, regra) }
                            })
                            val pool = listaPools.firstOrNull { it.id == id }
                            PokedexScreen(
                                vm,
                                aoAbrir = { p -> escopo.launch { pools.alternarBanido(id, p.id) } },
                                tituloEscolha = "Banir de ${pool?.nome ?: "pool"} (toque de novo para desbanir)",
                                marcados = pool?.banidos.orEmpty(),
                                marcadoClicavel = true,
                                iconeMarcado = Icons.Default.Block,
                                descricaoMarcado = "Banido",
                                aoVoltar = { nav.popBackStack() },
                            )
                        }
                    }
                }
              }
            }
        }
    }
}

@Composable
private fun BarraTopo(
    pools: List<Pool>,
    ativo: Pool,
    aoEscolher: (String) -> Unit,
    aoGerenciar: () -> Unit,
    aoTema: () -> Unit,
) {
    val p = LocalPaleta.current
    Row(
        Modifier.fillMaxWidth().background(p.fundo).statusBarsPadding().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            SeletorPool(pools, ativo, aoEscolher, modifier = Modifier, aoGerenciar = aoGerenciar)
        }
        IconButton(onClick = aoTema) { Icon(Icons.Default.Settings, "Ajustes", tint = p.sub) }
    }
}

private fun NavController.irParaAba(rota: String) = navigate(rota) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
