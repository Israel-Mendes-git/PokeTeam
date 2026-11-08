package com.poketeam.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.poketeam.app.BuildConfig
import com.poketeam.app.domain.Versao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Versão nova publicada nas Releases do GitHub. */
data class Novidade(val versao: Versao, val notas: String, val urlApk: String, val tamanho: Long)

sealed interface EstadoAtualizacao {
    data object Parado : EstadoAtualizacao
    data object Checando : EstadoAtualizacao
    data object EmDia : EstadoAtualizacao
    data class Disponivel(val novidade: Novidade) : EstadoAtualizacao
    data class Baixando(val novidade: Novidade, val progresso: Float) : EstadoAtualizacao
    data class Erro(val mensagem: String) : EstadoAtualizacao
}

@Serializable
private data class Release(
    @SerialName("tag_name") val tag: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<Asset> = emptyList(),
)

@Serializable
private data class Asset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
)

/**
 * Procura a última Release de [BuildConfig.REPO_GITHUB], baixa o APK anexado e abre o instalador do Android.
 * A versão nova precisa estar assinada com a mesma chave da instalada, senão o Android recusa.
 */
class Atualizador(private val context: Context, private val prefs: Prefs) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.Parado)
    val estado: StateFlow<EstadoAtualizacao> = _estado

    val versaoAtual: String = BuildConfig.VERSION_NAME
    val configurado: Boolean = BuildConfig.REPO_GITHUB.isNotBlank()

    /** Checagem automática ao abrir: no máximo uma vez a cada [INTERVALO_MS], e só avisa se houver novidade. */
    suspend fun checarSeFaltar() {
        if (!configurado) return
        val ultima = prefs.ultimaChecagem()
        if (System.currentTimeMillis() - ultima < INTERVALO_MS) return
        checar(silencioso = true)
    }

    /** [silencioso]: erro de rede vira Parado em vez de mensagem (checagem automática não incomoda). */
    suspend fun checar(silencioso: Boolean = false) {
        if (!configurado) {
            _estado.value = EstadoAtualizacao.Erro("Este build não tem repositório de atualizações configurado.")
            return
        }
        _estado.value = EstadoAtualizacao.Checando
        _estado.value = try {
            val r = withContext(Dispatchers.IO) { buscarUltima() }
            prefs.marcarChecagem(System.currentTimeMillis())
            val atual = Versao.ler(versaoAtual)
            val nova = Versao.ler(r.tag)
            val apk = r.assets.firstOrNull { it.name.endsWith(".apk") }
            when {
                nova == null || apk == null -> EstadoAtualizacao.EmDia
                atual != null && nova <= atual -> EstadoAtualizacao.EmDia
                else -> EstadoAtualizacao.Disponivel(Novidade(nova, r.body.orEmpty().trim(), apk.url, apk.size))
            }
        } catch (e: Exception) {
            if (silencioso) EstadoAtualizacao.Parado
            else EstadoAtualizacao.Erro("Não deu para falar com o GitHub. Confira a internet e tente de novo.")
        }
    }

    fun dispensar() {
        _estado.value = EstadoAtualizacao.Parado
    }

    /** Baixa o APK (com progresso) e abre o instalador. */
    suspend fun baixarEInstalar(n: Novidade) {
        if (!podeInstalar()) {
            pedirPermissaoDeInstalar()
            return
        }
        _estado.value = EstadoAtualizacao.Baixando(n, 0f)
        val arquivo = try {
            withContext(Dispatchers.IO) { baixar(n) }
        } catch (e: Exception) {
            _estado.value = EstadoAtualizacao.Erro("O download falhou. Tente de novo.")
            return
        }
        _estado.value = EstadoAtualizacao.Disponivel(n)
        instalar(arquivo)
    }

    /** Android 8+: o usuário precisa liberar "instalar apps desconhecidos" para este app uma vez. */
    fun podeInstalar(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun pedirPermissaoDeInstalar() {
        val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    private fun buscarUltima(): Release {
        val url = URL("${BuildConfig.API_GITHUB}/repos/${BuildConfig.REPO_GITHUB}/releases/latest")
        val con = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "PokeTeam-app")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (con.responseCode != 200) throw IllegalStateException("HTTP ${con.responseCode}")
            return json.decodeFromString<Release>(con.inputStream.bufferedReader().use { it.readText() })
        } finally {
            con.disconnect()
        }
    }

    private fun baixar(n: Novidade): File {
        val pasta = File(context.cacheDir, "atualizacao").apply { mkdirs() }
        pasta.listFiles()?.forEach { it.delete() } // só guarda a versão em andamento
        val destino = File(pasta, "PokeTeam-${n.versao}.apk")
        var url = URL(n.urlApk)
        // O link do GitHub redireciona para outro domínio; HttpURLConnection não segue troca de domínio sozinho.
        repeat(5) {
            val con = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "PokeTeam-app")
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            when (con.responseCode) {
                in 300..399 -> {
                    url = URL(url, con.getHeaderField("Location"))
                    con.disconnect()
                }
                200 -> {
                    val total = con.contentLengthLong.takeIf { it > 0 } ?: n.tamanho
                    con.inputStream.use { entrada ->
                        destino.outputStream().use { saida ->
                            val buf = ByteArray(64 * 1024)
                            var lidos = 0L
                            while (true) {
                                val q = entrada.read(buf)
                                if (q < 0) break
                                saida.write(buf, 0, q)
                                lidos += q
                                if (total > 0) _estado.value = EstadoAtualizacao.Baixando(n, (lidos.toFloat() / total).coerceAtMost(1f))
                            }
                        }
                    }
                    con.disconnect()
                    return destino
                }
                else -> throw IllegalStateException("HTTP ${con.responseCode}")
            }
        }
        throw IllegalStateException("Redirecionamentos demais")
    }

    private fun instalar(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.arquivos", apk)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    companion object {
        const val INTERVALO_MS = 24L * 60 * 60 * 1000
    }
}
