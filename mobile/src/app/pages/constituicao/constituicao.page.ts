import { Component, ChangeDetectionStrategy } from '@angular/core';
import {
  IonHeader,
  IonToolbar,
  IonTitle,
  IonButtons,
  IonMenuButton,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardSubtitle,
  IonCardContent,
  IonBadge,
  IonIcon,
  IonList,
  IonItem,
  IonLabel,
} from '@ionic/angular';

@Component({
  selector: 'app-constituicao',
  templateUrl: './constituicao.page.html',
  styleUrls: ['./constituicao.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IonHeader,
    IonToolbar,
    IonTitle,
    IonButtons,
    IonMenuButton,
    IonContent,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonBadge,
    IonIcon,
    IonList,
    IonItem,
    IonLabel,
  ],
})
export class ConstituicaoPage {
  estruturaPrevista = [
    { titulo: 'Parte I — Natureza, Princípios e Fé', artigos: 'Artigos 1 a 12' },
    { titulo: 'Parte II — Estrutura e Governo da Igreja', artigos: 'Artigos 13 a 35' },
    { titulo: 'Parte III — Ministérios e Ordenação', artigos: 'Artigos 36 a 58' },
    { titulo: 'Parte IV — Património e Finanças', artigos: 'Artigos 59 a 70' },
    { titulo: 'Apêndice — Emendas Constitucionais', artigos: 'Disposições Finais' },
  ];
}
