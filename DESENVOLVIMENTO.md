# Desenvolvimento

## Compilar

Android Studio, ou JDK 17+ e Android SDK 36:

```sh
./gradlew testDebugUnitTest assembleRelease
```

Sem `keystore.properties` na raiz, o APK sai assinado com a chave de debug.

## Publicar uma versão

```sh
git tag -a v0.3.0 -m "O que mudou"
git push origin v0.3.0
```

O GitHub Actions testa, assina e cria a Release; a mensagem da tag vira as notas. Toda versão precisa da mesma chave, senão o celular recusa a atualização. A chave fica fora do Git e vai para os Secrets com `sh tools/configurar_segredos.sh`.

Em fork: gere sua chave, crie o `keystore.properties` e troque `poketeam.repo` em `gradle.properties`.

## Dados

| Pasta em `app/src/main/assets/` | Como gerar |
|---|---|
| `pokemon.json` | `python tools/gerar_pokedex.py <csvs da PokéAPI>` |
| `sprites/` | `sh tools/baixar_sprites.sh` |
| `elencos/` | `python tools/gerar_elencos.py` |

Os elencos de modpack ficam em `tools/elencos.json`: cada item aponta para um modpack do Modrinth (`"modrinth": "cobbleverse"`) ou para a pasta de uma instância local (`"pasta": "..."`) e vira uma opção em **Novo pool**.
