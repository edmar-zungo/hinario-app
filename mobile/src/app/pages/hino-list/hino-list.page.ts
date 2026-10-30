import {
  Component,
  OnInit,
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
  IonContent,
  IonList,
  IonLabel,
  IonMenuButton,
  IonItem,
  IonBadge,
  IonSegment,
  IonSegmentButton,
  IonButton,
  IonSearchbar,
  AlertController,
} from '@ionic/angular';
import { Router, RouterLink } from '@angular/router';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinarioExport, HinoExport } from 'src/app/model/hino-export.model';

@Component({
  selector: 'app-hino-list',
  templateUrl: './hino-list.page.html',
  styleUrls: ['./hino-list.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    IonHeader,
    IonToolbar,
    IonButtons,
    IonTitle,
    IonMenuButton,
    IonContent,
    IonList,
    IonItem,
    IonLabel,
    IonBadge,
    IonSegment,
    IonSegmentButton,
    IonButton,
    IonSearchbar,
  ],
})
export class HinoListPage implements OnInit {
  hinarios: HinarioExport[] = [];
  hinosExibidos: HinoExport[] = [];
  todosHinos: HinoExport[] = [];

  filtro = 'todos';
  carregando = true;

  constructor(
    private router: Router,
    private hinarioService: HinarioService,
    private alertController: AlertController,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.carregarDados();
  }

  carregarDados() {
    this.carregando = true;
    this.hinarioService.getHinarios().subscribe((hinarios) => {
      this.hinarios = hinarios;
      this.todosHinos = hinarios.flatMap((h) => h.hinos);
      this.aplicarFiltro();
      this.carregando = false;
      this.cdr.markForCheck();
    });
  }

  onFiltroChange(event: any) {
    this.filtro = event.detail.value;
    this.aplicarFiltro();
  }

  aplicarFiltro() {
    if (this.filtro === 'todos') {
      this.hinosExibidos = this.todosHinos;
    } else {
      const hinario = this.hinarios.find((h) => h.id === this.filtro);
      this.hinosExibidos = hinario ? hinario.hinos : [];
    }
  }

  /**
   * "Ir ao nº": Abre um diálogo rápido para saltar directamente para o número desejado
   */
  async abrirIrAoNumero() {
    const alert = await this.alertController.create({
      header: 'Ir ao Número',
      subHeader: 'Digite o número do hino para abrir imediatamente:',
      inputs: [
        {
          name: 'numeroHino',
          type: 'number',
          placeholder: 'Ex: 42',
          min: 1,
          max: 999,
        },
      ],
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel',
        },
        {
          text: 'Ir ao Hino',
          handler: (data) => {
            const num = parseInt(data.numeroHino, 10);
            if (!isNaN(num) && num > 0) {
              this.navegarParaNumero(num);
            }
          },
        },
      ],
    });

    await alert.present();
  }

  navegarParaNumero(num: number) {
    // Procura no hinário actual ou em todos
    let candidatos: HinoExport[] = [];
    if (this.filtro !== 'todos') {
      candidatos = this.hinosExibidos.filter((h) => h.numero === num);
    } else {
      candidatos = this.todosHinos.filter((h) => h.numero === num);
    }

    if (candidatos.length === 1) {
      this.router.navigate(['/hino-read', candidatos[0].id]);
    } else if (candidatos.length > 1) {
      // Se houver mais de um hino com este número em línguas diferentes, deixa escolher
      this.mostrarEscolhaDeHino(candidatos);
    } else {
      this.mostrarHinoNaoEncontrado(num);
    }
  }

  async mostrarEscolhaDeHino(candidatos: HinoExport[]) {
    const botoes = candidatos.map((h) => ({
      text: `${h.hinarioNome || h.lingua}: ${h.titulo}`,
      handler: () => {
        this.router.navigate(['/hino-read', h.id]);
      },
    }));

    botoes.push({
      text: 'Cancelar',
      handler: () => {},
    } as any);

    const alert = await this.alertController.create({
      header: 'Escolha o Hinário',
      subHeader: `Hino nº ${candidatos[0].numero} encontrado em:`,
      buttons: botoes,
    });

    await alert.present();
  }

  async mostrarHinoNaoEncontrado(num: number) {
    const alert = await this.alertController.create({
      header: 'Hino Não Encontrado',
      message: `Nenhum hino com o número ${num} foi encontrado no filtro selecionado.`,
      buttons: ['OK'],
    });
    await alert.present();
  }
}
