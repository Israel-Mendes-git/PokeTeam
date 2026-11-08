package com.poketeam.app.domain

import kotlinx.serialization.Serializable

/** Build recomendada do Smogon. Campos com mais de uma opção trazem todas, na ordem do Smogon. */
@Serializable
data class Build(
    val formato: String,
    val nome: String,
    /** Cada vaga de golpe pode ter alternativas: [["Earthquake"], ["Fire Fang", "Dragon Tail"]]. */
    val golpes: List<List<String>> = emptyList(),
    val item: List<String> = emptyList(),
    val habilidade: List<String> = emptyList(),
    val nature: List<String> = emptyList(),
    val evs: Map<String, Int> = emptyMap(),
    val tera: List<String> = emptyList(),
) {
    /** EVs no formato curto: "252 HP / 4 SpD / 252 Spe". */
    val evsTexto: String
        get() = ORDEM_EVS.mapNotNull { (k, rotulo) -> evs[k]?.takeIf { it > 0 }?.let { "$it $rotulo" } }.joinToString(" / ")

    /** Texto no formato de importação do Showdown (primeira opção de cada campo). */
    fun paraShowdown(nomePokemon: String): String = buildString {
        append(nomePokemon)
        item.firstOrNull()?.let { append(" @ ").append(it) }
        appendLine()
        habilidade.firstOrNull()?.let { appendLine("Ability: $it") }
        tera.firstOrNull()?.let { appendLine("Tera Type: $it") }
        if (evsTexto.isNotEmpty()) appendLine("EVs: $evsTexto")
        nature.firstOrNull()?.let { appendLine("$it Nature") }
        golpes.forEach { opcoes -> opcoes.firstOrNull()?.let { appendLine("- $it") } }
    }.trimEnd()

    companion object {
        private val ORDEM_EVS = listOf("hp" to "HP", "atk" to "Atk", "def" to "Def", "spa" to "SpA", "spd" to "SpD", "spe" to "Spe")
    }
}

/** Uma nature sobe um stat 10% e desce outro 10% (as neutras não mexem em nada). */
data class Natureza(val nome: String, val sobe: Int?, val desce: Int?) {
    val neutra: Boolean get() = sobe == null

    /** "+Vel −At. Esp." ou "Neutra". Índices seguem [Pokemon.NOMES_STATS]. */
    val efeito: String
        get() = if (neutra) "Neutra" else "+${CURTO[sobe!!]} −${CURTO[desce!!]}"

    companion object {
        private val CURTO = listOf("PS", "At.", "Def.", "At. Esp.", "Def. Esp.", "Vel.")

        // Ordem do jogo: linhas = stat que sobe (At, Def, Vel, At.Esp, Def.Esp), colunas = stat que desce.
        private val TABELA = listOf(
            listOf("Hardy", "Lonely", "Brave", "Adamant", "Naughty"),
            listOf("Bold", "Docile", "Relaxed", "Impish", "Lax"),
            listOf("Timid", "Hasty", "Serious", "Jolly", "Naive"),
            listOf("Modest", "Mild", "Quiet", "Bashful", "Rash"),
            listOf("Calm", "Gentle", "Sassy", "Careful", "Quirky"),
        )
        private val STATS_TABELA = listOf(1, 2, 5, 3, 4) // At, Def, Vel, At.Esp, Def.Esp

        val todas: List<Natureza> = TABELA.flatMapIndexed { i, linha ->
            linha.mapIndexed { j, nome ->
                if (i == j) Natureza(nome, null, null) else Natureza(nome, STATS_TABELA[i], STATS_TABELA[j])
            }
        }

        private val porNome = todas.associateBy { it.nome.lowercase() }
        fun porNome(nome: String): Natureza? = porNome[nome.trim().lowercase()]
    }
}
