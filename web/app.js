const welcome = document.querySelector("#welcome");
const ballot = document.querySelector("#ballot");
const finished = document.querySelector("#finished");
const stateUrl = "/api/state";
let currentState;
let requesting = false;

async function request(path, method = "GET") {
  const response = await fetch(path, { method, headers: { "Accept": "application/json" } });
  const data = await response.json();
  if (!response.ok) throw new Error(data.erro || "Não foi possível concluir esta ação.");
  return data;
}

function showError(error) {
  const message = document.querySelector("#message");
  message.textContent = error.message || "Falha de comunicação com a urna.";
  message.dataset.type = "ERRO";
}

function show(state) {
  currentState = state;
  document.querySelectorAll("button").forEach(button => { button.disabled = false; });
  welcome.hidden = state.iniciada;
  ballot.hidden = !state.iniciada || state.finalizada;
  finished.hidden = !state.iniciada || !state.finalizada;

  if (!state.iniciada) return;
  if (state.finalizada) {
    document.querySelector("#finish-message").textContent = state.mensagem || "Votação encerrada.";
    return;
  }

  document.querySelector("#office-title").textContent = titleCase(state.cargo);
  document.querySelector("#progress").textContent = state.progresso;
  document.querySelector("#progress-fill").style.width = `${(state.posicao || cargoNumero(state.progresso)) / totalCargos(state.progresso) * 100}%`;
  document.querySelector("#explanation").textContent = state.explicacao || "Escolha uma candidatura ou vote em branco.";
  document.querySelector("#persistence").textContent = state.persistencia || "Armazenamento local";
  document.querySelector("#persistence-short").textContent = state.persistencia || "";
  document.querySelector("#message").textContent = state.mensagem || "Digite o número e confira sua escolha.";
  document.querySelector("#message").dataset.type = state.tipoMensagem || "INFO";
  document.querySelector("#candidate-count").textContent = state.candidatos?.length ?? 0;

  const display = document.querySelector("#digit-display");
  display.replaceChildren();
  const digitCount = state.posicoes > 0 ? state.posicoes : Math.max(2, state.digitado?.length || 0);
  for (let index = 0; index < digitCount; index++) {
    const box = document.createElement("span");
    box.className = `digit-box${state.digitado?.[index] ? " filled" : ""}`;
    box.textContent = state.digitado?.[index] || "";
    display.append(box);
  }

  const selection = document.querySelector("#selection");
  selection.replaceChildren();
  if (state.modo === "BRANCO" || state.titulo === "VOTO EM BRANCO") {
    addLine(selection, "Voto em branco marcado", "Confirme para registrar ou corrija para escolher outro voto.");
  } else if (["CANDIDATO", "NUMERO_ERRADO"].includes(state.modo)) {
    addLine(selection, state.titulo, state.detalhe);
  } else if (state.modo === "DIGITANDO" && state.titulo !== "Digite o número do candidato") {
    addLine(selection, state.titulo, state.detalhe);
  }

  drawCandidates(state.candidatos || [], state.digitado || "", state.titulo);
  const enabled = state.controlesHabilitados;
  document.querySelectorAll("[data-digit], #white, #correct").forEach(button => { button.disabled = !enabled; });
  document.querySelector("#confirm").disabled = !enabled || !( ["CANDIDATO", "NUMERO_ERRADO", "BRANCO"].includes(state.modo)
    || state.titulo === "VOTO DE LEGENDA");
}

function drawCandidates(candidates, typed, selectedTitle) {
  const list = document.querySelector("#candidates");
  list.replaceChildren();
  for (const candidate of candidates) {
    const card = document.createElement("article");
    card.className = `candidate${typed === candidate.numero && selectedTitle === candidate.nome ? " is-selected" : ""}`;
    const top = document.createElement("div");
    top.className = "candidate-top";
    const number = document.createElement("span");
    number.className = "candidate-number";
    number.textContent = candidate.numero;
    const identity = document.createElement("div");
    const name = document.createElement("h3");
    name.textContent = candidate.nome;
    const party = document.createElement("p");
    party.className = "party";
    party.textContent = candidate.partido;
    identity.append(name, party);
    top.append(number, identity);
    const details = document.createElement("div");
    details.className = "candidate-details";
    if (candidate.vice) {
      const vice = document.createElement("p");
      vice.textContent = `Vice: ${candidate.vice}`;
      details.append(vice);
    }
    if (candidate.propostas?.length) {
      const proposals = document.createElement("ul");
      candidate.propostas.forEach(text => {
        const item = document.createElement("li");
        item.textContent = text;
        proposals.append(item);
      });
      details.append(proposals);
    }
    card.append(top, details);
    list.append(card);
  }
}

function addLine(container, title, detail) {
  const heading = document.createElement("strong");
  heading.textContent = title;
  container.append(heading);
  if (detail) {
    const line = document.createElement("span");
    line.textContent = detail;
    container.append(line);
  }
}

function titleCase(value) {
  return value.toLocaleLowerCase("pt-BR").replace(/(^|\s)\S/g, letter => letter.toLocaleUpperCase("pt-BR"));
}

function cargoNumero(progresso) {
  return Number(progresso.match(/CARGO (\d+)/)?.[1] || 1);
}

function totalCargos(progresso) {
  return Number(progresso.match(/DE (\d+)/)?.[1] || 5);
}

async function act(path) {
  if (requesting) return;
  requesting = true;
  setBusy(true);
  let actionError;
  try { show(await request(path, "POST")); }
  catch (error) { actionError = error; }
  finally {
    requesting = false;
    if (currentState) show(currentState);
    if (actionError) showError(actionError);
  }
}

function setBusy(busy) {
  if (busy) document.querySelectorAll("button").forEach(button => { button.disabled = true; });
}

document.querySelector("#start").addEventListener("click", () => act("/api/start"));
document.querySelector("#restart").addEventListener("click", () => act("/api/start"));
document.querySelectorAll("[data-digit]").forEach(button => button.addEventListener("click", () => act(`/api/digit?valor=${button.dataset.digit}`)));
document.querySelector("#white").addEventListener("click", () => act("/api/white"));
document.querySelector("#correct").addEventListener("click", () => act("/api/correct"));
document.querySelector("#confirm").addEventListener("click", () => act("/api/confirm"));

document.addEventListener("keydown", event => {
  if (event.altKey || event.ctrlKey || event.metaKey || welcome.hidden === false || finished.hidden === false) return;
  if (/^[0-9]$/.test(event.key)) { event.preventDefault(); act(`/api/digit?valor=${event.key}`); }
  else if (event.key.toLowerCase() === "b") { event.preventDefault(); act("/api/white"); }
  else if (event.key === "Backspace") { event.preventDefault(); act("/api/correct"); }
  else if (event.key === "Enter") { event.preventDefault(); act("/api/confirm"); }
});

request(stateUrl).then(show).catch(showError);
