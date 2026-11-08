package com.poketeam.app.ui.amigos

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Conta
import com.poketeam.app.data.ErroServidor
import com.poketeam.app.data.ListaAmigos
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Times
import com.poketeam.app.data.TimesDoAmigo
import com.poketeam.app.data.Treinador
import com.poketeam.app.domain.Pokemon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AmigosViewModel(private val conta: Conta, private val times: Times) : ViewModel() {
    val disponivel = conta.disponivel
    val modoTeste = conta.modoTeste
    val eu: StateFlow<Treinador?> = conta.eu
    val amigos: StateFlow<ListaAmigos?> = conta.amigos

    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem
    fun mensagemVista() { _mensagem.value = null }

    /** Roda [bloco] mostrando "ocupado" e transformando erro em mensagem para a tela. */
    private fun tarefa(sucesso: String? = null, bloco: suspend () -> Unit) = viewModelScope.launch {
        _ocupado.value = true
        try {
            bloco()
            if (sucesso != null) _mensagem.value = sucesso
        } catch (e: ErroServidor) {
            _mensagem.value = e.message
        } catch (e: Exception) {
            _mensagem.value = "Algo deu errado: ${e.message ?: e::class.simpleName}"
        } finally {
            _ocupado.value = false
        }
    }

    fun atualizar() = tarefa { conta.atualizarPerfil(); conta.atualizarAmigos() }
    fun entrarComGoogle(activity: Activity) = tarefa { conta.entrarComGoogle(activity) }
    fun entrarComoTeste(nome: String) = tarefa { conta.entrarComoTeste(nome) }
    fun sair() = tarefa { conta.sair() }
    fun apagarConta() = tarefa("Conta apagada.") { conta.apagarConta() }
    fun mudarApelido(a: String) = tarefa { conta.mudarApelido(a) }
    fun mudarFoto(uri: Uri) = tarefa { conta.mudarFoto(uri) }
    fun aceitar(id: Int) = tarefa { conta.aceitar(id) }
    fun desfazer(id: Int) = tarefa { conta.desfazer(id) }

    fun adicionar(codigo: String) = viewModelScope.launch {
        _ocupado.value = true
        try {
            _mensagem.value = if (conta.adicionar(codigo)) "Agora vocês são amigos!" else "Pedido enviado. Falta a outra pessoa aceitar."
        } catch (e: ErroServidor) {
            _mensagem.value = e.message
        } finally {
            _ocupado.value = false
        }
    }
}

/** Perfil de um amigo: os times que ele compartilhou, com os pokémons resolvidos. */
class AmigoViewModel(
    private val id: Int,
    private val conta: Conta,
    private val times: Times,
    private val pokedex: Pokedex,
) : ViewModel() {
    data class Tela(val dados: TimesDoAmigo, val pokemons: Map<Int, Pokemon>)

    private val _tela = MutableStateFlow<Tela?>(null)
    val tela: StateFlow<Tela?> = _tela
    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem
    fun mensagemVista() { _mensagem.value = null }

    init {
        viewModelScope.launch {
            try {
                val d = conta.timesDe(id)
                val ids = (d.times.flatMap { it.membros + listOfNotNull(it.mega) } + d.favoritos).toSet()
                _tela.value = Tela(d, ids.mapNotNull { pokedex.porId(it) }.associateBy { it.id })
            } catch (e: ErroServidor) {
                _mensagem.value = e.message
            }
        }
    }

    /** Copia um time do amigo para "Meus times" (com builds e Mega). */
    fun copiar(indice: Int) = viewModelScope.launch {
        val t = _tela.value?.dados ?: return@launch
        val time = t.times.getOrNull(indice) ?: return@launch
        times.importar("${time.nome} · ${t.treinador.apelido}", time.membros,
            time.builds.mapKeys { it.key.toInt() }, time.mega)
        _mensagem.value = "Copiado para Meus times."
    }
}
