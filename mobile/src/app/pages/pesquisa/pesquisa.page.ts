import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader,
  IonSearchbar,
  IonToolbar,
  IonContent,
  IonLabel,
  IonItem,
  IonList,
  IonSegment,
  IonSegmentButton,
  IonBadge,
  IonButtons,
  IonMenuButton,
  IonButton,
} from '@ionic/angular';
import { Router, RouterLink } from '@angular/router';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoExport } from 'src/app/model/hino-export.model';

import { Subject, Subscription, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

@Component({
  selector: 'app-pesquisa',
  templateUrl: './pesquisa.page.html',
  styleUrls: ['./pesquisa.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonSearchbar,
    IonContent,
    IonLabel,
    IonItem,
    IonList,
    IonSegment,
    IonSegmentButton,
    IonBadge,
    IonButtons,
    IonMenuButton,
    IonButton,
  ],
})
export class PesquisaPage {
  hinosResult: HinoExport[] = [];
  termoPesquisa = '';
  filtroLingua = 'todos';
  numeroDigitado: number | null = null;
  pesquisando = false;

  private searchSubject = new Subject<{ termo: string; lingua: string }>();
  private sub?: Subscription;

  constructor(
    private hinarioService: HinarioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.sub = this.searchSubject
      .pipe(
        debounceTime(150),
        distinctUntilChanged((a, b) => a.termo === b.termo && a.lingua === b.lingua),
        switchMap(({ termo, lingua }) => {
          if (!termo) {
            this.pesquisando = false;
            this.hinosResult = [];
            this.cdr.markForCheck();
            return [];
          }
          this.pesquisando = true;
          this.cdr.markForCheck();
          return this.hinarioService.pesquisar(termo, lingua);
        })
      )
      .subscribe((resp) => {
        this.hinosResult = resp;
        this.pesquisando = false;
        this.cdr.markForCheck();
      });
  }

  onSearchInput(event: any) {
    const query = event.target.value?.toString() || '';
    this.termoPesquisa = query.trim();

    // Atalho directo "Ir ao nº" se foi digitado exclusivamente um número
    const parsedNum = parseInt(this.termoPesquisa, 10);
    if (!isNaN(parsedNum) && /^\d+$/.test(this.termoPesquisa)) {
      this.numeroDigitado = parsedNum;
    } else {
      this.numeroDigitado = null;
    }

    this.searchSubject.next({
      termo: this.termoPesquisa,
      lingua: this.filtroLingua,
    });
  }

  onFiltroLinguaChange(event: any) {
    this.filtroLingua = event.detail.value;
    if (this.termoPesquisa) {
      this.searchSubject.next({
        termo: this.termoPesquisa,
        lingua: this.filtroLingua,
      });
    }
  }

  limparPesquisa() {
    this.termoPesquisa = '';
    this.numeroDigitado = null;
    this.hinosResult = [];
    this.pesquisando = false;
    this.cdr.markForCheck();
  }

  irDirectoAoNumero(num: number) {
    this.hinarioService
      .buscarPorNumero(num, this.filtroLingua)
      .subscribe((hinos) => {
        if (hinos.length === 1) {
          this.router.navigate(['/hino-read', hinos[0].id]);
        } else if (hinos.length > 0) {
          this.hinosResult = hinos;
          this.cdr.markForCheck();
        }
      });
  }
}
