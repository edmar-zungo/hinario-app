import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { async } from 'rxjs';

@Component({
  selector: 'app-hinos-favoritos',
  templateUrl: './hinos-favoritos.page.html',
  styleUrls: ['./hinos-favoritos.page.scss'],
  standalone: true,
  imports: [
    IonicModule,
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
