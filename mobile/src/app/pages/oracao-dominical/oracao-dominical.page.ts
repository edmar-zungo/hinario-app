import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonToolbar, IonButtons, IonContent, IonTitle, IonMenuButton } from '@ionic/angular/standalone';

@Component({
  selector: 'app-oracao-dominical',
  templateUrl: './oracao-dominical.page.html',
  styleUrls: ['./oracao-dominical.page.scss'],
  standalone: true,
  imports: [IonTitle, IonContent, IonButtons,IonMenuButton, IonToolbar, IonHeader, CommonModule, FormsModule]
})
export class OracaoDominicalPage{

  constructor() { }

}
