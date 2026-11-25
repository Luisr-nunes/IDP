'use strict';
// Todo dado vindo da API entra na tela via textContent (nunca innerHTML), para nao abrir brecha de XSS.

const LIMITE_BYTES = 5 * 1024 * 1024;
const TIPOS = ['application/pdf', 'image/png', 'image/jpeg'];
const ROTULOS = {
    nome: 'Nome', cpf: 'CPF', endereco: 'Endereço', cidade: 'Cidade', dataEmissao: 'Emissão',
    tipo: 'Tipo', validade: 'Validade'
};
const STATUS = { APROVADO: 'Aprovado', REPROVADO: 'Reprovado', REVISAO_MANUAL: 'Revisão manual' };

const $ = (sel, raiz = document) => raiz.querySelector(sel);
const docs = {};

function el(tag, classe, texto) {
    const e = document.createElement(tag);
    if (classe) e.className = classe;
    if (texto !== undefined && texto !== null) e.textContent = texto;
    return e;
}

function formatarData(valor) {
    if (!valor) return null;
    const [a, m, d] = valor.split('-');
    return d && m && a ? `${d}/${m}/${a}` : valor;
}

function iniciarDocumento(caixa) {
    const estado = { caixa, aba: 'arquivo', arquivo: null, nomeTexto: null };
    const entrada = $('input[type=file]', caixa);
    const soltar = $('.soltar', caixa);
    const textoSoltar = $('.soltar-texto', caixa);
    const original = textoSoltar.textContent;

    estado.trocarAba = aba => {
        estado.aba = aba;
        caixa.querySelectorAll('.aba').forEach(b => {
            const ativa = b.dataset.aba === aba;
            b.classList.toggle('ativa', ativa);
            b.setAttribute('aria-selected', String(ativa));
        });
        caixa.querySelectorAll('[data-painel]').forEach(p => { p.hidden = p.dataset.painel !== aba; });
    };
    estado.escolher = arquivo => {
        if (!arquivo) return;
        if (!TIPOS.includes(arquivo.type)) return avisar('Envie um PDF, PNG ou JPG.');
        if (arquivo.size > LIMITE_BYTES) return avisar('O arquivo passa de 5 MB.');
        avisar(null);
        estado.arquivo = arquivo;
        textoSoltar.textContent = arquivo.name;
        soltar.classList.add('escolhido');
    };
    estado.limpar = () => {
        estado.arquivo = null;
        entrada.value = '';
        textoSoltar.textContent = original;
        soltar.classList.remove('escolhido');
        $('textarea', caixa).value = '';
        estado.nomeTexto = null;
        estado.trocarAba('arquivo');
    };

    caixa.querySelectorAll('.aba').forEach(b => b.addEventListener('click', () => estado.trocarAba(b.dataset.aba)));
    entrada.addEventListener('change', () => estado.escolher(entrada.files[0]));
    soltar.addEventListener('dragover', e => { e.preventDefault(); soltar.classList.add('sobre'); });
    soltar.addEventListener('dragleave', () => soltar.classList.remove('sobre'));
    soltar.addEventListener('drop', e => {
        e.preventDefault();
        soltar.classList.remove('sobre');
        estado.escolher(e.dataTransfer.files[0]);
    });
    docs[caixa.dataset.doc] = estado;
}

function lerBase64(arquivo) {
    return new Promise((ok, erro) => {
        const leitor = new FileReader();
        leitor.onload = () => ok(String(leitor.result).split(',')[1]);
        leitor.onerror = () => erro(new Error('Nao foi possivel ler o arquivo.'));
        leitor.readAsDataURL(arquivo);
    });
}

/** Monta {nomeArquivo, mimeType, conteudoDocumento|imagemBase64} da aba ativa; null se estiver vazia. */
async function montar(estado, nomePadrao) {
    if (estado.aba === 'arquivo') {
        if (!estado.arquivo) return null;
        return {
            nomeArquivo: estado.arquivo.name,
            mimeType: estado.arquivo.type,
            imagemBase64: await lerBase64(estado.arquivo)
        };
    }
    const texto = $('textarea', estado.caixa).value.trim();
    return texto ? { nomeArquivo: estado.nomeTexto || nomePadrao, mimeType: 'application/pdf', conteudoDocumento: texto } : null;
}

function avisar(mensagem) {
    const aviso = $('#aviso');
    aviso.hidden = !mensagem;
    aviso.textContent = mensagem || '';
}

