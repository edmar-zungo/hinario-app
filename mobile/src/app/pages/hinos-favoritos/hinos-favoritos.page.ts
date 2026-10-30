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
  IonContent,
  IonToolbar,
  IonButtons,
  IonTitle,
  IonList,
  IonLabel,
  IonItem,
  IonMenuButton,
  IonButton,
  IonBadge,
} from '@ionic/angular';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoExport } from 'src/app/model/hino-export.model';

@Component({
  selector: 'app-hinos-favoritos',
  templateUrl: './hinos-favoritos.page.html',
  styleUrls: ['./hinos-favoritos.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    IonHeader,
    IonContent,
    IonToolbar,
    IonButtons,
    IonTitle,
    IonList,
    IonLabel,
    IonItem,
    IonMenuButton,
    IonButton,
    IonBadge,
  ],
})
export class HinosFavoritosPage implements OnInit, OnDestroy {
  hinosFavoritos: HinoExport[] = [];
  carregando = true;

  private favSub?: Subscription;

  constructor(
    private hinarioService: HinarioService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.carregarFavoritos();
  }

  ngOnDestroy() {
    this.favSub?.unsubscribe();
  }

  carregarFavoritos() {
    this.carregando = true;
    this.favSub = this.hinarioService.favoritos$.subscribe(() => {
      this.hinarioService.getHinosFavoritos().subscribe((hinos) => {
        this.hinosFavoritos = hinos;
        this.carregando = false;
        this.cdr.markForCheck();
      });
    });
  }

  removerFavorito(event: Event, hinoId: string) {
    event.stopPropagation();
    event.preventDefault();
    this.hinarioService.toggleFavorito(hinoId);
  }
}
