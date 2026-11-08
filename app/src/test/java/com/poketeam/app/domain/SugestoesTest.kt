package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SugestoesTest {
    private fun poke(id: Int, vararg tipos: Tipo, dex: Int = id, total: Int = 480) = Pokemon(
        id = id, dex = dex, nome = "P$id", chavesTipos = tipos.map { it.chave }, stats = List(6) { total / 6 },
        geracao = 1, lendario = false, mitico = false, bebe = false, final = true,
    )

    // Dois voadores: fracos a Elétrico e Pedra, ninguém segurando.
    private val time = listOf(poke(1, AGUA, VOADOR), poke(2, FOGO, VOADOR))

    @Test fun terrestreAcoResolveEletricoEPedra() {
        val candidatos = listOf(poke(10, PLANTA), poke(11, TERRESTRE, ACO), poke(12, GELO))
        val s = Sugestoes.para(time, candidatos)
        assertEquals(11, s.first().pokemon.id)
        assertEquals(setOf(ELETRICO, PEDRA), s.first().resolve.toSet())
        assertTrue(s.first().cria.isEmpty())
    }

    @Test fun naoSugereEspecieQueJaEstaNoTime() {
        val s = Sugestoes.para(time, listOf(poke(99, TERRESTRE, dex = 1), poke(12, GELO)))
        assertTrue(s.none { it.pokemon.dex == 1 })
    }

    @Test fun avisaQuandoCriaFraqueza() {
        // Planta/Voador é fraco a Gelo; com um Dragão/Voador, vira 2 fracos e ninguém segurando.
        val t = listOf(poke(1, PLANTA, VOADOR), poke(2, NORMAL))
        val s = Sugestoes.para(t, listOf(poke(5, DRAGAO, VOADOR)))
        assertTrue(GELO in s.single().cria)
    }

    @Test fun timeVazioOuCheioNaoSugere() {
        assertTrue(Sugestoes.para(emptyList(), listOf(poke(1, FOGO))).isEmpty())
        assertTrue(Sugestoes.para((1..6).map { poke(it, NORMAL) }, listOf(poke(9, FOGO))).isEmpty())
    }
}
