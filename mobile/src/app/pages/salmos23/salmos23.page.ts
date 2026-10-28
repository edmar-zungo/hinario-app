import { Component, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonToolbar,
  IonButtons,
  IonTitle,
  IonContent,
  IonMenuButton,
} from '@ionic/angular';

@Component({
  selector: 'app-salmos23',
  templateUrl: './salmos23.page.html',
  styleUrls: ['./salmos23.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IonContent,
    IonTitle,
    IonButtons,
    IonToolbar,
    IonHeader,
    IonMenuButton,
    FormsModule,
  ],
})
export class Salmos23Page {
  salmos23 = [
    'O SENHOR é o meu pastor, nada me faltará.',
    'Deitar-me faz em verdes pastos, guia-me mansamente a águas tranquilas.',
    'Refrigera a minha alma; guia-me pelas veredas da justiça, por amor do seu nome.',
    'Ainda que eu andasse pelo vale da sombra da morte, não temeria mal algum, porque tu estás comigo; a tua varra e o teu cajado me consolam.',
    'Preparas uma mesa perante a mim na presença dos meus inimigos, unges a minha cabeça com óleo e o meu cálice transborda.',
    'Certamente que a bondade e a misericórdia me seguirão todos os dias da minha vida; e habitarei na casa do Senhor por longos dias.',
  ];

}
