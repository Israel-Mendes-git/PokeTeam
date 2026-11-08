package com.poketeam.app.data

import com.poketeam.app.domain.RegrasSorteio
import com.poketeam.app.domain.AjustesSugestao
import com.poketeam.app.domain.ConfigRecomendar
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.ResultadoSorteio
import com.poketeam.app.domain.Sorteio
import com.poketeam.app.domain.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Sorteia vagas de um time usando o pool ativo, com as regras salvas nas preferências. */
class Sorteador(
    private val pokedex: Pokedex,
    private val pools: Pools,
    private val times: Times,
    private val prefs: Prefs,
    private val builds: Builds,
) {
    val regras: StateFlow<RegrasSorteio> = prefs.regrasSorteio
    val favoritos: StateFlow<Set<Int>> = prefs.favoritos

    val configRecomendar: StateFlow<ConfigRecomendar> = prefs.configRecomendar
    fun mudarRecomendar(f: (ConfigRecomendar) -> ConfigRecomendar) = prefs.mudarRecomendar(f)

    /** Candidatos do pool ativo que passam nas regras individuais do sorteio. */
    suspend fun candidatos(r: RegrasSorteio = regras.value): List<Pokemon> {
        val pool = pools.ativo.value
        val favs = favoritos.value
        return pokedex.todos().filter { pool.permite(it) && r.permite(it, favs) }
    }

    /**
     * Candidatos e ajustes do Recomendar (e das sugestões). Com "usar regras do sorteio" desligado,
     * só o pool e "só evolução final" valem.
     */
    suspend fun paraRecomendar(): Pair<List<Pokemon>, AjustesSugestao> {
        val c = configRecomendar.value
        val r = regras.value
        val candidatos = if (c.usarRegrasDoSorteio) candidatos(r)
        else pokedex.todos().filter { pools.ativo.value.permite(it) && (!r.soFinais || it.final) }
        val ajustes = AjustesSugestao(
            forca = builds.todasForcas(),
            pesoForca = c.estilo.pesoForca,
            doFormato = c.formatoSmogon?.let { builds.idsDoFormato(it) }.orEmpty(),
            favoritos = if (c.preferirFavoritos) favoritos.value else emptySet(),
            semTipoRepetido = c.usarRegrasDoSorteio && r.semTipoRepetido,
            tipoLivre = if (c.usarRegrasDoSorteio) r.tipoMono else null,
            tiposObrigatorios = if (c.usarRegrasDoSorteio) r.obrigatorios else emptySet(),
        )
        return candidatos to ajustes
    }

    fun mudarRegras(f: (RegrasSorteio) -> RegrasSorteio) = prefs.mudarRegras(f)

    /** Completa as vagas vazias do time [id] e grava. */
    suspend fun completar(id: String): ResultadoSorteio? = withContext(Dispatchers.Default) {
        val time = times.todos.value?.firstOrNull { it.id == id } ?: return@withContext null
        val travados = time.membros.mapNotNull { pokedex.porId(it) }
        val candidatos = candidatos()
        val r = Sorteio.sortear(candidatos, travados, regras.value, Time.MAXIMO)
        times.atualizar(id) { it.copy(membros = r.time.map { p -> p.id }) }
        r
    }
}
