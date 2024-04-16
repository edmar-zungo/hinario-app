import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonToolbar, IonButtons,IonMenuButton, IonContent, IonTitle } from '@ionic/angular/standalone';
import { Mandamento } from 'src/app/model/mandamentos';

@Component({
  selector: 'app-dez-mandamentos',
  templateUrl: './dez-mandamentos.page.html',
  styleUrls: ['./dez-mandamentos.page.scss'],
  standalone: true,
  imports: [IonTitle, IonContent, IonButtons,IonMenuButton, IonToolbar, IonHeader, CommonModule, FormsModule],
})
export class DezMandamentosPage {
  dezMandamentos: Mandamento[] = [
    { numero: 1, descricao: 'Não terás outros deuses diante de mim;' },
    { numero: 2, descricao: 'Não farás para ti imagens de escultura;' },
    { numero: 3, descricao: 'Não tomarás o nome do Senhor teu Deus em vão;' },
    { numero: 4, descricao: 'Lembra-te do dia do sábado para o santificar;' },
    { numero: 5, descricao: 'Honra o teu pai e a tua mãe para que se prolongue os teus dias na terra;' },
    { numero: 6, descricao: 'Não matarás;' },
    { numero: 7, descricao: 'Não adulterarás;' },
    { numero: 8, descricao: 'Não furtarás;' },
    { numero: 9, descricao: 'Não dirás falsos testemunhos contra o teu próximo;' },
    { numero: 10, descricao: 'Não cobiçarás a casa do teu próximo.' },
  ];

  constructor() {}
}
