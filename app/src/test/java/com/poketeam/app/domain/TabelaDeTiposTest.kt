package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Test

class TabelaDeTiposTest {
    @Test fun charizardTomaQuatroDePedra() =
        assertEquals(4.0, TabelaDeTipos.multiplicador(PEDRA, listOf(FOGO, VOADOR)), 0.0)

    @Test fun gyaradosImuneATerraETomaQuatroDeEletrico() {
        val d = TabelaDeTipos.defesa(listOf(AGUA, VOADOR))
        assertEquals(0.0, d[TERRESTRE]!!, 0.0)
        assertEquals(4.0, d[ELETRICO]!!, 0.0)
    }

    @Test fun ferrothornSoTemFraquezaDeFogoELutador() {
        val fracos = TabelaDeTipos.defesa(listOf(PLANTA, ACO)).filterValues { it > 1 }
        assertEquals(mapOf(FOGO to 4.0, LUTADOR to 2.0), fracos)
    }

    @Test fun normalNaoAcertaFantasma() = assertEquals(0.0, TabelaDeTipos.multiplicador(NORMAL, FANTASMA), 0.0)

    @Test fun normalizarTiraAcentoEPontuacao() = assertEquals("farfetchd galar", normalizar("Farfetch’d Galar"))
}
