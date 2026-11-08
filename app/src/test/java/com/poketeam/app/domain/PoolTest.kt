package com.poketeam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoolTest {
    private fun poke(
        id: Int, geracao: Int = 1, lendario: Boolean = false, mitico: Boolean = false,
        final: Boolean = true, bebe: Boolean = false, forma: String? = null,
    ) = Pokemon(id = id, dex = id, nome = "P$id", forma = forma, chavesTipos = listOf("normal"),
        stats = List(6) { 50 }, geracao = geracao, lendario = lendario, mitico = mitico, bebe = bebe, final = final)

    @Test fun tudoPermiteTudo() {
        listOf(poke(1, lendario = true), poke(2, mitico = true, geracao = 9), poke(3, final = false, bebe = true, forma = "Alola"))
            .forEach { assertTrue(Pool.TUDO.permite(it)) }
    }

    @Test fun regrasSeSomam() {
        val pool = Pool("x", "x", geracoes = setOf(1, 2), semLendarios = true, soFinais = true)
        assertTrue(pool.permite(poke(1, geracao = 2)))
        assertFalse(pool.permite(poke(2, geracao = 3)))
        assertFalse(pool.permite(poke(3, geracao = 1, lendario = true)))
        assertFalse(pool.permite(poke(4, geracao = 1, final = false)))
    }

    @Test fun banidoSaiMasContinuaNasRegras() {
        val pool = Pool("x", "x", banidos = setOf(7))
        assertFalse(pool.permite(poke(7)))
        assertTrue(pool.permiteSemBanidos(poke(7)))
    }

    @Test fun semFormasTiraSoAsFormas() {
        val pool = Pool("x", "x", semFormas = true)
        assertFalse(pool.permite(poke(10037, forma = "Alola")))
        assertTrue(pool.permite(poke(37)))
    }

    @Test fun resumo() {
        assertEquals("Todos os pokémons", Pool.TUDO.resumo())
        assertEquals("Ger. 1–4, 7 · sem lendários · 1 banido",
            Pool("x", "x", geracoes = setOf(7, 1, 2, 3, 4), semLendarios = true, banidos = setOf(5)).resumo())
    }
}
