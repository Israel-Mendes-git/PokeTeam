package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnaliseTimeTest {
    private fun poke(id: Int, vararg tipos: Tipo, stats: List<Int> = List(6) { 100 }) = Pokemon(
        id = id, dex = id, nome = "P$id", chavesTipos = tipos.map { it.chave }, stats = stats,
        geracao = 1, lendario = false, mitico = false, bebe = false, final = true,
    )

    @Test fun tresVoadoresViramProblemaContraEletrico() {
        val a = AnaliseTime.de(listOf(poke(1, AGUA, VOADOR), poke(2, FOGO, VOADOR), poke(3, NORMAL, VOADOR)))
        val eletrico = a.defesa.first { it.tipo == ELETRICO }
        assertEquals(3, eletrico.fracos)
        assertTrue(eletrico.problema)
        // Pedra também: os três são fracos e ninguém resiste.
        // Gelo não: Água e Fogo anulam a fraqueza do Voador, só o Normal/Voador fica fraco.
        assertEquals(setOf(ELETRICO, PEDRA), a.problemas.map { it.tipo }.toSet())
        // Terrestre: os três são imunes.
        assertEquals(3, a.defesa.first { it.tipo == TERRESTRE }.imunes)
    }

    @Test fun tresFracosPrecisamDeDoisSegurando() {
        // Três Voadores fracos a Elétrico: com 1 Terrestre ainda é problema, com 2 deixa de ser.
        val voadores = listOf(poke(1, AGUA, VOADOR), poke(2, FOGO, VOADOR), poke(3, NORMAL, VOADOR))
        assertTrue(AnaliseTime.de(voadores + poke(4, TERRESTRE)).defesa.first { it.tipo == ELETRICO }.problema)
        assertFalse(AnaliseTime.de(voadores + poke(4, TERRESTRE) + poke(5, TERRESTRE, ACO)).defesa.first { it.tipo == ELETRICO }.problema)
    }

    @Test fun fraquezaCobertaPorResistenciaNaoEProblema() {
        // Dois de Planta (fracos a Fogo) + um de Água (resiste Fogo)
        val a = AnaliseTime.de(listOf(poke(1, PLANTA), poke(2, PLANTA), poke(3, AGUA)))
        assertFalse(a.defesa.first { it.tipo == FOGO }.problema)
    }

    @Test fun coberturaStab() {
        // Só Normal: não acerta ninguém super efetivo.
        assertEquals(Tipo.entries.toList(), AnaliseTime.de(listOf(poke(1, NORMAL))).semCoberturaStab)
        // Lutador + Terrestre + Voador + Fada cobrem bastante coisa, mas não Fantasma nem Voador.
        val a = AnaliseTime.de(listOf(poke(1, LUTADOR), poke(2, TERRESTRE, VOADOR), poke(3, FADA)))
        assertTrue(FANTASMA in a.semCoberturaStab)
        assertFalse(DRAGAO in a.semCoberturaStab)
    }

    @Test fun mediaDeStats() {
        val a = AnaliseTime.de(listOf(poke(1, NORMAL, stats = listOf(100, 50, 50, 50, 50, 50)),
            poke(2, NORMAL, stats = listOf(50, 50, 50, 50, 50, 150))))
        assertEquals(listOf(75, 50, 50, 50, 50, 100), a.mediaStats)
    }

    @Test fun timeVazioNaoQuebra() {
        val a = AnaliseTime.de(emptyList())
        assertTrue(a.problemas.isEmpty())
        assertTrue(a.semCoberturaStab.isEmpty())
    }
}
