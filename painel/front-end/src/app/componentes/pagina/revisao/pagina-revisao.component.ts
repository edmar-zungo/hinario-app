import { Component, ChangeDetectionStrategy, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BlocoModel, DivergenciaModel, PaginaModel } from '../pagina-model';
import { PaginaService } from '../service/pagina.service';

/**
 * Ecrã de revisão em três painéis (PRD Fase 6): lista de páginas, foto com
 * zoom e editor de linhas com a estrutura do hino. As correções gravam-se em
 * `blocos` (PUT /linhas); a leitura original nunca é tocada.
 */
@Component({
  selector: 'app-pagina-revisao',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './pagina-revisao.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pagina-revisao.component.css'
})
export class PaginaRevisaoComponent implements OnInit {

  numero = 0;
  pagina?: PaginaModel;
  paginas: PaginaModel[] = [];

  escala = 1;
  mostrarOriginal = false;
  modoTexto = false;
  textoInteiro = '';

  salvando = false;
  mensagem = '';
  erro = '';

  focoBloco = -1;
  focoLinha = -1;

  readonly paleta = ['á', 'é', 'í', 'ó', 'ú', 'â', 'ê', 'ô', 'ã', 'õ', 'ç', 'ò', 'ń', '’'];

  private divergenciasPorLinha = new Map<string, DivergenciaModel>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    protected paginasService: PaginaService
  ) {}

  ngOnInit(): void {
    this.paginasService.listar().subscribe(resp => this.paginas = resp ?? []);
    this.route.paramMap.subscribe(params => {
      const n = Number(params.get('numero'));
      if (Number.isFinite(n) && n > 0) {
        this.carregar(n);
      }
    });
  }

  carregar(numero: number): void {
    this.mensagem = '';
    this.erro = '';
    this.modoTexto = false;
    this.focoBloco = -1;
    this.focoLinha = -1;
    this.paginasService.obter(numero).subscribe({
      next: pagina => {
        this.numero = numero;
        this.pagina = pagina;
        if (!pagina.blocos) {
          pagina.blocos = [];
        }
        this.indexarDivergencias();
        this.router.navigate(['/paginas/revisar', numero], { replaceUrl: true });
      },
      error: () => {
        this.pagina = undefined;
        this.erro = `Página ${numero} não encontrada.`;
      }
    });
  }

  // ---- painel: lista ----

  proxima(): void {
    const seguinte = this.paginas.find(p => p.pagina > this.numero);
    if (seguinte) { this.carregar(seguinte.pagina); }
  }

  anterior(): void {
    const anteriores = this.paginas.filter(p => p.pagina < this.numero);
    if (anteriores.length) { this.carregar(anteriores[anteriores.length - 1].pagina); }
  }

  // ---- painel: foto ----

  ampliar(): void { this.escala = Math.min(4, this.escala + 0.25); }
  reduzir(): void { this.escala = Math.max(0.25, this.escala - 0.25); }

  // ---- painel: editor ----

  blocos(): BlocoModel[] { return this.pagina?.blocos ?? []; }

  tituloBloco(bloco: BlocoModel): string {
    switch (bloco.tipo) {
      case 'titulo_hino':
        return `Hino ${bloco.numeroHino ?? '?'} — ${bloco.titulo ?? ''}`;
      case 'estrofe':
        return `Estrofe ${bloco.numeroEstrofe ?? '?'}`;
      case 'coro':
        return 'Coro';
      default:
        return 'Bloco sem número';
    }
  }

  divergencia(bloco: number, linha: number): DivergenciaModel | undefined {
    return this.divergenciasPorLinha.get(`${bloco}-${linha}`);
  }

  temPontoInterrogacao(texto: string): boolean {
    return texto.includes('[?]');
  }

  focar(bloco: number, linha: number): void {
    this.focoBloco = bloco;
    this.focoLinha = linha;
  }

  inserir(carater: string): void {
    const el = document.activeElement as HTMLInputElement | HTMLTextAreaElement | null;
    if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA')) {
      return;
    }
    const inicio = el.selectionStart ?? el.value.length;
    const fim = el.selectionEnd ?? inicio;
    el.value = el.value.slice(0, inicio) + carater + el.value.slice(fim);
    el.selectionStart = el.selectionEnd = inicio + carater.length;
    el.dispatchEvent(new Event('input'));
  }

  cicloRecuo(): void {
    const linha = this.linhaFocada();
    if (linha) {
      linha.recuo = (linha.recuo + 1) % 3;
    }
  }

  marcarCoro(): void {
    const bloco = this.blocos()[this.focoBloco];
    if (!bloco || bloco.tipo === 'titulo_hino') { return; }
    bloco.tipo = bloco.tipo === 'coro' ? 'bloco_sem_numero' : 'coro';
  }

  entrarEmTextoInteiro(): void {
    this.textoInteiro = this.blocos()
      .flatMap(b => b.linhas)
      .map(l => l.texto)
      .join('\n');
    this.modoTexto = true;
  }

  aplicarTextoInteiro(): void {
    const linhas = this.textoInteiro.split('\n');
    const atuais = this.blocos().flatMap(b => b.linhas);
    if (linhas.length !== atuais.length) {
      this.erro = `O texto inteiro tem ${linhas.length} linhas mas a página tem ` +
        `${atuais.length}. Não junte nem divida linhas: cada linha impressa ` +
        'continua a ser uma linha.';
      return;
    }
    let i = 0;
    for (const bloco of this.blocos()) {
      for (const linha of bloco.linhas) {
        linha.texto = linhas[i++];
      }
    }
    this.erro = '';
    this.modoTexto = false;
  }

  // ---- ações ----

  salvar(): void {
    if (!this.pagina?.blocos?.length) { return; }
    this.salvando = true;
    this.mensagem = '';
    this.erro = '';
    this.paginasService.guardarLinhas(this.numero, this.pagina.blocos).subscribe({
      next: pagina => {
        this.salvando = false;
        this.pagina = pagina;
        this.indexarDivergencias();
        this.mensagem = 'Correções gravadas.';
      },
      error: err => {
        this.salvando = false;
        this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao gravar.';
      }
    });
  }

  confirmar(): void {
    if (!this.pagina?.blocos?.length) { return; }
    this.salvando = true;
    this.mensagem = '';
    this.erro = '';
    this.paginasService.guardarLinhas(this.numero, this.pagina.blocos).subscribe({
      next: gravada => {
        this.pagina = gravada;
        this.paginasService.confirmar(this.numero).subscribe({
          next: confirmada => {
            this.salvando = false;
            this.pagina = confirmada;
            this.mensagem = 'Página confirmada: linhas marcadas como revistas.';
          },
          error: err => {
            this.salvando = false;
            this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao confirmar.';
          }
        });
      },
      error: err => {
        this.salvando = false;
        this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao gravar.';
      }
    });
  }

  estruturar(): void {
    this.paginasService.estruturar(this.numero).subscribe({
      next: pagina => {
        this.pagina = pagina;
        this.indexarDivergencias();
        this.mensagem = 'Estrutura proposta gerada.';
      },
      error: err => {
        this.erro = err?.error?.erro ?? err?.message ?? 'Falha ao estruturar.';
      }
    });
  }

  // ---- atalhos de teclado (PRD Fase 6) ----
  // Ctrl+Enter confirmar · Ctrl+M marcar coro · Ctrl+R recuo
  // Ctrl+→ página seguinte · Ctrl+← anterior

  @HostListener('document:keydown', ['$event'])
  atalhos(evento: KeyboardEvent): void {
    if (!evento.ctrlKey || !this.pagina) { return; }
    const tecla = evento.key;
    if (tecla === 'Enter') {
      evento.preventDefault();
      this.confirmar();
    } else if (tecla === 'm' || tecla === 'M') {
      evento.preventDefault();
      this.marcarCoro();
    } else if (tecla === 'r' || tecla === 'R') {
      evento.preventDefault();
      this.cicloRecuo();
    } else if (tecla === 'ArrowRight') {
      evento.preventDefault();
      this.proxima();
    } else if (tecla === 'ArrowLeft') {
      evento.preventDefault();
      this.anterior();
    }
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

  private linhaFocada() {
    const bloco = this.blocos()[this.focoBloco];
    return bloco?.linhas?.[this.focoLinha];
  }

  private indexarDivergencias(): void {
    this.divergenciasPorLinha = new Map();
    for (const d of this.pagina?.divergencias ?? []) {
      this.divergenciasPorLinha.set(`${d.bloco}-${d.linha}`, d);
    }
  }
}
