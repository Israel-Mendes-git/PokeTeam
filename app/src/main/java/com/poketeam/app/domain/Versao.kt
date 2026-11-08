package com.poketeam.app.domain

/** Versão "maior.menor.correção" (aceita "v1.2.3" e partes faltando). */
data class Versao(val maior: Int, val menor: Int, val correcao: Int) : Comparable<Versao> {
    override fun compareTo(other: Versao) = compareValuesBy(this, other, Versao::maior, Versao::menor, Versao::correcao)
    override fun toString() = "$maior.$menor.$correcao"

    companion object {
        fun ler(texto: String): Versao? {
            val partes = texto.trim().removePrefix("v").removePrefix("V").split(".")
            val nums = partes.map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: return null }
            if (nums.isEmpty() || nums.size > 3) return null
            val (a, b, c) = (nums + listOf(0, 0)).take(3)
            return Versao(a, b, c)
        }
    }
}
