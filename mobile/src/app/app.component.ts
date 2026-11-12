import { Component, ChangeDetectionStrategy, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import {
  IonApp,
  IonSplitPane,
  IonMenu,
  IonContent,
  IonList,
  IonListHeader,
  IonNote,
  IonMenuToggle,
  IonItem,
  IonLabel,
  IonRouterOutlet,
  IonTitle,
  IonHeader,
  IonFooter,
  IonToolbar,
} from '@ionic/angular';
import { HinarioService } from './services/hinario.service';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    IonToolbar,
    IonFooter,
    IonHeader,
    IonTitle,
    RouterLink,
    RouterLinkActive,
    IonApp,
    IonSplitPane,
    IonMenu,
    IonContent,
    IonList,
    IonListHeader,
    IonNote,
    IonMenuToggle,
    IonItem,
    IonLabel,
    IonRouterOutlet,
  ],
})
export class AppComponent implements OnInit {
  private hinarioService = inject(HinarioService);

  ngOnInit() {
    this.hinarioService.tema$.subscribe((tema) => {
      if (typeof document !== 'undefined') {
        const body = document.body;
        body.classList.remove('tema-claro', 'tema-sepia', 'tema-escuro');
        body.classList.add(`tema-${tema}`);

        // Define dark class para compatibilidade nativa com Ionic
        if (tema === 'escuro') {
          body.classList.add('dark');
        } else {
          body.classList.remove('dark');
        }
      }
    });
  }
}

