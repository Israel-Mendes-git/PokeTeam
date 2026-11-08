#!/bin/sh
# Baixa os sprites de todos os pokémons do pokemon.json para app/src/main/assets/sprites.
# Uso (na raiz do projeto): sh tools/baixar_sprites.sh
set -e
destino=app/src/main/assets/sprites
mkdir -p "$destino"
python -I -c "import json;[print(x['id']) for x in json.load(open('app/src/main/assets/pokemon.json',encoding='utf-8'))]" |
  xargs -P 16 -I{} sh -c "[ -s $destino/{}.png ] || curl -sfL -o $destino/{}.png https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{}.png || echo 'falhou {}'"
