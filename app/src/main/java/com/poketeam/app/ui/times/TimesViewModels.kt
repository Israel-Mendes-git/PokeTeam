package com.poketeam.app.ui.times

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poketeam.app.data.Pokedex
import com.poketeam.app.data.Times
import com.poketeam.app.data.Pools
import com.poketeam.app.data.Builds
import com.poketeam.app.data.Conta
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.debounce
import com.poketeam.app.domain.Build
import com.poketeam.app.domain.Sugestao
import com.poketeam.app.domain.Sugestoes
import kotlinx.coroutines.flow.combine
import com.poketeam.app.data.Sorteador
import com.poketeam.app.domain.RegrasSorteio
import com.poketeam.app.domain.ConfigRecomendar
import kotlinx.coroutines.flow.MutableStateFlow
import com.poketeam.app.domain.AnaliseTime
import com.poketeam.app.domain.Pokemon
import com.poketeam.app.domain.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Time com os pokémons já resolvidos a partir dos ids. */
/**
 * Time com os pokémons resolvidos. [pokemons] são os membros como escolhidos; [emBatalha] troca
 * o membro que mega evolui pela forma Mega (é o que a análise de fraquezas considera).
 */
data class TimeResolvido(val time: Time, val pokemons: List<Pokemon>, val megaAtiva: Pokemon? = null) {
    val emBatalha: List<Pokemon> = pokemons.map { p -> if (megaAtiva != null && megaAtiva.dex == p.dex) megaAtiva else p }
    val analise: AnaliseTime by lazy { AnaliseTime.de(emBatalha) }
}

private suspend fun Pokedex.resolver(t: Time): TimeResolvido {
    val membros = t.membros.mapNotNull { porId(it) }
    val mega = t.mega?.let { porId(it) }?.takeIf { m -> membros.any { it.dex == m.dex } }
    return TimeResolvido(t, membros, mega)
}

