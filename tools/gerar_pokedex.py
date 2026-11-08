"""Gera app/src/main/assets/pokemon.json a partir dos CSVs da PokéAPI.

Uso:  python -I tools/gerar_pokedex.py <pasta_dos_csv>
Os CSVs vêm de https://github.com/PokeAPI/pokeapi/tree/master/data/v2/csv
"""
import csv
import json
import sys
from pathlib import Path

EN = "9"
EXCLUIR = ("gmax", "totem", "starter", "-cap", "cosplay", "eternamax", "-busted")
REGIOES = {"alola": "Alola", "galar": "Galar", "hisui": "Hisui", "paldea": "Paldea"}
TRADUCOES = {"Female": "Fêmea", "Male": "Macho"}


def ler(pasta: Path, nome: str):
    with open(pasta / f"{nome}.csv", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def nome_forma(ident: str, especie_ident: str) -> str:
    sufixo = ident[len(especie_ident) + 1:] if ident.startswith(especie_ident + "-") else ident
    partes = [p for p in sufixo.split("-") if p not in ("breed", "standard")]
    regiao = [REGIOES[p] for p in partes if p in REGIOES]
    resto = " ".join(p for p in partes if p not in REGIOES).title()
    resto = TRADUCOES.get(resto, resto)
    return " ".join(regiao + ([resto] if resto else []))


def main(pasta: Path, saida: Path):
    tipos = {t["id"]: t["identifier"] for t in ler(pasta, "types")}
    especies = {s["id"]: s for s in ler(pasta, "pokemon_species")}
    nomes = {n["pokemon_species_id"]: n["name"] for n in ler(pasta, "pokemon_species_names")
             if n["local_language_id"] == EN}
    tem_evolucao = {s["evolves_from_species_id"] for s in especies.values() if s["evolves_from_species_id"]}
    formas = {f["pokemon_id"]: f for f in ler(pasta, "pokemon_forms") if f["is_default"] == "1"}

    tipos_de = {}
    for row in ler(pasta, "pokemon_types"):
        tipos_de.setdefault(row["pokemon_id"], []).append((int(row["slot"]), tipos[row["type_id"]]))
    stats_de = {}
    for row in ler(pasta, "pokemon_stats"):
        stats_de.setdefault(row["pokemon_id"], {})[int(row["stat_id"])] = int(row["base_stat"])

    vistos = set()
    lista = []
    pokemons = sorted(ler(pasta, "pokemon"), key=lambda p: (int(p["species_id"]), int(p["is_default"]) * -1, int(p["id"])))
    for p in pokemons:
        pid, sid = p["id"], p["species_id"]
        esp = especies.get(sid)
        if esp is None or pid not in tipos_de or pid not in stats_de:
            continue
        forma = formas.get(pid)
        padrao = p["is_default"] == "1"
        # Megas entram como forma de batalha (o app mostra na ficha e deixa um membro do time mega evoluir).
        mega = forma is not None and forma["is_mega"] == "1"
        if not padrao and not mega:
            if forma is None or forma["is_battle_only"] == "1":
                continue
            if any(x in p["identifier"] for x in EXCLUIR):
                continue
        t = [nome for _, nome in sorted(tipos_de[pid])]
        s = [stats_de[pid].get(i, 0) for i in range(1, 7)]
        chave = (sid, tuple(t), tuple(s))
        if chave in vistos:  # forma só cosmética
            continue
        vistos.add(chave)
        lista.append({
            "id": int(pid),
            "dex": int(sid),
            "nome": nomes.get(sid, esp["identifier"].title()),
            "forma": None if padrao else nome_forma(p["identifier"], esp["identifier"]),
            "tipos": t,
            "stats": s,
            "geracao": int(esp["generation_id"]),
            "lendario": esp["is_legendary"] == "1",
            "mitico": esp["is_mythical"] == "1",
            "bebe": esp["is_baby"] == "1",
            "final": sid not in tem_evolucao,
            **({"mega": True} if mega else {}),
        })

    saida.parent.mkdir(parents=True, exist_ok=True)
    saida.write_text(json.dumps(lista, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"{len(lista)} pokémons ({sum(1 for x in lista if x['forma'])} formas, {sum(1 for x in lista if x.get('mega'))} megas) -> {saida}")


if __name__ == "__main__":
    raiz = Path(__file__).resolve().parent.parent
    main(Path(sys.argv[1]), raiz / "app/src/main/assets/pokemon.json")
