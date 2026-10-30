import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonToolbar,
  IonButtons,
  IonTitle,
  IonLabel,
  IonContent,
  IonNote,
  IonFooter,
  IonButton,
  IonBackButton,
  IonBadge,
  IonPopover,
  IonList,
  IonItem,
  ToastController,
} from '@ionic/angular';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { HinarioService, TemaLeitor } from 'src/app/services/hinario.service';
import { HinoExport } from 'src/app/model/hino-export.model';

@Component({
  selector: 'app-hino-read',
  templateUrl: './hino-read.page.html',
  styleUrls: ['./hino-read.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    IonBackButton,
    IonButton,
    IonFooter,
    IonNote,
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
    FormsModule,
  ],
})
export class HinoReadPage implements OnInit, OnDestroy {
  hino?: HinoExport;
  anterior?: HinoExport;
  seguinte?: HinoExport;

  liked = false;
  fontSize = 19;
  tema: TemaLeitor = 'claro';

  private routeSub?: Subscription;
  private favSub?: Subscription;

  constructor(
    private activatedRoute: ActivatedRoute,
    private hinarioService: HinarioService,
    private toastController: ToastController,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.carregarPreferencias();

    this.routeSub = this.activatedRoute.params.subscribe((params) => {
      const hinoId = params['hinoId'];
      if (hinoId) {
        this.carregarHino(hinoId);
      }
    });

    this.favSub = this.hinarioService.favoritos$.subscribe((favIds) => {
      if (this.hino) {
        this.liked = favIds.includes(this.hino.id);
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
    this.favSub?.unsubscribe();
  }

  async carregarPreferencias() {
    this.fontSize = await this.hinarioService.getLeitorFontSize();
    this.tema = await this.hinarioService.getLeitorTema();
    this.cdr.markForCheck();
  }

  carregarHino(hinoId: string) {
    this.hinarioService.getHinoById(hinoId).subscribe(async (hino) => {
      this.hino = hino;
      if (hino) {
        this.liked = await this.hinarioService.isFavorito(hino.id);
        this.carregarAdjacentes(hino.id);
      }
      this.cdr.markForCheck();
    });
  }

  carregarAdjacentes(hinoId: string) {
    this.hinarioService.getAdjacentHinos(hinoId).subscribe((adj) => {
      this.anterior = adj.anterior;
      this.seguinte = adj.seguinte;
      this.cdr.markForCheck();
    });
  }

  async alternarFavorito() {
    if (!this.hino) return;
    const novoEstado = await this.hinarioService.toggleFavorito(this.hino.id);
    this.liked = novoEstado;

    const toast = await this.toastController.create({
      message: novoEstado ? 'Adicionado aos Favoritos' : 'Removido dos Favoritos',
      color: novoEstado ? 'success' : 'medium',
      duration: 1200,
      position: 'bottom',
    });
    await toast.present();
    this.cdr.markForCheck();
  }

  irParaHino(outroHino?: HinoExport) {
    if (outroHino) {
      this.router.navigate(['/hino-read', outroHino.id]);
    }
  }

  async aumentarFonte() {
    if (this.fontSize < 36) {
      this.fontSize += 1.5;
      await this.hinarioService.setLeitorFontSize(this.fontSize);
      this.cdr.markForCheck();
    }
  }

  async diminuirFonte() {
    if (this.fontSize > 13) {
      this.fontSize -= 1.5;
      await this.hinarioService.setLeitorFontSize(this.fontSize);
      this.cdr.markForCheck();
    }
  }

  async definirTema(novoTema: TemaLeitor) {
    this.tema = novoTema;
    await this.hinarioService.setLeitorTema(novoTema);
    this.cdr.markForCheck();
  }
}
