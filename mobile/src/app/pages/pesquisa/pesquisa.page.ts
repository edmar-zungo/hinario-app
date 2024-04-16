import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonSearchbar,
  IonToolbar,
  IonContent,
  IonLabel,
  IonBackButton,
  IonItem,
} from '@ionic/angular/standalone';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-pesquisa',
  templateUrl: './pesquisa.page.html',
  styleUrls: ['./pesquisa.page.scss'],
  standalone: true,
  imports: [
    IonBackButton,
    IonLabel,
    IonContent,
    IonToolbar,
    IonSearchbar,
    IonHeader,
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    IonItem,
  ],
})
export class PesquisaPage {
  hinosResult: HinoModel[] = [];
  autoFocus: boolean = true;

  constructor(private hinarioService: HinarioService) {}

  setVezio() {
    this.hinosResult = [];
  }

  pesquisaOnJSON(query: string | undefined) {
    this.hinarioService.getAllHinoByJSON().subscribe((resp) => {
      this.hinosResult =
        resp.filter(
          (x) =>
            x.titulo.includes(query!) ||
            x.titulo.toUpperCase().includes(query!.toUpperCase()) ||
            x.numero.toString().includes(query!) ||
            x.pagina.toString().includes(query!)
        ) ?? [];
    });
  }
}
