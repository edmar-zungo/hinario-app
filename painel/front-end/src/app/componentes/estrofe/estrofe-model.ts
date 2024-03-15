import { HinoModel } from "../hino/hino-model";

export interface EstrofeModel {
     id?: string,
     numero: number,
     descricao: string,
     refrao: boolean,
     hino: Pick<HinoModel, 'id'|'titulo'> | null;

}
