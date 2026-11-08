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
    /** Só entre os pokémons favoritos. */
    val soFavoritos: Boolean = false,
    val semLendarios: Boolean = false,
    /** Total mínimo de stats base (0 = sem mínimo). */
    val totalMinimo: Int = 0,
    /** Chaves de [Tipo]: o time precisa ter ao menos um de cada. */
    val tiposObrigatorios: Set<String> = emptySet(),
    /** Chaves de [Tipo]: nenhum membro sorteado pode ter. */
    val tiposProibidos: Set<String> = emptySet(),
) {
    val obrigatorios: Set<Tipo> get() = tiposObrigatorios.mapNotNull { runCatching { Tipo.daChave(it) }.getOrNull() }.toSet()

    /** Filtros que valem para cada candidato isoladamente (o resto depende do time inteiro). */
    fun permite(p: Pokemon, favoritos: Set<Int> = emptySet()): Boolean =
        (!soFinais || p.final) &&
            (!soFavoritos || p.id in favoritos) &&
            (!semLendarios || !(p.lendario || p.mitico)) &&
            p.total >= totalMinimo &&
            p.chavesTipos.none { it in tiposProibidos }

    /** Quantas regras fogem do padrão (para mostrar no botão). */
    val ativas: Int
        get() = listOf(!soFinais, !semTipoRepetido, !evitarFraquezas, soFavoritos, semLendarios, totalMinimo > 0).count { it } +
            tiposObrigatorios.size + tiposProibidos.size
}

data class ResultadoSorteio(
    val time: List<Pokemon>,
    /** Vagas que não deu para preencher (pool pequeno demais para as regras). */
    val faltaram: Int,
    /** Tipos obrigatórios que não couberam no time. */
    val tiposFaltando: Set<Tipo> = emptySet(),
)

object Sorteio {
    private const val TENTATIVAS = 300

    /**
     * Completa [travados] até [tamanho] com pokémons de [candidatos].
     * Nunca repete espécie (mesmo número de dex) nem mexe nos travados.
     * Aplica os filtros individuais de [regras] (menos "só favoritos", que quem chama resolve, pois
     * depende da lista de favoritos) e os que dependem do time: tipo repetido e tipos obrigatórios.
     */
    fun sortear(
        candidatos: List<Pokemon>,
        travados: List<Pokemon>,
        regras: RegrasSorteio = RegrasSorteio(),
        tamanho: Int = Time.MAXIMO,
        random: Random = Random.Default,
    ): ResultadoSorteio {
        val obrigatorios = regras.obrigatorios
        val vagas = tamanho - travados.size
        if (vagas <= 0) return ResultadoSorteio(travados.take(tamanho), 0, faltando(travados, obrigatorios))

        val filtro = regras.copy(soFavoritos = false)
        val base = candidatos.filter { c -> filtro.permite(c) && travados.none { it.dex == c.dex } }
        val tentativas = if (regras.evitarFraquezas || obrigatorios.isNotEmpty()) TENTATIVAS else 1

        var melhor: List<Pokemon>? = null
        var melhorNota = Int.MAX_VALUE
        repeat(tentativas) {
            val time = montar(base.shuffled(random), travados, vagas, regras, obrigatorios)
            // Peso: vagas vazias > tipo obrigatório faltando > fraquezas graves.
            val nota = (tamanho - time.size) * 1000 + faltando(time, obrigatorios).size * 100 +
                (if (regras.evitarFraquezas) AnaliseTime.de(time).problemas.size else 0)
            if (nota < melhorNota) {
                melhor = time
                melhorNota = nota
            }
            if (melhorNota == 0) return ResultadoSorteio(time, 0)
        }
        val final = melhor ?: travados
        return ResultadoSorteio(final, tamanho - final.size, faltando(final, obrigatorios))
    }

    private fun faltando(time: List<Pokemon>, obrigatorios: Set<Tipo>) =
        obrigatorios - time.flatMap { it.tipos }.toSet()

    private fun cabe(time: List<Pokemon>, c: Pokemon, regras: RegrasSorteio) =
        time.none { it.dex == c.dex } && !(regras.semTipoRepetido && time.any { m -> m.tipos.any { it in c.tipos } })

    private fun montar(
        embaralhados: List<Pokemon>,
        travados: List<Pokemon>,
        vagas: Int,
        regras: RegrasSorteio,
        obrigatorios: Set<Tipo>,
    ): List<Pokemon> {
        val time = travados.toMutableList()
        var faltam = vagas
        // Primeiro um de cada tipo obrigatório que o time ainda não tem.
        for (t in faltando(time, obrigatorios)) {
            if (faltam == 0) break
            if (t !in faltando(time, obrigatorios)) continue // outro escolhido já trouxe esse tipo
            val c = embaralhados.firstOrNull { t in it.tipos && cabe(time, it, regras) } ?: continue
            time += c
            faltam--
        }
        for (c in embaralhados) {
            if (faltam == 0) break
            if (!cabe(time, c, regras)) continue
            time += c
            faltam--
        }
        return time
    }
}
