package com.poketeam.app.domain

import kotlinx.serialization.Serializable

/** Conjunto de pokémons que vale numa sessão. Regras vazias/false = sem restrição. */
@Serializable
data class Pool(
    val id: String,
    val nome: String,
    /** Gerações permitidas; vazio = todas. */
    val geracoes: Set<Int> = emptySet(),
    val semLendarios: Boolean = false,
    val semMiticos: Boolean = false,
    val soFinais: Boolean = false,
    val semBebes: Boolean = false,
    /** Sem formas regionais/alternativas (Vulpix de Alola, Rotom Wash...). */
    val semFormas: Boolean = false,
    val banidos: Set<Int> = emptySet(),
) {
    val fixo: Boolean get() = id == ID_TUDO

    fun permite(p: Pokemon): Boolean =
        p.id !in banidos && permiteSemBanidos(p)

    /** As regras sem a lista de banidos (usado na tela de banir, que precisa mostrar os banidos). */
    fun permiteSemBanidos(p: Pokemon): Boolean =
        (geracoes.isEmpty() || p.geracao in geracoes) &&
            !(semLendarios && p.lendario) &&
            !(semMiticos && p.mitico) &&
            !(soFinais && !p.final) &&
            !(semBebes && p.bebe) &&
            !(semFormas && p.forma != null)

    /** Resumo curto das regras, ex.: "Ger. 1–4 · sem lendários · 3 banidos". */
    fun resumo(): String {
        val partes = buildList {
            if (geracoes.isNotEmpty()) add("Ger. " + faixas(geracoes.sorted()))
            if (semLendarios) add("sem lendários")
            if (semMiticos) add("sem míticos")
            if (soFinais) add("só evolução final")
            if (semBebes) add("sem bebês")
            if (semFormas) add("sem formas")
            if (banidos.isNotEmpty()) add(if (banidos.size == 1) "1 banido" else "${banidos.size} banidos")
        }
        return partes.ifEmpty { listOf("Todos os pokémons") }.joinToString(" · ")
    }

    companion object {
        const val ID_TUDO = "tudo"
        val TUDO = Pool(ID_TUDO, "Tudo")

        /** [1,2,3,4,7] -> "1–4, 7" */
        fun faixas(ns: List<Int>): String {
            if (ns.isEmpty()) return ""
            val grupos = mutableListOf(mutableListOf(ns.first()))
            ns.drop(1).forEach { n -> if (n == grupos.last().last() + 1) grupos.last() += n else grupos += mutableListOf(n) }
            return grupos.joinToString(", ") { g -> if (g.size == 1) "${g[0]}" else "${g.first()}–${g.last()}" }
        }
    }
}
