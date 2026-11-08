"""Gera o ícone adaptativo (roda dos 18 tipos em pixel art) em res/mipmap-*.

Uso: python -I tools/gerar_icone.py
"""
import math
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent
RES = RAIZ / "app/src/main/res"
FUNDO = "#2b2d42"
# Ordem dos tipos como no jogo, uma fatia de 20° cada.
TIPOS = ["#9fa19f", "#ff8000", "#81b9ef", "#9141cb", "#915121", "#afa981", "#91a119", "#704170", "#60a1b8",
         "#e62829", "#2980ef", "#3fa129", "#fac000", "#ef4179", "#3dcef3", "#5060e1", "#624d4e", "#ef70ef"]
N = 32
C = 15.5


def roda() -> Image.Image:
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    for y in range(N):
        for x in range(N):
            d = math.hypot(x - C, y - C)
            if d > 14:
                continue
            a = (math.degrees(math.atan2(y - C, x - C)) + 360 + 90) % 360
            cor = TIPOS[int(a / 20) % 18]
            if d <= 5:
                cor = "#f4f4f4"
            if d > 12.8 or 5 < d < 6:
                cor = "#0d0e17"
            px[x, y] = Image.new("RGBA", (1, 1), cor).getpixel((0, 0))
    return im


def main():
    r = roda()
    # Foreground adaptativo: 108dp de lado, conteúdo visível garantido nos 66dp do meio.
    for pasta, escala in {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
        lado = round(108 * escala)
        alvo = round(72 * escala)  # a roda (28 de 32 células) fica com ~63dp: dentro da área segura
        img = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
        grande = r.resize((alvo, alvo), Image.NEAREST)
        img.paste(grande, ((lado - alvo) // 2, (lado - alvo) // 2), grande)
        destino = RES / f"mipmap-{pasta}"
        destino.mkdir(parents=True, exist_ok=True)
        img.save(destino / "ic_launcher_foreground.png")
    anydpi = RES / "mipmap-anydpi-v26"
    anydpi.mkdir(parents=True, exist_ok=True)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
           '    <background android:drawable="@color/icone_fundo" />\n'
           '    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n'
           '</adaptive-icon>\n')
    (anydpi / "ic_launcher.xml").write_text(xml, encoding="utf-8")
    (anydpi / "ic_launcher_round.xml").write_text(xml, encoding="utf-8")
    (RES / "values/icone.xml").write_text(
        f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="icone_fundo">{FUNDO}</color>\n</resources>\n',
        encoding="utf-8")
    print("ícone gerado")


if __name__ == "__main__":
    main()
