package com.poketeam.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Um valor guardado num arquivo JSON, mantido inteiro na memória.
 * Cada mudança atualiza [valor] na hora e regrava o arquivo em segundo plano
 * (sempre o estado mais recente, via arquivo temporário + rename).
 */
class ArquivoJson<T : Any>(
    private val arquivo: File,
    private val serializer: KSerializer<T>,
    private val padrao: () -> T,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()
    private val mutexArquivo = Mutex()
    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _valor = MutableStateFlow<T?>(null)
    /** null enquanto carrega. */
    val valor: StateFlow<T?> = _valor

    init {
        escopo.launch {
            _valor.value = runCatching { json.decodeFromString(serializer, arquivo.readText()) }.getOrNull() ?: padrao()
        }
    }

    suspend fun <R> alterar(bloco: (T) -> Pair<T, R>): R = mutex.withLock {
        while (_valor.value == null) delay(10)
        val (novo, resultado) = bloco(_valor.value!!)
        _valor.value = novo
        escopo.launch { gravar() }
        resultado
    }

    private suspend fun gravar() = mutexArquivo.withLock {
        val atual = _valor.value ?: return@withLock
        val tmp = File(arquivo.parentFile, arquivo.name + ".tmp")
        tmp.writeText(json.encodeToString(serializer, atual))
        if (!tmp.renameTo(arquivo)) {
            arquivo.delete()
            tmp.renameTo(arquivo)
        }
    }
}
