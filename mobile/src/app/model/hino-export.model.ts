export interface LinhaExport {
  texto: string;
  recuo: number; // 0 = normal, 1 = recuado, 2 = mais recuado
}

export type TipoBloco = 'estrofe' | 'coro';

export interface BlocoExport {
  tipo: TipoBloco;
  numero?: number | null;
  linhas: LinhaExport[];
}

export interface HinoExport {
  id: string;
  numero: number;
  pagina: number;
  titulo: string;
  autor?: string | null;
  blocos: BlocoExport[];
  lingua?: string; // 'por' | 'kko' | 'kkr' | 'kmb'
  hinarioNome?: string;
}

export interface HinarioExport {
  id: string;
  lingua: string;
  nome: string;
  geradoEm?: string;
  hinos: HinoExport[];
}

export interface HinarioMetadata {
  id: string;
  lingua: string;
  nome: string;
  arquivo: string;
  totalHinos?: number;
}
