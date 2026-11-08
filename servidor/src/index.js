// Servidor de amigos do PokeTeam: login com Google, perfil (apelido e foto), amizades por código
// e times compartilhados. Cloudflare Workers + D1 (banco e fotos).

const MAX_FOTO = 200 * 1024;          // a foto chega reduzida do app (~20 KB); isto é só um teto
const MAX_TIME = 32 * 1024;
const MAX_TIMES_POR_USUARIO = 50;
const LETRAS_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem 0/O e 1/I para não confundir

export default {
  async fetch(req, env) {
    try {
      return await rotear(req, env);
    } catch (e) {
      if (e instanceof ErroHttp) return json({ erro: e.message }, e.status);
      console.error(e);
      return json({ erro: "Erro interno" }, 500);
    }
  },
};

class ErroHttp extends Error {
  constructor(status, mensagem) { super(mensagem); this.status = status; }
}

function json(dados, status = 200) {
  return new Response(JSON.stringify(dados), { status, headers: { "content-type": "application/json; charset=utf-8" } });
}

async function corpo(req, max = MAX_TIME) {
  const texto = await req.text();
  if (texto.length > max) throw new ErroHttp(413, "Grande demais");
  try { return JSON.parse(texto || "{}"); } catch { throw new ErroHttp(400, "JSON inválido"); }
}

async function rotear(req, env) {
  const url = new URL(req.url);
  const p = url.pathname.replace(/\/+$/, "") || "/";
  const m = req.method;

  if (m === "GET" && p === "/") return json({ ok: true, app: "PokeTeam" });
  if (m === "POST" && p === "/entrar") return entrar(req, env);
  if (m === "GET" && p.startsWith("/fotos/")) return servirFoto(env, decodeURIComponent(p.slice(7)));

  const eu = await autenticar(req, env);
  if (m === "GET" && p === "/eu") return json(await perfil(env, eu, true, url.origin));
  if (m === "PATCH" && p === "/eu") return mudarApelido(req, env, eu);
  if (m === "PUT" && p === "/eu/foto") return mudarFoto(req, env, eu, url);
  if (m === "PUT" && p === "/eu/favoritos") return mudarFavoritos(req, env, eu);
  if (m === "DELETE" && p === "/eu") return apagarConta(env, eu);
  if (m === "POST" && p === "/sair") return sair(req, env);

  if (m === "GET" && p === "/amigos") return listarAmigos(env, eu, url);
  if (m === "POST" && p === "/amigos") return pedirAmizade(req, env, eu);
  let r = p.match(/^\/amigos\/(\d+)\/aceitar$/);
  if (m === "POST" && r) return aceitar(env, eu, +r[1]);
  r = p.match(/^\/amigos\/(\d+)$/);
  if (m === "DELETE" && r) return desfazer(env, eu, +r[1]);
  r = p.match(/^\/amigos\/(\d+)\/times$/);
  if (m === "GET" && r) return timesDoAmigo(env, eu, +r[1], url);

  if (m === "GET" && p === "/times") return meusTimes(env, eu);
  r = p.match(/^\/times\/([\w-]{1,64})$/);
  if (m === "PUT" && r) return salvarTime(req, env, eu, r[1]);
  if (m === "DELETE" && r) return apagarTime(env, eu, r[1]);

  throw new ErroHttp(404, "Não encontrado");
}

// ---------- login ----------

/** Troca o token do Google (ou, só em teste local, um token falso) por uma sessão do app. */
async function entrar(req, env) {
  const { idToken } = await corpo(req);
  if (typeof idToken !== "string" || !idToken) throw new ErroHttp(400, "Falta o idToken");
  const conta = env.MODO_TESTE === "1" && idToken.startsWith("teste:")
    ? contaDeTeste(idToken)
    : await verificarTokenGoogle(idToken, env.GOOGLE_CLIENT_ID);

  let u = await env.DB.prepare("SELECT * FROM usuarios WHERE google_sub = ?").bind(conta.sub).first();
  if (!u) {
    const apelido = limparApelido(conta.nome) || "Treinador";
    for (let tentativa = 0; tentativa < 5 && !u; tentativa++) {
      try {
        u = await env.DB.prepare(
          "INSERT INTO usuarios (google_sub, apelido, foto, codigo, criado_em) VALUES (?, ?, ?, ?, ?) RETURNING *",
        ).bind(conta.sub, apelido, conta.foto || null, novoCodigo(), Date.now()).first();
      } catch (e) {
        if (!String(e).includes("UNIQUE")) throw e; // código repetido: tenta outro
      }
    }
  }
  const token = aleatorio(32);
  await env.DB.prepare("INSERT INTO sessoes (hash, usuario, criado_em) VALUES (?, ?, ?)")
    .bind(await sha256(token), u.id, Date.now()).run();
  return json({ sessao: token, eu: await perfil(env, u.id, true, new URL(req.url).origin) });
}

