package com.poketeam.app.ui.tema

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletasTest {
    private fun contraste(a: Color, b: Color): Double {
        val (claro, escuro) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (claro + 0.05) / (escuro + 0.05)
    }

    /** Texto aparece tanto no fundo da tela quanto dentro das caixas: precisa ler bem nos dois. */
    @Test fun textoLegivelNoFundoENasCaixas() {
        val falhas = Paletas.todas.flatMap { p ->
            listOf(
                "texto/fundo" to contraste(p.texto, p.fundo),
                "texto/painel" to contraste(p.texto, p.painel),
                "botão" to contraste(p.textoAcento, p.acento),
                "detalhe/fundo" to contraste(p.sub, p.fundo),
                "detalhe/painel" to contraste(p.sub, p.painel),
            ).filter { (tipo, c) -> c < (if (tipo.startsWith("detalhe")) 3.0 else 4.5) }
                .map { (tipo, c) -> "${p.nome} $tipo %.1f".format(c) }
        }
        assertTrue("Contraste baixo: $falhas", falhas.isEmpty())
    }

    @Test fun idsUnicos() = assertTrue(Paletas.todas.map { it.id }.toSet().size == Paletas.todas.size)
}
