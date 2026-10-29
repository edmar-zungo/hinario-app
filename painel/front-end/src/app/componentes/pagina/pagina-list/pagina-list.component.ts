import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PaginaModel } from '../pagina-model';
import { PaginaService } from '../service/pagina.service';
import { FilaService, FilaEstadoModel } from '../service/fila.service';

@Component({
  selector: 'app-pagina-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pagina-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pagina-list.component.css'
})
export class PaginaListComponent implements OnInit {

  paginas: PaginaModel[] = [];
  fila?: FilaEstadoModel;
  ficheiros: File[] = [];
  hinarioId = 'kkr';
  rotacao = 0;

  enviando = false;
  mensagem = '';
  erro = '';

  readonly hinarios = [
    { codigo: 'por', nome: 'Hinário Português' },
    { codigo: 'kkr', nome: 'Hinário Kikongo (revisado)' },
    { codigo: 'kko', nome: 'Hinário Kikongo (origem)' },
    { codigo: 'kmb', nome: 'Hinário Kimbundu' }
  ];

  constructor(private paginaService: PaginaService, private filaService: FilaService) {}

  ngOnInit(): void {
    this.listar();
  }

  listar() {
    this.paginaService.listar().subscribe(resp => {
      this.paginas = resp ?? [];
    });
    this.filaService.estado().subscribe({
      next: estado => this.fila = estado,
      error: () => this.fila = undefined
    });
  }

  aoEscolherFicheiros(evento: Event) {
    const input = evento.target as HTMLInputElement;
    this.ficheiros = input.files ? Array.from(input.files) : [];
    this.mensagem = '';
    this.erro = '';
  }

  importar() {
    if (!this.ficheiros.length) {
      this.erro = 'Escolha pelo menos uma foto.';
      return;
    }
    this.enviando = true;
    this.mensagem = '';
    this.erro = '';

    this.paginaService.importar(this.ficheiros, this.hinarioId, this.rotacao).subscribe({
      next: (resp) => {
        this.enviando = false;
        this.mensagem = `${resp.length} página(s) importada(s).`;
        this.ficheiros = [];
        const input = document.getElementById('ficheiros') as HTMLInputElement | null;
        if (input) { input.value = ''; }
        this.listar();
      },
      error: (err) => {
        this.enviando = false;
        this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao importar as fotos.';
      }
    });
  }

  ler(numero: number) {
    this.mensagem = '';
    this.erro = '';
    this.paginaService.ler(numero).subscribe({
      next: () => {
        this.mensagem = `Página ${numero} na fila de leitura.`;
        this.listar();
      },
      error: (err) => {
        this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao enfileirar.';
      }
    });
  }

  classeEstado(estado: string): string {
    switch (estado) {
      case 'importada': return 'bg-secondary';
      case 'confirmada': return 'bg-success';
      case 'erro_leitura':
      case 'bloqueada_pela_ia': return 'bg-danger';
      case 'pausada_quota':
      case 'em_revisao': return 'bg-warning text-dark';
      default: return 'bg-info text-dark';
    }
  }
}
