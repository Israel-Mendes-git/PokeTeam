package com.poketeam.app.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Conta
import com.poketeam.app.data.DraftOnline
import com.poketeam.app.data.ErroServidor
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Sala
import com.poketeam.app.data.Times
import com.poketeam.app.domain.ModoDraft
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.normalizar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Tudo que a tela da sala precisa. */
data class TelaSala(
    val sala: Sala,
    val tela: TelaDraft,
    /** Meu índice entre os jogadores (null = não estou na sala). */
    val eu: Int?,
    val souDono: Boolean,
) {
    val vez: Int? get() = tela.draft.vez
    val minhaVez: Boolean get() = sala.estado == "andamento" && vez != null && vez == eu
}

class SalaViewModel(
    val codigo: String,
    private val online: DraftOnline,
    private val conta: Conta,
    private val pokedex: Pokedex,
    private val times: Times,
) : ViewModel() {
    private val _sala = MutableStateFlow<Sala?>(null)
    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro
    private val _enviando = MutableStateFlow(false)
    val enviando: StateFlow<Boolean> = _enviando
    /** A sala sumiu (anfitrião encerrou ou passou de 24 h). */
    private val _encerrada = MutableStateFlow(false)
    val encerrada: StateFlow<Boolean> = _encerrada

    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca
    /** "Outras opções": muda a semente das 3 opções da minha vez. */
    private val _sorteio = MutableStateFlow(0)

    private val todos = flow { emit(pokedex.todos()) }

    val tela: StateFlow<TelaSala?> = combine(_sala, todos, _busca, _sorteio, conta.eu) { s, lista, busca, sorteio, eu ->
        if (s == null) return@combine null
        val porId = lista.associateBy { it.id }
        val d0 = s.comoDraft()
        val meu = s.indiceDe(eu?.id)
        // As opções saem no meu celular, sempre iguais para a mesma vez (sair e voltar não troca).
        val d = if (s.estado == "andamento" && d0.vez == meu && s.modo == ModoDraft.OPCOES)
            d0.comNovasOpcoes(lista, Random(s.codigo.hashCode() * 31 + d0.escolhas.size * 1009 + sorteio))
        else d0
        val disponiveis = d.disponiveis(lista)
        val termos = normalizar(busca).split(' ').filter(String::isNotEmpty)
        TelaSala(
            sala = s,
            tela = TelaDraft(
                draft = d,
                opcoes = d.opcoes.mapNotNull(porId::get),
                escolhasPorJogador = d.jogadores.indices.map { j -> d.escolhasDe(j).mapNotNull(porId::get) },
                lista = disponiveis.filter { p -> termos.all { p.textoBusca.contains(it) } },
                totalDisponiveis = disponiveis.size,
            ),
            eu = meu,
            souDono = eu?.id == s.dono,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Consulta a sala a cada 2 s enquanto a tela está aberta (chamado num LaunchedEffect). */
    suspend fun acompanhar() {
        while (true) {
            try {
                val novo = online.ver(codigo, _sala.value?.versao ?: 0)
                if (novo != null) mudarSala(novo)
                _erro.value = null
            } catch (e: ErroServidor) {
                if (e.message?.contains("não encontrada") == true) { _encerrada.value = true; return }
                _erro.value = e.message
            }
            delay(2_000)
        }
    }

    private fun mudarSala(s: Sala) {
        if (s.escolhas.size != _sala.value?.escolhas?.size) { _sorteio.value = 0; _busca.value = "" }
        _sala.value = s
    }

    private fun agir(acao: suspend () -> Sala?) = viewModelScope.launch {
        if (_enviando.value) return@launch
        _enviando.value = true
        try {
            acao()?.let(::mudarSala)
            _erro.value = null
        } catch (e: ErroServidor) {
            _erro.value = e.message
            runCatching { online.ver(codigo, 0) }.getOrNull()?.let(::mudarSala) // pega o estado certo
        } finally {
            _enviando.value = false
        }
    }

    fun buscar(t: String) { _busca.value = t }
    fun outrasOpcoes() { _sorteio.value++ }
    fun iniciar() = agir { online.iniciar(codigo) }
    fun desfazer() = agir { online.desfazer(codigo) }
    fun escolher(p: Pokemon) = agir {
        val s = _sala.value ?: return@agir null
        online.escolher(codigo, p, s.escolhas.size)
    }

    fun sair(depois: () -> Unit) = viewModelScope.launch {
        runCatching { online.sair(codigo) }
        depois()
    }

    /** Salva em Meus times: só o meu, ou o de todo mundo. */
    fun salvar(todos: Boolean, depois: () -> Unit) = viewModelScope.launch {
        val t = tela.value ?: return@launch
        t.sala.jogadores.forEachIndexed { i, j ->
            if (!todos && i != t.eu) return@forEachIndexed
            val ids = t.tela.draft.escolhasDe(i)
            if (ids.isNotEmpty()) times.criarCom("Draft · ${j.apelido}", ids)
        }
        depois()
    }
}
