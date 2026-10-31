import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  IonSearchbar,
  IonList,
  IonItem,
  IonLabel,
  IonAccordion,
  IonAccordionGroup,
  IonSpinner,
} from '@ionic/angular';
import { RouterLink } from '@angular/router';
import {
  Constituicao,
  ResultadoPesquisaConstituicao,
} from 'src/app/model/constituicao.model';
import { ConstituicaoService } from 'src/app/services/constituicao.service';

@Component({
  selector: 'app-constituicao',
  templateUrl: './constituicao.page.html',
  styleUrls: ['./constituicao.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
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
    IonSearchbar,
    IonList,
    IonItem,
    IonLabel,
    IonAccordion,
    IonAccordionGroup,
    IonSpinner,
  ],
})
export class ConstituicaoPage implements OnInit {
  constituicao: Constituicao | null = null;
  carregando = true;
  erro = false;
  termo = '';
  pesquisando = false;
  resultados: ResultadoPesquisaConstituicao[] = [];
  totalCapitulos = 0;

  constructor(
    private constituicaoService: ConstituicaoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.constituicaoService.getConstituicao().subscribe({
      next: (c) => {
        this.constituicao = c;
        this.erro = c === null;
        this.carregando = false;
        if (c) {
          this.totalCapitulos = c.partes.reduce(
            (n, p) => n + p.capitulos.length,
            0
          );
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.erro = true;
        this.carregando = false;
        this.cdr.markForCheck();
      },
    });
  }

  onSearchInput(event: Event): void {
    const alvo = event.target as HTMLIonSearchbarElement | null;
    const query = (alvo?.value || '').toString().trim();
    this.termo = query;

    if (!query) {
      this.resultados = [];
      this.pesquisando = false;
      this.cdr.markForCheck();
      return;
    }

    this.pesquisando = true;
    this.constituicaoService.pesquisar(query).subscribe({
      next: (res) => {
        this.resultados = res;
        this.pesquisando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.resultados = [];
        this.pesquisando = false;
        this.cdr.markForCheck();
      },
    });
  }

  limparPesquisa(): void {
    this.termo = '';
    this.resultados = [];
    this.pesquisando = false;
    this.cdr.markForCheck();
  }
}
