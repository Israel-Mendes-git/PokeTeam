package com.poketeam.app.ui.pokedex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Pokedex
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Tipo
import com.poketeam.app.domain.normalizar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FiltroPokedex(
    val busca: String = "",
    val tipos: Set<Tipo> = emptySet(),
    val geracoes: Set<Int> = emptySet(),
    val soFinais: Boolean = false,
    val semLendarios: Boolean = false,
    val soFavoritos: Boolean = false,
) {
    val ativos: Int get() = tipos.size + geracoes.size + (if (soFinais) 1 else 0) + (if (semLendarios) 1 else 0) + (if (soFavoritos) 1 else 0)
}

/**
 * @param regraPool o que entra na lista além da busca/filtros. Normalmente o pool ativo;
 * na tela de banir são as regras do pool sem os banidos.
 */
class PokedexViewModel(
    private val pokedex: Pokedex,
    regraPool: Flow<(Pokemon) -> Boolean> = flowOf { true },
    /** Ids favoritados; marcados com estrela na lista e usados no filtro "Favoritos". */
    favoritos: Flow<Set<Int>> = flowOf(emptySet()),
) : ViewModel() {
    val favoritos: StateFlow<Set<Int>> = favoritos.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val todos = MutableStateFlow<List<Pokemon>?>(null)
    private val _filtro = MutableStateFlow(FiltroPokedex())
    val filtro: StateFlow<FiltroPokedex> = _filtro

    val resultado: StateFlow<List<Pokemon>?> = combine(todos, _filtro, regraPool, this.favoritos) { lista, f, regra, favs ->
        // Termos normalizados uma vez por busca, não uma vez por pokémon.
        val termos = normalizar(f.busca).split(' ').filter(String::isNotEmpty)
        lista?.filter { p -> regra(p) && passa(p, f, termos) && (!f.soFavoritos || p.id in favs) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { todos.value = pokedex.todos() }
    }

    fun buscar(texto: String) = _filtro.update { it.copy(busca = texto) }
    fun alternarTipo(t: Tipo) = _filtro.update { it.copy(tipos = it.tipos.alternar(t)) }
    fun alternarGeracao(g: Int) = _filtro.update { it.copy(geracoes = it.geracoes.alternar(g)) }
    fun alternarFinais() = _filtro.update { it.copy(soFinais = !it.soFinais) }
    fun alternarLendarios() = _filtro.update { it.copy(semLendarios = !it.semLendarios) }
    fun alternarFavoritos() = _filtro.update { it.copy(soFavoritos = !it.soFavoritos) }
    fun limparFiltros() = _filtro.update { FiltroPokedex(busca = it.busca) }

    private fun passa(p: Pokemon, f: FiltroPokedex, termos: List<String>): Boolean {
        if (termos.isNotEmpty() && !termos.all { p.textoBusca.contains(it) }) return false
        // Vários tipos marcados = o pokémon precisa ter todos (ex.: Água + Voador).
        if (f.tipos.isNotEmpty() && !p.tipos.containsAll(f.tipos)) return false
        if (f.geracoes.isNotEmpty() && p.geracao !in f.geracoes) return false
        if (f.soFinais && !p.final) return false
        if (f.semLendarios && (p.lendario || p.mitico)) return false
        return true
    }
}

private fun <T> Set<T>.alternar(x: T): Set<T> = if (x in this) this - x else this + x