class MeusTimesViewModel(
    private val times: Times,
    private val pokedex: Pokedex,
    private val sorteador: Sorteador,
    private val conta: Conta,
) : ViewModel() {
    val lista: StateFlow<List<TimeResolvido>?> = times.todos
        .map { l -> l?.map { pokedex.resolver(it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun novo(aoCriar: (Time) -> Unit) = viewModelScope.launch { aoCriar(times.criar()) }

    /** Cria um time já sorteado com as regras atuais e abre. */
    fun novoSorteado(aoCriar: (Time) -> Unit) = viewModelScope.launch {
        val t = times.criar()
        sorteador.completar(t.id)
        aoCriar(t)
    }
    fun duplicar(id: String) = viewModelScope.launch { times.duplicar(id) }
    fun excluir(id: String) = viewModelScope.launch {
        times.excluir(id)
        if (id in conta.compartilhados.value) runCatching { conta.pararDeCompartilhar(id) }
    }
    fun alternarFavorito(id: String) = viewModelScope.launch { times.atualizar(id) { it.copy(favorito = !it.favorito) } }
}

class EditorTimeViewModel(
    private val id: String,
    private val times: Times,
    private val pokedex: Pokedex,
    private val sorteador: Sorteador,
    private val pools: Pools,
    private val builds: Builds,
    private val conta: Conta,
) : ViewModel() {
    /** Só aparece "Compartilhar com amigos" para quem entrou na conta. */
    val logado: StateFlow<Boolean> = conta.eu.map { it != null }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val compartilhado: StateFlow<Boolean> = conta.compartilhados.map { id in it }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _erroCompartilhar = MutableStateFlow<String?>(null)
    val erroCompartilhar: StateFlow<String?> = _erroCompartilhar

    fun alternarCompartilhar() = viewModelScope.launch {
        val t = times.todos.value?.firstOrNull { it.id == id } ?: return@launch
        _erroCompartilhar.value = runCatching {
            if (compartilhado.value) conta.pararDeCompartilhar(id) else conta.compartilhar(t)
        }.exceptionOrNull()?.message
    }

    init {
        // Time compartilhado: cada mudança vai para o servidor (agrupada, 1,5 s depois da última).
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            times.todos.map { l -> l?.firstOrNull { it.id == id } }.filterNotNull().distinctUntilChanged().drop(1).debounce(1500)
                .collect { t -> if (id in conta.compartilhados.value) runCatching { conta.compartilhar(t) } }
        }
    }

    val time: StateFlow<TimeResolvido?> = times.todos
        .map { l -> l?.firstOrNull { it.id == id }?.let { pokedex.resolver(it) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val regras: StateFlow<RegrasSorteio> = sorteador.regras

    /** Quem do pool ativo mais fecha os pontos fracos do time (vazio se o time está vazio ou cheio). */
    val configRecomendar: StateFlow<ConfigRecomendar> = sorteador.configRecomendar
    fun mudarRecomendar(f: (ConfigRecomendar) -> ConfigRecomendar) = sorteador.mudarRecomendar(f)

    val sugestoes: StateFlow<List<Sugestao>> =
        combine(time, pools.ativo, sorteador.regras, sorteador.favoritos, sorteador.configRecomendar) { t, _, _, _, _ ->
            if (t == null) emptyList()
            else sorteador.paraRecomendar().let { (candidatos, ajustes) -> Sugestoes.para(t.pokemons, candidatos, 6, ajustes) }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun adicionar(p: Pokemon) = viewModelScope.launch { esquecerDesfazer(); times.adicionarMembro(id, p.id) }

    /** Builds de cada membro do time (id do pokémon -> builds do Smogon). */
    /** Build padrão de cada membro: a primeira do formato escolhido no Recomendar, se houver. */
    val buildPadrao: StateFlow<Map<Int, Int>> = combine(time, sorteador.configRecomendar) { t, c ->
        val f = c.formatoSmogon ?: return@combine emptyMap()
        t?.pokemons.orEmpty().associate { p -> p.id to builds.de(p.id).indexOfFirst { f.casa(it.formato) }.coerceAtLeast(0) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun indiceBuild(t: Time, pokemonId: Int): Int = t.builds[pokemonId] ?: buildPadrao.value[pokemonId] ?: 0

    val buildsDoTime: StateFlow<Map<Int, List<Build>>> = time
        .map { t -> t?.pokemons.orEmpty().associate { it.id to builds.de(it.id) } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Formas Mega disponíveis para cada membro (id do membro -> megas). */
    val megasDoTime: StateFlow<Map<Int, List<Pokemon>>> = time
        .map { t -> t?.pokemons.orEmpty().associate { it.id to pokedex.megasDe(it.dex) }.filterValues { it.isNotEmpty() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Ativa a Mega (ou desliga, se já era ela). Só uma por time. */
    fun alternarMega(megaId: Int) = viewModelScope.launch {
        times.atualizar(id) { t -> t.copy(mega = if (t.mega == megaId) null else megaId) }
    }

    fun escolherBuild(pokemonId: Int, indice: Int) = viewModelScope.launch {
        times.atualizar(id) { t -> t.copy(builds = t.builds + (pokemonId to indice)) }
    }

    /** Completa as vagas vazias com a recomendação (cobertura de tipos + força no Smogon), no pool ativo. */
    fun recomendar() = viewModelScope.launch {
        esquecerDesfazer()
        val t = time.value ?: return@launch
        val (candidatos, ajustes) = sorteador.paraRecomendar()
        val completo = Sugestoes.completar(t.pokemons, candidatos, ajustes)
        times.atualizar(id) { it.copy(membros = completo.map { p -> p.id }) }
        val faltam = ajustes.tiposObrigatorios - completo.flatMap { it.tipos }.toSet()
        _aviso.value = when {
            completo.size < Time.MAXIMO -> "Não há candidatos suficientes com essas configurações: ${Time.MAXIMO - completo.size} vaga(s) vazia(s)."
            faltam.isNotEmpty() -> "Nenhum candidato trouxe: " + faltam.joinToString { it.nome } + "."
            else -> null
        }
    }

    /** Time em texto para mandar aos amigos; os nomes seguem o formato de importação do Showdown. */
    fun textoParaCompartilhar(): String? {
        val t = time.value ?: return null
        return buildString {
            appendLine(t.time.nome)
            appendLine()
            val bs = buildsDoTime.value
            t.pokemons.forEach { p ->
                val lista = bs[p.id].orEmpty()
                val b = lista.getOrNull(indiceBuild(t.time, p.id))
                appendLine(b?.paraShowdown(p.nomeShowdown) ?: p.nomeShowdown)
                appendLine()
            }
        }.trimEnd()
    }
    fun mudarRegras(f: (RegrasSorteio) -> RegrasSorteio) = sorteador.mudarRegras(f)

    private val _sorteando = MutableStateFlow(false)
    val sorteando: StateFlow<Boolean> = _sorteando

    /** Mensagem curta depois do sorteio (ex.: vagas que o pool não conseguiu preencher). */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso
    fun avisoVisto() { _aviso.value = null }

    fun sortear() = viewModelScope.launch {
        esquecerDesfazer()
        _sorteando.value = true
        val r = sorteador.completar(id)
        _sorteando.value = false
        if (r != null && r.tiposFaltando.isNotEmpty() && r.faltaram == 0) {
            _aviso.value = "Não coube nenhum pokémon de: " + r.tiposFaltando.joinToString { it.nome } + "."
        }
        if (r != null && r.faltaram > 0) {
            val origem = if (sorteador.regras.value.soFavoritos) "Seus favoritos não bastam" else "O pool ativo não tem pokémons suficientes"
            _aviso.value = "$origem para essas regras: " +
                if (r.faltaram == 1) "1 vaga ficou vazia." else "${r.faltaram} vagas ficaram vazias."
        }
    }


    fun renomear(nome: String) = viewModelScope.launch { times.atualizar(id) { it.copy(nome = nome) } }

    fun remover(indice: Int) = viewModelScope.launch {
        times.atualizar(id) { t -> t.copy(membros = t.membros.filterIndexed { i, _ -> i != indice }) }
    }

    fun descartarSeVazio() = times.descartarSeVazio(id)

    // Limpar o time inteiro de uma vez, com "Desfazer" logo em seguida.
    private var antesDeLimpar: Time? = null
    private val _podeDesfazer = MutableStateFlow(false)
    val podeDesfazer: StateFlow<Boolean> = _podeDesfazer

    fun limpar() = viewModelScope.launch {
        val atual = time.value?.time ?: return@launch
        if (atual.membros.isEmpty()) return@launch
        antesDeLimpar = atual
        _aviso.value = null
        times.atualizar(id) { it.copy(membros = emptyList(), builds = emptyMap(), mega = null) }
        _podeDesfazer.value = true
    }

    fun desfazerLimpar() = viewModelScope.launch {
        val antes = antesDeLimpar ?: return@launch
        times.atualizar(id) { it.copy(membros = antes.membros, builds = antes.builds, mega = antes.mega) }
        antesDeLimpar = null
        _podeDesfazer.value = false
    }

    fun esquecerDesfazer() { antesDeLimpar = null; _podeDesfazer.value = false }
}
