import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-salmos23',
  templateUrl: './salmos23.page.html',
  styleUrls: ['./salmos23.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class Salmos23Page {

  salmos23 = ["O Senhor é o meu Pastor, nada me faltará. Deitar-me faz em verdes pastos, guia-me mansamente a águas tranquilas.", "Refrigera a minha alma; guia-me pelas veredas da justiça, por amor do seu nome.", "Ainda que eu andasse pelo vale da sombra da morte, não temeria mal algum, porque tu estás comigo; a tua varra e o meu cajado consolam.", "Preparas uma mesa perante a mim na presença dos meus- inimigos, unges a minha cabeça com cabeça com óleo, o meu cálice transborda."]

  constructor() { }

 

}
