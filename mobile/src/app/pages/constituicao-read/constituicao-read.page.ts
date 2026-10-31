import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  IonHeader,
  IonToolbar,
  IonButtons,
  IonTitle,
  IonLabel,
  IonContent,
  IonFooter,
  IonButton,
  IonBackButton,
  IonBadge,
  IonPopover,
  IonList,
  IonItem,
} from '@ionic/angular';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { HinarioService, TemaLeitor } from 'src/app/services/hinario.service';
import { ConstituicaoService } from 'src/app/services/constituicao.service';
import {
  BlocoConteudo,
  TipoBlocoConstituicao,
  UnidadeLeitura,
} from 'src/app/model/constituicao.model';

interface LinhaConteudo {
  nivel: number;
  marcador: string | null;
  texto: string;
  tipo: TipoBlocoConstituicao;
}

@Component({
  selector: 'app-constituicao-read',
  templateUrl: './constituicao-read.page.html',
  styleUrls: ['./constituicao-read.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    RouterLink,
    IonBackButton,
    IonButton,
    IonFooter,
    IonContent,
    IonLabel,
    IonTitle,
    IonButtons,
    IonToolbar,
    IonHeader,
    IonBadge,
    IonPopover,
    IonList,
    IonItem,
  ],
})
export class ConstituicaoReadPage implements OnInit, OnDestroy {
  unidade?: UnidadeLeitura;
  anterior?: UnidadeLeitura;
  seguinte?: UnidadeLeitura;
  linhas: LinhaConteudo[] = [];
  naoEncontrada = false;

  fontSize = 17;
  tema: TemaLeitor = 'claro';

  private routeSub?: Subscription;

  constructor(
    private activatedRoute: ActivatedRoute,
    private constituicaoService: ConstituicaoService,
    private hinarioService: HinarioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.carregarPreferencias();
    this.routeSub = this.activatedRoute.params.subscribe((params) => {
      const unidadeId = params['unidadeId'];
      if (unidadeId) {
        this.carregarUnidade(unidadeId);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  async carregarPreferencias(): Promise<void> {
    this.fontSize = await this.hinarioService.getLeitorFontSize();
    this.tema = await this.hinarioService.getLeitorTema();
    this.cdr.markForCheck();
  }

  carregarUnidade(unidadeId: string): void {
    this.naoEncontrada = false;
    this.constituicaoService.getUnidadeById(unidadeId).subscribe({
      next: (unidade) => {
        this.unidade = unidade;
        this.naoEncontrada = !unidade;
        this.linhas = unidade && unidade.artigo
          ? this.aplantarBlocos(unidade)
          : [];
        if (unidade) {
          this.carregarAdjacentes(unidade.id);
        } else {
          this.anterior = undefined;
          this.seguinte = undefined;
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.naoEncontrada = true;
        this.cdr.markForCheck();
      },
    });
  }

  carregarAdjacentes(unidadeId: string): void {
    this.constituicaoService.getAdjacentes(unidadeId).subscribe((adj) => {
      this.anterior = adj.anterior;
      this.seguinte = adj.seguinte;
      this.cdr.markForCheck();
    });
  }

  irParaUnidade(outra?: UnidadeLeitura): void {
    if (outra) {
      this.router.navigate(['/constituicao-read', outra.id]);
    }
  }

  async aumentarFonte(): Promise<void> {
    if (this.fontSize < 36) {
      this.fontSize += 1.5;
      await this.hinarioService.setLeitorFontSize(this.fontSize);
      this.cdr.markForCheck();
    }
  }

  async diminuirFonte(): Promise<void> {
    if (this.fontSize > 13) {
      this.fontSize -= 1.5;
      await this.hinarioService.setLeitorFontSize(this.fontSize);
      this.cdr.markForCheck();
    }
  }

  async definirTema(novoTema: TemaLeitor): Promise<void> {
    this.tema = novoTema;
    await this.hinarioService.setLeitorTema(novoTema);
    this.cdr.markForCheck();
  }

  private aplantarBlocos(unidade: UnidadeLeitura): LinhaConteudo[] {
    const linhas: LinhaConteudo[] = [];
    const visitar = (bloco: BlocoConteudo, nivel: number) => {
      linhas.push({
        nivel,
        marcador: this.formatarMarcador(bloco.marcador, bloco.tipo),
        texto: bloco.texto,
        tipo: bloco.tipo,
      });
      for (const item of bloco.itens || []) {
        visitar(item, nivel + 1);
      }
    };
    for (const bloco of unidade.artigo?.conteudo || []) {
      visitar(bloco, 0);
    }
    return linhas;
  }

  private formatarMarcador(
    marcador: string | null,
    tipo: TipoBlocoConstituicao
  ): string | null {
    if (!marcador) {
      return null;
    }
    if (tipo === 'numero') {
      return `${marcador}.`;
    }
    if (tipo === 'alinea') {
      return `${marcador})`;
    }
    return marcador;
  }
}
