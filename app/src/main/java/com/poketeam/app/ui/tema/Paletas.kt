package com.poketeam.app.ui.tema

import androidx.compose.ui.graphics.Color

/**
 * Cores de um tema pixel. [borda] é a moldura principal, [bordaFundo] a linha escura de fora
 * e a sombra dura dos botões, [destaque] os títulos.
 */
data class Paleta(
    val id: String,
    val nome: String,
    val claro: Boolean,
    val fundo: Color,
    val painel: Color,
    val vaga: Color,
    val borda: Color,
    val bordaFundo: Color,
    val texto: Color,
    val sub: Color,
    val acento: Color,
    val textoAcento: Color,
    val destaque: Color,
    val alerta: Color,
    val bom: Color,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

object Paletas {
    val todas = listOf(
        Paleta("noite", "Noite", false, c(0x1a1c2c), c(0x29304a), c(0x333c57), c(0x94b0c2), c(0x0d0e17),
            c(0xf4f4f4), c(0x94b0c2), c(0xef7d57), c(0x1a1c2c), c(0xffcd75), c(0xff6b6b), c(0xa7f070)),
        Paleta("dialogo", "Diálogo clássico", true, c(0xd8cdb0), c(0xf6efd9), c(0xe4dcc3), c(0x2b2b2b), c(0x8d8d8d),
            c(0x2b2b2b), c(0x6b6457), c(0xb8322a), c(0xffffff), c(0xe8a33d), c(0xc0392b), c(0x3f8f4f)),
        Paleta("portatil", "Portátil", false, c(0x0f2318), c(0x1d3b2a), c(0x2a4d38), c(0x9bd46a), c(0x071109),
            c(0xe8f5d0), c(0x9bbf88), c(0xffd23f), c(0x0f2318), c(0xffd23f), c(0xff7b54), c(0x9bd46a)),
        Paleta("pokebola", "Pokébola", true, c(0xf2f2f2), c(0xffffff), c(0xececec), c(0x1c1c1c), c(0x9a9a9a),
            c(0x1c1c1c), c(0x6e6e6e), c(0xc42d0b), c(0xffffff), c(0xe3350d), c(0xc62828), c(0x2e7d32)),
        Paleta("ceu", "Céu", true, c(0xbfe3ff), c(0xffffff), c(0xe6f3ff), c(0x22355e), c(0x6f8fc7),
            c(0x22355e), c(0x5a6f98), c(0xffcb05), c(0x22355e), c(0x22355e), c(0xd1495b), c(0x2e8b57)),
        Paleta("por-do-sol", "Pôr do sol", false, c(0x2d1b3d), c(0x4a2c5e), c(0x5c3a72), c(0xff9e6d), c(0x140c1c),
            c(0xffeedd), c(0xd8a8c8), c(0xff5d8f), c(0x2d1b3d), c(0xffb86b), c(0xffd166), c(0x9be38b)),
        Paleta("oceano", "Oceano", false, c(0x0b2e3a), c(0x124555), c(0x185a6c), c(0x7fd8d0), c(0x04161c),
            c(0xe9fbf8), c(0x8bc4c0), c(0xff7f66), c(0x0b2e3a), c(0xffd37a), c(0xff9a8a), c(0x7fe0a0)),
        Paleta("fliperama", "Fliperama", false, c(0x0a0a12), c(0x16162a), c(0x22223d), c(0x29f0ff), c(0xff2bd6),
            c(0xf5f5ff), c(0x8f8fc0), c(0xff2bd6), c(0x0a0a12), c(0x29f0ff), c(0xffe14d), c(0x5dff8a)),
        Paleta("floresta", "Floresta", true, c(0xe9dfc4), c(0xfbf6e6), c(0xefe6cf), c(0x4a3524), c(0xa8916e),
            c(0x3a2a1c), c(0x7a6449), c(0x2f7a3f), c(0xffffff), c(0x3f8f4f), c(0xb5442c), c(0x2f7a3f)),
        Paleta("lavanda", "Lavanda", true, c(0xe6defa), c(0xfaf7ff), c(0xeee8fb), c(0x3b2f63), c(0x9d8fcf),
            c(0x2e2550), c(0x6d6194), c(0x7b5cd6), c(0xffffff), c(0x7b5cd6), c(0xd0466b), c(0x2f8f6a)),
        Paleta("vulcao", "Vulcão", false, c(0x1c0f0d), c(0x34191a), c(0x452122), c(0xff8a3d), c(0x0b0605),
            c(0xfff1e6), c(0xd9a48f), c(0xff4f2e), c(0x1c0f0d), c(0xffc145), c(0xffe066), c(0x8fe388)),
        Paleta("papel", "Papel", true, c(0xffffff), c(0xffffff), c(0xf3f3f3), c(0x111111), c(0x111111),
            c(0x111111), c(0x5e5e5e), c(0x111111), c(0xffffff), c(0x111111), c(0xb00020), c(0x1b5e20)),
    )

    val padrao = todas.first()
    fun porId(id: String?) = todas.firstOrNull { it.id == id } ?: padrao
}
