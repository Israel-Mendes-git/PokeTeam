-- Draft online com amigos.
-- Salas de draft online. "dados" guarda o estado inteiro (JSON); "participantes" é ",id,id," para busca.
CREATE TABLE IF NOT EXISTS salas (
  codigo TEXT PRIMARY KEY,
  dono INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  participantes TEXT NOT NULL,
  estado TEXT NOT NULL,            -- espera | andamento | fim
  dados TEXT NOT NULL,
  versao INTEGER NOT NULL DEFAULT 1,
  alterado_em INTEGER NOT NULL
);
