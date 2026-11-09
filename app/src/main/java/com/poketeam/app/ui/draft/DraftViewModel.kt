package com.poketeam.app.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Drafts
import com.poketeam.app.data.Conta
import com.poketeam.app.data.DraftOnline
import com.poketeam.app.data.ErroServidor
import com.poketeam.app.data.ListaSalas
import kotlinx.coroutines.flow.map
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Pools
import com.poketeam.app.data.Prefs
import com.poketeam.app.domain.ConfigDraft
import com.poketeam.app.domain.Draft
import com.poketeam.app.domain.ModoDraft
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Pool
import com.poketeam.app.domain.normalizar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/** O que a tela precisa, já com pokémons resolvidos. */
data class TelaDraft(
    val draft: Draft,
    val opcoes: List<Pokemon>,
    val escolhasPorJogador: List<List<Pokemon>>,
    /** Disponíveis filtrados pela busca (modo livre ou "escolher da lista"). */
    val lista: List<Pokemon>,
    val totalDisponiveis: Int,
)

class DraftViewModel(
    private val drafts: Drafts,
    private val pokedex: Pokedex,
    private val pools: Pools,
    private val prefs: Prefs,
    private val conta: Conta,
    private val online: DraftOnline,
) : ViewModel() {
    // ---------- online ----------
    val onlineDisponivel: Boolean = conta.disponivel
    val logado: StateFlow<Boolean> = conta.eu.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, conta.eu.value != null)
    private val _salas = MutableStateFlow<ListaSalas?>(null)
    val salas: StateFlow<ListaSalas?> = _salas
    private val _erroOnline = MutableStateFlow<String?>(null)
    val erroOnline: StateFlow<String?> = _erroOnline
    private val _ocupadoOnline = MutableStateFlow(false)
    val ocupadoOnline: StateFlow<Boolean> = _ocupadoOnline

    fun atualizarSalas() = viewModelScope.launch {
        runCatching { online.listar() }.onSuccess { _salas.value = it; _erroOnline.value = null }
            .onFailure { _erroOnline.value = it.message }
    }

    private fun online(acao: suspend () -> String, depois: (String) -> Unit) = viewModelScope.launch {
        if (_ocupadoOnline.value) return@launch
        _ocupadoOnline.value = true
        try {
            val codigo = acao()
            _erroOnline.value = null
            depois(codigo)
        } catch (e: ErroServidor) {
            _erroOnline.value = e.message
        } finally {
            _ocupadoOnline.value = false
        }
    }

    fun criarSala(aoCriar: (String) -> Unit) = online({ online.criar(_config.value, pools.ativo.value).codigo }, aoCriar)
    fun entrarNaSala(codigo: String, aoEntrar: (String) -> Unit) = online({ online.entrar(codigo).codigo }, aoEntrar)

    // Cópia local (o cursor não pula ao digitar nomes); cada mudança também vai para as preferências.
    private val _config = MutableStateFlow(ConfigDraft())
    val config: StateFlow<ConfigDraft> = _config

    init {
        viewModelScope.launch { _config.value = prefs.valor.filterNotNull().first().configDraft }
    }

    private fun mudarConfig(f: (ConfigDraft) -> ConfigDraft) {
        _config.update(f)
        val atual = _config.value
        prefs.mudarConfigDraft { atual }
    }
    val poolAtivo: StateFlow<Pool> = pools.ativo

    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca

    private val todos = flow { emit(pokedex.todos()) }

    /** null = carregando; Result com draft null = sem draft em andamento. */
    val tela: StateFlow<Carga> = combine(drafts.estado, todos, _busca) { e, lista, busca ->
        if (e == null) return@combine Carga.Carregando
        val d = e.atual ?: return@combine Carga.SemDraft
        val porId = lista.associateBy { it.id }
        val disponiveis = d.disponiveis(lista)
        val termos = normalizar(busca).split(' ').filter(String::isNotEmpty)
        Carga.EmAndamento(TelaDraft(
            draft = d,
            opcoes = d.opcoes.mapNotNull(porId::get),
            escolhasPorJogador = d.jogadores.indices.map { j -> d.escolhasDe(j).mapNotNull(porId::get) },
            lista = disponiveis.filter { p -> termos.all { p.textoBusca.contains(it) } },
            totalDisponiveis = disponiveis.size,
        ))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Carga.Carregando)

    // Configuração
    fun nomear(i: Int, nome: String) = mudarConfig { c -> c.copy(jogadores = c.jogadores.mapIndexed { j, n -> if (j == i) nome else n }) }
    fun adicionarJogador() = mudarConfig { c ->
        if (c.jogadores.size >= Draft.MAX_JOGADORES) c else c.copy(jogadores = c.jogadores + "Jogador ${c.jogadores.size + 1}")
    }
    fun removerJogador() = mudarConfig { c ->
        if (c.jogadores.size <= Draft.MIN_JOGADORES) c else c.copy(jogadores = c.jogadores.dropLast(1))
    }
    fun mudarPorJogador(n: Int) = mudarConfig { it.copy(porJogador = n.coerceIn(1, 6)) }
    fun alternarSerpente() = mudarConfig { it.copy(serpente = !it.serpente) }
    fun mudarModo(m: ModoDraft) = mudarConfig { it.copy(modo = m) }
    fun alternarFinais() = mudarConfig { it.copy(soFinais = !it.soFinais) }

    fun iniciar() = viewModelScope.launch {
        val c = _config.value
        drafts.iniciar(Draft(
            jogadores = c.jogadores.mapIndexed { i, n -> n.ifBlank { "Jogador ${i + 1}" } },
            porJogador = c.porJogador, serpente = c.serpente, modo = c.modo,
            pool = pools.ativo.value, soFinais = c.soFinais,
        ))
    }

    // Durante o draft
    fun buscar(t: String) { _busca.value = t }
    fun escolher(p: Pokemon) = viewModelScope.launch { drafts.escolher(p.id); _busca.value = "" }
    fun desfazer() = viewModelScope.launch { drafts.desfazer() }
    fun outrasOpcoes() = viewModelScope.launch { drafts.outrasOpcoes() }
    fun encerrar() = viewModelScope.launch { drafts.encerrar() }
    fun salvarComoTimes(depois: () -> Unit) = viewModelScope.launch { drafts.salvarComoTimes(); depois() }
}

sealed interface Carga {
    data object Carregando : Carga
    data object SemDraft : Carga
    data class EmAndamento(val tela: TelaDraft) : Carga
}
