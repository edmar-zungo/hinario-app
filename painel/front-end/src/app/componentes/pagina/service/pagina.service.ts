import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PaginaModel, BlocoModel } from '../pagina-model';

@Injectable({
  providedIn: 'root'
})
export class PaginaService {

  apiUrl = 'http://localhost:8080/paginas';

  constructor(private http: HttpClient) { }

  listar(): Observable<PaginaModel[]> {
    return this.http.get<PaginaModel[]>(`${this.apiUrl}`);
  }

  obter(pagina: number): Observable<PaginaModel> {
    return this.http.get<PaginaModel>(`${this.apiUrl}/${pagina}`);
  }

  importar(ficheiros: File[], hinarioId: string, rotacao: number): Observable<PaginaModel[]> {
    const dados = new FormData();
    ficheiros.forEach(f => dados.append('ficheiros', f, f.name));
    dados.append('hinarioId', hinarioId);
    dados.append('rotacao', String(rotacao));
    return this.http.post<PaginaModel[]>(`${this.apiUrl}`, dados);
  }

  ler(pagina: number): Observable<PaginaModel> {
    return this.http.post<PaginaModel>(`${this.apiUrl}/${pagina}/ler`, null);
  }

  estruturar(pagina: number): Observable<PaginaModel> {
    return this.http.post<PaginaModel>(`${this.apiUrl}/${pagina}/estruturar`, null);
  }

  guardarLinhas(pagina: number, blocos: BlocoModel[]): Observable<PaginaModel> {
    return this.http.put<PaginaModel>(`${this.apiUrl}/${pagina}/linhas`, blocos);
  }

  confirmar(pagina: number): Observable<PaginaModel> {
    return this.http.post<PaginaModel>(`${this.apiUrl}/${pagina}/confirmar`, null);
  }

  /** URL da foto para o ecrã de revisão (versão processada por omissão). */
  imagemUrl(pagina: number, original = false): string {
    return `${this.apiUrl}/${pagina}/imagem${original ? '?original=true' : ''}`;
  }
}
