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

    suspend fun todos(): List<Pokemon> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            val texto = context.assets.open("pokemon.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<List<Pokemon>>(texto)
        }.also {
            cache = it
            porId = it.associateBy(Pokemon::id)
        }
    }

    suspend fun porId(id: Int): Pokemon? {
        todos()
        return porId[id]
    }
}
