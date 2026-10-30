import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ResultadoExportacaoModel } from '../exportacao-model';
import { ExportacaoService } from '../service/exportacao.service';

@Component({
  selector: 'app-exportar',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './exportar.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './exportar.component.css'
})
export class ExportarComponent {

  hinarioId = '';
  resultados: ResultadoExportacaoModel[] = [];
  exportando = false;
  erro = '';

  readonly hinarios = [
    { codigo: '', nome: 'Todos os hinários' },
    { codigo: 'por', nome: 'Hinário Português' },
    { codigo: 'kkr', nome: 'Hinário Kikongo (revisado)' },
    { codigo: 'kko', nome: 'Hinário Kikongo (origem)' },
    { codigo: 'kmb', nome: 'Hinário Kimbundu' }
  ];

  constructor(private exportacaoService: ExportacaoService) {}

  exportar() {
    this.exportando = true;
    this.erro = '';
    this.exportacaoService.exportar(this.hinarioId || undefined).subscribe({
      next: (resultados) => {
        this.exportando = false;
        this.resultados = resultados ?? [];
      },
      error: (err) => {
        this.exportando = false;
        this.erro = err?.error?.erro ?? err?.error?.message ?? err?.message
          ?? 'Falha ao exportar.';
      }
    });
  }
}
