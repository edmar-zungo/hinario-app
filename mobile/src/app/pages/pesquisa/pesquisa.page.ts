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

  constructor(
    private hinarioService: HinarioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onSearchInput(event: any) {
    const query = event.target.value?.toString() || '';
    this.termoPesquisa = query.trim();

    // Verifica se digitou apenas um número para atalho "Ir ao nº"
    const parsedNum = parseInt(this.termoPesquisa, 10);
    if (!isNaN(parsedNum) && /^\d+$/.test(this.termoPesquisa)) {
      this.numeroDigitado = parsedNum;
    } else {
      this.numeroDigitado = null;
    }

    if (!this.termoPesquisa) {
      this.hinosResult = [];
      this.pesquisando = false;
      this.cdr.markForCheck();
      return;
    }

    this.pesquisando = true;
    this.hinarioService
      .pesquisar(this.termoPesquisa, this.filtroLingua)
      .subscribe((resp) => {
        this.hinosResult = resp;
        this.pesquisando = false;
        this.cdr.markForCheck();
      });
  }

  onFiltroLinguaChange(event: any) {
    this.filtroLingua = event.detail.value;
    if (this.termoPesquisa) {
      this.onSearchInput({ target: { value: this.termoPesquisa } });
    }
  }

  limparPesquisa() {
    this.termoPesquisa = '';
    this.numeroDigitado = null;
    this.hinosResult = [];
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
