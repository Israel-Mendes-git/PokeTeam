package com.poketeam.app.data

import android.content.Context
import com.poketeam.app.domain.ConfigDraft
import com.poketeam.app.domain.RegrasSorteio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Preferencias(
    val regrasSorteio: RegrasSorteio = RegrasSorteio(),
    val configDraft: ConfigDraft = ConfigDraft(),
    /** id de uma paleta em ui/tema/Paletas.kt. */
    val tema: String? = null,
    /** Última vez (ms) que o app procurou atualização. */
    val ultimaChecagem: Long = 0,
)

/** Escolhas que o app lembra entre aberturas, em filesDir/preferencias.json. */
class Prefs(context: Context) {
    private val arquivo = ArquivoJson(File(context.filesDir, "preferencias.json"), Preferencias.serializer()) { Preferencias() }

    /** null enquanto carrega. */
    val valor: StateFlow<Preferencias?> = arquivo.valor

    val regrasSorteio: StateFlow<RegrasSorteio> = arquivo.valor.map { it?.regrasSorteio ?: RegrasSorteio() }
        .stateIn(arquivo.escopo, SharingStarted.Eagerly, RegrasSorteio())
    val configDraft: StateFlow<ConfigDraft> = arquivo.valor.map { it?.configDraft ?: ConfigDraft() }
        .stateIn(arquivo.escopo, SharingStarted.Eagerly, ConfigDraft())

    suspend fun ultimaChecagem(): Long = arquivo.alterar { it to it.ultimaChecagem }

    suspend fun marcarChecagem(quando: Long) = arquivo.alterar { it.copy(ultimaChecagem = quando) to Unit }

    fun mudarTema(id: String) {
        arquivo.escopo.launch { arquivo.alterar { it.copy(tema = id) to Unit } }
    }

    fun mudarRegras(f: (RegrasSorteio) -> RegrasSorteio) {
        arquivo.escopo.launch { arquivo.alterar { it.copy(regrasSorteio = f(it.regrasSorteio)) to Unit } }
    }

    fun mudarConfigDraft(f: (ConfigDraft) -> ConfigDraft) {
        arquivo.escopo.launch { arquivo.alterar { it.copy(configDraft = f(it.configDraft)) to Unit } }
    }
}
