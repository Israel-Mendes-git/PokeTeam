package com.poketeam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DraftTest {
    private fun poke(id: Int, tipo: Tipo, dex: Int = id) = Pokemon(
        id = id, dex = dex, nome = "P$id", chavesTipos = listOf(tipo.chave), stats = List(6) { 80 },
        geracao = 1, lendario = false, mitico = false, bebe = false, final = true,
    )

    private val todos = Tipo.entries.mapIndexed { i, t -> poke(i + 1, t) } +
        listOf(poke(100, Tipo.FOGO, dex = 10)) // forma da mesma espécie do #10

    private fun draft(jogadores: Int = 3, porJogador: Int = 2, serpente: Boolean = true, modo: ModoDraft = ModoDraft.LIVRE) =
        Draft(jogadores = (1..jogadores).map { "J$it" }, porJogador = porJogador, serpente = serpente, modo = modo, pool = Pool.TUDO)

    @Test fun ordemSerpente() {
        val d = draft(jogadores = 3, porJogador = 3)
        assertEquals(listOf(0, 1, 2, 2, 1, 0, 0, 1, 2), (0 until 9).map(d::jogadorDaEscolha))
    }

    @Test fun ordemFixa() {
        val d = draft(jogadores = 2, porJogador = 3, serpente = false)
        assertEquals(listOf(0, 1, 0, 1, 0, 1), (0 until 6).map(d::jogadorDaEscolha))
    }

    @Test fun escolhaSaiParaTodosPorEspecie() {
        val d = draft().escolher(10, todos)
        assertEquals(listOf(10), d.escolhasDe(0))
        assertEquals(1, d.vez)
        val disp = d.disponiveis(todos).map { it.id }
        assertTrue(10 !in disp && 100 !in disp) // a forma da mesma espécie também saiu
        // Tentar pegar de novo não muda nada.
        assertEquals(d, d.escolher(100, todos))
    }

    @Test fun terminaEDesfaz() {
        var d = draft(jogadores = 2, porJogador = 1)
        d = d.escolher(1, todos).escolher(2, todos)
        assertTrue(d.terminou)
        assertNull(d.vez)
        d = d.desfazer(todos)
        assertEquals(1, d.vez)
        assertEquals(listOf(1), d.escolhasDe(0))
    }

    @Test fun modoOpcoesSorteiaTresDisponiveis() {
        val d = draft(modo = ModoDraft.OPCOES).comNovasOpcoes(todos, Random(1)).let { it.escolher(it.opcoes.first(), todos, Random(2)) }
        assertEquals(Draft.QTD_OPCOES, d.opcoes.size)
        val disp = d.disponiveis(todos).map { it.id }.toSet()
        assertTrue(d.opcoes.all { it in disp })
    }
}
