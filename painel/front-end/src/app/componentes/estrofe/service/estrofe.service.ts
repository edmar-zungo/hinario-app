import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EstrofeModel } from '../estrofe-model';

@Injectable({
  providedIn: 'root'
})
export class EstrofeService {

  constructor(private http: HttpClient) { }

  apiUrl = 'http://localhost:8080/api/estrofe';

  getAll(): Observable<EstrofeModel[]>{
    return this.http.get<EstrofeModel[]>(`${this.apiUrl}/all`);
  }

  createEstrofe(estrofe: EstrofeModel): Observable<EstrofeModel>{
    return this.http.post<EstrofeModel>(`${this.apiUrl}/create`, estrofe);
  }

  getEstrofeById(estrofeId: string): Observable<EstrofeModel>{
    return this.http.get<EstrofeModel>(`${this.apiUrl}/${estrofeId}`);
  }

  updateEstrofe(estrofeId: string | undefined, estrofe: EstrofeModel): Observable<EstrofeModel>{
    return this.http.put<EstrofeModel>(`${this.apiUrl}/update/${estrofeId}`, estrofe);
  }

  deleteEstrofe(estrofeId: string):Observable<string>{
   return this.http.delete<string>(`${this.apiUrl}/delete/${estrofeId}`);
  }
}
