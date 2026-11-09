// Aplica uma migração no banco de produção: node migrar.mjs migracoes_00X_nome.sql
// Usa --command (o --file do wrangler falha com "Authentication error 10000" nesta conta).
import { readFileSync } from "node:fs";
import { execFileSync } from "node:child_process";

const arquivo = process.argv[2];
if (!arquivo) { console.error("Uso: node migrar.mjs <arquivo.sql>"); process.exit(1); }
const sql = readFileSync(arquivo, "utf8").replace(/--.*$/gm, "").replace(/\s+/g, " ").trim();
execFileSync(process.execPath, ["node_modules/wrangler/bin/wrangler.js", "d1", "execute", "poketeam", "--remote", `--command=${sql}`], { stdio: "inherit" });
