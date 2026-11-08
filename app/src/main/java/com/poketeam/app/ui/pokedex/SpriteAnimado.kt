package com.poketeam.app.ui.pokedex

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.poketeam.app.domain.Pokemon
import kotlinx.serialization.json.Json

/** Ids com GIF animado na PokeAPI (assets/animados.json, gerado por tools/baixar_animados.py). */
val LocalAnimados = staticCompositionLocalOf<Set<Int>> { emptySet() }

fun carregarAnimados(context: Context): Set<Int> = runCatching {
    Json.decodeFromString<List<Int>>(context.assets.open("animados.json").bufferedReader().use { it.readText() }).toSet()
}.getOrDefault(emptySet())

private fun urlAnimado(id: Int) =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-v/black-white/animated/$id.gif"

/**
 * Sprite com vida: GIF estilo Black/White quando existe (baixado na primeira vez e guardado em cache);
 * enquanto carrega, sem internet ou sem GIF, o sprite parado "respira" (escala sutil vinda do chão).
 */
@Composable
fun SpriteAnimado(p: Pokemon, tamanhoDp: Int, modifier: Modifier = Modifier, pulaAoEntrar: Boolean = false) {
    val entrada = remember(p.id) { Animatable(if (pulaAoEntrar) 0.4f else 1f) }
    LaunchedEffect(p.id) {
        if (pulaAoEntrar) entrada.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val temGif = p.id in LocalAnimados.current
    var gifPronto by remember(p.id) { mutableStateOf(false) }

    val respiro = rememberInfiniteTransition(label = "respiro")
    val escala by respiro.animateFloat(
        initialValue = 1f, targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escala",
    )

    Box(
        modifier.size(tamanhoDp.dp).background(MaterialTheme.colorScheme.surfaceVariant)
            .graphicsLayer { scaleX = entrada.value; scaleY = entrada.value },
        contentAlignment = Alignment.Center,
    ) {
        if (!gifPronto) {
            AsyncImage(
                model = p.sprite,
                contentDescription = p.nomeCompleto,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleY = escala
                    scaleX = 2f - escala // achata um pouco ao "inspirar", como sprite de jogo
                    transformOrigin = TransformOrigin(0.5f, 0.9f)
                },
            )
        }
        if (temGif) {
            AsyncImage(
                model = urlAnimado(p.id),
                contentDescription = if (gifPronto) p.nomeCompleto else null,
                filterQuality = FilterQuality.None,
                onState = { gifPronto = it is AsyncImagePainter.State.Success },
                // GIFs de Black/White são menores que o quadro: ocupa ~80% para não ficar borrado demais.
                modifier = Modifier.size((tamanhoDp * 0.8f).dp),
            )
        }
    }
}
