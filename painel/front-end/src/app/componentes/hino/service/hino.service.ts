import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HinoModel } from '../hino-model';

@Injectable({
  providedIn: 'root'
})
export class HinoService {

  apiUrl = 'http://localhost:8080/api/hino';

  constructor(private http: HttpClient) { }

  getAll(): Observable<HinoModel[]>{
    return this.http.get<HinoModel[]>(`${this.apiUrl}/all`);
  }

  createHino(hino: HinoModel): Observable<HinoModel>{
    return this.http.post<HinoModel>(`${this.apiUrl}/create`, hino);
  }

  getHinoById(hinoId: string): Observable<HinoModel>{
    return this.http.get<HinoModel>(`${this.apiUrl}/${hinoId}`);
  }

  updateHino(hinoId: string | undefined, hino: HinoModel): Observable<HinoModel>{
    return this.http.put<HinoModel>(`${this.apiUrl}/update/${hinoId}`, hino);
  }

  deleteHino(hinoId: string):Observable<string>{
   return this.http.delete<string>(`${this.apiUrl}/delete/${hinoId}`);
  }

  addFavoritos(hinoId: string | undefined): Observable<HinoModel>{
    return this.http.post<HinoModel>(`${this.apiUrl}/favorito/add/${hinoId}`, null);
  }

  getAllFavoritos(): Observable<HinoModel[]>{
    return this.http.get<HinoModel[]>(`${this.apiUrl}/favorito/all`);
  }


}