function contaDeTeste(token) {
  const [, sub, nome] = token.split(":");
  return { sub: "teste-" + sub, nome: nome || sub, foto: null };
}

/** Confere assinatura (RS256 com as chaves públicas do Google), emissor, público e validade. */
async function verificarTokenGoogle(token, clientId) {
  const partes = token.split(".");
  if (partes.length !== 3) throw new ErroHttp(401, "Token inválido");
  if (!clientId) throw new ErroHttp(500, "GOOGLE_CLIENT_ID não configurado");
  const cab = JSON.parse(b64urlTexto(partes[0]));
  const dados = JSON.parse(b64urlTexto(partes[1]));
  const chaves = await chavesGoogle();
  const jwk = chaves.find((k) => k.kid === cab.kid);
  if (!jwk || cab.alg !== "RS256") throw new ErroHttp(401, "Token inválido");
  const chave = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", chave, b64urlBytes(partes[2]),
    new TextEncoder().encode(partes[0] + "." + partes[1]));
  const agora = Date.now() / 1000;
  if (!ok || !["accounts.google.com", "https://accounts.google.com"].includes(dados.iss)
    || dados.aud !== clientId || dados.exp < agora - 60) throw new ErroHttp(401, "Token inválido");
  return { sub: dados.sub, nome: dados.given_name || dados.name || "", foto: dados.picture || null };
}

let cacheChaves = { ate: 0, chaves: [] };
async function chavesGoogle() {
  if (Date.now() < cacheChaves.ate) return cacheChaves.chaves;
  const r = await fetch("https://www.googleapis.com/oauth2/v3/certs");
  const { keys } = await r.json();
  const idade = Number((r.headers.get("cache-control") || "").match(/max-age=(\d+)/)?.[1] || 3600);
  cacheChaves = { ate: Date.now() + idade * 1000, chaves: keys };
  return keys;
}

