-- Banco criado antes dos favoritos: acrescenta a coluna.
ALTER TABLE usuarios ADD COLUMN favoritos TEXT NOT NULL DEFAULT '[]';