async function validar() {
    const botao = $('#validar');
    avisar(null);
    try {
        const comprovante = await montar(docs.comprovante, 'comprovante.txt');
        if (!comprovante) return avisar('Envie o comprovante de residência (arquivo ou texto).');
        const identificacao = await montar(docs.identificacao, 'identificacao.txt');
        botao.disabled = true;
        botao.textContent = 'Validando...';
        const resposta = await fetch('/api/documentos/validar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ...comprovante, identificacao })
        });
        const corpo = await resposta.json().catch(() => ({}));
        if (!resposta.ok) {
            const erros = Array.isArray(corpo.erros) ? corpo.erros.join('\n') : (corpo.detail || 'Erro ao validar.');
            return avisar(erros);
        }
        mostrar(corpo);
        carregarHistorico();
    } catch (e) {
        avisar('Nao foi possivel falar com o servidor. Verifique se a aplicacao esta rodando.');
    } finally {
        botao.disabled = false;
        botao.textContent = 'Validar documentos';
    }
}

function preencherPropriedades(dl, dados, campos) {
    dl.replaceChildren();
    campos.forEach(campo => {
        let valor = dados[campo];
        if (campo === 'dataEmissao' || campo === 'validade') valor = formatarData(valor);
        dl.append(el('dt', null, ROTULOS[campo]));
        dl.append(valor ? el('dd', null, valor) : el('dd', 'vazio', 'Não encontrado'));
    });
}

function mostrar(r) {
    const status = r.resultado.status;
    const tag = $('#status');
    tag.className = 'tag ' + status;
    tag.textContent = STATUS[status] || status;

    const etapas = $('#etapas');
    etapas.replaceChildren();
    r.etapas.forEach(etapa => {
        const li = el('li');
        const corpo = el('div');
        corpo.append(el('div', 'etapa-nome', etapa.nome), el('div', 'etapa-desc', etapa.descricao),
            el('span', 'etapa-res', etapa.resultado));
        li.append(el('span', 'etapa-num', etapa.ordem), corpo);
        etapas.append(li);
    });

    preencherPropriedades($('#dados'), r.dadosExtraidos, ['nome', 'cpf', 'endereco', 'cidade', 'dataEmissao']);
    const temId = !!r.identificacao;
    $('#bloco-identificacao').hidden = !temId;
    if (temId) {
        $('#t-identificacao').textContent = r.identificacao.tipo === 'CNH' ? 'Dados da CNH' : 'Dados do RG';
        const campos = r.identificacao.tipo === 'CNH' ? ['nome', 'cpf', 'validade'] : ['nome', 'cpf'];
        preencherPropriedades($('#identificacao'), r.identificacao, campos);
    }

    const regras = $('#regras');
    regras.replaceChildren();
    const item = (texto, ok) => {
        const li = el('li', ok ? 'ok' : 'falha');
        li.append($(ok ? '#icone-ok' : '#icone-falha').content.cloneNode(true), el('span', null, texto));
        regras.append(li);
    };
    r.resultado.regrasAtendidas.forEach(t => item(t, true));
    r.resultado.pendencias.forEach(t => item(t.replace(/^Falhou: /, ''), false));

    const secao = $('#resultado');
    secao.hidden = false;
    secao.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
}

async function carregarHistorico() {
    const corpo = $('#historico');
    try {
        const lista = await (await fetch('/api/documentos')).json();
        corpo.replaceChildren();
        if (!lista.length) {
            const tr = el('tr');
            const td = el('td', 'vazio', 'Nenhuma validação ainda.');
            td.colSpan = 5;
            tr.append(td);
            return corpo.append(tr);
        }
        lista.forEach(v => {
            const tr = el('tr');
            const status = el('td');
            status.append(el('span', 'tag ' + v.status, STATUS[v.status] || v.status));
            tr.append(el('td', 'num', v.id), el('td', null, v.nomeArquivo || '-'), status,
                el('td', null, v.pendencias.length ? v.pendencias.map(p => p.replace(/^Falhou: /, '')).join('; ') : '-'),
                el('td', null, new Date(v.criadoEm).toLocaleString('pt-BR')));
            corpo.append(tr);
        });
    } catch (e) {
        corpo.replaceChildren();
    }
}

async function usarExemplo() {
    avisar(null);
    const exemplo = await (await fetch('/api/documentos/exemplo')).json();
    docs.comprovante.limpar();
    docs.identificacao.limpar();
    docs.comprovante.trocarAba('texto');
    $('textarea', docs.comprovante.caixa).value = exemplo.conteudoDocumento;
    docs.comprovante.nomeTexto = exemplo.nomeArquivo;
    if (exemplo.identificacao) {
        docs.identificacao.trocarAba('texto');
        $('textarea', docs.identificacao.caixa).value = exemplo.identificacao.conteudoDocumento;
        docs.identificacao.nomeTexto = exemplo.identificacao.nomeArquivo;
    }
}

document.querySelectorAll('.doc').forEach(iniciarDocumento);
$('#validar').addEventListener('click', validar);
$('#exemplo').addEventListener('click', usarExemplo);
$('#limpar').addEventListener('click', () => {
    docs.comprovante.limpar();
    docs.identificacao.limpar();
    avisar(null);
    $('#resultado').hidden = true;
});
$('#atualizar').addEventListener('click', carregarHistorico);
carregarHistorico();
