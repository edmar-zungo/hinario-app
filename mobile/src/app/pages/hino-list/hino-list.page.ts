import { Component, OnInit, Optional } from '@angular/core';
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
  IonFab,
  IonIcon,
  IonMenuButton,
  IonItem,
  IonPopover,
  IonButton,
  IonRouterOutlet,
} from '@ionic/angular/standalone';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Platform } from '@ionic/angular';
import { App } from '@capacitor/app';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';

@Component({
  selector: 'app-hino-list',
  templateUrl: './hino-list.page.html',
  styleUrls: ['./hino-list.page.scss'],
  standalone: true,
  imports: [
    IonButton,
    IonIcon,
    IonFab,
    IonLabel,
    IonList,
    IonContent,
    IonTitle,
    IonButtons,
    IonToolbar,
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    IonHeader,
    IonMenuButton,
    IonItem,
    IonPopover,
    IonRouterOutlet,
  ],
})
export class HinoListPage implements OnInit {
  hinosByJSON: HinoModel[] = [];

  filtro: string | null = null;

  constructor(
    private router: Router,
    private hinarioService: HinarioService,
    private platform: Platform,
    @Optional() private routerOutlet?: IonRouterOutlet
  ) {
    // this.sairDoApp();

    this.platform.backButton.subscribeWithPriority(-1, () => {
      if (!this.routerOutlet!.canGoBack()) {
        if (window.confirm('Deseja realmente sair do app?')) {
          App.exitApp(); // Sair do aplicativo
        }
      }
    });
  }

  ngOnInit() {
    this.getAllhinosByJSON();
  }

  getAllhinosByJSON() {
    this.hinarioService.getAllHinoByJSON().subscribe((resp) => {
      this.filtro = 'todos';
      this.hinosByJSON = resp ?? [];
    });
  }

  goToSearchPage() {
    this.router.navigate(['/searchPage']);
  }

  getAllhinosByLinguaFromJSON(
    lingua: 'PORTUGUES' | 'KIKONGO_REVISADO' | 'KIKONGO_ORIGEM' | 'KINBUMDU'
  ) {
    this.filtro = lingua;
    this.hinarioService.getAllHinoByJSON().subscribe((resp) => {
      this.hinosByJSON = resp.filter((x) => x.lingua === lingua) ?? [];
    });
  }

  // sairDoApp() {
  //   this.platform.ready().then(() => {
  //     this.platform.backButton.subscribeWithPriority(10, () => {
  //       if (this.router.url === '/hinos-list') {
  //         if (window.confirm("Você realmente deseja sair do app?")) {
  //           App.exitApp(); // Sair do aplicativo
  //         }
  //       } else {
  //         this.router.navigate(['/hinos-list']); // Navegar para a página inicial
  //       }
  //     });
  //   });
  // }
}
