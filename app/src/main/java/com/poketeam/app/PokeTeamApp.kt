package com.poketeam.app

import android.app.Application
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.poketeam.app.ui.pokedex.carregarAnimados
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Times
import com.poketeam.app.data.Pools
import com.poketeam.app.data.Prefs
import com.poketeam.app.data.Elencos
import com.poketeam.app.data.Builds
import com.poketeam.app.data.Conta
import com.poketeam.app.data.Atualizador
import com.poketeam.app.data.Drafts
import com.poketeam.app.data.Sorteador

class PokeTeamApp : Application(), ImageLoaderFactory {
    /** GIFs animados: decodificador nativo no Android 9+, o do Coil antes disso. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory()) }
        .build()

    val animados by lazy { carregarAnimados(this) }

    private val escopo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // Favoritos visíveis aos amigos: sincroniza ao entrar na conta e a cada mudança (agrupada).
        escopo.launch {
            kotlinx.coroutines.flow.combine(conta.eu, prefs.favoritos) { eu, favs -> if (eu != null) favs else null }
                .filterNotNull().distinctUntilChanged().debounce(2000)
                .collect { favs -> runCatching { conta.enviarFavoritos(favs) } }
        }
    }
    val pokedex by lazy { Pokedex(this) }
    val times by lazy { Times(this) }
    val pools by lazy { Pools(this) }
    val drafts by lazy { Drafts(this, pokedex, times) }
    val prefs by lazy { Prefs(this) }
    val elencos by lazy { Elencos(this) }
    val builds by lazy { Builds(this) }
    val conta by lazy { Conta(this) }
    val atualizador by lazy { Atualizador(this, prefs) }
    val sorteador by lazy { Sorteador(pokedex, pools, times, prefs, builds) }
}
