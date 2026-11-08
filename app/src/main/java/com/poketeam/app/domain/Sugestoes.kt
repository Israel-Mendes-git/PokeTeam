package com.poketeam.app.domain

import kotlinx.serialization.Serializable

/** Um candidato a entrar no time e o que ele resolve. */
data class Sugestao(
    val pokemon: Pokemon,
    /** Fraquezas graves do time que somem com ele. */
    val resolve: List<Tipo>,
    /** Fraquezas graves que ele cria (ex.: o time vira 3 fracos a Gelo). */
    val cria: List<Tipo>,
)

/** Quanto a força no Smogon pesa na recomendação. */
@Serializable
enum class EstiloRecomendar(val rotulo: String, val pesoForca: Int) {
    CASUAL("Casual", 0),
    EQUILIBRADO("Equilibrado", 8),
    COMPETITIVO("Competitivo", 20),
}

/** Formatos do Smogon que dá para escolher; [prefixos] casam com o rótulo das builds. */
enum class FormatoSmogon(val rotulo: String, val prefixos: List<String>) {
    OU("OU", listOf("OU")),
    UBERS("Ubers", listOf("Ubers")),
    UU("UU", listOf("UU")),
    RU_NU("RU / NU", listOf("RU", "NU")),
    PU_ZU("PU / ZU", listOf("PU", "ZU")),
    LC("LC", listOf("LC")),
    NATIONAL_DEX("National Dex", listOf("National Dex")),
    DUPLAS("VGC / Duplas", listOf("VGC", "Battle Stadium", "Duplas")),
    MONOTYPE("Monotype", listOf("Monotype")),
    ;

    /** "Gen 8 Ubers" e "Ubers" contam para Ubers; "National Dex UU" não conta para UU. */
    fun casa(rotuloBuild: String): Boolean {
        val r = rotuloBuild.replace(Regex("^Gen \\d+ "), "")
        return prefixos.any { r.startsWith(it) }
    }
}

@Serializable
data class ConfigRecomendar(
    val estilo: EstiloRecomendar = EstiloRecomendar.EQUILIBRADO,
    /** Nome de um [FormatoSmogon]; null = qualquer formato. */
    val formato: String? = null,
    val preferirFavoritos: Boolean = true,
    /** Recomendar e sugestões respeitam as regras do sorteio (tipos, lendários, total mínimo...). */
    val usarRegrasDoSorteio: Boolean = true,
) {
    val formatoSmogon: FormatoSmogon? get() = formato?.let { f -> FormatoSmogon.entries.firstOrNull { it.name == f } }
}

/** Tudo que muda a nota de um candidato, além da cobertura de tipos. */
data class AjustesSugestao(
    /** id -> força no Smogon (menor = mais forte). */
    val forca: Map<Int, Int> = emptyMap(),
    val pesoForca: Int = EstiloRecomendar.EQUILIBRADO.pesoForca,
    /** Ids com build no formato escolhido. */
    val doFormato: Set<Int> = emptySet(),
    val favoritos: Set<Int> = emptySet(),
    val semTipoRepetido: Boolean = false,
    val tiposObrigatorios: Set<Tipo> = emptySet(),
)

object Sugestoes {
    /**
     * Completa o time vaga por vaga com a melhor sugestão de cada momento.
     * Time vazio começa pelo melhor candidato isolado (forte, do formato, favorito, tipo obrigatório).
     */
    fun completar(time: List<Pokemon>, candidatos: List<Pokemon>, ajustes: AjustesSugestao = AjustesSugestao()): List<Pokemon> {
        val atual = time.toMutableList()
        if (atual.isEmpty()) {
            candidatos.maxByOrNull { notaSozinho(it, ajustes) }?.let { atual += it } ?: return atual
        }
        while (atual.size < Time.MAXIMO) {
            val proximo = para(atual, candidatos, 1, ajustes).firstOrNull()?.pokemon ?: break
            atual += proximo
        }
        return atual
    }

    private fun bonus(c: Pokemon, a: AjustesSugestao): Int =
        (a.forca[c.id]?.let { (20 - it).coerceIn(0, 20) * a.pesoForca } ?: 0) +
            (if (c.id in a.doFormato) 150 else 0) +
            (if (c.id in a.favoritos) 120 else 0)

    private fun notaSozinho(c: Pokemon, a: AjustesSugestao) =
        bonus(c, a) + c.tipos.count { it in a.tiposObrigatorios } * 2000 + c.total / 100

    /**
     * Os [quantos] melhores de [candidatos] para entrar em [time].
     * Prioridade: tipo obrigatório que falta; menos fraquezas graves no time resultante; quantas
     * fraquezas do time ele segura; tipos ainda sem cobertura de STAB; força, formato e favoritos.
     */
    fun para(time: List<Pokemon>, candidatos: List<Pokemon>, quantos: Int = 6, ajustes: AjustesSugestao = AjustesSugestao()): List<Sugestao> {
        if (time.isEmpty() || time.size >= Time.MAXIMO) return emptyList()
        val antes = AnaliseTime.de(time)
        val problemasAntes = antes.problemas.map { it.tipo }.toSet()
        // Tipos de ataque em que o time tem mais fracos do que quem segura.
        val pontosFracos = antes.defesa.filter { it.fracos > it.resistentes + it.imunes }.map { it.tipo }
        val dexNoTime = time.map { it.dex }.toSet()
        val tiposNoTime = time.flatMap { it.tipos }.toSet()
        val obrigatoriosFaltando = ajustes.tiposObrigatorios - tiposNoTime

        return candidatos.asSequence()
            .filter { it.dex !in dexNoTime }
            .filter { c -> !ajustes.semTipoRepetido || c.tipos.none { it in tiposNoTime } }
            .map { c ->
                val depois = AnaliseTime.de(time + c)
                val problemasDepois = depois.problemas.map { it.tipo }.toSet()
                val segura = pontosFracos.count { TabelaDeTipos.multiplicador(it, c.tipos) < 1.0 }
                val coberturaNova = antes.semCoberturaStab.count { d -> c.tipos.any { TabelaDeTipos.multiplicador(it, d) > 1.0 } }
                val nota = c.tipos.count { it in obrigatoriosFaltando } * 5000 - problemasDepois.size * 1000 +
                    segura * 100 + coberturaNova * 10 + bonus(c, ajustes) + c.total / 100
                nota to Sugestao(c, (problemasAntes - problemasDepois).toList(), (problemasDepois - problemasAntes).toList())
            }
            .sortedByDescending { it.first }
            .take(quantos)
            .map { it.second }
            .toList()
    }
}
