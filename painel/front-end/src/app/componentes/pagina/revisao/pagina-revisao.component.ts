import { Component, ChangeDetectionStrategy, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { BlocoModel, PaginaModel } from '../pagina-model';
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
  rotacao = 0;
  ajustarLargura = true;
  mostrarOriginal = false;
  modoTexto = false;
  textoInteiro = '';

  salvando = false;
  mensagem = '';
  erro = '';

  focoBloco = -1;
  focoLinha = -1;
  blocosSelecionados = new Set<number>();
  linhasSelecionadas = new Set<string>();

  readonly paleta = ['á', 'é', 'í', 'ó', 'ú', 'â', 'ê', 'ô', 'ã', 'õ', 'ç', 'ò', 'ń', '’'];

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
    this.rotacao = 0;
    this.modoTexto = false;
    this.focoBloco = -1;
    this.focoLinha = -1;
    this.limparSelecao();
    this.paginasService.obter(numero).subscribe({
      next: pagina => {
        this.numero = numero;
        this.pagina = pagina;
        if (!pagina.blocos) {
          pagina.blocos = [];
        }
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

  get temAnterior(): boolean {
    return this.paginas.some(p => p.pagina < this.numero);
  }

  get temProxima(): boolean {
    return this.paginas.some(p => p.pagina > this.numero);
  }

  // ---- painel: foto ----

  rodarEsquerda(): void {
    this.rotacao = (this.rotacao - 90) % 360;
  }

  rodarDireita(): void {
    this.rotacao = (this.rotacao + 90) % 360;
  }

  ampliar(): void {
    this.ajustarLargura = false;
    this.escala = Math.min(4, this.escala + 0.25);
  }

  reduzir(): void {
    this.ajustarLargura = false;
    this.escala = Math.max(0.25, this.escala - 0.25);
  }

  ajustarImagem(): void {
    this.ajustarLargura = true;
    this.escala = 1;
  }

  // ---- painel: editor ----

  blocos(): BlocoModel[] { return this.pagina?.blocos ?? []; }

  get blocasTotalLinhas(): number {
    return this.blocos().reduce((acc, b) => acc + (b.linhas?.length ?? 0), 0);
  }

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

  alternarRecuo(blocoIdx: number, linhaIdx: number): void {
    const linha = this.blocos()[blocoIdx]?.linhas?.[linhaIdx];
    if (linha) {
      linha.recuo = (linha.recuo + 1) % 3;
    }
  }

  marcarCoro(): void {
    const bloco = this.blocos()[this.focoBloco];
    if (!bloco || bloco.tipo === 'titulo_hino') { return; }
    bloco.tipo = bloco.tipo === 'coro' ? 'bloco_sem_numero' : 'coro';
    if (bloco.tipo === 'coro') {
      bloco.numeroEstrofe = null;
    }
  }

  aoMudarTipo(bloco: BlocoModel): void {
    if (bloco.tipo === 'coro') {
      bloco.numeroEstrofe = null;
    }
  }

  alternarSelecaoBloco(bi: number): void {
    if (this.blocosSelecionados.has(bi)) {
      this.blocosSelecionados.delete(bi);
    } else {
      this.blocosSelecionados.add(bi);
    }
  }

  alternarSelecaoLinha(bi: number, li: number): void {
    const chave = `${bi}-${li}`;
    if (this.linhasSelecionadas.has(chave)) {
      this.linhasSelecionadas.delete(chave);
    } else {
      this.linhasSelecionadas.add(chave);
    }
  }

  linhaSelecionada(bi: number, li: number): boolean {
    return this.linhasSelecionadas.has(`${bi}-${li}`);
  }

  definirBlocosSelecionadosComo(tipo: 'coro' | 'estrofe'): void {
    for (const bi of this.blocosSelecionados) {
      const bloco = this.blocos()[bi];
      if (!bloco || bloco.tipo === 'titulo_hino') { continue; }
      bloco.tipo = tipo;
      if (tipo === 'coro') {
        bloco.numeroEstrofe = null;
      }
    }
    this.blocosSelecionados.clear();
  }

  definirBlocoComoCoro(bi: number): void {
    const bloco = this.blocos()[bi];
    if (!bloco || bloco.tipo === 'titulo_hino') { return; }
    bloco.tipo = bloco.tipo === 'coro' ? 'estrofe' : 'coro';
    if (bloco.tipo === 'coro') {
      bloco.numeroEstrofe = null;
    }
  }

  separarLinhasComoCoro(): void {
    if (!this.pagina?.blocos || !this.linhasSelecionadas.size) { return; }
    const novosBlocos: BlocoModel[] = [];

    this.pagina.blocos.forEach((bloco, bi) => {
      if (bloco.tipo === 'titulo_hino' || !bloco.linhas?.length) {
        novosBlocos.push(bloco);
        return;
      }
      const escolhidas = bloco.linhas.filter((_, li) => this.linhaSelecionada(bi, li));
      if (!escolhidas.length) {
        novosBlocos.push(bloco);
        return;
      }
      if (escolhidas.length === bloco.linhas.length) {
        bloco.tipo = 'coro';
        bloco.numeroEstrofe = null;
        novosBlocos.push(bloco);
        return;
      }
      // Preserva a ordem: agrupa fatias contíguas de linhas normais vs. coro
      let grupoAtual: typeof bloco.linhas = [];
      let grupoEhCoro = this.linhaSelecionada(bi, 0);
      let primeiroGrupo = true;
      bloco.linhas.forEach((linha, li) => {
        const ehCoro = this.linhaSelecionada(bi, li);
        if (ehCoro !== grupoEhCoro && grupoAtual.length) {
          novosBlocos.push(this.criarBlocoFatia(bloco, grupoAtual, grupoEhCoro, primeiroGrupo));
          primeiroGrupo = false;
          grupoAtual = [];
        }
        grupoEhCoro = ehCoro;
        grupoAtual.push(linha);
      });
      if (grupoAtual.length) {
        novosBlocos.push(this.criarBlocoFatia(bloco, grupoAtual, grupoEhCoro, primeiroGrupo));
      }
    });

    this.pagina.blocos = novosBlocos;
    this.linhasSelecionadas.clear();
  }

  limparSelecao(): void {
    this.blocosSelecionados.clear();
    this.linhasSelecionadas.clear();
  }

  private criarBlocoFatia(
    origem: BlocoModel,
    linhas: BlocoModel['linhas'],
    ehCoro: boolean,
    manterCabecalho: boolean
  ): BlocoModel {
    if (ehCoro) {
      return {
        tipo: 'coro',
        numeroHino: origem.numeroHino,
        numeroEstrofe: null,
        linhas
      };
    }
    return {
      ...origem,
      numeroEstrofe: manterCabecalho ? origem.numeroEstrofe : null,
      titulo: manterCabecalho ? origem.titulo : null,
      linhas
    };
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
        this.mensagem = 'Correções gravadas e hinário atualizado.';
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
            this.mensagem = 'Página confirmada e hinário atualizado. Exportação disponível.';
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

}
