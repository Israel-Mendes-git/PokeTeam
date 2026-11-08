package com.poketeam.app.ui.times

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Times
import com.poketeam.app.data.Pools
import com.poketeam.app.domain.Sugestao
import com.poketeam.app.domain.Sugestoes
import kotlinx.coroutines.flow.combine
import com.poketeam.app.data.Sorteador
import com.poketeam.app.domain.RegrasSorteio
import kotlinx.coroutines.flow.MutableStateFlow
import com.poketeam.app.domain.AnaliseTime
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Time com os pokémons já resolvidos a partir dos ids. */
data class TimeResolvido(val time: Time, val pokemons: List<Pokemon>) {
    val analise: AnaliseTime by lazy { AnaliseTime.de(pokemons) }
}

private suspend fun Pokedex.resolver(t: Time) = TimeResolvido(t, t.membros.mapNotNull { porId(it) })

class MeusTimesViewModel(private val times: Times, private val pokedex: Pokedex, private val sorteador: Sorteador) : ViewModel() {
    val lista: StateFlow<List<TimeResolvido>?> = times.todos
        .map { l -> l?.map { pokedex.resolver(it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun novo(aoCriar: (Time) -> Unit) = viewModelScope.launch { aoCriar(times.criar()) }

    /** Cria um time já sorteado com as regras atuais e abre. */
    fun novoSorteado(aoCriar: (Time) -> Unit) = viewModelScope.launch {
        val t = times.criar()
        sorteador.completar(t.id)
        aoCriar(t)
    }
    fun duplicar(id: String) = viewModelScope.launch { times.duplicar(id) }
    fun excluir(id: String) = viewModelScope.launch { times.excluir(id) }
    fun alternarFavorito(id: String) = viewModelScope.launch { times.atualizar(id) { it.copy(favorito = !it.favorito) } }
}

class EditorTimeViewModel(
    private val id: String,
    private val times: Times,
    private val pokedex: Pokedex,
    private val sorteador: Sorteador,
    pools: Pools,
) : ViewModel() {
    val time: StateFlow<TimeResolvido?> = times.todos
        .map { l -> l?.firstOrNull { it.id == id }?.let { pokedex.resolver(it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val regras: StateFlow<RegrasSorteio> = sorteador.regras

    /** Quem do pool ativo mais fecha os pontos fracos do time (vazio se o time está vazio ou cheio). */
    val sugestoes: StateFlow<List<Sugestao>> = combine(time, pools.ativo, sorteador.regras) { t, pool, regras ->
        if (t == null) emptyList()
        else Sugestoes.para(t.pokemons, pokedex.todos().filter { pool.permite(it) && (!regras.soFinais || it.final) })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun adicionar(p: Pokemon) = viewModelScope.launch { times.adicionarMembro(id, p.id) }

    /** Time em texto para mandar aos amigos; os nomes seguem o formato de importação do Showdown. */
    fun textoParaCompartilhar(): String? {
        val t = time.value ?: return null
        return buildString {
            appendLine(t.time.nome)
            appendLine()
            t.pokemons.forEach { appendLine(it.nomeShowdown); appendLine() }
        }.trimEnd()
    }
    fun mudarRegras(f: (RegrasSorteio) -> RegrasSorteio) = sorteador.mudarRegras(f)

    private val _sorteando = MutableStateFlow(false)
    val sorteando: StateFlow<Boolean> = _sorteando

    /** Mensagem curta depois do sorteio (ex.: vagas que o pool não conseguiu preencher). */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso
    fun avisoVisto() { _aviso.value = null }

    fun sortear() = viewModelScope.launch {
        _sorteando.value = true
        val r = sorteador.completar(id)
        _sorteando.value = false
        if (r != null && r.faltaram > 0) {
            _aviso.value = "O pool ativo não tem pokémons suficientes para essas regras: " +
                if (r.faltaram == 1) "1 vaga ficou vazia." else "${r.faltaram} vagas ficaram vazias."
        }
    }


    fun renomear(nome: String) = viewModelScope.launch { times.atualizar(id) { it.copy(nome = nome) } }

    fun remover(indice: Int) = viewModelScope.launch {
        times.atualizar(id) { t -> t.copy(membros = t.membros.filterIndexed { i, _ -> i != indice }) }
    }

    fun descartarSeVazio() = times.descartarSeVazio(id)
}
