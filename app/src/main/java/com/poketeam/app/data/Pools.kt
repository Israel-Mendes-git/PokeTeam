package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.Pool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
data class EstadoPools(
    /** Pools criados pelo usuário ("Tudo" é fixo e não fica no arquivo). */
    val criados: List<Pool> = emptyList(),
    val ativoId: String = Pool.ID_TUDO,
) {
    val todos: List<Pool> get() = listOf(Pool.TUDO) + criados
    val ativo: Pool get() = todos.firstOrNull { it.id == ativoId } ?: Pool.TUDO
}

/** Pools salvos em filesDir/pools.json. */
class Pools(context: Context) {
    private val arquivo = ArquivoJson(File(context.filesDir, "pools.json"), EstadoPools.serializer()) {
        // Primeiro uso: já vem um pool "Cobblemon" (começa com todos; o usuário bane o que a versão dele não tem).
        EstadoPools(criados = listOf(Pool(id = UUID.randomUUID().toString(), nome = "Cobblemon")))
    }

    val estado: StateFlow<EstadoPools?> = arquivo.valor

    /** Pool ativo (Tudo enquanto carrega). */
    val ativo: StateFlow<Pool> = arquivo.valor
        .map { it?.ativo ?: Pool.TUDO }
        .stateIn(arquivo.escopo, SharingStarted.Eagerly, Pool.TUDO)

    suspend fun ativar(id: String) = arquivo.alterar { it.copy(ativoId = id) to Unit }

    /** Pool novo; com [nome]/[banidos] quando vem de um elenco de modpack. */
    suspend fun criar(nome: String? = null, banidos: Set<Int> = emptySet()): Pool = arquivo.alterar { e ->
        val novo = Pool(id = UUID.randomUUID().toString(), nome = nome ?: "Pool ${e.criados.size + 1}", banidos = banidos)
        e.copy(criados = e.criados + novo) to novo
    }

    suspend fun atualizar(id: String, mudanca: (Pool) -> Pool) = arquivo.alterar { e ->
        e.copy(criados = e.criados.map { if (it.id == id) mudanca(it).copy(id = id) else it }) to Unit
    }

    suspend fun alternarBanido(id: String, pokemonId: Int) = atualizar(id) { p ->
        p.copy(banidos = if (pokemonId in p.banidos) p.banidos - pokemonId else p.banidos + pokemonId)
    }

    suspend fun duplicar(id: String) = arquivo.alterar { e ->
        val original = e.todos.firstOrNull { it.id == id } ?: return@alterar e to Unit
        e.copy(criados = e.criados + original.copy(id = UUID.randomUUID().toString(), nome = "${original.nome} (cópia)")) to Unit
    }

    /** Excluir o pool ativo volta para "Tudo". */
    suspend fun excluir(id: String) = arquivo.alterar { e ->
        e.copy(
            criados = e.criados.filterNot { it.id == id },
            ativoId = if (e.ativoId == id) Pool.ID_TUDO else e.ativoId,
        ) to Unit
    }
}
