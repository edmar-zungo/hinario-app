import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PaginaModel } from '../pagina-model';
import { PaginaService } from '../service/pagina.service';
import { HinoModel } from '../../hino/hino-model';
import { HinoService } from '../../hino/service/hino.service';

export interface ItemExtracao {
  pagina: number;
  estado: 'aguardar' | 'extraindo' | 'concluida' | 'erro';
  detalhe?: string;
}

@Component({
  selector: 'app-pagina-list',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pagina-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pagina-list.component.css'
})
export class PaginaListComponent {

  paginas: PaginaModel[] = [];
  hinosRevisados: HinoModel[] = [];
  ficheiros: File[] = [];
  hinarioId = 'kkr';
  rotacao = 0;

  enviando = false;
  mensagem = '';
  erro = '';

  modalAvisoRevisadoAberta = false;
  paginasJaRevisadas: number[] = [];

  modalExtracaoAberta = false;
  extraindo = false;
  faseModal = '';
  itensExtracao: ItemExtracao[] = [];

  readonly hinarios = [
    { codigo: 'por', nome: 'Hinário Português' },
    { codigo: 'kkr', nome: 'Hinário Kikongo (revisado)' },
    { codigo: 'kko', nome: 'Hinário Kikongo (origem)' },
    { codigo: 'kmb', nome: 'Hinário Kimbundu' }
  ];

  constructor(
    private paginaService: PaginaService,
    private hinoService: HinoService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.listar();
  }

  listar() {
    this.paginaService.listar().subscribe({
      next: resp => {
        this.paginas = resp ?? [];
      },
      error: err => {
        this.erro = this.mensagemErro(err, 'Não foi possível carregar as páginas.');
      }
    });
    this.hinoService.getAll().subscribe({
      next: hinos => {
        this.hinosRevisados = (hinos ?? []).filter(h => !!h.revisado);
      }
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

    const paginasRevisadasSet = new Set<number>([
      ...this.paginas.filter(p => p.estado === 'confirmada').map(p => p.pagina),
      ...this.hinosRevisados.map(h => Number(h.pagina)).filter(n => Number.isFinite(n) && n > 0)
    ]);
    const encontradas = new Set<number>();
    for (const f of this.ficheiros) {
      const match = f.name.match(/\d+/);
      if (match) {
        const num = Number(match[0]);
        if (paginasRevisadasSet.has(num)) {
          encontradas.add(num);
        }
      }
    }
    if (encontradas.size > 0) {
      this.paginasJaRevisadas = Array.from(encontradas).sort((a, b) => a - b);
      this.modalAvisoRevisadoAberta = true;
      return;
    }

    this.executarImportacao();
  }

  confirmarImportacaoMesmoAssim() {
    this.modalAvisoRevisadoAberta = false;
    this.paginasJaRevisadas = [];
    this.executarImportacao();
  }

  cancelarImportacaoRevisada() {
    this.modalAvisoRevisadoAberta = false;
    this.paginasJaRevisadas = [];
  }

  private executarImportacao() {
    this.enviando = true;
    this.mensagem = '';
    this.erro = '';
    this.modalExtracaoAberta = true;
    this.extraindo = true;
    this.faseModal = 'A importar imagens…';
    this.itensExtracao = [];

    this.paginaService.importar(this.ficheiros, this.hinarioId, this.rotacao).subscribe({
      next: (resp) => {
        this.enviando = false;
        this.ficheiros = [];
        const input = document.getElementById('ficheiros') as HTMLInputElement | null;
        if (input) { input.value = ''; }
        const importadas = resp ?? [];
        if (!importadas.length) {
          this.extraindo = false;
          this.modalExtracaoAberta = false;
          this.erro = 'Nenhuma página foi importada.';
          return;
        }
        this.itensExtracao = importadas.map(p => ({ pagina: p.pagina, estado: 'aguardar' }));
        this.extrairSequencialmente(0);
      },
      error: (err) => {
        this.enviando = false;
        this.extraindo = false;
        this.modalExtracaoAberta = false;
        this.erro = this.mensagemErro(err, 'Falha ao importar as fotos.');
      }
    });
  }

  ler(numero: number) {
    this.mensagem = '';
    this.erro = '';
    this.modalExtracaoAberta = true;
    this.extraindo = true;
    this.faseModal = `A extrair página ${numero} com Gemini…`;
    this.itensExtracao = [{ pagina: numero, estado: 'aguardar' }];
    this.extrairSequencialmente(0);
  }

  fecharModal() {
    if (this.extraindo) {
      return;
    }
    this.modalExtracaoAberta = false;
  }

  get progressoExtracao(): number {
    if (!this.itensExtracao.length) {
      return this.extraindo ? 10 : 0;
    }
    const concluidos = this.itensExtracao.filter(
      i => i.estado === 'concluida' || i.estado === 'erro'
    ).length;
    const emCurso = this.itensExtracao.some(i => i.estado === 'extraindo') ? 0.5 : 0;
    return Math.round(((concluidos + emCurso) / this.itensExtracao.length) * 100);
  }

  private extrairSequencialmente(indice: number): void {
    if (indice >= this.itensExtracao.length) {
      this.extraindo = false;
      this.faseModal = 'Extração concluída';
      this.listar();
      if (this.itensExtracao.length === 1 && this.itensExtracao[0].estado === 'concluida') {
        const pagina = this.itensExtracao[0].pagina;
        this.modalExtracaoAberta = false;
        this.cdr.detectChanges();
        this.router.navigate(['/paginas/revisar', pagina]);
        return;
      }
      this.cdr.detectChanges();
      return;
    }

    const item = this.itensExtracao[indice];
    item.estado = 'extraindo';
    this.faseModal = `A extrair página ${item.pagina} (${indice + 1} de ${this.itensExtracao.length})…`;

    this.paginaService.ler(item.pagina).subscribe({
      next: resp => {
        if (resp?.estado === 'estruturada' || resp?.estado === 'lida') {
          item.estado = 'concluida';
        } else {
          item.estado = 'erro';
          item.detalhe = resp?.leituras?.gemini?.erro ?? resp?.estado ?? 'Falha na leitura.';
        }
        this.extrairSequencialmente(indice + 1);
      },
      error: err => {
        item.estado = 'erro';
        item.detalhe = this.mensagemErro(err, 'Falha ao extrair a página.');
        this.extrairSequencialmente(indice + 1);
      }
    });
  }

  private mensagemErro(err: any, fallback: string): string {
    return err?.error?.message ?? err?.error?.erro ?? err?.message ?? fallback;
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
