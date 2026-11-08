// Teste de ponta a ponta contra o servidor local (npm run dev com MODO_TESTE=1 em .dev.vars).
// Uso: npm run teste   (ou BASE=http://127.0.0.1:8787 node teste.mjs)
const BASE = process.env.BASE || "http://127.0.0.1:8787";
let falhas = 0, ok = 0;
const certo = (cond, msg) => { if (cond) ok++; else { falhas++; console.log("FALHOU:", msg); } };

async function chamar(metodo, caminho, { sessao, corpo, tipo, bruto } = {}) {
  const headers = {};
  if (sessao) headers.authorization = "Bearer " + sessao;
  if (corpo !== undefined) headers["content-type"] = tipo || "application/json";
  const r = await fetch(BASE + caminho, { method: metodo, headers, body: bruto ?? (corpo !== undefined ? JSON.stringify(corpo) : undefined) });
  const texto = await r.text();
  let dados; try { dados = JSON.parse(texto); } catch { dados = texto; }
  return { status: r.status, dados };
}

const sufixo = Date.now().toString(36);
const entrar = async (nome) => (await chamar("POST", "/entrar", { corpo: { idToken: `teste:${nome}-${sufixo}:${nome}` } })).dados;

const a = await entrar("Ana"), b = await entrar("Bia");
certo(a.sessao && a.eu.codigo?.length === 6, "login cria sessão e código");
certo((await chamar("GET", "/eu")).status === 401, "sem sessão = 401");
certo((await chamar("POST", "/entrar", { corpo: { idToken: "falso" } })).status === 401, "token inválido = 401");

const ap = await chamar("PATCH", "/eu", { sessao: a.sessao, corpo: { apelido: "  Ana Ketchum  " } });
certo(ap.dados.apelido === "Ana Ketchum", "muda apelido (com trim)");
certo((await chamar("PATCH", "/eu", { sessao: a.sessao, corpo: { apelido: "x" } })).status === 400, "apelido curto recusado");

const png = Uint8Array.from(atob("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="), (c) => c.charCodeAt(0));
const foto = await chamar("PUT", "/eu/foto", { sessao: a.sessao, corpo: "", tipo: "image/png", bruto: png });
certo(foto.status === 200 && foto.dados.foto?.includes("/fotos/"), "envia foto");
const servida = await fetch(foto.dados.foto);
certo(servida.status === 200 && servida.headers.get("content-type") === "image/png", "serve a foto");

certo((await chamar("POST", "/amigos", { sessao: a.sessao, corpo: { codigo: "ZZZZZZ" } })).status === 404, "código inexistente");
certo((await chamar("POST", "/amigos", { sessao: a.sessao, corpo: { codigo: a.eu.codigo } })).status === 400, "não adiciona a si mesma");
const pedido = await chamar("POST", "/amigos", { sessao: a.sessao, corpo: { codigo: b.eu.codigo.toLowerCase() } });
certo(pedido.dados.estado === "pedido-enviado", "pede amizade (código sem caixa)");
let lb = (await chamar("GET", "/amigos", { sessao: b.sessao })).dados;
certo(lb.recebidos.length === 1 && lb.recebidos[0].apelido === "Ana Ketchum", "Bia vê o pedido");

await chamar("PUT", "/times/t1", { sessao: a.sessao, corpo: { nome: "Chuva", membros: [130, 6], builds: { "130": 1 }, mega: null } });
certo((await chamar("GET", `/amigos/${a.eu.id}/times`, { sessao: b.sessao })).status === 403, "não amigo não vê times");

certo((await chamar("POST", `/amigos/${a.eu.id}/aceitar`, { sessao: b.sessao })).dados.estado === "amigos", "Bia aceita");
const la = (await chamar("GET", "/amigos", { sessao: a.sessao })).dados;
certo(la.amigos.length === 1 && la.amigos[0].apelido === "Bia", "amizade vale para os dois");
const vt = (await chamar("GET", `/amigos/${a.eu.id}/times`, { sessao: b.sessao })).dados;
certo(vt.times.length === 1 && vt.times[0].nome === "Chuva" && vt.times[0].membros[0] === 130, "amiga vê o time compartilhado");
certo(vt.treinador.foto?.startsWith("http"), "foto do amigo com endereço completo");
certo((await chamar("PUT", "/times/t2", { sessao: a.sessao, corpo: { nome: "x", membros: [1, 2, 3, 4, 5, 6, 7] } })).status === 400, "time com 7 recusado");

await chamar("DELETE", "/times/t1", { sessao: a.sessao });
certo((await chamar("GET", `/amigos/${a.eu.id}/times`, { sessao: b.sessao })).dados.times.length === 0, "tirar compartilhamento some para a amiga");

await chamar("DELETE", `/amigos/${a.eu.id}`, { sessao: b.sessao });
certo((await chamar("GET", `/amigos/${a.eu.id}/times`, { sessao: b.sessao })).status === 403, "desfazer amizade corta acesso");

certo((await chamar("POST", "/sair", { sessao: a.sessao })).status === 200, "sair");
certo((await chamar("GET", "/eu", { sessao: a.sessao })).status === 401, "sessão some ao sair");
await chamar("DELETE", "/eu", { sessao: b.sessao });
certo((await chamar("GET", "/eu", { sessao: b.sessao })).status === 401, "apagar conta encerra tudo");

console.log(`${ok} ok, ${falhas} falhas`);
process.exit(falhas ? 1 : 0);
