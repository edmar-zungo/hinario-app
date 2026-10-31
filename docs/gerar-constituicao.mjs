#!/usr/bin/env node
/**
 * Gera mobile/src/assets/data/constituicao.json a partir de
 * docs/constituicao_iera_v2.json (transcrição oficial do PDF da IERA).
 *
 * Regras (PRD Fase 8):
 *  - Texto verbatim (regra de ouro): nenhuma alteração além de estrutura e NFC.
 *  - IDs compostos (I-art-1) porque os números de artigo repetem-se entre partes.
 *  - Notas editoriais (editorial_notes) ficam fora do ficheiro publicado.
 *
 * Uso: node docs/gerar-constituicao.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const aqui = dirname(fileURLToPath(import.meta.url));
const ENTRADA = join(aqui, 'constituicao_iera_v2.json');
const SAIDA = join(aqui, '..', 'mobile', 'src', 'assets', 'data', 'constituicao.json');

const TIPOS = {
  text: 'texto',
  number: 'numero',
  clause: 'alinea',
  paragraph: 'paragrafo',
  department: 'departamento',
  sector: 'sector',
};

const nfc = (s) => (typeof s === 'string' ? s.normalize('NFC') : s);

const semAcentos = (s) =>
  s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();

const slug = (s) =>
  semAcentos(s)
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

function transformarBloco(bloco) {
  if (!bloco || typeof bloco !== 'object') {
    throw new Error('Bloco inválido: ' + JSON.stringify(bloco));
  }
  const tipo = TIPOS[bloco.type];
  if (!tipo) {
    throw new Error(`Tipo de bloco desconhecido: ${bloco.type}`);
  }
  if (typeof bloco.text !== 'string') {
    throw new Error('Bloco sem texto: ' + JSON.stringify(bloco).slice(0, 120));
  }
  const saida = {
    marcador: bloco.marker ?? null,
    tipo,
    texto: nfc(bloco.text),
  };
  if (Array.isArray(bloco.items) && bloco.items.length > 0) {
    saida.itens = bloco.items.map(transformarBloco);
  }
  return saida;
}

function transformarArtigo(artigo, parteNumero) {
  if (!artigo?.number) {
    throw new Error('Artigo sem número na parte ' + parteNumero);
  }
  return {
    id: `${parteNumero}-art-${artigo.number}`,
    numero: Number(artigo.number),
    rotulo: nfc(artigo.label || `Artigo ${artigo.number}º`),
    titulo: nfc(artigo.title || ''),
    paginaInicio: artigo.page_start ?? null,
    paginaFim: artigo.page_end ?? null,
    conteudo: (artigo.content || []).map(transformarBloco),
  };
}

function transformarSecao(secao, parteNumero, capituloNumero) {
  return {
    id: `${parteNumero}-cap-${capituloNumero}-sec-${secao.number}`,
    numero: nfc(secao.number),
    titulo: nfc(secao.title || ''),
    paginaInicio: secao.page_start ?? null,
    artigos: (secao.articles || []).map((a) => transformarArtigo(a, parteNumero)),
  };
}

function transformarCapitulo(capitulo, parteNumero) {
  return {
    id: `${parteNumero}-cap-${capitulo.number}`,
    numero: nfc(capitulo.number),
    titulo: nfc(capitulo.title || ''),
    paginaInicio: capitulo.page_start ?? null,
    artigos: (capitulo.articles || []).map((a) => transformarArtigo(a, parteNumero)),
    seccoes: (capitulo.sections || []).map((s) =>
      transformarSecao(s, parteNumero, capitulo.number)
    ),
  };
}

function transformarParte(parte) {
  return {
    id: nfc(parte.number),
    numero: nfc(parte.number),
    titulo: nfc(parte.title || ''),
    paginaInicio: parte.page_start ?? null,
    textosFrontal: (parte.front_matter || []).map((fm) => ({
      id: `${parte.number}-${slug(fm.title || 'texto')}`,
      titulo: nfc(fm.title || ''),
      paginaInicio: fm.page_start ?? null,
      paragrafos: (fm.paragraphs || []).map(nfc),
    })),
    capitulos: (parte.chapters || []).map((c) => transformarCapitulo(c, parte.number)),
  };
}

function contarArtigos(partes) {
  let n = 0;
  for (const parte of partes) {
    for (const cap of parte.capitulos) {
      n += cap.artigos.length;
      for (const sec of cap.seccoes) {
        n += sec.artigos.length;
      }
    }
  }
  return n;
}

function validar(partes, apendices, totalArtigos, esperado) {
  const ids = [];
  const ver = (id, contexto) => {
    if (!id) throw new Error(`ID vazio em ${contexto}`);
    if (ids.includes(id)) throw new Error(`ID duplicado: ${id}`);
    ids.push(id);
  };

  for (const parte of partes) {
    ver(parte.id, 'parte');
    for (const fm of parte.textosFrontal) ver(fm.id, 'texto frontal');
    for (const cap of parte.capitulos) {
      ver(cap.id, 'capítulo');
      for (const art of cap.artigos) ver(art.id, 'artigo');
      for (const sec of cap.seccoes) {
        ver(sec.id, 'secção');
        for (const art of sec.artigos) ver(art.id, 'artigo de secção');
      }
    }
  }
  for (const ap of apendices) ver(ap.id, 'apêndice');

  if (esperado && totalArtigos !== esperado) {
    throw new Error(`Contagem de artigos divergente: gerados ${totalArtigos}, fonte ${esperado}`);
  }
  return ids.length;
}

const bruto = JSON.parse(readFileSync(ENTRADA, 'utf8'));
const doc = bruto.document;
if (!doc?.parts?.length) {
  throw new Error(' Estrutura inesperada: document.parts em falta');
}

const partes = doc.parts.map(transformarParte);
const apendices = (doc.appendices || []).map((ap) => ({
  id: `apendice-${slug(ap.title || 'apendice')}`,
  titulo: nfc(ap.title || ''),
  paginaInicio: ap.page_start ?? null,
  paragrafos: (ap.paragraphs || []).map(nfc),
}));

const totalArtigos = contarArtigos(partes);
const totalEntidades = validar(partes, apendices, totalArtigos, doc.article_count ?? null);

const constituicao = {
  id: 'constituicao-iera',
  titulo: nfc(doc.title || 'Constituição da IERA'),
  fonte: nfc(doc.source_file || ''),
  paginasFonte: doc.source_pages ?? null,
  esquemaVersao: '1.0',
  fonteEsquemaVersao: String(doc.schema_version ?? ''),
  geradoDe: 'constituicao_iera_v2.json',
  geradoEm: new Date().toISOString(),
  totalArtigos,
  indice: (doc.index || []).map(nfc),
  partes,
  apendices,
};

mkdirSync(dirname(SAIDA), { recursive: true });
writeFileSync(SAIDA, JSON.stringify(constituicao, null, 2) + '\n', 'utf8');

const capituloCount = partes.reduce((n, p) => n + p.capitulos.length, 0);
const seccaoCount = partes.reduce(
  (n, p) => n + p.capitulos.reduce((m, c) => m + c.seccoes.length, 0),
  0
);
const tamanho = (readFileSync(SAIDA).length / 1024).toFixed(1);
console.log(`✔ ${SAIDA}`);
console.log(
  `  partes=${partes.length} capítulos=${capituloCount} secções=${seccaoCount} ` +
    `artigos=${totalArtigos} apêndices=${apendices.length} entidades=${totalEntidades} (${tamanho} KB)`
);
