package com.poketeam.app.data

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.poketeam.app.BuildConfig
import com.poketeam.app.domain.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class Treinador(val id: Int, val apelido: String, val foto: String? = null, val codigo: String? = null)

@Serializable
data class ListaAmigos(
    val amigos: List<Treinador> = emptyList(),
    val recebidos: List<Treinador> = emptyList(),
    val enviados: List<Treinador> = emptyList(),
)

/** Time como o servidor guarda: só o necessário para o amigo ver e copiar. */
@Serializable
data class TimeCompartilhado(
    val id: String,
    val nome: String,
    val membros: List<Int>,
    val builds: Map<String, Int> = emptyMap(),
    val mega: Int? = null,
)

@Serializable
data class TimesDoAmigo(val treinador: Treinador, val times: List<TimeCompartilhado> = emptyList())

@Serializable
private data class EstadoConta(val sessao: String? = null, val eu: Treinador? = null, val compartilhados: Set<String> = emptySet())

@Serializable
private data class RespostaEntrar(val sessao: String, val eu: Treinador)

@Serializable
private data class RespostaPedido(val estado: String = "")

class ErroServidor(mensagem: String) : Exception(mensagem)

/**
 * Conta do usuário no servidor de amigos (login com Google). Tudo aqui é opcional: sem login,
 * o app funciona offline como antes.
 */
