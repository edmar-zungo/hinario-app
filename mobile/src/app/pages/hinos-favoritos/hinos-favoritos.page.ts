import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonContent, IonToolbar, IonButtons, IonTitle, IonList, IonLabel, IonItem, IonMenuButton } from '@ionic/angular/standalone';
import { HinoModel } from 'src/app/model/hino-model';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-hinos-favoritos',
  templateUrl: './hinos-favoritos.page.html',
  styleUrls: ['./hinos-favoritos.page.scss'],
  standalone: true,
  imports: [IonLabel, IonList, IonTitle, IonButtons, IonToolbar, IonContent, IonHeader,IonMenuButton, IonItem, 
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
  ],
})
export class HinosFavoritosPage implements OnInit {
  
  hinosFavoritos: HinoModel[] = [];

  constructor() {}

  ngOnInit() {
      this.getAllHinos()
  }

  getAllHinos(){
    this.hinosFavoritos = JSON.parse(localStorage.getItem('hinos')!)
  }

  


}
