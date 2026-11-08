package com.poketeam.app

import android.app.Application
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Times
import com.poketeam.app.data.Pools
import com.poketeam.app.data.Prefs
import com.poketeam.app.data.Elencos
import com.poketeam.app.data.Builds
import com.poketeam.app.data.Atualizador
import com.poketeam.app.data.Drafts
import com.poketeam.app.data.Sorteador

class PokeTeamApp : Application() {
    val pokedex by lazy { Pokedex(this) }
    val times by lazy { Times(this) }
    val pools by lazy { Pools(this) }
    val drafts by lazy { Drafts(this, pokedex, times) }
    val prefs by lazy { Prefs(this) }
    val elencos by lazy { Elencos(this) }
    val builds by lazy { Builds(this) }
    val atualizador by lazy { Atualizador(this, prefs) }
    val sorteador by lazy { Sorteador(pokedex, pools, times, prefs) }
}