class Conta(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val arquivo = ArquivoJson(File(context.filesDir, "conta.json"), EstadoConta.serializer()) { EstadoConta() }

    /** O build tem servidor configurado (config.properties ou Secrets do GitHub)? */
    val disponivel: Boolean = BuildConfig.SERVIDOR.isNotBlank()
    /** Build de teste local: mostra "entrar como teste" (o servidor local aceita tokens falsos). */
    val modoTeste: Boolean = BuildConfig.SERVIDOR.startsWith("http://")

    val eu: StateFlow<Treinador?> = arquivo.valor.map { it?.eu }.stateIn(arquivo.escopo, SharingStarted.Eagerly, null)
    val compartilhados: StateFlow<Set<String>> = arquivo.valor.map { it?.compartilhados.orEmpty() }
        .stateIn(arquivo.escopo, SharingStarted.Eagerly, emptySet())

    private val _amigos = MutableStateFlow<ListaAmigos?>(null)
    val amigos: StateFlow<ListaAmigos?> = _amigos

    // ---------- login ----------

    /** Abre o seletor de contas Google. Devolve false se a pessoa cancelou. */
    suspend fun entrarComGoogle(activity: Activity): Boolean {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) throw ErroServidor("Login com Google não configurado neste build.")
        val pedido = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build())
            .build()
        val resposta = try {
            CredentialManager.create(activity).getCredential(activity, pedido)
        } catch (e: GetCredentialCancellationException) {
            return false
        }
        val cred = resposta.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ErroServidor("O Google não devolveu uma conta válida.")
        }
        entrarComToken(GoogleIdTokenCredential.createFrom(cred.data).idToken)
        return true
    }

    /** Só em build de teste local: o servidor com MODO_TESTE aceita "teste:<id>:<nome>". */
    suspend fun entrarComoTeste(nome: String) = entrarComToken("teste:${nome.lowercase()}:$nome")

    private suspend fun entrarComToken(token: String) {
        val r = json.decodeFromString<RespostaEntrar>(chamar("POST", "/entrar", buildJsonObject { put("idToken", token) }.toString(), autenticado = false))
        arquivo.alterar { it.copy(sessao = r.sessao, eu = r.eu) to Unit }
        atualizarAmigos()
    }

    suspend fun sair() {
        runCatching { chamar("POST", "/sair") }
        arquivo.alterar { EstadoConta() to Unit }
        _amigos.value = null
    }

    suspend fun apagarConta() {
        chamar("DELETE", "/eu")
        arquivo.alterar { EstadoConta() to Unit }
        _amigos.value = null
    }

    // ---------- perfil ----------

    suspend fun atualizarPerfil() {
        if (eu.value == null) return
        val p = json.decodeFromString<Treinador>(chamar("GET", "/eu"))
        arquivo.alterar { it.copy(eu = p) to Unit }
    }

    suspend fun mudarApelido(apelido: String) {
        val p = json.decodeFromString<Treinador>(chamar("PATCH", "/eu", buildJsonObject { put("apelido", apelido) }.toString()))
        arquivo.alterar { it.copy(eu = p) to Unit }
    }

    /** Reduz a imagem escolhida para 256 px (JPEG ~20 KB) antes de enviar. */
    suspend fun mudarFoto(uri: Uri) {
        val bytes = withContext(Dispatchers.Default) {
            val original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: throw ErroServidor("Não deu para abrir a imagem.")
            val lado = minOf(original.width, original.height)
            val quadrado = Bitmap.createBitmap(original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado)
            val pequeno = Bitmap.createScaledBitmap(quadrado, 256, 256, true)
            ByteArrayOutputStream().also { pequeno.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        }
        val p = json.decodeFromString<Treinador>(chamar("PUT", "/eu/foto", bytes, "image/jpeg"))
        arquivo.alterar { it.copy(eu = p) to Unit }
    }

    // ---------- amigos ----------

    suspend fun atualizarAmigos() {
        if (eu.value == null) return
        _amigos.value = json.decodeFromString<ListaAmigos>(chamar("GET", "/amigos"))
    }

    /** Devolve true se virou amizade na hora (a outra pessoa já tinha pedido). */
    suspend fun adicionar(codigo: String): Boolean {
        val r = json.decodeFromString<RespostaPedido>(chamar("POST", "/amigos", buildJsonObject { put("codigo", codigo) }.toString()))
        atualizarAmigos()
        return r.estado == "amigos"
    }

    suspend fun aceitar(id: Int) { chamar("POST", "/amigos/$id/aceitar"); atualizarAmigos() }
    suspend fun desfazer(id: Int) { chamar("DELETE", "/amigos/$id"); atualizarAmigos() }

    suspend fun timesDe(id: Int): TimesDoAmigo = json.decodeFromString(chamar("GET", "/amigos/$id/times"))

    // ---------- times compartilhados ----------

    suspend fun compartilhar(t: Time) {
        val corpo = json.encodeToString(TimeCompartilhado.serializer(),
            TimeCompartilhado(t.id, t.nome, t.membros, t.builds.mapKeys { it.key.toString() }, t.mega))
        chamar("PUT", "/times/${t.id}", corpo)
        arquivo.alterar { it.copy(compartilhados = it.compartilhados + t.id) to Unit }
    }

    suspend fun pararDeCompartilhar(timeId: String) {
        chamar("DELETE", "/times/$timeId")
        arquivo.alterar { it.copy(compartilhados = it.compartilhados - timeId) to Unit }
    }

    // ---------- HTTP ----------

    private suspend fun chamar(metodo: String, caminho: String, corpo: String? = null, autenticado: Boolean = true): String =
        chamar(metodo, caminho, corpo?.toByteArray(), "application/json", autenticado)

    private suspend fun chamar(
        metodo: String, caminho: String, corpo: ByteArray?, tipo: String, autenticado: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        if (!disponivel) throw ErroServidor("Amigos não está configurado neste build.")
        val con = (URL(BuildConfig.SERVIDOR.trimEnd('/') + caminho).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 10_000
            readTimeout = 15_000
            if (autenticado) arquivo.valor.value?.sessao?.let { setRequestProperty("Authorization", "Bearer $it") }
            if (corpo != null) {
                doOutput = true
                setRequestProperty("Content-Type", tipo)
            }
        }
        try {
            if (corpo != null) con.outputStream.use { it.write(corpo) }
            val codigo = con.responseCode
            val texto = (if (codigo < 400) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (codigo == 401 && autenticado) {
                arquivo.alterar { EstadoConta() to Unit } // sessão perdida: volta para a tela de login
                throw ErroServidor("Sua sessão expirou. Entre de novo.")
            }
            if (codigo >= 400) {
                val msg = runCatching { json.decodeFromString<JsonObject>(texto)["erro"]?.toString()?.trim('"') }.getOrNull()
                throw ErroServidor(msg ?: "O servidor respondeu com erro $codigo.")
            }
            texto
        } catch (e: java.io.IOException) {
            throw ErroServidor("Sem conexão com o servidor.")
        } finally {
            con.disconnect()
        }
    }
}
