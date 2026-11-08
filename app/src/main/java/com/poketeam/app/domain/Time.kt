package com.poketeam.app.domain

import kotlinx.serialization.Serializable

@Serializable
data class Time(
    val id: String,
    val nome: String,
    /** ids de [Pokemon], no máximo [MAXIMO]. */
    val membros: List<Int> = emptyList(),
    val favorito: Boolean = false,
    /** Build escolhida de cada membro: id do pokémon -> índice na lista de builds dele. */
    val builds: Map<Int, Int> = emptyMap(),
    /** Forma Mega ativada (id da Mega); no máximo uma por time, como no jogo. */
    val mega: Int? = null,
    val criadoEm: Long,
    val alteradoEm: Long = criadoEm,
) {
    val cheio: Boolean get() = membros.size >= MAXIMO

    companion object {
        const val MAXIMO = 6
    }
}

/** Como o time inteiro se sai contra um tipo de ataque. */
data class LinhaDefesa(val tipo: Tipo, val fracos: Int, val resistentes: Int, val imunes: Int) {
    /**
     * Fraqueza que pede atenção: 2+ fracos e os fracos superam em 2+ quem segura (resiste/imune).
     * 2 fracos e ninguém segurando = problema; 3 fracos e 2 segurando = ok.
     */
    val problema: Boolean get() = fracos >= 2 && fracos - (resistentes + imunes) >= 2
}

data class AnaliseTime(
    /** Um item por tipo de ataque, do mais problemático para o mais tranquilo. */
    val defesa: List<LinhaDefesa>,
    /** Tipos que nenhum membro acerta super efetivo com golpe do próprio tipo (STAB). */
    val semCoberturaStab: List<Tipo>,
    /** Média de cada stat base, na ordem de [Pokemon.NOMES_STATS]. */
    val mediaStats: List<Int>,
) {
    val problemas: List<LinhaDefesa> get() = defesa.filter { it.problema }

    companion object {
        fun de(time: List<Pokemon>): AnaliseTime {
            val defesa = Tipo.entries.map { ataque ->
                val mults = time.map { TabelaDeTipos.multiplicador(ataque, it.tipos) }
                LinhaDefesa(
                    tipo = ataque,
                    fracos = mults.count { it > 1.0 },
                    resistentes = mults.count { it > 0.0 && it < 1.0 },
                    imunes = mults.count { it == 0.0 },
                )
            }.sortedWith(compareByDescending<LinhaDefesa> { it.problema }
                .thenByDescending { it.fracos - it.resistentes - it.imunes })

            val tiposDoTime = time.flatMap { it.tipos }.toSet()
            val semCobertura = if (time.isEmpty()) emptyList() else Tipo.entries.filter { defensor ->
                tiposDoTime.none { TabelaDeTipos.multiplicador(it, defensor) > 1.0 }
            }

            val media = if (time.isEmpty()) List(6) { 0 } else
                (0 until 6).map { i -> time.sumOf { it.stats[i] } / time.size }

            return AnaliseTime(defesa, semCobertura, media)
        }
    }
}
