import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonToolbar, IonButtons,IonMenuButton, IonTitle, IonContent } from '@ionic/angular/standalone';

@Component({
  selector: 'app-credo-apostolico',
  templateUrl: './credo-apostolico.page.html',
  styleUrls: ['./credo-apostolico.page.scss'],
  standalone: true,
  imports: [IonContent, IonTitle, IonButtons,IonMenuButton, IonToolbar, IonHeader, CommonModule, FormsModule]
})
export class CredoApostolicoPage {

  constructor() { }

}
