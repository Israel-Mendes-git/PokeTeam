package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.Draft
import com.poketeam.app.domain.Pokemon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class EstadoDraft(val atual: Draft? = null)

/** O draft em andamento, salvo em filesDir/draft.json (fechar o app no meio não perde nada). */
class Drafts(context: Context, private val pokedex: Pokedex, private val times: Times) {
    private val arquivo = ArquivoJson(File(context.filesDir, "draft.json"), EstadoDraft.serializer()) { EstadoDraft() }

    /** null enquanto carrega; EstadoDraft(atual = null) quando não há draft. */
    val estado: StateFlow<EstadoDraft?> = arquivo.valor

    suspend fun iniciar(draft: Draft) {
        val todos = pokedex.todos()
        arquivo.alterar { EstadoDraft(draft.comNovasOpcoes(todos)) to Unit }
    }

    suspend fun escolher(pokemonId: Int) = mudar { d, todos -> d.escolher(pokemonId, todos) }
    suspend fun desfazer() = mudar { d, todos -> d.desfazer(todos) }
    suspend fun outrasOpcoes() = mudar { d, todos -> d.comNovasOpcoes(todos) }
    suspend fun encerrar() = arquivo.alterar { EstadoDraft() to Unit }

    /** Cria um time por jogador em "Meus times" e encerra o draft. */
    suspend fun salvarComoTimes() {
        val d = estado.value?.atual ?: return
        d.jogadores.forEachIndexed { i, nome ->
            val ids = d.escolhasDe(i)
            if (ids.isNotEmpty()) times.criarCom("Draft · $nome", ids)
        }
        encerrar()
    }

    private suspend fun mudar(f: (Draft, List<Pokemon>) -> Draft) {
        val todos = pokedex.todos()
        arquivo.alterar { e -> EstadoDraft(e.atual?.let { f(it, todos) }) to Unit }
    }
}
