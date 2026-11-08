"""Gera app/src/main/assets/builds.json com builds recomendadas do Smogon para cada pokémon do app.

Uso:  python -I tools/gerar_builds.py

Fonte: sets do Smogon compilados pelo projeto pkmn (https://github.com/pkmn/smogon), uma
lista por geração. Usa a geração 9 primeiro; pokémons que não estão nela pegam as builds da
geração mais recente em que aparecem. Cache em tools/.cache/smogon.
"""
import json
import re
import sys
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
CACHE = RAIZ / "tools/.cache/smogon"
SAIDA = RAIZ / "app/src/main/assets/builds.json"
URL = "https://pkmn.github.io/smogon/data/sets/gen{}.json"
AGENTE = "PokeTeam-gerador-de-builds"
MAX_BUILDS = 4

# Formatos em ordem de preferência; o índice também mede o quão forte o pokémon é (menor = mais forte).
FORMATOS = [
    ("ubers", "Ubers"), ("ou", "OU"), ("uu", "UU"), ("ru", "RU"), ("nu", "NU"), ("pu", "PU"), ("zu", "ZU"),
    ("lc", "LC"), ("nationaldex", "National Dex"), ("nationaldexuu", "National Dex UU"),
    ("doublesou", "Duplas OU"), ("monotype", "Monotype"), ("1v1", "1v1"),
]
# Formatos de VGC/Battle Stadium mudam de nome a cada ano; entram depois dos de cima.
PADRAO_VGC = re.compile(r"^(vgc\d{4}.*|battlestadium.*)$")
IGNORAR = re.compile(r"(godlygift|stabmons|almostanyability|balancedhackmons|mixandmega|camomons|"
                     r"partnersincrime|purehackmons|metronome|350cup|tiershift|inheritance|anythinggoes|cap)")

# Rótulo de forma do app -> sufixo do Showdown, quando a regra geral não acerta.
ALIAS_FORMA = {"femea": "f", "macho": "", "paldea combat": "paldea-combat", "paldea blaze": "paldea-blaze",
               "paldea aqua": "paldea-aqua", "rapid strike": "rapid-strike", "dusk": "dusk-mane", "dawn": "dawn-wings",
               "wellspring mask": "wellspring", "hearthflame mask": "hearthflame", "cornerstone mask": "cornerstone"}


def to_id(s: str) -> str:
    return re.sub(r"[^a-z0-9]", "", s.lower())


def sem_acento(s: str) -> str:
    import unicodedata
    return "".join(c for c in unicodedata.normalize("NFD", s.lower()) if not unicodedata.combining(c))


def baixar(gen: int) -> dict:
    arq = CACHE / f"gen{gen}.json"
    if not arq.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(URL.format(gen), headers={"User-Agent": AGENTE})
        with urllib.request.urlopen(req) as r:
            arq.write_bytes(r.read())
    return json.loads(arq.read_text(encoding="utf-8"))


def chave_app(p: dict) -> str:
    if not p["forma"]:
        return to_id(p["nome"])
    f = sem_acento(p["forma"])
    return to_id(p["nome"] + ALIAS_FORMA.get(f, f))


def ordem_formato(fmt: str):
    for i, (k, _) in enumerate(FORMATOS):
        if fmt == k:
            return i
    if PADRAO_VGC.match(fmt):
        return len(FORMATOS)
    return None


def rotulo_formato(fmt: str, gen: int) -> str:
    nome = dict(FORMATOS).get(fmt)
    if nome is None:
        m = re.match(r"vgc(\d{4})(.*)", fmt)
        nome = f"VGC {m.group(1)}" if m else ("Battle Stadium" if fmt.startswith("battlestadium") else fmt)
    return nome if gen == 9 else f"Gen {gen} {nome}"


def limpar(build: dict) -> dict:
    """Só o que o app usa. Campos com várias opções viram listas."""
    def lista(v):
        return v if isinstance(v, list) else ([v] if v else [])
    golpes = [g if isinstance(g, list) else [g] for g in build.get("moves", [])]
    out = {
        "golpes": golpes,
        "item": lista(build.get("item")),
        "habilidade": lista(build.get("ability")),
        "nature": lista(build.get("nature")),
        "evs": build.get("evs") if isinstance(build.get("evs"), dict) else (build.get("evs") or [{}])[0],
        "tera": lista(build.get("teratypes")),
    }
    return {k: v for k, v in out.items() if v}


def main():
    app = json.loads((RAIZ / "app/src/main/assets/pokemon.json").read_text(encoding="utf-8"))
    por_chave = {chave_app(p): p for p in app}
    resultado = {}
    forca = {}
    nao_achados = set()
    for gen in range(9, 0, -1):
        dados = baixar(gen)
        for nome, formatos in dados.items():
            p = por_chave.get(to_id(nome))
            if p is None:
                nao_achados.add(nome)
                continue
            if str(p["id"]) in resultado:
                continue  # já tem builds de uma geração mais nova
            builds = []
            for fmt, sets in formatos.items():
                if IGNORAR.search(fmt):
                    continue
                o = ordem_formato(fmt)
                if o is None:
                    continue
                for nome_set, b in sets.items():
                    builds.append((o, {"formato": rotulo_formato(fmt, gen), "nome": nome_set, **limpar(b)}))
            if not builds:
                continue
            builds.sort(key=lambda t: t[0])
            resultado[str(p["id"])] = [b for _, b in builds[:MAX_BUILDS]]
            forca[str(p["id"])] = builds[0][0] + (9 - gen) * 3  # gerações antigas pesam menos
    nao_achados -= {n for n in nao_achados if any(x in n for x in ("-Mega", "-Gmax", "-Totem"))}
    SAIDA.write_text(json.dumps({"builds": resultado, "forca": forca}, ensure_ascii=False, separators=(",", ":")),
                     encoding="utf-8")
    com = len(resultado)
    print(f"{com} de {len(app)} pokémons com builds -> {SAIDA} ({SAIDA.stat().st_size // 1024} KB)")
    print(f"{len(nao_achados)} nomes do Smogon sem par no app (formas de batalha etc.):", ", ".join(sorted(nao_achados)[:40]))


if __name__ == "__main__":
    sys.exit(main())