async function autenticar(req, env) {
  const token = (req.headers.get("authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) throw new ErroHttp(401, "Faça login");
  const s = await env.DB.prepare("SELECT usuario FROM sessoes WHERE hash = ?").bind(await sha256(token)).first();
  if (!s) throw new ErroHttp(401, "Sessão expirada");
  return s.usuario;
}

async function sair(req, env) {
  const token = (req.headers.get("authorization") || "").replace(/^Bearer\s+/i, "");
  await env.DB.prepare("DELETE FROM sessoes WHERE hash = ?").bind(await sha256(token)).run();
  return json({ ok: true });
}

async function apagarConta(env, eu) {
  const u = await env.DB.prepare("SELECT foto FROM usuarios WHERE id = ?").bind(eu).first();
  await env.DB.batch([
    env.DB.prepare("DELETE FROM fotos WHERE usuario = ?").bind(eu),
    env.DB.prepare("DELETE FROM sessoes WHERE usuario = ?").bind(eu),
    env.DB.prepare("DELETE FROM amizades WHERE de = ? OR para = ?").bind(eu, eu),
    env.DB.prepare("DELETE FROM times WHERE dono = ?").bind(eu),
    env.DB.prepare("DELETE FROM usuarios WHERE id = ?").bind(eu),
  ]);
  return json({ ok: true });
}

// ---------- perfil ----------

function urlFoto(foto, base) {
  if (!foto) return null;
  return foto.startsWith("r2:") ? `${base}/fotos/${encodeURIComponent(foto.slice(3))}` : foto;
}

async function perfil(env, id, comCodigo = false, base = "") {
  const u = await env.DB.prepare("SELECT id, apelido, foto, codigo FROM usuarios WHERE id = ?").bind(id).first();
  if (!u) throw new ErroHttp(404, "Usuário não encontrado");
  const out = { id: u.id, apelido: u.apelido, foto: urlFoto(u.foto, base) };
  if (comCodigo) out.codigo = u.codigo;
  return out;
}

function limparApelido(s) {
  return String(s || "").replace(/[\u0000-\u001f]/g, "").trim().slice(0, 20);
}

async function mudarApelido(req, env, eu) {
  const { apelido } = await corpo(req);
  const a = limparApelido(apelido);
  if (a.length < 2) throw new ErroHttp(400, "Apelido precisa de pelo menos 2 letras");
  await env.DB.prepare("UPDATE usuarios SET apelido = ? WHERE id = ?").bind(a, eu).run();
  return json(await perfil(env, eu, true, new URL(req.url).origin));
}

async function mudarFoto(req, env, eu, url) {
  const tipo = req.headers.get("content-type") || "";
  if (!/^image\/(jpeg|png|webp)$/.test(tipo)) throw new ErroHttp(415, "Envie JPEG, PNG ou WebP");
  const bytes = await req.arrayBuffer();
  if (bytes.byteLength > MAX_FOTO) throw new ErroHttp(413, "Foto grande demais");
  const antiga = await env.DB.prepare("SELECT foto FROM usuarios WHERE id = ?").bind(eu).first();
  const chave = `${eu}-${aleatorio(8)}`;
  await env.DB.batch([
    env.DB.prepare("INSERT INTO fotos (chave, usuario, tipo, dados) VALUES (?, ?, ?, ?)").bind(chave, eu, tipo, bytes),
    env.DB.prepare("UPDATE usuarios SET foto = ? WHERE id = ?").bind("r2:" + chave, eu),
    ...(antiga?.foto?.startsWith("r2:") ? [env.DB.prepare("DELETE FROM fotos WHERE chave = ?").bind(antiga.foto.slice(3))] : []),
  ]);
  return json(await perfil(env, eu, true, url.origin));
}

const MAX_FAVORITOS = 100;

async function mudarFavoritos(req, env, eu) {
  const { favoritos } = await corpo(req);
  if (!Array.isArray(favoritos) || favoritos.length > MAX_FAVORITOS || !favoritos.every((f) => Number.isInteger(f) && f > 0)) {
    throw new ErroHttp(400, `Lista de favoritos inválida (máx. ${MAX_FAVORITOS})`);
  }
  await env.DB.prepare("UPDATE usuarios SET favoritos = ? WHERE id = ?").bind(JSON.stringify([...new Set(favoritos)]), eu).run();
  return json({ ok: true });
}

async function servirFoto(env, chave) {
  const f = await env.DB.prepare("SELECT tipo, dados FROM fotos WHERE chave = ?").bind(chave).first();
  if (!f) throw new ErroHttp(404, "Foto não encontrada");
  return new Response(new Uint8Array(f.dados), {
    headers: { "content-type": f.tipo, "cache-control": "public, max-age=31536000, immutable" },
  });
}

// ---------- amigos ----------

async function listarAmigos(env, eu, url) {
  const { results } = await env.DB.prepare(
    `SELECT u.id, u.apelido, u.foto, a.aceita, a.de = ? AS enviado
       FROM amizades a JOIN usuarios u ON u.id = CASE WHEN a.de = ? THEN a.para ELSE a.de END
      WHERE a.de = ? OR a.para = ?
      ORDER BY u.apelido COLLATE NOCASE`,
  ).bind(eu, eu, eu, eu).all();
  const pessoa = (r) => ({ id: r.id, apelido: r.apelido, foto: urlFoto(r.foto, url.origin) });
  return json({
    amigos: results.filter((r) => r.aceita).map(pessoa),
    recebidos: results.filter((r) => !r.aceita && !r.enviado).map(pessoa),
    enviados: results.filter((r) => !r.aceita && r.enviado).map(pessoa),
  });
}

/** Pede amizade pelo código. Se a outra pessoa já tinha pedido, vira amizade na hora. */
async function pedirAmizade(req, env, eu) {
  const { codigo } = await corpo(req);
  const c = String(codigo || "").toUpperCase().replace(/[^A-Z0-9]/g, "");
  const alvo = await env.DB.prepare("SELECT id FROM usuarios WHERE codigo = ?").bind(c).first();
  if (!alvo) throw new ErroHttp(404, "Nenhum treinador com esse código");
  if (alvo.id === eu) throw new ErroHttp(400, "Esse código é seu");
  const inverso = await env.DB.prepare("SELECT aceita FROM amizades WHERE de = ? AND para = ?").bind(alvo.id, eu).first();
  if (inverso) {
    await env.DB.prepare("UPDATE amizades SET aceita = 1 WHERE de = ? AND para = ?").bind(alvo.id, eu).run();
    return json({ estado: "amigos" });
  }
  await env.DB.prepare("INSERT OR IGNORE INTO amizades (de, para, aceita, criado_em) VALUES (?, ?, 0, ?)")
    .bind(eu, alvo.id, Date.now()).run();
  return json({ estado: "pedido-enviado" });
}

async function aceitar(env, eu, de) {
  const r = await env.DB.prepare("UPDATE amizades SET aceita = 1 WHERE de = ? AND para = ?").bind(de, eu).run();
  if (!r.meta.changes) throw new ErroHttp(404, "Pedido não encontrado");
  return json({ estado: "amigos" });
}

/** Recusa pedido, cancela pedido enviado ou desfaz amizade. */
async function desfazer(env, eu, outro) {
  await env.DB.prepare("DELETE FROM amizades WHERE (de = ? AND para = ?) OR (de = ? AND para = ?)")
    .bind(eu, outro, outro, eu).run();
  return json({ ok: true });
}

async function saoAmigos(env, a, b) {
  return !!(await env.DB.prepare(
    "SELECT 1 FROM amizades WHERE aceita = 1 AND ((de = ? AND para = ?) OR (de = ? AND para = ?))",
  ).bind(a, b, b, a).first());
}

// ---------- times compartilhados ----------

async function listarTimes(env, dono) {
  const { results } = await env.DB.prepare("SELECT dados FROM times WHERE dono = ? ORDER BY alterado_em DESC").bind(dono).all();
  return results.map((r) => JSON.parse(r.dados));
}

async function meusTimes(env, eu) {
  return json({ times: await listarTimes(env, eu) });
}

async function timesDoAmigo(env, eu, amigo, url) {
  if (!(await saoAmigos(env, eu, amigo))) throw new ErroHttp(403, "Vocês não são amigos");
  const fav = await env.DB.prepare("SELECT favoritos FROM usuarios WHERE id = ?").bind(amigo).first();
  return json({
    treinador: await perfil(env, amigo, false, url.origin),
    times: await listarTimes(env, amigo),
    favoritos: JSON.parse(fav?.favoritos || "[]"),
  });
}

async function salvarTime(req, env, eu, id) {
  const t = await corpo(req, MAX_TIME);
  if (typeof t.nome !== "string" || !Array.isArray(t.membros) || t.membros.length > 6
    || !t.membros.every((m) => Number.isInteger(m))) throw new ErroHttp(400, "Time inválido");
  const dados = JSON.stringify({ id, nome: t.nome.slice(0, 40), membros: t.membros, builds: t.builds || {}, mega: t.mega ?? null });
  const existe = await env.DB.prepare("SELECT 1 FROM times WHERE dono = ? AND id = ?").bind(eu, id).first();
  if (!existe) {
    const { n } = await env.DB.prepare("SELECT COUNT(*) AS n FROM times WHERE dono = ?").bind(eu).first();
    if (n >= MAX_TIMES_POR_USUARIO) throw new ErroHttp(400, `Limite de ${MAX_TIMES_POR_USUARIO} times compartilhados`);
  }
  await env.DB.prepare("INSERT OR REPLACE INTO times (dono, id, dados, alterado_em) VALUES (?, ?, ?, ?)")
    .bind(eu, id, dados, Date.now()).run();
  return json({ ok: true });
}

async function apagarTime(env, eu, id) {
  await env.DB.prepare("DELETE FROM times WHERE dono = ? AND id = ?").bind(eu, id).run();
  return json({ ok: true });
}

// ---------- utilidades ----------

function novoCodigo() {
  const b = crypto.getRandomValues(new Uint8Array(6));
  return Array.from(b, (x) => LETRAS_CODIGO[x % LETRAS_CODIGO.length]).join("");
}

function aleatorio(bytes) {
  return Array.from(crypto.getRandomValues(new Uint8Array(bytes)), (x) => x.toString(16).padStart(2, "0")).join("");
}

async function sha256(texto) {
  const h = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(texto));
  return Array.from(new Uint8Array(h), (x) => x.toString(16).padStart(2, "0")).join("");
}

function b64urlBytes(s) {
  const b = atob(s.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((s.length + 3) % 4));
  return Uint8Array.from(b, (c) => c.charCodeAt(0));
}

function b64urlTexto(s) {
  return new TextDecoder().decode(b64urlBytes(s));
}
