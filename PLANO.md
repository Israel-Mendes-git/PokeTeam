# PokeTeam — criador de times Pokémon (Android)

Nome provisório. Uso pessoal com os amigos.

## Problema
Na hora de jogar (geralmente Cobblemon, mas sem prender a um jogo) vai tempo demais em três coisas:
1. **Decidir quem entra**: combinações, cobertura de tipos, fraquezas do time.
2. **Combinar entre amigos**: sorteio, draft, regras da sessão, não repetir pokémon.
3. **Lembrar times antigos**: guardar e reaproveitar.

A pesquisa em sites separados some porque os dados ficam dentro do app, offline.

## Princípio: o "pool"
O app não sabe "qual jogo" é. Ele trabalha com um **pool**: o conjunto de pokémons que vale naquela sessão.
- Presets: Nacional inteiro, por geração, "Cobblemon" (lista editável), lista personalizada.
- Filtros por cima: sem lendários, sem míticos, só estágio final, banlist manual.
- Busca, sorteio, draft e sugestões usam só o pool ativo.

## Telas (v1)
| Tela | O que faz |
|---|---|
| **Pokédex** | Busca ao digitar, filtro por tipo/geração/pool, ficha com tipos, stats base e fraquezas |
| **Montar time** | 6 slots; painel ao vivo com fraquezas compartilhadas, tipos sem cobertura e stats médios |
| **Sortear** | Gera time aleatório dentro do pool com regras (sem tipo repetido, um de cada papel, etc.) |
| **Draft** | Passa o celular: 2 a 4 jogadores, ordem em serpente, cada escolha sai do pool de todos |
| **Meus times** | Biblioteca com nome, tags, data de uso, favorito; duplicar e editar |

## Depois (v2+)
- Sugestão "o que fecha esse buraco" (pokémons do pool que cobrem as fraquezas do time).
- Compartilhar time por QR ou texto no formato Showdown (importar/exportar).
- Movimentos, habilidades e itens por pokémon.
- Draft em rede (cada um no seu celular), só se o passa-celular não bastar.

## Técnica
- **Kotlin + Jetpack Compose**, Material 3, um módulo `app` só (separar depois se crescer).
- **Times salvos num JSON** em filesDir (`data/Times.kt`), sem Room: poucos dados, um plugin a menos (KSP). Trocar por Room só se pools/sessões pedirem consultas.
- Dados de pokémon: **dados de pokémon empacotados** no APK: `tools/gerar_pokedex.py` gera `assets/pokemon.json` a partir dos CSVs da PokéAPI (1025 espécies + 105 formas, sem mega/gmax) e `tools/baixar_sprites.sh` baixa os sprites para `assets/sprites`. Sem servidor, sem conta, sem permissão de internet.
- **Coil** para exibir os sprites.
- Versões: AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21 (o Android Studio instalado abre sem atualizar).
- Release com R8 (APK de 19 MB caiu para 3,1 MB). APK de release assinado com a chave de debug (instalável direto nos celulares dos amigos), sem lint no release. Abre em ~1,5 s no emulador; o debug leva 6-9 s.
- Build: `JAVA_HOME` = JBR do Android Studio.
- Camadas por pacote: `data` (Room, seed JSON) → `domain` (tabela de tipos, cálculo de cobertura, motor de sorteio/draft, tudo testável sem Android) → `ui` (telas Compose + ViewModel).
- minSdk 26, targetSdk 36, JDK do Android Studio.

## Cuidado
Nomes e sprites são da Nintendo/Game Freak. Para uso entre amigos (APK instalado direto) tudo bem; publicar na Play Store com isso é arriscado.

## Ordem de construção
1. ~~Esqueleto do projeto + script que gera o JSON de pokémons + Pokédex com busca.~~ Pronto em 07/11/2026.
2. ~~Montar time com painel de tipos + salvar em Meus times.~~ Pronto em 07/11/2026.
3. ~~Pools e filtros.~~ Pronto em 07/11/2026 (aba Pools, pool ativo filtra Pokédex e escolha do time, banidos; `data/Pools.kt`, gravação comum em `data/ArquivoJson.kt`).
4. ~~Sortear.~~ Pronto em 07/11/2026 (`domain/Sorteio.kt` + `data/Sorteador.kt`; "Sortear time" na lista e "Sortear vagas vazias" no editor, regras num painel).
5. ~~Draft passa-celular.~~ Pronto em 07/11/2026 (aba Draft: 2-4 jogadores, serpente/fixa, 3 opções sorteadas ou busca livre, desfazer, salvo em `draft.json`, resultado vira times). **v1 completa.**

## Rodada pós-v1 (08/11/2026)
- Sugestões no editor: quem do pool fecha os pontos fracos (`domain/Sugestoes.kt`).
- Regra de fraqueza grave revista: 2+ fracos e fracos − (resistem+imunes) ≥ 2.
- Confirmação ao excluir time/pool e ao encerrar/descartar draft.
- Regras do sorteio e configuração do draft salvas (`preferencias.json`).
- Compartilhar time em texto (nomes no formato do Showdown).
- Elencos de modpack configuráveis em `tools/elencos.json` (Modrinth ou pasta local), gerados por `tools/gerar_elencos.py` em `assets/elencos/`; "Novo pool" oferece cada um. Hoje: COBBLEVERSE 1.7.42 (1025 espécies, tudo).

## Interface pixel (08/11/2026)
- Fonte Pixelify Sans (OFL), cantos retos, `CaixaPixel`/`BotaoPixel`/`bordaPixel` em `ui/tema/Tema.kt`.
- 12 paletas escolhíveis (`ui/tema/Paletas.kt`, tela Tema), salvas em `preferencias.json`; `PaletasTest` exige contraste ≥ 4,5 (texto) e ≥ 3 (detalhes).
- Navegação: 3 abas (Times, Draft, Pokédex); pool e tema na barra de topo; Pools abre por "Gerenciar pools…".
- Editor: vagas e pontos fracos em caixas; defesa, STAB e stats recolhidos em "Ver detalhes".

## Atualização automática (08/11/2026)
- `data/Atualizador.kt`: Releases de `poketeam.repo` (1x/dia ao abrir + botão em Ajustes), baixa o APK e abre o instalador via FileProvider.
- Versão vem da tag (`VERSAO`); versionCode = maior*10000+menor*100+correção.
- Chave de release própria (CN=PokeTeam) fora do repositório; GitHub Actions `publicar.yml` assina com os Secrets.
- Teste local: `-PapiGithub=http://10.0.2.2:8765 -PsufixoTeste=1` com servidor falso; ciclo 0.2.0 → 0.2.1 validado no emulador.
