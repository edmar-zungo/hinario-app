export type TipoBlocoConstituicao =
  | 'texto'
  | 'numero'
  | 'alinea'
  | 'paragrafo'
  | 'departamento'
  | 'sector';

export interface BlocoConteudo {
  marcador: string | null;
  tipo: TipoBlocoConstituicao;
  texto: string;
  itens?: BlocoConteudo[];
}

export interface ArtigoConstituicao {
  id: string;
  numero: number;
  rotulo: string;
  titulo: string;
  paginaInicio?: number | null;
  paginaFim?: number | null;
  conteudo: BlocoConteudo[];
}

export interface SecaoConstituicao {
  id: string;
  numero: string;
  titulo: string;
  paginaInicio?: number | null;
  artigos: ArtigoConstituicao[];
}

export interface CapituloConstituicao {
  id: string;
  numero: string;
  titulo: string;
  paginaInicio?: number | null;
  artigos: ArtigoConstituicao[];
  seccoes: SecaoConstituicao[];
}

export interface TextoFrontal {
  id: string;
  titulo: string;
  paginaInicio?: number | null;
  paragrafos: string[];
}

export interface ParteConstituicao {
  id: string;
  numero: string;
  titulo: string;
  paginaInicio?: number | null;
  textosFrontal: TextoFrontal[];
  capitulos: CapituloConstituicao[];
}

export interface ApendiceConstituicao {
  id: string;
  titulo: string;
  paginaInicio?: number | null;
  paragrafos: string[];
}

export interface Constituicao {
  id: string;
  titulo: string;
  fonte: string;
  paginasFonte?: number | null;
  esquemaVersao: string;
  fonteEsquemaVersao: string;
  geradoDe: string;
  geradoEm: string;
  totalArtigos: number;
  indice: string[];
  partes: ParteConstituicao[];
  apendices: ApendiceConstituicao[];
}

export type TipoUnidade = 'artigo' | 'texto';

export interface UnidadeLeitura {
  id: string;
  tipo: TipoUnidade;
  rotulo: string;
  titulo: string;
  contexto: string;
  artigo?: ArtigoConstituicao;
  paragrafos?: string[];
}

export interface ResultadoPesquisaConstituicao {
  unidade: UnidadeLeitura;
  trecho: string;
}
