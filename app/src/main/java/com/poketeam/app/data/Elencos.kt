package com.poketeam.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Pokémons disponíveis num modpack (gerado por tools/gerar_elenco_cobblemon.py). */
@Serializable
data class Elenco(val nome: String, val permitidos: List<Int>)

/** Elencos empacotados na pasta assets/elencos (um .json por modpack). */
class Elencos(private val context: Context) {
    private var cache: List<Elenco>? = null

    suspend fun todos(): List<Elenco> = cache ?: withContext(Dispatchers.IO) {
        val json = Json { ignoreUnknownKeys = true }
        context.assets.list("elencos").orEmpty().filter { it.endsWith(".json") }.sorted().map { arquivo ->
            json.decodeFromString<Elenco>(context.assets.open("elencos/$arquivo").bufferedReader().use { it.readText() })
        }
    }.also { cache = it }
}
