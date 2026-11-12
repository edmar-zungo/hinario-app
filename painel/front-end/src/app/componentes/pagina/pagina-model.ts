export interface PaginaModel {
  pagina: number;
  hinarioId: string;
  imagem: string;
  imagemProcessada: string;
  hash: string;
  estado: string;
  nomeOriginal: string;
  importadoEm: string;
  largura: number;
  altura: number;
  leituras?: LeiturasModel;
  blocos?: BlocoModel[];
}

export interface LeiturasModel {
  gemini?: {
    resposta?: { blocos?: BlocoModel[] };
    modelo?: string;
    erro?: string;
    validacao?: { alfabeto?: { desconhecidos?: string[] } };
  };
}

export interface BlocoModel {
  tipo: 'titulo_hino' | 'estrofe' | 'coro' | 'bloco_sem_numero';
  numeroHino?: number | null;
  titulo?: string | null;
  numeroEstrofe?: number | null;
  italico?: boolean;
  incompleta?: boolean;
  sequenciaSuspeita?: boolean;
  possivelCoro?: boolean;
  ligacao?: {
    hinoAnterior?: number | null;
    estrofeAnterior?: number | null;
    esperado?: number | null;
    encontrado?: number | null;
    confirmar: boolean;
  } | null;
  linhas: LinhaModel[];
  duvidas?: string[];
}

export interface LinhaModel {
  texto: string;
  recuo: number;
  revisto?: boolean;
}
