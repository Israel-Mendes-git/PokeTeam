package com.poketeam.app.domain

/** Um candidato a entrar no time e o que ele resolve. */
data class Sugestao(
    val pokemon: Pokemon,
    /** Fraquezas graves do time que somem com ele. */
    val resolve: List<Tipo>,
    /** Fraquezas graves que ele cria (ex.: o time vira 3 fracos a Gelo). */
    val cria: List<Tipo>,
)

object Sugestoes {
    /**
     * Completa o time vaga por vaga com a melhor sugestão de cada momento.
     * Time vazio começa pelo mais forte dos candidatos.
     */
    fun completar(time: List<Pokemon>, candidatos: List<Pokemon>, forca: Map<Int, Int>): List<Pokemon> {
        val atual = time.toMutableList()
        if (atual.isEmpty()) {
            candidatos.filter { it.id in forca }.minByOrNull { forca.getValue(it.id) }?.let { atual += it }
                ?: return atual
        }
        while (atual.size < Time.MAXIMO) {
            val proximo = para(atual, candidatos, 1, forca).firstOrNull()?.pokemon ?: break
            atual += proximo
        }
        return atual
    }

    /**
     * Os [quantos] melhores de [candidatos] para entrar em [time].
     * Prioridade: menos fraquezas graves no time resultante; depois quantas fraquezas
     * do time ele segura (resiste/imune); depois tipos ainda sem cobertura de STAB; por fim stats.
     */
    /**
     * [forca]: id -> força no Smogon (menor = mais forte). Quem tem build ganha bônus, para a
     * recomendação puxar pokémons que funcionam no competitivo e não só os que fecham tipos.
     */
    fun para(time: List<Pokemon>, candidatos: List<Pokemon>, quantos: Int = 6, forca: Map<Int, Int> = emptyMap()): List<Sugestao> {
        if (time.isEmpty() || time.size >= Time.MAXIMO) return emptyList()
        val antes = AnaliseTime.de(time)
        val problemasAntes = antes.problemas.map { it.tipo }.toSet()
        // Tipos de ataque em que o time tem mais fracos do que quem segura.
        val pontosFracos = antes.defesa.filter { it.fracos > it.resistentes + it.imunes }.map { it.tipo }
        val dexNoTime = time.map { it.dex }.toSet()

        return candidatos.asSequence()
            .filter { it.dex !in dexNoTime }
            .map { c ->
                val depois = AnaliseTime.de(time + c)
                val problemasDepois = depois.problemas.map { it.tipo }.toSet()
                val segura = pontosFracos.count { TabelaDeTipos.multiplicador(it, c.tipos) < 1.0 }
                val coberturaNova = antes.semCoberturaStab.count { d -> c.tipos.any { TabelaDeTipos.multiplicador(it, d) > 1.0 } }
                val bonusForca = forca[c.id]?.let { (20 - it).coerceIn(0, 20) * 8 } ?: 0
                val nota = -problemasDepois.size * 1000 + segura * 100 + coberturaNova * 10 + bonusForca + c.total / 100
                nota to Sugestao(c, (problemasAntes - problemasDepois).toList(), (problemasDepois - problemasAntes).toList())
            }
            .sortedByDescending { it.first }
            .take(quantos)
            .map { it.second }
            .toList()
    }
}
