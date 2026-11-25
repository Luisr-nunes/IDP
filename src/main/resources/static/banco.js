'use strict';
// Todo dado vindo da API entra na tela via textContent (nunca innerHTML), para nao abrir brecha de XSS.

const STATUS = { APROVADO: 'Aprovado', REPROVADO: 'Reprovado', REVISAO_MANUAL: 'Revisão manual' };
const CAMPOS_COMPROVANTE = [['nome', 'Nome'], ['cpf', 'CPF'], ['endereco', 'Endereço'], ['cidade', 'Cidade'],
    ['cep', 'CEP'], ['dataEmissao', 'Emissão']];
const CAMPOS_IDENTIFICACAO = [['tipo', 'Tipo'], ['nome', 'Nome'], ['cpf', 'CPF'], ['validade', 'Validade']];

const $ = sel => document.querySelector(sel);
let todos = [];
let filtroStatus = '';

function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto !== undefined && texto !== null) e.textContent = texto;
    return e;
}

function data(valor) {
    if (!valor) return null;
    const [a, m, d] = valor.split('-');
    return d && m && a ? `${d}/${m}/${a}` : valor;
}

function quando(instante) {
    return instante ? new Date(instante).toLocaleString('pt-BR') : null;
}

function semFalhou(p) {
    return p.replace(/^Falhou: /, '');
}

function propriedades(titulo, dados, campos) {
    const bloco = el('div');
    bloco.append(el('h3', null, titulo));
    const dl = el('dl', 'propriedades');
    campos.forEach(([chave, rotulo]) => {
        let valor = dados ? dados[chave] : null;
        if (chave === 'dataEmissao' || chave === 'validade') valor = data(valor);
        dl.append(el('dt', null, rotulo), valor ? el('dd', null, valor) : el('dd', 'vazio', '-'));
    });
    bloco.append(dl);
    return bloco;
}

function baixar(nome, objeto) {
    const url = URL.createObjectURL(new Blob([JSON.stringify(objeto, null, 2)], { type: 'application/json' }));
    const a = el('a');
    a.href = url;
    a.download = nome;
    document.body.append(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
}

function linhaDetalhe(r) {
    const tr = el('tr', 'detalhe');
    tr.hidden = true;
    const td = el('td');
    td.colSpan = 9;
    const grade = el('div', 'detalhe-grade');
    grade.append(propriedades('Comprovante de residência', r.comprovante, CAMPOS_COMPROVANTE));
    if (r.identificacao) grade.append(propriedades('Documento de identificação', r.identificacao, CAMPOS_IDENTIFICACAO));

    const resultado = el('div');
    resultado.append(el('h3', null, 'Resultado'));
    const dl = el('dl', 'propriedades');
    dl.append(el('dt', null, 'Status'), el('dd', null, STATUS[r.status] || r.status));
    dl.append(el('dt', null, 'Pendências'), el('dd', null, r.pendencias.length ? r.pendencias.map(semFalhou).join('; ') : '-'));
    dl.append(el('dt', null, 'Validado em'), el('dd', null, quando(r.criadoEm)));
    if (r.revisadoManualmente) {
        dl.append(el('dt', null, 'Revisão'), el('dd', null, 'Decidido pelo analista em ' + quando(r.revisadoEm)));
        dl.append(el('dt', null, 'Motivo'), el('dd', null, r.motivoRevisao));
    }
    resultado.append(dl);
    grade.append(resultado);

    const acoes = el('div', 'detalhe-acoes');
    const botao = el('button', 'secundario pequeno', 'Baixar JSON deste registro');
    botao.type = 'button';
    botao.addEventListener('click', () => baixar(`idp-registro-${r.id}.json`, r));
    acoes.append(botao);
    td.append(grade, acoes);
    tr.append(td);
    return tr;
}

function seta() {
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('aria-hidden', 'true');
    const caminho = document.createElementNS(ns, 'path');
    caminho.setAttribute('d', 'm9 6 6 6-6 6');
    svg.append(caminho);
    return svg;
}

function linha(r) {
    const tr = el('tr');
    const status = el('td');
    status.append(el('span', 'tag ' + r.status, STATUS[r.status] || r.status));
    if (r.revisadoManualmente) status.append(el('span', 'revisado', 'decidido pelo analista'));
    const c = r.comprovante || {};
    const detalhe = linhaDetalhe(r);

    const expandir = el('button', 'botao-expandir');
    expandir.type = 'button';
    expandir.setAttribute('aria-expanded', 'false');
    expandir.setAttribute('aria-label', 'Ver dados do registro ' + r.id);
    expandir.append(seta());
    expandir.addEventListener('click', () => {
        detalhe.hidden = !detalhe.hidden;
        expandir.setAttribute('aria-expanded', String(!detalhe.hidden));
    });

    const acao = el('td');
    acao.append(expandir);
    tr.append(el('td', 'num', r.id), el('td', null, r.nomeArquivo || '-'), status,
        el('td', null, c.nome || '-'), el('td', null, c.cpf || '-'), el('td', null, c.cidade || '-'),
        el('td', 'quebra', r.pendencias.length ? r.pendencias.map(semFalhou).join('; ') : '-'),
        el('td', null, quando(r.criadoEm)), acao);
    return [tr, detalhe];
}

function normal(s) {
    return (s || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

function desenhar() {
    const busca = normal($('#busca').value.trim());
    const visiveis = todos.filter(r => {
        if (filtroStatus && r.status !== filtroStatus) return false;
        if (!busca) return true;
        const c = r.comprovante || {};
        const i = r.identificacao || {};
        return [r.nomeArquivo, c.nome, c.cpf, i.nome, i.cpf].some(v => normal(v).includes(busca));
    });
    const corpo = $('#registros');
    corpo.replaceChildren();
    if (!visiveis.length) {
        const tr = el('tr');
        const td = el('td', 'vazio', todos.length ? 'Nenhum registro com esse filtro.' : 'Nenhuma validação gravada ainda. Valide um documento na tela Validação.');
        td.colSpan = 9;
        tr.append(td);
        corpo.append(tr);
    }
    visiveis.forEach(r => corpo.append(...linha(r)));
    $('#contagem').textContent = `${visiveis.length} de ${todos.length} registros`;
}

async function carregar() {
    const aviso = $('#aviso');
    try {
        const resposta = await fetch('/api/banco');
        if (!resposta.ok) throw new Error();
        todos = await resposta.json();
        aviso.hidden = true;
        desenhar();
    } catch (e) {
        aviso.hidden = false;
        aviso.textContent = 'Nao foi possivel carregar o banco de dados. Verifique se a aplicacao esta rodando.';
    }
}

document.querySelectorAll('.filtro').forEach(b => b.addEventListener('click', () => {
    filtroStatus = b.dataset.status;
    document.querySelectorAll('.filtro').forEach(f => f.classList.toggle('ativo', f === b));
    desenhar();
}));
$('#busca').addEventListener('input', desenhar);
$('#atualizar').addEventListener('click', carregar);
carregar();
