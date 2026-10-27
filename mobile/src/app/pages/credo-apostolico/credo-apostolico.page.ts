import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonToolbar,
  IonButtons,
  IonMenuButton,
  IonTitle,
  IonContent,
} from '@ionic/angular/standalone';

@Component({
  selector: 'app-credo-apostolico',
  templateUrl: './credo-apostolico.page.html',
  styleUrls: ['./credo-apostolico.page.scss'],
  imports: [
    IonContent,
    IonTitle,
    IonButtons,
    IonMenuButton,
    IonToolbar,
    IonHeader,
    FormsModule,
  ],
})
export class CredoApostolicoPage {
  constructor() {}
}
