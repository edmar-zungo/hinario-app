import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ComparacaoHinoModel, ResultadoExportacaoModel } from '../exportacao-model';

@Injectable({
  providedIn: 'root'
})
export class ExportacaoService {

  apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) { }

  /** POST /exportar — grava export/hinario-<id>.json só com linhas revistas. */
  exportar(hinarioId?: string): Observable<ResultadoExportacaoModel[]> {
    const params = hinarioId ? new HttpParams().set('hinarioId', hinarioId) : undefined;
    return this.http.post<ResultadoExportacaoModel[]>(`${this.apiUrl}/exportar`, null, { params });
  }

  /** GET /comparar/{hinoId} — leitura nova contra o JSON actual. */
  comparar(hinoId: string): Observable<ComparacaoHinoModel> {
    return this.http.get<ComparacaoHinoModel>(`${this.apiUrl}/comparar/${hinoId}`);
  }
}
