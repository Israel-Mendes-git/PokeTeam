// Servidor de amigos do PokeTeam: login com Google, perfil (apelido e foto), amizades por código
// times compartilhados e salas de draft online. Cloudflare Workers + D1 (banco e fotos).

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

  if (m === "GET" && p === "/salas") return listarSalas(env, eu);
  if (m === "POST" && p === "/salas") return criarSala(req, env, eu);
  r = p.match(/^\/salas\/([A-Z0-9]{4,8})(\/\w+)?$/);
  if (r) {
    const [, cod, acao = ""] = r;
    if (m === "GET" && !acao) return verSala(env, eu, cod, url);
    if (m === "POST" && acao === "/entrar") return entrarSala(env, eu, cod);
    if (m === "POST" && acao === "/sair") return sairSala(env, eu, cod);
    if (m === "POST" && acao === "/iniciar") return iniciarSala(env, eu, cod);
    if (m === "POST" && acao === "/escolher") return escolherSala(req, env, eu, cod);
    if (m === "POST" && acao === "/desfazer") return desfazerSala(env, eu, cod);
  }

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
    env.DB.prepare("DELETE FROM salas WHERE dono = ?").bind(eu),
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

// ---------- draft online ----------

const MAX_SALA = 64 * 1024;          // o pool vai junto (lista de banidos etc.)
const MAX_JOGADORES_SALA = 4;
const VIDA_SALA = 24 * 60 * 60 * 1000;

/** Jogador da escolha número n (0 = primeira), com ordem serpente ou fixa. Igual ao app. */
function jogadorDaEscolha(sala, n) {
  const j = sala.jogadores.length;
  const rodada = Math.floor(n / j), pos = n % j;
  return sala.serpente && rodada % 2 === 1 ? j - 1 - pos : pos;
}

function resumoSala(s) {
  return { codigo: s.codigo, estado: s.estado, dono: s.dono, jogadores: s.jogadores.map((j) => j.apelido), porJogador: s.porJogador };
}

async function lerSala(env, cod) {
  const l = await env.DB.prepare("SELECT dados, versao, estado FROM salas WHERE codigo = ?").bind(cod).first();
  if (!l) throw new ErroHttp(404, "Sala não encontrada (talvez tenha sido encerrada)");
  return { ...JSON.parse(l.dados), versao: l.versao, estado: l.estado };
}

/** Grava só se ninguém mudou a sala no meio (versão igual); senão pede para tentar de novo. */
async function gravarSala(env, s) {
  const { versao, ...dados } = s;
  const part = "," + s.jogadores.map((j) => j.id).join(",") + ",";
  const r = await env.DB.prepare(
    "UPDATE salas SET dados = ?, participantes = ?, estado = ?, versao = versao + 1, alterado_em = ? WHERE codigo = ? AND versao = ?",
  ).bind(JSON.stringify(dados), part, s.estado, Date.now(), s.codigo, versao).run();
  if (!r.meta.changes) throw new ErroHttp(409, "A sala mudou ao mesmo tempo; tente de novo");
  return json({ ...dados, versao: versao + 1 });
}

async function listarSalas(env, eu) {
  await env.DB.prepare("DELETE FROM salas WHERE alterado_em < ?").bind(Date.now() - VIDA_SALA).run();
  const minhas = await env.DB.prepare("SELECT dados, estado FROM salas WHERE participantes LIKE ? ORDER BY alterado_em DESC")
    .bind(`%,${eu},%`).all();
  // Salas esperando gente, abertas por amigos.
  const abertas = await env.DB.prepare(
    `SELECT s.dados, s.estado FROM salas s
      WHERE s.estado = 'espera' AND s.participantes NOT LIKE ? AND s.dono IN (
        SELECT CASE WHEN de = ? THEN para ELSE de END FROM amizades WHERE aceita = 1 AND (de = ? OR para = ?))
      ORDER BY s.alterado_em DESC`,
  ).bind(`%,${eu},%`, eu, eu, eu).all();
  const res = (l) => resumoSala({ ...JSON.parse(l.dados), estado: l.estado });
  return json({ minhas: minhas.results.map(res), abertas: abertas.results.map(res) });
}

