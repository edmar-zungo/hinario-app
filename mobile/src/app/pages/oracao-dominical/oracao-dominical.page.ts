import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonToolbar,
  IonButtons,
  IonContent,
  IonTitle,
  IonMenuButton,
} from '@ionic/angular/standalone';

@Component({
  selector: 'app-oracao-dominical',
  templateUrl: './oracao-dominical.page.html',
  styleUrls: ['./oracao-dominical.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IonTitle,
    IonContent,
    IonButtons,
    IonMenuButton,
    IonToolbar,
    IonHeader,
    FormsModule,
  ],
})
export class OracaoDominicalPage {
  constructor() {}
}
