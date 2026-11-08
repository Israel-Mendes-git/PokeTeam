package com.poketeam.app.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class ModoDraft { OPCOES, LIVRE }

/** Configuração da tela de início do draft (lembrada entre drafts). */
@Serializable
data class ConfigDraft(
    val jogadores: List<String> = listOf("Jogador 1", "Jogador 2"),
    val porJogador: Int = 6,
    val serpente: Boolean = true,
    val modo: ModoDraft = ModoDraft.OPCOES,
    val soFinais: Boolean = true,
)

@Serializable
data class Escolha(val jogador: Int, val pokemonId: Int)

/**
 * Draft passa-celular. Os jogadores escolhem em turnos; o que um escolhe sai para todos
 * (por espécie: se alguém pegou Vulpix, ninguém pega Vulpix de Alola).
 */
@Serializable
data class Draft(
    val jogadores: List<String>,
    val porJogador: Int = Time.MAXIMO,
    /** Serpente: 1-2-3, 3-2-1, 1-2-3... Senão sempre 1-2-3. */
    val serpente: Boolean = true,
    val modo: ModoDraft = ModoDraft.OPCOES,
    /** Cópia do pool no início do draft (mudar o pool ativo no meio não afeta). */
    val pool: Pool,
    val soFinais: Boolean = true,
    val escolhas: List<Escolha> = emptyList(),
    /** Opções mostradas na vez atual (modo OPCOES). */
    val opcoes: List<Int> = emptyList(),
) {
    val total: Int get() = jogadores.size * porJogador
    val terminou: Boolean get() = escolhas.size >= total

    /** Jogador da escolha número [n] (0 = primeira escolha do draft). */
    fun jogadorDaEscolha(n: Int): Int {
        val j = jogadores.size
        val rodada = n / j
        val pos = n % j
        return if (serpente && rodada % 2 == 1) j - 1 - pos else pos
    }

    val vez: Int? get() = if (terminou) null else jogadorDaEscolha(escolhas.size)

    fun escolhasDe(jogador: Int): List<Int> = escolhas.filter { it.jogador == jogador }.map { it.pokemonId }

    /** Pokémons que ainda podem ser escolhidos. */
    fun disponiveis(todos: List<Pokemon>): List<Pokemon> {
        val pegos = escolhas.map { it.pokemonId }.toSet()
        val dexPegos = todos.filter { it.id in pegos }.map { it.dex }.toSet()
        return todos.filter { p -> pool.permite(p) && (!soFinais || p.final) && p.dex !in dexPegos }
    }

    fun escolher(pokemonId: Int, todos: List<Pokemon>, random: Random = Random.Default): Draft {
        val j = vez ?: return this
        if (disponiveis(todos).none { it.id == pokemonId }) return this
        return copy(escolhas = escolhas + Escolha(j, pokemonId)).comNovasOpcoes(todos, random)
    }

    fun desfazer(todos: List<Pokemon>, random: Random = Random.Default): Draft =
        if (escolhas.isEmpty()) this else copy(escolhas = escolhas.dropLast(1)).comNovasOpcoes(todos, random)

    /** Sorteia [QTD_OPCOES] opções para a vez atual (vazio se terminou ou no modo livre). */
    fun comNovasOpcoes(todos: List<Pokemon>, random: Random = Random.Default): Draft =
        if (terminou || modo != ModoDraft.OPCOES) copy(opcoes = emptyList())
        else copy(opcoes = Sorteio.sortear(
            disponiveis(todos), emptyList(),
            RegrasSorteio(soFinais = false, semTipoRepetido = true, evitarFraquezas = false),
            tamanho = QTD_OPCOES, random = random,
        ).time.map { it.id })

    companion object {
        const val QTD_OPCOES = 3
        const val MIN_JOGADORES = 2
        const val MAX_JOGADORES = 4
    }
}
