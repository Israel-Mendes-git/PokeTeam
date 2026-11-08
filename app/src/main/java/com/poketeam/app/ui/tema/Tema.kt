package com.poketeam.app.ui.tema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poketeam.app.R
import com.poketeam.app.domain.Tipo

/** Paleta do tema ativo. */
val LocalPaleta = staticCompositionLocalOf { Paletas.padrao }

/** Jersey 10 (OFL): pixel legível, números e letras sem ambiguidade. Um peso só; negrito é sintetizado. */
private val Pixel = FontFamily(Font(R.font.jersey10))

/**
 * A Jersey 10 desenha menor que a fonte padrão: tudo sobe 30%. Ligaduras desligadas
 * ("fi" e "fl" viram desenhos estranhos em fontes pixel).
 */
private fun tipografia(): Typography {
    val base = Typography()
    fun TextStyle.px() = copy(
        fontFamily = Pixel,
        fontSize = fontSize * 1.3f,
        lineHeight = lineHeight * 1.2f,
        letterSpacing = 0.5.sp,
        fontFeatureSettings = "liga 0, clig 0",
    )
    return Typography(
        displayLarge = base.displayLarge.px(), displayMedium = base.displayMedium.px(), displaySmall = base.displaySmall.px(),
        headlineLarge = base.headlineLarge.px(), headlineMedium = base.headlineMedium.px(), headlineSmall = base.headlineSmall.px(),
        titleLarge = base.titleLarge.px(), titleMedium = base.titleMedium.px(), titleSmall = base.titleSmall.px(),
        bodyLarge = base.bodyLarge.px(), bodyMedium = base.bodyMedium.px(), bodySmall = base.bodySmall.px(),
        labelLarge = base.labelLarge.px(), labelMedium = base.labelMedium.px(), labelSmall = base.labelSmall.px(),
    )
}

/** Cantos retos em todo componente do Material (botões, campos, menus, diálogos). */
private val Quadrado = CutCornerShape(0.dp)
private val FormasPixel = Shapes(Quadrado, Quadrado, Quadrado, Quadrado, Quadrado)

@Composable
fun PokeTeamTema(paleta: Paleta = Paletas.padrao, conteudo: @Composable () -> Unit) {
    val p = paleta
    val esquema = remember(p) {
        val base = if (p.claro) lightColorScheme() else darkColorScheme()
        base.copy(
            primary = p.acento, onPrimary = p.textoAcento,
            primaryContainer = p.acento, onPrimaryContainer = p.textoAcento,
            secondary = p.destaque, onSecondary = p.fundo,
            // Chips marcados e o indicador da barra de abas usam secondaryContainer: precisa saltar aos olhos.
            secondaryContainer = p.acento, onSecondaryContainer = p.textoAcento,
            tertiary = p.bom,
            background = p.fundo, onBackground = p.texto,
            surface = p.painel, onSurface = p.texto,
            surfaceVariant = p.vaga, onSurfaceVariant = p.sub,
            surfaceContainer = p.painel, surfaceContainerHigh = p.painel, surfaceContainerHighest = p.vaga,
            surfaceContainerLow = p.painel, surfaceContainerLowest = p.fundo,
            outline = p.borda, outlineVariant = p.sub,
            error = p.alerta,
        )
    }
    val tipos = remember { tipografia() }
    CompositionLocalProvider(LocalPaleta provides p) {
        MaterialTheme(colorScheme = esquema, typography = tipos, shapes = FormasPixel) {
            // Surface define a cor padrão do texto.
            Surface(color = p.fundo, contentColor = p.texto, content = conteudo)
        }
    }
}

/**
 * Moldura pixel: linha escura por fora, borda grossa da cor do tema e um filete escuro por dentro,
 * como as caixas de diálogo dos jogos antigos.
 */
fun Modifier.bordaPixel(p: Paleta, grossura: Dp = 3.dp, dupla: Boolean = true): Modifier = drawBehind {
    val g = grossura.toPx()
    val fio = 2.dp.toPx()
    drawRect(p.bordaFundo, style = Stroke(fio * 2))
    drawRect(p.borda, topLeft = Offset(fio, fio), size = Size(size.width - fio * 2, size.height - fio * 2), style = Stroke(g))
    if (dupla) {
        val d = fio + g
        drawRect(p.bordaFundo, topLeft = Offset(d, d), size = Size(size.width - d * 2, size.height - d * 2), style = Stroke(fio))
    }
}

/** Caixa de diálogo pixel. */
@Composable
fun CaixaPixel(
    modifier: Modifier = Modifier,
    cor: Color? = null,
    dupla: Boolean = true,
    preenchimento: PaddingValues = PaddingValues(14.dp),
    conteudo: @Composable BoxScope.() -> Unit,
) {
    val p = LocalPaleta.current
    Box(
        modifier.background(cor ?: p.painel).bordaPixel(p, dupla = dupla).padding(preenchimento),
        content = conteudo,
    )
}

/** Botão pixel com sombra dura que "afunda" ao apertar. */
@Composable
fun BotaoPixel(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cor: Color? = null,
    corTexto: Color? = null,
    conteudo: @Composable RowScope.() -> Unit,
) {
    val p = LocalPaleta.current
    val interacao = remember { MutableInteractionSource() }
    val apertado by interacao.collectIsPressedAsState()
    val sombra = if (apertado || !enabled) 0.dp else 4.dp
    val fundo = if (enabled) (cor ?: p.acento) else p.vaga
    val texto = if (enabled) (corTexto ?: p.textoAcento) else p.sub
    val sombraCor = p.bordaFundo
    // A sombra é desenhada fora dos limites do próprio botão, então o tamanho segue o modifier de quem chama.
    Row(
        modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .offset(4.dp - sombra, 4.dp - sombra)
            .drawBehind {
                if (sombra > 0.dp) {
                    val d = sombra.toPx()
                    drawRect(sombraCor, topLeft = Offset(d, d), size = size)
                }
            }
            .background(fundo)
            .bordaPixel(p, grossura = 2.dp, dupla = false)
            .clickable(interactionSource = interacao, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides texto) { conteudo() }
    }
}

@Composable
fun SeloTipo(tipo: Tipo, modifier: Modifier = Modifier, compacto: Boolean = false) {
    Text(
        text = tipo.nome.uppercase(),
        color = Color.White,
        fontSize = if (compacto) 11.sp else 15.sp,
        maxLines = 1,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.labelSmall.copy(
            shadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), Offset(1f, 1f), 0f),
        ),
        modifier = modifier
            .background(Color(tipo.cor))
            .drawBehind { drawRect(Color(0x73000000), style = Stroke(1.dp.toPx() * 2)) }
            .padding(horizontal = if (compacto) 4.dp else 7.dp, vertical = if (compacto) 1.dp else 2.dp),
    )
}

/** Título de seção com o "▶" de menu de jogo. */
@Composable
fun TituloPixel(texto: String, modifier: Modifier = Modifier) {
    val p = LocalPaleta.current
    Text("▶ " + texto.uppercase(), color = p.destaque, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold, modifier = modifier)
}
