-- Usuários entram com Google; "sub" é o id estável da conta Google.
CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  google_sub TEXT NOT NULL UNIQUE,
  apelido TEXT NOT NULL,
  foto TEXT,                       -- URL (Google) ou chave no R2 ("r2:...")
  codigo TEXT NOT NULL UNIQUE,     -- código de amizade, ex.: K7QX2M
  criado_em INTEGER NOT NULL
);

-- Sessão do app: guardamos só o hash do token.
CREATE TABLE IF NOT EXISTS sessoes (
  hash TEXT PRIMARY KEY,
  usuario INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  criado_em INTEGER NOT NULL
);

-- Amizade: de -> para. Pendente até "para" aceitar; aceita vale nos dois sentidos.
CREATE TABLE IF NOT EXISTS amizades (
  de INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  para INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  aceita INTEGER NOT NULL DEFAULT 0,
  criado_em INTEGER NOT NULL,
  PRIMARY KEY (de, para)
);

-- Times que o dono escolheu mostrar aos amigos (JSON do app).
CREATE TABLE IF NOT EXISTS times (
  dono INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  id TEXT NOT NULL,
  dados TEXT NOT NULL,
  alterado_em INTEGER NOT NULL,
  PRIMARY KEY (dono, id)
);
