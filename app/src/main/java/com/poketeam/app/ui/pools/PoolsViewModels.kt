package com.poketeam.app.ui.pools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Pools
import com.poketeam.app.data.Elenco
import com.poketeam.app.data.Elencos
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Pool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PoolComContagem(val pool: Pool, val total: Int, val ativo: Boolean)

class PoolsViewModel(private val pools: Pools, private val pokedex: Pokedex, elencos: Elencos) : ViewModel() {
    private val todos = flow { emit(pokedex.todos()) }

    val lista: StateFlow<List<PoolComContagem>?> = combine(pools.estado, todos) { e, lista ->
        e?.todos?.map { p -> PoolComContagem(p, lista.count(p::permite), p.id == e.ativoId) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ativar(id: String) = viewModelScope.launch { pools.ativar(id) }
    /** Elencos de modpack prontos para virar pool (com quantos pokémons cada um tem). */
    val elencos: StateFlow<List<Pair<Elenco, Int>>> = flow { emit(elencos.todos().map { it to it.permitidos.size }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun novo(aoCriar: (Pool) -> Unit) = viewModelScope.launch { aoCriar(pools.criar()) }

    /** Pool com tudo que não está no elenco já banido. */
    fun novoDeElenco(e: Elenco, aoCriar: (Pool) -> Unit) = viewModelScope.launch {
        val permitidos = e.permitidos.toSet()
        val banidos = pokedex.todos().map { it.id }.filterNot { it in permitidos }.toSet()
        aoCriar(pools.criar(e.nome, banidos))
    }
    fun duplicar(id: String) = viewModelScope.launch { pools.duplicar(id) }
    fun excluir(id: String) = viewModelScope.launch { pools.excluir(id) }
}

data class EstadoEditorPool(val pool: Pool, val total: Int, val banidos: List<Pokemon>)

class EditorPoolViewModel(private val id: String, private val pools: Pools, pokedex: Pokedex) : ViewModel() {
    private val todos = flow { emit(pokedex.todos()) }

    val estado: StateFlow<EstadoEditorPool?> = combine(pools.estado, todos) { e, lista ->
        e?.todos?.firstOrNull { it.id == id }?.let { p ->
            EstadoEditorPool(p, lista.count(p::permite), lista.filter { it.id in p.banidos })
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun mudar(f: (Pool) -> Pool) = viewModelScope.launch { pools.atualizar(id, f) }

    fun renomear(nome: String) = mudar { it.copy(nome = nome) }
    fun alternarGeracao(g: Int) = mudar { it.copy(geracoes = if (g in it.geracoes) it.geracoes - g else it.geracoes + g) }
    fun alternarLendarios() = mudar { it.copy(semLendarios = !it.semLendarios) }
    fun alternarMiticos() = mudar { it.copy(semMiticos = !it.semMiticos) }
    fun alternarFinais() = mudar { it.copy(soFinais = !it.soFinais) }
    fun alternarBebes() = mudar { it.copy(semBebes = !it.semBebes) }
    fun alternarFormas() = mudar { it.copy(semFormas = !it.semFormas) }
    fun desbanir(pokemonId: Int) = viewModelScope.launch { pools.alternarBanido(id, pokemonId) }
    fun limparBanidos() = mudar { it.copy(banidos = emptySet()) }
}
