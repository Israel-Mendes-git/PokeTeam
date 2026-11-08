package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*

/** Os 18 tipos. [chave] é o identificador da PokéAPI usado no pokemon.json. */
enum class Tipo(val chave: String, val nome: String, val cor: Long) {
    NORMAL("normal", "Normal", 0xFF9FA19F),
    LUTADOR("fighting", "Lutador", 0xFFFF8000),
    VOADOR("flying", "Voador", 0xFF81B9EF),
    VENENOSO("poison", "Venenoso", 0xFF9141CB),
    TERRESTRE("ground", "Terrestre", 0xFF915121),
    PEDRA("rock", "Pedra", 0xFFAFA981),
    INSETO("bug", "Inseto", 0xFF91A119),
    FANTASMA("ghost", "Fantasma", 0xFF704170),
    ACO("steel", "Aço", 0xFF60A1B8),
    FOGO("fire", "Fogo", 0xFFE62829),
    AGUA("water", "Água", 0xFF2980EF),
    PLANTA("grass", "Planta", 0xFF3FA129),
    ELETRICO("electric", "Elétrico", 0xFFFAC000),
    PSIQUICO("psychic", "Psíquico", 0xFFEF4179),
    GELO("ice", "Gelo", 0xFF3DCEF3),
    DRAGAO("dragon", "Dragão", 0xFF5060E1),
    SOMBRIO("dark", "Sombrio", 0xFF624D4E),
    FADA("fairy", "Fada", 0xFFEF70EF);

    companion object {
        private val porChave = entries.associateBy { it.chave }
        fun daChave(chave: String): Tipo = porChave.getValue(chave)
    }
}

object TabelaDeTipos {
    // Apenas as entradas diferentes de 1x. atacante -> (defensor -> multiplicador)
    private val tabela: Map<Tipo, Map<Tipo, Double>> =
        mapOf(
            NORMAL to mapOf(PEDRA to .5, FANTASMA to 0.0, ACO to .5),
            LUTADOR to mapOf(NORMAL to 2.0, VOADOR to .5, VENENOSO to .5, PEDRA to 2.0, INSETO to .5,
                FANTASMA to 0.0, ACO to 2.0, PSIQUICO to .5, GELO to 2.0, SOMBRIO to 2.0, FADA to .5),
            VOADOR to mapOf(LUTADOR to 2.0, PEDRA to .5, INSETO to 2.0, ACO to .5, PLANTA to 2.0, ELETRICO to .5),
            VENENOSO to mapOf(VENENOSO to .5, TERRESTRE to .5, PEDRA to .5, FANTASMA to .5, ACO to 0.0,
                PLANTA to 2.0, FADA to 2.0),
            TERRESTRE to mapOf(VOADOR to 0.0, VENENOSO to 2.0, PEDRA to 2.0, INSETO to .5, ACO to 2.0,
                FOGO to 2.0, PLANTA to .5, ELETRICO to 2.0),
            PEDRA to mapOf(LUTADOR to .5, VOADOR to 2.0, TERRESTRE to .5, INSETO to 2.0, ACO to .5,
                FOGO to 2.0, GELO to 2.0),
            INSETO to mapOf(LUTADOR to .5, VOADOR to .5, VENENOSO to .5, FANTASMA to .5, ACO to .5,
                FOGO to .5, PLANTA to 2.0, PSIQUICO to 2.0, SOMBRIO to 2.0, FADA to .5),
            FANTASMA to mapOf(NORMAL to 0.0, FANTASMA to 2.0, PSIQUICO to 2.0, SOMBRIO to .5),
            ACO to mapOf(PEDRA to 2.0, ACO to .5, FOGO to .5, AGUA to .5, ELETRICO to .5, GELO to 2.0, FADA to 2.0),
            FOGO to mapOf(PEDRA to .5, INSETO to 2.0, ACO to 2.0, FOGO to .5, AGUA to .5, PLANTA to 2.0,
                GELO to 2.0, DRAGAO to .5),
            AGUA to mapOf(TERRESTRE to 2.0, PEDRA to 2.0, FOGO to 2.0, AGUA to .5, PLANTA to .5, DRAGAO to .5),
            PLANTA to mapOf(VOADOR to .5, VENENOSO to .5, TERRESTRE to 2.0, PEDRA to 2.0, INSETO to .5,
                ACO to .5, FOGO to .5, AGUA to 2.0, PLANTA to .5, DRAGAO to .5),
            ELETRICO to mapOf(VOADOR to 2.0, TERRESTRE to 0.0, AGUA to 2.0, PLANTA to .5, ELETRICO to .5, DRAGAO to .5),
            PSIQUICO to mapOf(LUTADOR to 2.0, VENENOSO to 2.0, ACO to .5, PSIQUICO to .5, SOMBRIO to 0.0),
            GELO to mapOf(VOADOR to 2.0, TERRESTRE to 2.0, ACO to .5, FOGO to .5, AGUA to .5, PLANTA to 2.0,
                GELO to .5, DRAGAO to 2.0),
            DRAGAO to mapOf(ACO to .5, DRAGAO to 2.0, FADA to 0.0),
            SOMBRIO to mapOf(LUTADOR to .5, FANTASMA to 2.0, PSIQUICO to 2.0, SOMBRIO to .5, FADA to .5),
            FADA to mapOf(LUTADOR to 2.0, VENENOSO to .5, ACO to .5, FOGO to .5, DRAGAO to 2.0, SOMBRIO to 2.0),
        )

    fun multiplicador(ataque: Tipo, defensor: Tipo): Double = tabela[ataque]?.get(defensor) ?: 1.0

    fun multiplicador(ataque: Tipo, defensores: List<Tipo>): Double =
        defensores.fold(1.0) { acc, d -> acc * multiplicador(ataque, d) }

    /** Quanto cada tipo de ataque causa num pokémon com esses tipos. */
    fun defesa(defensores: List<Tipo>): Map<Tipo, Double> =
        Tipo.entries.associateWith { multiplicador(it, defensores) }
}
