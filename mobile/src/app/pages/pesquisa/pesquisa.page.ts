import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule, SearchbarInputEventDetail } from '@ionic/angular';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { IonSearchbarCustomEvent } from '@ionic/core';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-pesquisa',
  templateUrl: './pesquisa.page.html',
  styleUrls: ['./pesquisa.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule, RouterLink, RouterLinkActive]
})
export class PesquisaPage{


  hinosResult: HinoModel[] = [];

  constructor(private hinarioService: HinarioService) { }

 setVezio() {
  this.hinosResult = [];
  }

  pesquisaOnJSON(query: string | undefined){
    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      this.hinosResult = resp.filter(x => x.titulo.includes(query!) || x.titulo.toLocaleUpperCase().includes(query!.toLocaleUpperCase()) || x.numero.toString().includes(query!) || x.pagina.toString().includes(query!)) ?? [];
    })
    
  }

}
