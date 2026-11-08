package com.poketeam.app.domain

import com.poketeam.app.domain.Tipo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SorteioTest {
    private fun poke(id: Int, vararg tipos: Tipo, dex: Int = id, final: Boolean = true) = Pokemon(
        id = id, dex = dex, nome = "P$id", chavesTipos = tipos.map { it.chave }, stats = List(6) { 80 },
        geracao = 1, lendario = false, mitico = false, bebe = false, final = final,
    )

    /** Um pokémon de cada tipo, mais uns duplicados e não-finais para atrapalhar. */
    private val pool = Tipo.entries.mapIndexed { i, t -> poke(i + 1, t) } +
        listOf(poke(100, FOGO, AGUA), poke(101, PLANTA, final = false), poke(102, DRAGAO, dex = 16))

    @Test fun completaSeisSemRepetirTipoNemEspecie() {
        repeat(50) { seed ->
            val r = Sorteio.sortear(pool, emptyList(), random = Random(seed))
            assertEquals(6, r.time.size)
            assertEquals(0, r.faltaram)
            assertEquals(6, r.time.map { it.dex }.toSet().size)
            val tipos = r.time.flatMap { it.tipos }
            assertEquals("tipo repetido em ${r.time}", tipos.size, tipos.toSet().size)
            assertTrue(r.time.all { it.final })
        }
    }

    @Test fun mantemTravadosNaFrente() {
        val travados = listOf(pool[0], pool[9]) // Normal, Fogo
        val r = Sorteio.sortear(pool, travados, random = Random(1))
        assertEquals(travados, r.time.take(2))
        assertEquals(6, r.time.size)
    }

    @Test fun poolPequenoDemaisDevolveParcial() {
        val pequeno = listOf(poke(1, FOGO), poke(2, FOGO), poke(3, AGUA))
        val r = Sorteio.sortear(pequeno, emptyList(), random = Random(1))
        assertEquals(2, r.time.size) // só um de Fogo + o de Água
        assertEquals(4, r.faltaram)
    }

    @Test fun semRegrasAceitaTipoRepetido() {
        val soFogo = (1..6).map { poke(it, FOGO) }
        val r = Sorteio.sortear(soFogo, emptyList(), RegrasSorteio(semTipoRepetido = false, evitarFraquezas = false), random = Random(1))
        assertEquals(6, r.time.size)
    }

    @Test fun timeCheioNaoMexe() {
        val cheio = pool.take(6)
        assertEquals(cheio, Sorteio.sortear(pool, cheio).time)
    }

    @Test fun regrasIndividuaisFiltram() {
        val r = RegrasSorteio(semLendarios = true, totalMinimo = 500, tiposProibidos = setOf("dragon"))
        val forte = Pokemon(1, 1, "a", chavesTipos = listOf("fire"), stats = List(6) { 90 }, geracao = 1, lendario = false, mitico = false, bebe = false, final = true)
        assertTrue(r.permite(forte))
        assertTrue(!r.permite(forte.copy(stats = List(6) { 80 })))
        assertTrue(!r.permite(forte.copy(lendario = true)))
        assertTrue(!r.permite(forte.copy(chavesTipos = listOf("dragon"))))
    }

    @Test fun garanteTiposObrigatorios() {
        val r = RegrasSorteio(tiposObrigatorios = setOf("ice", "ghost"), evitarFraquezas = false)
        repeat(30) { seed ->
            val res = Sorteio.sortear(pool, emptyList(), r, random = Random(seed))
            val tipos = res.time.flatMap { it.tipos }
            assertTrue("seed $seed: $tipos", Tipo.GELO in tipos && Tipo.FANTASMA in tipos)
            assertTrue(res.tiposFaltando.isEmpty())
        }
    }

    @Test fun avisaTipoObrigatorioImpossivel() {
        val semFada = pool.filter { Tipo.FADA !in it.tipos }
        val res = Sorteio.sortear(semFada, emptyList(), RegrasSorteio(tiposObrigatorios = setOf("fairy")), random = Random(1))
        assertEquals(setOf(Tipo.FADA), res.tiposFaltando)
    }
}