async function criarSala(req, env, eu) {
  const c = await corpo(req, MAX_SALA);
  const porJogador = Number(c.porJogador);
  if (!Number.isInteger(porJogador) || porJogador < 1 || porJogador > 6) throw new ErroHttp(400, "Pokémons por jogador: de 1 a 6");
  if (!c.pool || typeof c.pool !== "object") throw new ErroHttp(400, "Falta o pool");
  await env.DB.prepare("DELETE FROM salas WHERE alterado_em < ?").bind(Date.now() - VIDA_SALA).run();
  const eu_ = await perfil(env, eu);
  const sala = {
    dono: eu, estado: "espera", porJogador, serpente: c.serpente !== false,
    modo: c.modo === "LIVRE" ? "LIVRE" : "OPCOES", soFinais: c.soFinais !== false, pool: c.pool,
    jogadores: [{ id: eu, apelido: eu_.apelido }], escolhas: [],
  };
  for (let tentativa = 0; tentativa < 5; tentativa++) {
    sala.codigo = novoCodigo().slice(0, 5);
    try {
      await env.DB.prepare(
        "INSERT INTO salas (codigo, dono, participantes, estado, dados, versao, alterado_em) VALUES (?, ?, ?, 'espera', ?, 1, ?)",
      ).bind(sala.codigo, eu, `,${eu},`, JSON.stringify(sala), Date.now()).run();
      return json({ ...sala, versao: 1 });
    } catch (e) {
      if (!String(e).includes("UNIQUE")) throw e;
    }
  }
  throw new ErroHttp(500, "Não deu para criar a sala");
}

/** ?versao=N: se a sala não mudou, responde só {mudou:false} (o app consulta a cada poucos segundos). */
async function verSala(env, eu, cod, url) {
  const v = Number(url.searchParams.get("versao") || 0);
  const l = await env.DB.prepare("SELECT versao FROM salas WHERE codigo = ?").bind(cod).first();
  if (!l) throw new ErroHttp(404, "Sala não encontrada (talvez tenha sido encerrada)");
  if (v && v === l.versao) return json({ mudou: false });
  return json(await lerSala(env, cod));
}

async function entrarSala(env, eu, cod) {
  const s = await lerSala(env, cod);
  if (s.jogadores.some((j) => j.id === eu)) return json(s);
  if (s.estado !== "espera") throw new ErroHttp(400, "O draft dessa sala já começou");
  if (s.jogadores.length >= MAX_JOGADORES_SALA) throw new ErroHttp(400, `A sala já tem ${MAX_JOGADORES_SALA} jogadores`);
  const p = await perfil(env, eu);
  s.jogadores.push({ id: eu, apelido: p.apelido });
  return gravarSala(env, s);
}

/** Anfitrião sair encerra a sala; outro jogador só sai antes de começar. */
async function sairSala(env, eu, cod) {
  const s = await lerSala(env, cod);
  if (s.dono === eu) {
    await env.DB.prepare("DELETE FROM salas WHERE codigo = ?").bind(cod).run();
    return json({ ok: true });
  }
  if (s.estado !== "espera") return json({ ok: true }); // depois de começar, a vaga fica (as escolhas são dele)
  s.jogadores = s.jogadores.filter((j) => j.id !== eu);
  await gravarSala(env, s);
  return json({ ok: true });
}

async function iniciarSala(env, eu, cod) {
  const s = await lerSala(env, cod);
  if (s.dono !== eu) throw new ErroHttp(403, "Só o anfitrião começa o draft");
  if (s.estado !== "espera") throw new ErroHttp(400, "O draft já começou");
  if (s.jogadores.length < 2) throw new ErroHttp(400, "Precisa de pelo menos 2 jogadores");
  s.estado = "andamento";
  return gravarSala(env, s);
}

/** {pokemonId, dex, n}: n é o número da escolha que o app viu (evita escolha dupla com rede lenta). */
async function escolherSala(req, env, eu, cod) {
  const { pokemonId, dex, n } = await corpo(req);
  if (!Number.isInteger(pokemonId) || !Number.isInteger(dex) || !Number.isInteger(n)) throw new ErroHttp(400, "Escolha inválida");
  const s = await lerSala(env, cod);
  if (s.estado !== "andamento") throw new ErroHttp(400, "O draft não está em andamento");
  if (n !== s.escolhas.length) throw new ErroHttp(409, "Alguém escolheu antes; atualize");
  const vez = jogadorDaEscolha(s, n);
  if (s.jogadores[vez]?.id !== eu) throw new ErroHttp(403, "Não é a sua vez");
  if (s.escolhas.some((e) => e.dex === dex)) throw new ErroHttp(409, "Esse pokémon já foi escolhido");
  s.escolhas.push({ jogador: vez, pokemonId, dex });
  if (s.escolhas.length >= s.jogadores.length * s.porJogador) s.estado = "fim";
  return gravarSala(env, s);
}

async function desfazerSala(env, eu, cod) {
  const s = await lerSala(env, cod);
  if (s.dono !== eu) throw new ErroHttp(403, "Só o anfitrião desfaz escolhas");
  if (!s.escolhas.length) throw new ErroHttp(400, "Nada para desfazer");
  s.escolhas.pop();
  s.estado = "andamento";
  return gravarSala(env, s);
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
