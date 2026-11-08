package com.poketeam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildTest {
    private val garchomp = Build(
        formato = "OU", nome = "Swords Dance",
        golpes = listOf(listOf("Swords Dance"), listOf("Scale Shot"), listOf("Earthquake"), listOf("Fire Fang", "Dragon Tail")),
        item = listOf("Loaded Dice"), habilidade = listOf("Rough Skin"), nature = listOf("Jolly"),
        evs = mapOf("atk" to 252, "spd" to 4, "spe" to 252), tera = listOf("Fire", "Ghost"),
    )

    @Test fun exportaNoFormatoDoShowdown() {
        assertEquals(
            """
            Garchomp @ Loaded Dice
            Ability: Rough Skin
            Tera Type: Fire
            EVs: 252 Atk / 4 SpD / 252 Spe
            Jolly Nature
            - Swords Dance
            - Scale Shot
            - Earthquake
            - Fire Fang
            """.trimIndent(),
            garchomp.paraShowdown("Garchomp"),
        )
    }

    @Test fun naturesBatemComOJogo() {
        assertEquals(25, Natureza.todas.size)
        assertEquals(5, Natureza.todas.count { it.neutra })
        assertEquals("+Vel. −At. Esp.", Natureza.porNome("Jolly")!!.efeito)
        assertEquals("+At. −At. Esp.", Natureza.porNome("adamant")!!.efeito)
        assertEquals("+At. Esp. −At.", Natureza.porNome("Modest")!!.efeito)
        assertEquals("+Vel. −At.", Natureza.porNome("Timid")!!.efeito)
        assertTrue(Natureza.porNome("Serious")!!.neutra)
    }
}
