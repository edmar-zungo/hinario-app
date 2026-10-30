/** Blocos e linhas do esquema novo (docs/esquema-export.md). */
export interface LinhaNovoModel {
  texto: string;
  recuo: number;
}

export interface BlocoNovoModel {
  tipo: string;
  numero: number | null;
  linhas: LinhaNovoModel[];
}

/** Resultado de POST /exportar por hinário. */
export interface ResultadoExportacaoModel {
  ficheiro: string;
  gerado: boolean;
  hinos: number;
  estrofes: number;
  coros: number;
  erros: string[];
  avisos: string[];
}

/** Uma linha alinhada de GET /comparar/{hinoId}. */
export interface LinhaComparadaModel {
  actual: string | null;
  nova: string | null;
  tipo: string;
  blocoActual: number | null;
  linhaActual: number | null;
  blocoNova: number | null;
  linhaNova: number | null;
}

/** Resposta de GET /comparar/{hinoId}. */
export interface ComparacaoHinoModel {
  hinoId: string;
  numero: number;
  lingua: string;
  titulo: string;
  temLeituraNova: boolean;
  actual: BlocoNovoModel[];
  nova: BlocoNovoModel[];
  diferencas: LinhaComparadaModel[];
  linhasIguais: number;
}
