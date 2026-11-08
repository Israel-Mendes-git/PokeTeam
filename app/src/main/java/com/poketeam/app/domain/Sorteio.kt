package com.poketeam.app.domain

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class RegrasSorteio(
    val soFinais: Boolean = true,
    /** Nenhum tipo aparece em dois membros (Água/Voador impede outro Água ou outro Voador). */
    val semTipoRepetido: Boolean = true,
    /** Tenta várias combinações e fica com a de menos fraquezas graves. */
    val evitarFraquezas: Boolean = true,
    /** Sortear, Recomendar e sugestões só entre os pokémons favoritos. */
    val soFavoritos: Boolean = false,
)

data class ResultadoSorteio(
    val time: List<Pokemon>,
    /** Vagas que não deu para preencher (pool pequeno demais para as regras). */
    val faltaram: Int,
)

object Sorteio {
    private const val TENTATIVAS = 300

    /**
     * Completa [travados] até [tamanho] com pokémons de [candidatos].
     * Nunca repete espécie (mesmo número de dex) nem mexe nos travados.
     */
    fun sortear(
        candidatos: List<Pokemon>,
        travados: List<Pokemon>,
        regras: RegrasSorteio = RegrasSorteio(),
        tamanho: Int = Time.MAXIMO,
        random: Random = Random.Default,
    ): ResultadoSorteio {
        val vagas = tamanho - travados.size
        if (vagas <= 0) return ResultadoSorteio(travados.take(tamanho), 0)

        val base = candidatos.filter { c ->
            (!regras.soFinais || c.final) && travados.none { it.dex == c.dex }
        }
        val tentativas = if (regras.evitarFraquezas) TENTATIVAS else 1

        var melhor: List<Pokemon>? = null
        var melhorNota = Int.MAX_VALUE
        repeat(tentativas) {
            val time = montar(base.shuffled(random), travados, vagas, regras)
            // Menos vagas vazias pesa mais que fraquezas.
            val nota = (tamanho - time.size) * 100 + AnaliseTime.de(time).problemas.size
            if (nota < melhorNota) {
                melhor = time
                melhorNota = nota
            }
            if (melhorNota == 0) return ResultadoSorteio(time, 0)
        }
        val final = melhor ?: travados
        return ResultadoSorteio(final, tamanho - final.size)
    }

    private fun montar(embaralhados: List<Pokemon>, travados: List<Pokemon>, vagas: Int, regras: RegrasSorteio): List<Pokemon> {
        val time = travados.toMutableList()
        var faltam = vagas
        for (c in embaralhados) {
            if (faltam == 0) break
            if (time.any { it.dex == c.dex }) continue
            if (regras.semTipoRepetido && time.any { m -> m.tipos.any { it in c.tipos } }) continue
            time += c
            faltam--
        }
        return time
    }
}
