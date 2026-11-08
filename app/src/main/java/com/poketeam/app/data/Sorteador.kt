package com.poketeam.app.data

import com.poketeam.app.domain.RegrasSorteio
import com.poketeam.app.domain.ResultadoSorteio
import com.poketeam.app.domain.Sorteio
import com.poketeam.app.domain.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Sorteia vagas de um time usando o pool ativo, com as regras salvas nas preferências. */
class Sorteador(private val pokedex: Pokedex, private val pools: Pools, private val times: Times, private val prefs: Prefs) {
    val regras: StateFlow<RegrasSorteio> = prefs.regrasSorteio
    val favoritos: StateFlow<Set<Int>> = prefs.favoritos

    /** Candidatos do pool ativo segundo as regras (inclusive "só favoritos"). */
    suspend fun candidatos(r: RegrasSorteio = regras.value): List<com.poketeam.app.domain.Pokemon> {
        val pool = pools.ativo.value
        val favs = favoritos.value
        return pokedex.todos().filter { pool.permite(it) && (!r.soFavoritos || it.id in favs) }
    }
    fun mudarRegras(f: (RegrasSorteio) -> RegrasSorteio) = prefs.mudarRegras(f)

    /** Completa as vagas vazias do time [id] e grava. */
    suspend fun completar(id: String): ResultadoSorteio? = withContext(Dispatchers.Default) {
        val time = times.todos.value?.firstOrNull { it.id == id } ?: return@withContext null
        val travados = time.membros.mapNotNull { pokedex.porId(it) }
        val candidatos = candidatos()
        val r = Sorteio.sortear(candidatos, travados, regras.value, Time.MAXIMO)
        times.atualizar(id) { it.copy(membros = r.time.map { p -> p.id }) }
        r
    }
}
