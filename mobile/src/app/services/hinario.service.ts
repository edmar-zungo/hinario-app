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

  getAllHinosFavoritos(): HinoModel[] {
    return JSON.parse(localStorage.getItem('hinos')!);
  }

  addOuRemoveDosFavoritos(hinoId: string | undefined) {
    var hinoResult: HinoModel;
    this.getAllHinoByJSON().subscribe((resp) => {
      hinoResult = resp.find((x) => x.id === hinoId)!;
      if (!localStorage.getItem('hinos')) {
        localStorage.setItem('hinos', JSON.stringify([]));
        this.hinosFavoritos.push(hinoResult);
        localStorage.setItem('hinos', JSON.stringify(this.hinosFavoritos));
      } else {
        this.hinosFavoritos = JSON.parse(
          localStorage.getItem('hinos' || '[]')!
        );
        if (this.hinosFavoritos.find((x) => x.id === hinoResult.id)) {
          const hinoFounded = this.hinosFavoritos.find(
            (x) => x.id === hinoResult.id
          );
          this.hinosFavoritos.splice(
            this.hinosFavoritos.indexOf(hinoFounded!),
            1
          );
          
          localStorage.setItem('hinos', JSON.stringify(this.hinosFavoritos));
          this.getAllHinosFavoritos();
        } else {
          this.hinosFavoritos.push(hinoResult);
          localStorage.setItem('hinos', JSON.stringify(this.hinosFavoritos));
          this.getAllHinosFavoritos();
        }
      }

      // console.log(this.hinosFavoritos);
    });
  }
}
