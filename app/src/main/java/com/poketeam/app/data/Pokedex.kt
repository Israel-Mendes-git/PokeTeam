package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.Pokemon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Base de pokémons empacotada em assets/pokemon.json (gerada por tools/gerar_pokedex.py). */
class Pokedex(private val context: Context) {
    private val mutex = Mutex()
    private var cache: List<Pokemon>? = null
    private var porId: Map<Int, Pokemon> = emptyMap()
    private var megasPorDex: Map<Int, List<Pokemon>> = emptyMap()

    /** Todos os pokémons escolhíveis (sem as formas Mega, que são de batalha). */
    suspend fun todos(): List<Pokemon> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            val texto = context.assets.open("pokemon.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<List<Pokemon>>(texto)
        }.also { tudo ->
            porId = tudo.associateBy(Pokemon::id)
            megasPorDex = tudo.filter { it.mega }.groupBy { it.dex }
        }.filterNot { it.mega }.also { cache = it }
    }

    /** Formas Mega da espécie (Charizard tem duas). */
    suspend fun megasDe(dex: Int): List<Pokemon> {
        todos()
        return megasPorDex[dex].orEmpty()
    }

    suspend fun porId(id: Int): Pokemon? {
        todos()
        return porId[id]
    }
}
