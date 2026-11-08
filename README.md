# PokeTeam

App Android para montar times Pokémon rápido, sozinho ou com amigos. Funciona offline e não amarra a um jogo: você escolhe o **pool** (quais pokémons valem na sessão) e o app cuida do resto.

## O que faz

- **Pokédex**: busca ao digitar, filtros por tipo, geração, evolução final e lendários; ficha com stats e fraquezas.
- **Montar time**: 6 vagas e análise ao vivo (fraquezas graves, defesa contra cada tipo, tipos sem STAB super efetivo, média de stats), mais sugestões de quem fecha os pontos fracos.
- **Sortear**: time inteiro ou só as vagas vazias, com regras (só evolução final, sem tipo repetido, evitar fraquezas).
- **Draft passa-celular**: 2 a 4 jogadores, ordem em serpente ou fixa, 3 opções sorteadas por vez ou busca livre; o resultado vira times.
- **Pools**: gerações, regras e banidos; pools prontos a partir do elenco de um modpack de Cobblemon.
- **Compartilhar** um time em texto (nomes no formato do Showdown).
- **12 temas** em pixel art (ícone de paleta no topo); as cores ficam em `ui/tema/Paletas.kt` e um teste confere o contraste do texto de cada uma.

## Compilar

Requer Android Studio (ou JDK 17+ e o Android SDK 36).

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # testes de domínio
```

O APK de release sai assinado com a chave de debug, para instalar direto em celulares. Para publicar numa loja, troque por uma chave própria em `app/build.gradle.kts`.

## Dados

Tudo vai dentro do APK; o app não usa internet.

| Arquivo | Gerado por | Fonte |
|---|---|---|
| `app/src/main/assets/pokemon.json` | `tools/gerar_pokedex.py <pasta_dos_csv>` | CSVs da [PokéAPI](https://github.com/PokeAPI/pokeapi/tree/master/data/v2/csv) |
| `app/src/main/assets/sprites/` | `tools/baixar_sprites.sh` | [PokeAPI/sprites](https://github.com/PokeAPI/sprites) |
| `app/src/main/assets/elencos/` | `tools/gerar_elencos.py` | modpacks listados em `tools/elencos.json` |

## Elencos de modpack (Cobblemon)

O botão **Novo pool** oferece um pool pronto para cada elenco em `assets/elencos/`. Os elencos são configurados em `tools/elencos.json`:

```json
{
  "elencos": [
    { "saida": "cobbleverse", "nome": "COBBLEVERSE {versao}", "modrinth": "cobbleverse", "versao": "mais-recente" },
    { "saida": "meu-servidor", "nome": "Meu servidor", "pasta": "C:/caminho/da/instancia" }
  ]
}
```

- `modrinth`: slug do modpack no Modrinth; `versao` é `mais-recente` ou um número como `1.7.42`. O script baixa o pack e só os mods que mexem em espécies (cache em `tools/.cache/`).
- `pasta`: uma instância local do Minecraft; lê `mods/`, `datapacks/` e `saves/*/datapacks/`.
- `{versao}` no nome é trocado pela versão encontrada.

Depois de editar:

```sh
python tools/gerar_elencos.py   # regenera todos e apaga os que saíram da lista
./gradlew assembleRelease
```

A regra: uma espécie entra se estiver marcada como implementada no Cobblemon ou em qualquer mod/datapack do pack. O elenco diz o que **existe** no modpack, não o que **nasce** no mundo.

## Atualização automática

O app procura a última Release de `poketeam.repo` (em `gradle.properties`) uma vez por dia ao abrir, e há um botão em **Ajustes**. Se houver versão nova, baixa o APK anexado e abre o instalador do Android (na primeira vez o Android pede para liberar "instalar apps desconhecidos").

Publicar uma versão:

```sh
git tag v0.3.0 -m "O que mudou nesta versão"
git push origin v0.3.0
```

O workflow `.github/workflows/publicar.yml` roda os testes, compila o APK assinado e cria a Release com a mensagem da tag como notas.

**Assinatura:** toda versão precisa da mesma chave, senão o Android recusa a atualização. A chave fica fora do Git (`keystore.properties` aponta para ela). Configure uma vez, depois de criar o repositório:

```sh
sh tools/configurar_segredos.sh   # envia a chave para os Secrets do GitHub
```

Em fork, gere a sua chave (`keytool -genkeypair ...`), crie o `keystore.properties` e troque `poketeam.repo`.

## Aviso

Projeto de fã, sem fins lucrativos e sem relação com Nintendo, Game Freak, The Pokémon Company ou a equipe do Cobblemon. Pokémon e os nomes e imagens dos personagens são marcas e propriedade dos seus donos. Dados e sprites vêm da PokéAPI. Fonte: [Pixelify Sans](https://github.com/eifetx/Pixelify-Sans), licença SIL OFL 1.1 (cópia em `app/src/main/assets/licencas/`).
