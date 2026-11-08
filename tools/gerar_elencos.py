"""Gera os elencos de modpack (assets/elencos/*.json) listados em tools/elencos.json.

Uso:  python -I tools/gerar_elencos.py            # todos
      python -I tools/gerar_elencos.py cobbleverse  # só os com essa "saida"

Cada item de tools/elencos.json pode ser:
  {"saida": "x", "nome": "Nome {versao}", "modrinth": "<slug>", "versao": "mais-recente" | "1.7.42"}
      baixa o .mrpack do Modrinth e os mods que mexem em espécies (cache em tools/.cache)
  {"saida": "x", "nome": "Nome", "pasta": "<pasta de uma instância do Minecraft>"}
      lê mods/*.jar, datapacks/*.zip e saves/*/datapacks/*.zip dessa pasta

Regra: uma espécie entra se "implemented": true no Cobblemon ou em qualquer species /
species_additions de mods e datapacks do pack. Formas (Alola, Hisui, Rotom Heat...) entram
se a espécie entra e o pack declara a forma.
Elencos que saíram do elencos.json são apagados de assets/elencos.
"""
import io
import json
import re
import sys
import unicodedata
import urllib.request
import zipfile
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
CONFIG = RAIZ / "tools/elencos.json"
CACHE = RAIZ / "tools/.cache"
DESTINO = RAIZ / "app/src/main/assets/elencos"
AGENTE = "PokeTeam-gerador-de-elencos"
PADRAO_ESPECIE = re.compile(r"^data/([^/]+)/(species|species_additions)/.+\.json$")

# Nome de forma do app (PokéAPI, normalizado) -> nome no Cobblemon. "" = forma base.
SINONIMOS = {
    "femea": "f", "macho": "",
    "dusk": "duskmane", "dawn": "dawnwings",
    "wellspringmask": "wellspring", "hearthflamemask": "hearthflame", "cornerstonemask": "cornerstone",
    "10powerconstruct": "10c",
    "red": "",  # Minior (Red) da PokéAPI é o núcleo, forma base no Cobblemon
}


def chave(s: str) -> str:
    s = unicodedata.normalize("NFD", s.lower())
    s = "".join(c for c in s if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]", "", s)


def baixar_json(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": AGENTE})
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read().decode("utf-8"))


def baixar_arquivo(url: str, destino: Path):
    if destino.exists() and destino.stat().st_size > 0:
        return
    destino.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": AGENTE})
    with urllib.request.urlopen(req) as r:
        destino.write_bytes(r.read())


def tem_especies(z: zipfile.ZipFile) -> bool:
    return any(PADRAO_ESPECIE.match(n) for n in z.namelist())


# ---------- fontes ----------

def fontes_modrinth(item, palavras):
    """Baixa o .mrpack e os mods relevantes; devolve (versao, [ZipFile])."""
    versoes = baixar_json(f"https://api.modrinth.com/v2/project/{item['modrinth']}/version")
    pedida = item.get("versao", "mais-recente")
    v = versoes[0] if pedida == "mais-recente" else next((x for x in versoes if x["version_number"] == pedida), None)
    if v is None:
        sys.exit(f"Versão {pedida} de {item['modrinth']} não encontrada no Modrinth.")
    pasta = CACHE / f"{item['modrinth']}-{v['version_number']}"
    arquivo = next(f for f in v["files"] if f["primary"])
    pacote = pasta / "pack.mrpack"
    print(f"  baixando {arquivo['filename']}...")
    baixar_arquivo(arquivo["url"], pacote)

    mr = zipfile.ZipFile(pacote)
    indice = json.loads(mr.read("modrinth.index.json"))
    zips = []
    for f in indice["files"]:
        nome = f["path"].split("/")[-1]
        if not f["path"].startswith("mods/") or not any(p in nome.lower() for p in palavras):
            continue
        jar = pasta / "mods" / nome
        baixar_arquivo(f["downloads"][0], jar)
        z = zipfile.ZipFile(jar)
        if tem_especies(z):
            print(f"  mod com espécies: {nome}")
            zips.append((nome, z))
    for n in sorted(mr.namelist()):  # datapacks embutidos no pack (inclusive datapacks/extra)
        if n.startswith("overrides/") and "datapacks/" in n and n.endswith(".zip"):
            z = zipfile.ZipFile(io.BytesIO(mr.read(n)))
            if tem_especies(z):
                print(f"  datapack com espécies: {n.split('/')[-1]}")
                zips.append((n, z))
    return v["version_number"], zips


