"""Gera app/src/main/assets/animados.json: ids dos pokémons que têm sprite animado (estilo Black/White).

Uso:  python -I tools/baixar_animados.py

Os GIFs somam ~47 MB, então não vão no APK: o app baixa cada um da PokeAPI/sprites quando ele
aparece na tela e guarda em cache. Este script baixa todos uma vez (cache em tools/.cache/animados)
só para saber quais existem e quanto pesam. Quem não tem GIF é animado por código no app.
"""
import json
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
DESTINO = RAIZ / "tools/.cache/animados"
LISTA = RAIZ / "app/src/main/assets/animados.json"
AGENTE = "PokeTeam-sprites"
POKEAPI = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/versions/generation-v/black-white/animated/{}.gif"



def baixar(url: str) -> bytes | None:
    for tentativa in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": AGENTE})
            with urllib.request.urlopen(req, timeout=20) as r:
                dados = r.read()
            return dados if dados[:3] == b"GIF" else None
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            time.sleep(1 + tentativa)
        except Exception:
            time.sleep(1 + tentativa)
    return None


def um(p: dict) -> tuple[int, str]:
    arq = DESTINO / f"{p['id']}.gif"
    if arq.exists() and arq.stat().st_size > 0:
        return p["id"], "já tinha"
    dados = baixar(POKEAPI.format(p["id"]))
    if dados:
        arq.write_bytes(dados)
        return p["id"], "pokeapi"
    return p["id"], "sem"


def main():
    app = json.loads((RAIZ / "app/src/main/assets/pokemon.json").read_text(encoding="utf-8"))
    DESTINO.mkdir(parents=True, exist_ok=True)
    with ThreadPoolExecutor(8) as ex:
        resultados = list(ex.map(um, app))
    contagem = {}
    for _, f in resultados:
        contagem[f] = contagem.get(f, 0) + 1
    total = sum(a.stat().st_size for a in DESTINO.glob("*.gif"))
    ids = sorted(i for i, f in resultados if f != "sem")
    LISTA.write_text(json.dumps(ids, separators=(",", ":")), encoding="utf-8")
    print(contagem, f"{len(ids)} com GIF ({total / 1024 / 1024:.1f} MB no total) -> {LISTA}")


if __name__ == "__main__":
    sys.exit(main())
