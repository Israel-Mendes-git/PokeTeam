package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.Time
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.util.UUID

/** Times salvos em filesDir/times.json. */
class Times(context: Context) {
    private val arquivo = ArquivoJson(File(context.filesDir, "times.json"), ListSerializer(Time.serializer())) { emptyList() }

    /** null enquanto carrega. Favoritos primeiro, depois os alterados mais recentemente. */
    val todos: StateFlow<List<Time>?> = arquivo.valor
        .map { it?.let(::ordenar) }
        .stateIn(arquivo.escopo, SharingStarted.Eagerly, null)

    suspend fun criar(): Time = arquivo.alterar { lista ->
        val agora = System.currentTimeMillis()
        val novo = Time(id = UUID.randomUUID().toString(), nome = "Time ${lista.size + 1}", criadoEm = agora)
        lista + novo to novo
    }

    suspend fun criarCom(nome: String, membros: List<Int>): Time = arquivo.alterar { lista ->
        val agora = System.currentTimeMillis()
        val novo = Time(id = UUID.randomUUID().toString(), nome = nome, membros = membros.take(Time.MAXIMO), criadoEm = agora)
        lista + novo to novo
    }

    suspend fun atualizar(id: String, mudanca: (Time) -> Time) = arquivo.alterar { lista ->
        lista.map { if (it.id == id) mudanca(it).copy(alteradoEm = System.currentTimeMillis()) else it } to Unit
    }

    suspend fun duplicar(id: String) = arquivo.alterar { lista ->
        val original = lista.firstOrNull { it.id == id } ?: return@alterar lista to Unit
        val agora = System.currentTimeMillis()
        lista + original.copy(id = UUID.randomUUID().toString(), nome = "${original.nome} (cópia)",
            favorito = false, criadoEm = agora, alteradoEm = agora) to Unit
    }

    suspend fun excluir(id: String) = arquivo.alterar { lista -> lista.filterNot { it.id == id } to Unit }

    suspend fun adicionarMembro(id: String, pokemonId: Int) = atualizar(id) { t ->
        if (t.cheio || pokemonId in t.membros) t else t.copy(membros = t.membros + pokemonId)
    }

    /** Time sem ninguém e com nome padrão foi criado à toa: some ao sair do editor. Não depende de tela viva. */
    fun descartarSeVazio(id: String) {
        arquivo.escopo.launch {
            arquivo.alterar { lista ->
                lista.filterNot { it.id == id && it.membros.isEmpty() && it.nome.matches(NOME_PADRAO) } to Unit
            }
        }
    }

    private fun ordenar(l: List<Time>) =
        l.sortedWith(compareByDescending<Time> { it.favorito }.thenByDescending { it.alteradoEm })

    private companion object {
        val NOME_PADRAO = Regex("Time \\d+")
    }
}
