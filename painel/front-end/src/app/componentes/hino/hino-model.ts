import { Linguas } from "./linguas";

export interface HinoModel {

     id?: string,
     titulo: string,
     pagina: number,
     numero: number,
     autor: string,
     dataCriacao: Date,
     dataActualizacao: Date,
     comentario: string,
     isFavorito: boolean,
     lingua: Linguas
}