def fontes_pasta(item):
    """Lê uma instância local do Minecraft."""
    raiz = Path(item["pasta"])
    if not raiz.exists():
        sys.exit(f"Pasta não encontrada: {raiz}")
    candidatos = list((raiz / "mods").glob("*.jar")) + list((raiz / "datapacks").glob("*.zip")) \
        + list(raiz.glob("saves/*/datapacks/*.zip"))
    zips = []
    for c in sorted(candidatos):
        try:
            z = zipfile.ZipFile(c)
        except zipfile.BadZipFile:
            continue
        if tem_especies(z):
            print(f"  com espécies: {c.name}")
            zips.append((c.name, z))
    return "local", zips


# ---------- cálculo ----------

def elenco(zips, app):
    # Cobblemon primeiro; addons e datapacks depois sobrescrevem.
    zips = sorted(zips, key=lambda t: (not t[0].lower().startswith("cobblemon-"), t[0]))
    especies, adicoes = {}, []
    for _, z in zips:
        for n in z.namelist():
            m = PADRAO_ESPECIE.match(n)
            if not m:
                continue
            d = json.loads(z.read(n))
            if m.group(2) == "species":
                e = especies.setdefault(f"{m.group(1)}:{Path(n).stem}", {"dex": None, "impl": False, "formas": set()})
                e["dex"] = d.get("nationalPokedexNumber") or e["dex"]
                if "implemented" in d:
                    e["impl"] = bool(d["implemented"])
                e["formas"] |= {chave(f["name"]) for f in d.get("forms", []) if f.get("name")}
            else:
                adicoes.append(d)
    for d in adicoes:
        alvo = d.get("target") or ""
        alvo = alvo if ":" in alvo else "cobblemon:" + alvo  # alguns datapacks omitem o namespace
        if alvo in especies:
            if d.get("implemented") is True:
                especies[alvo]["impl"] = True
            especies[alvo]["formas"] |= {chave(f["name"]) for f in d.get("forms", []) if f.get("name")}

    por_dex = {}
    for e in especies.values():
        if e["dex"]:
            x = por_dex.setdefault(e["dex"], {"impl": False, "formas": set()})
            x["impl"] |= e["impl"]
            x["formas"] |= e["formas"]

    permitidos, fora = [], []
    for p in app:
        e = por_dex.get(p["dex"])
        if not e or not e["impl"]:
            fora.append(p)
            continue
        k = chave(p["forma"]) if p["forma"] else None
        if k is None or k in e["formas"] or SINONIMOS.get(k, k) in e["formas"] | {""}:
            permitidos.append(p["id"])
        else:
            fora.append(p)
    return permitidos, fora


def main(filtro):
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    palavras = config.get("palavras_mods", ["cobblemon"])
    app = json.loads((RAIZ / "app/src/main/assets/pokemon.json").read_text(encoding="utf-8"))
    DESTINO.mkdir(parents=True, exist_ok=True)

    saidas = {item["saida"] for item in config["elencos"]}
    for velho in DESTINO.glob("*.json"):
        if velho.stem not in saidas:
            velho.unlink()
            print(f"removido: {velho.name} (não está mais em elencos.json)")

    for item in config["elencos"]:
        if filtro and item["saida"] not in filtro:
            continue
        print(f"[{item['saida']}]")
        if "modrinth" in item:
            versao, zips = fontes_modrinth(item, palavras)
        elif "pasta" in item:
            versao, zips = fontes_pasta(item)
        else:
            sys.exit(f"Item {item['saida']}: falta \"modrinth\" ou \"pasta\".")
        if not zips:
            sys.exit(f"Item {item['saida']}: nenhum mod com dados de espécie encontrado.")
        permitidos, fora = elenco(zips, app)
        nome = item.get("nome", item["saida"]).replace("{versao}", versao)
        (DESTINO / f"{item['saida']}.json").write_text(
            json.dumps({"nome": nome, "permitidos": permitidos}, ensure_ascii=False), encoding="utf-8")
        especies_fora = sorted({p["nome"] for p in fora if p["forma"] is None})
        formas_fora = [f"{p['nome']} ({p['forma']})" for p in fora if p["forma"]]
        print(f"  {nome}: {len(permitidos)} pokémons; fora {len(especies_fora)} espécies, {len(formas_fora)} formas")
        if especies_fora:
            print("  espécies fora:", ", ".join(especies_fora))


if __name__ == "__main__":
    main(set(sys.argv[1:]))
