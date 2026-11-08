package com.poketeam.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Pokemon(
    val id: Int,
    val dex: Int,
    val nome: String,
    val forma: String? = null,
    @SerialName("tipos") val chavesTipos: List<String>,
    val stats: List<Int>,
    val geracao: Int,
    val lendario: Boolean,
    val mitico: Boolean,
    val bebe: Boolean,
    val final: Boolean,
    /** Forma de batalha (Mega): fica fora das listas; aparece na ficha e no time quando ativada. */
    val mega: Boolean = false,
) {
    @Transient
    val tipos: List<Tipo> = chavesTipos.map(Tipo::daChave)

    val nomeCompleto: String get() = if (forma == null) nome else "$nome ($forma)"
    val total: Int get() = stats.sum()

    /** Nome no jeito do Showdown: "Ninetales-Alola", "Indeedee-F". */
    val nomeShowdown: String get() = when (forma) {
        null -> nome
        "Fêmea" -> "$nome-F"
        "Macho" -> nome
        else -> nome + "-" + normalizar(forma).split(' ').filter(String::isNotEmpty)
            .joinToString("-") { it.replaceFirstChar(Char::uppercase) }
    }
    /** Sprite empacotado no APK (tools/baixar_sprites.sh). */
    val sprite: String get() = "file:///android_asset/sprites/$id.png"

    /** Texto normalizado para busca: sem acento, minúsculo, com número da dex. */
    @Transient
    val textoBusca: String = normalizar("$nome ${forma.orEmpty()} $dex")

    companion object {
        val NOMES_STATS = listOf("PS", "Ataque", "Defesa", "At. Esp.", "Def. Esp.", "Velocidade")
    }
}

fun normalizar(s: String): String =
    java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9 ]"), "")
