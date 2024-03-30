import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-oracao-dominical',
  templateUrl: './oracao-dominical.page.html',
  styleUrls: ['./oracao-dominical.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class OracaoDominicalPage implements OnInit {

  constructor() { }

  ngOnInit() {
  }

}
