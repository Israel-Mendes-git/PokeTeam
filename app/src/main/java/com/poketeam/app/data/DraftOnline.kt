package com.poketeam.app.data

import com.poketeam.app.domain.ConfigDraft
import com.poketeam.app.domain.Draft
import com.poketeam.app.domain.Escolha
import com.poketeam.app.domain.ModoDraft
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Pool
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class JogadorSala(val id: Int, val apelido: String)

@Serializable
data class EscolhaSala(val jogador: Int, val pokemonId: Int, val dex: Int)

/** Sala de draft online, como o servidor devolve. */
@Serializable
data class Sala(
    val codigo: String,
    val dono: Int,
    /** espera | andamento | fim */
    val estado: String,
    val porJogador: Int,
    val serpente: Boolean = true,
    val modo: ModoDraft = ModoDraft.OPCOES,
    val soFinais: Boolean = true,
    val pool: Pool,
    val jogadores: List<JogadorSala> = emptyList(),
    val escolhas: List<EscolhaSala> = emptyList(),
    val versao: Int = 0,
) {
    val emEspera: Boolean get() = estado == "espera"
    val terminou: Boolean get() = estado == "fim"

    /** O mesmo [Draft] do modo passa-celular, para reaproveitar regras e telas. */
    fun comoDraft(): Draft = Draft(
        jogadores = jogadores.map { it.apelido }, porJogador = porJogador, serpente = serpente, modo = modo,
        pool = pool, soFinais = soFinais, escolhas = escolhas.map { Escolha(it.jogador, it.pokemonId) },
    )

    fun indiceDe(usuario: Int?): Int? = jogadores.indexOfFirst { it.id == usuario }.takeIf { it >= 0 }
}

@Serializable
data class ResumoSala(
    val codigo: String,
    val estado: String,
    val dono: Int,
    val jogadores: List<String> = emptyList(),
    val porJogador: Int = 6,
)

@Serializable
data class ListaSalas(val minhas: List<ResumoSala> = emptyList(), val abertas: List<ResumoSala> = emptyList())

/** Draft com amigos, cada um no seu celular. Precisa estar logado (aba Amigos). */
class DraftOnline(private val conta: Conta) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun listar(): ListaSalas = json.decodeFromString(conta.chamar("GET", "/salas"))

    suspend fun criar(c: ConfigDraft, pool: Pool): Sala {
        val corpo = buildJsonObject {
            put("porJogador", c.porJogador)
            put("serpente", c.serpente)
            put("modo", c.modo.name)
            put("soFinais", c.soFinais)
            put("pool", json.encodeToJsonElement(Pool.serializer(), pool))
        }
        return json.decodeFromString(conta.chamar("POST", "/salas", corpo.toString()))
    }

    /** null = nada mudou desde [versao]. */
    suspend fun ver(codigo: String, versao: Int): Sala? {
        val texto = conta.chamar("GET", "/salas/$codigo?versao=$versao")
        val obj = json.decodeFromString<JsonObject>(texto)
        return if ("mudou" in obj) null else json.decodeFromJsonElement(Sala.serializer(), obj)
    }

    suspend fun entrar(codigo: String): Sala =
        json.decodeFromString(conta.chamar("POST", "/salas/${normalizarCodigo(codigo)}/entrar"))

    suspend fun sair(codigo: String) { conta.chamar("POST", "/salas/$codigo/sair") }
    suspend fun iniciar(codigo: String): Sala = json.decodeFromString(conta.chamar("POST", "/salas/$codigo/iniciar"))
    suspend fun desfazer(codigo: String): Sala = json.decodeFromString(conta.chamar("POST", "/salas/$codigo/desfazer"))

    suspend fun escolher(codigo: String, p: Pokemon, n: Int): Sala {
        val corpo = buildJsonObject { put("pokemonId", p.id); put("dex", p.dex); put("n", n) }
        return json.decodeFromString(conta.chamar("POST", "/salas/$codigo/escolher", corpo.toString()))
    }

    companion object {
        fun normalizarCodigo(c: String) = c.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }
    }
}
