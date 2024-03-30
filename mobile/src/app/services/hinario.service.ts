import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HinoModel } from '../model/hino-model';
import { environment } from 'src/environments/environment.prod';
import { EstrofeModel } from '../model/estrofe-model';

@Injectable({
  providedIn: 'root',
})
export class HinarioService {
  apiUrl = environment.apiUrl;
  apiHinoUrl = environment.apiHinoUrl;
  apiEstrofesUrl = environment.apiEstrofesUrl;
  apiHinosFavoritos = environment.apiHinosFavoritos;
  hinosFavoritos: HinoModel[] = [];
 
  constructor(private http: HttpClient) {}
  

  // addOrRemoveHinoFavorito(hinoId: string | undefined): Observable<HinoModel> {
  //   return this.http.post<HinoModel>(
  //     `${this.apiHinoUrl}${hinoId}`,
  //     null
  //   );
  // }

  /**
   * METODOS USANDO O ARQUIVO JSON COMO FONTE
   */

  getAllHinoByJSON(): Observable<HinoModel[]> {
    return this.http.get<HinoModel[]>(`${this.apiHinoUrl}`);
  }


  getAllEstrofesByJSON(): Observable<EstrofeModel[]> {
    return this.http.get<EstrofeModel[]>(`${this.apiEstrofesUrl}`);
  }

  getAllHinosFavoritos(): HinoModel{
    const hinoId = localStorage.getItem("hinos");
    var hinoResult;
    this.getAllHinoByJSON().subscribe(resp => {
     hinoResult = resp.find(x => x.id === hinoId);
    })
    return hinoResult!;

  }
}
