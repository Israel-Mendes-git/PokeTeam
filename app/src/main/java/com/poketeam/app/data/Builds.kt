package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ArquivoBuilds(
    val builds: Map<String, List<Build>> = emptyMap(),
    /** Quanto menor, mais forte no competitivo (índice do melhor formato do Smogon). */
    val forca: Map<String, Int> = emptyMap(),
)

/** Builds do Smogon empacotadas em assets/builds.json (gerado por tools/gerar_builds.py). */
class Builds(private val context: Context) {
    private val mutex = Mutex()
    private var dados: ArquivoBuilds? = null

    private suspend fun carregar(): ArquivoBuilds = mutex.withLock {
        dados ?: withContext(Dispatchers.IO) {
            val texto = context.assets.open("builds.json").bufferedReader().use { it.readText() }
            Json { ignoreUnknownKeys = true }.decodeFromString<ArquivoBuilds>(texto)
        }.also { dados = it }
    }

    suspend fun de(pokemonId: Int): List<Build> = carregar().builds[pokemonId.toString()].orEmpty()

    /** null = sem build no Smogon. */
    suspend fun forca(pokemonId: Int): Int? = carregar().forca[pokemonId.toString()]

    suspend fun todasForcas(): Map<Int, Int> = carregar().forca.mapKeys { it.key.toInt() }
}
