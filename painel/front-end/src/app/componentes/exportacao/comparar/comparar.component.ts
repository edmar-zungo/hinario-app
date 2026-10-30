import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HinoModel } from '../../hino/hino-model';
import { HinoService } from '../../hino/service/hino.service';
import { ComparacaoHinoModel } from '../exportacao-model';
import { ExportacaoService } from '../service/exportacao.service';

@Component({
  selector: 'app-comparar',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './comparar.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './comparar.component.css'
})
export class CompararComponent implements OnInit {

  hinos: HinoModel[] = [];
  hinoId = '';
  comparacao?: ComparacaoHinoModel;
  comparando = false;
  erro = '';

  constructor(private hinoService: HinoService,
              private exportacaoService: ExportacaoService) {}

  ngOnInit(): void {
    this.hinoService.getAll().subscribe({
      next: (hinos) => this.hinos = hinos ?? [],
      error: () => this.erro = 'Falha ao carregar os hinos.'
    });
  }

  comparar() {
    if (!this.hinoId) {
      this.erro = 'Escolha um hino.';
      return;
    }
    this.comparando = true;
    this.erro = '';
    this.comparacao = undefined;
    this.exportacaoService.comparar(this.hinoId).subscribe({
      next: (comparacao) => {
        this.comparando = false;
        this.comparacao = comparacao;
      },
      error: (err) => {
        this.comparando = false;
        this.erro = err?.error?.erro ?? err?.error?.message ?? err?.message
          ?? 'Falha ao comparar.';
      }
    });
  }

  classeTipo(tipo: string): string {
    switch (tipo) {
      case 'divergente': return 'bg-danger';
      case 'so_acento': return 'bg-warning text-dark';
      case 'so_pontuacao': return 'bg-info text-dark';
      case 'so_na_leitura': return 'bg-secondary';
      case 'so_no_novo': return 'bg-primary';
      default: return 'bg-light text-dark';
    }
  }

  legenda(tipo: string): string {
    switch (tipo) {
      case 'divergente': return 'texto diferente';
      case 'so_acento': return 'só acento';
      case 'so_pontuacao': return 'só pontuação';
      case 'so_na_leitura': return 'só no actual';
      case 'so_no_novo': return 'só na leitura';
      default: return tipo;
    }
  }

  nomeLingua(lingua: string): string {
    switch (lingua) {
      case 'PORTUGUES': return 'por';
      case 'KIKONGO_REVISADO': return 'kkr';
      case 'KIKONGO_ORIGEM': return 'kko';
      case 'KINBUMDU': return 'kmb';
      default: return lingua;
    }
  }
}
