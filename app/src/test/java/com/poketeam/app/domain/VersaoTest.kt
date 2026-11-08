package com.poketeam.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersaoTest {
    @Test fun leTagDoGithub() = assertEquals(Versao(1, 2, 3), Versao.ler("v1.2.3"))
    @Test fun completaPartesQueFaltam() = assertEquals(Versao(0, 2, 0), Versao.ler("0.2"))
    @Test fun ignoraSufixo() = assertEquals(Versao(1, 0, 0), Versao.ler("1.0.0-beta"))
    @Test fun recusaLixo() { assertNull(Versao.ler("abc")); assertNull(Versao.ler("1.2.3.4")) }
    @Test fun comparaNumericamente() {
        assertTrue(Versao.ler("0.10.0")!! > Versao.ler("0.9.9")!!)
        assertTrue(Versao.ler("1.0.0")!! > Versao.ler("0.99.99")!!)
    }
}
