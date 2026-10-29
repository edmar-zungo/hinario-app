import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface FilaEstadoModel {
  estado: string;
  pendentes: number;
  geminiDisponivel: boolean;
  tesseractDisponivel: boolean;
  quotaEsgotada: boolean;
  retomaAte?: string;
  paginas: { pagina: number; hinarioId: string; estado: string }[];
  modelos: {
    modelo: string;
    pausado: boolean;
    pausadoAte?: string;
    pedidosMinuto: number;
    pedidosDia: number;
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class FilaService {

  private apiUrl = 'http://localhost:8080/fila';

  constructor(private http: HttpClient) { }

  estado(): Observable<FilaEstadoModel> {
    return this.http.get<FilaEstadoModel>(this.apiUrl);
  }
}
