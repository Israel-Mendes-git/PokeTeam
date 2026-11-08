package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecomendacaoTest {
    private fun poke(id: Int, vararg tipos: Tipo) = Pokemon(
        id = id, dex = id, nome = "P$id", chavesTipos = tipos.map { it.chave }, stats = List(6) { 80 },
        geracao = 1, lendario = false, mitico = false, bebe = false, final = true,
    )

    private val pool = Tipo.entries.mapIndexed { i, t -> poke(i + 1, t) } + listOf(poke(50, TERRESTRE, ACO), poke(51, AGUA, FADA))

    @Test fun completaSeisSemRepetirEspecie() {
        val time = Sugestoes.completar(listOf(pool[0]), pool, emptyMap())
        assertEquals(6, time.size)
        assertEquals(6, time.map { it.dex }.toSet().size)
        assertEquals(pool[0], time.first())
    }

    @Test fun timeVazioComecaPeloMaisForte() {
        val forca = mapOf(51 to 1, 50 to 5, 3 to 9)
        assertEquals(51, Sugestoes.completar(emptyList(), pool, forca).first().id)
    }

    @Test fun forcaDesempataSugestoesParecidas() {
        // Os dois resistem a Elétrico e Pedra igual; o mais forte no Smogon vem primeiro.
        val voadores = listOf(poke(90, AGUA, VOADOR), poke(91, FOGO, VOADOR))
        val a = poke(60, TERRESTRE, ACO); val b = poke(61, TERRESTRE, ACO)
        assertEquals(61, Sugestoes.para(voadores, listOf(a, b), 1, mapOf(61 to 1)).first().pokemon.id)
        assertTrue(Sugestoes.para(voadores, listOf(a, b), 2).size == 2)
    }
}
