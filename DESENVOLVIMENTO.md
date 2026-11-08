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

## Amigos (servidor)

A pasta `servidor/` é um Cloudflare Worker (D1 para os dados, R2 para as fotos) com login pelo Google.

```sh
cd servidor
npm install
npm run banco:local && npm run dev    # servidor local em 127.0.0.1:8787, com login falso (.dev.vars)
npm run teste                         # 22 verificações de ponta a ponta
```

App apontando para o servidor local, com botões "Entrar como Ana/Bia":
`./gradlew assembleRelease -PservidorTeste=http://10.0.2.2:8787 -PsufixoTeste=1`

Para o app de verdade, copie `config.example.properties` para `config.properties` com o endereço do Worker publicado e o Client ID "Web" do Google. Esse arquivo fica fora do Git; no GitHub Actions os mesmos valores vêm dos Secrets `POKETEAM_SERVIDOR` e `POKETEAM_GOOGLE_CLIENT_ID`.
