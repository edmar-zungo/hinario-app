import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-credo-apostolico',
  templateUrl: './credo-apostolico.page.html',
  styleUrls: ['./credo-apostolico.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class CredoApostolicoPage {

  constructor() { }

}
