import { Component, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonToolbar, IonButtons, IonTitle, IonLabel, IonContent, IonNote, IonFab, IonFabButton, IonFooter, IonToast, IonIcon, IonItemGroup, IonButton, IonBackButton } from '@ionic/angular/standalone';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';
import { EstrofeModel } from 'src/app/model/estrofe-model';
import { ToastController } from '@ionic/angular';

@Component({
  selector: 'app-hino-read',
  templateUrl: './hino-read.page.html',
  styleUrls: ['./hino-read.page.scss'],
  standalone: true,
  imports: [IonBackButton, IonButton, IonFooter, IonFabButton, IonFab, IonNote, IonContent, IonLabel, IonTitle, IonButtons, IonToolbar, IonHeader,IonToast ,IonIcon,IonItemGroup,
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
  ],
})
export class HinoReadPage implements OnInit {


  hino?: HinoModel;

  estrofes: EstrofeModel[] = [];
  liked: boolean = false;

  constructor(
    private activatedRoute: ActivatedRoute,
    private hinarioService: HinarioService,
    private toastController: ToastController,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadHino();
    this.loadEstrofesPorHino();
  }

  loadHino() {
    const hinoId = this.activatedRoute.snapshot.params['hinoId'];

    this.hinarioService.getAllHinoByJSON().subscribe((resp) => {
      this.hino = resp.find((x) => x.id === hinoId);
      var hinos: HinoModel[] = JSON.parse(localStorage.getItem('hinos')!);
      if (hinos.find((x) => x.id === this.hino!.id)) {
        this.liked = true;
      } else {
        this.liked = false;
      }
    });


  }

  loadEstrofesPorHino() {
    const hinoId = this.activatedRoute.snapshot.params['hinoId'];

    this.hinarioService.getAllEstrofesByJSON().subscribe((resp) => {
      this.estrofes = resp.filter((x) => x.hino?.id === hinoId) ?? [];
    });
  }

  addOuRemoveDosFavoritos(hinoId: string | undefined, action: 'add' | 'rem') {
    this.hinarioService.addOuRemoveDosFavoritos(hinoId);

    this.createTost(action);
    this.loadHino();
  }

  async createTost(action: 'add' | 'rem') {
    var toast;

    switch (action) {
      case 'add':
        toast = await this.toastController.create({
          message: 'Adicionado aos Favoritos',
          color: 'primary',
          duration: 500,
          position: 'bottom'
        });
        break;
      case 'rem':
        toast = await this.toastController.create({
          message: 'Removido dos Favoritos',
          color: 'danger',
          duration: 500,
          position: 'bottom'
        });
        break;
    }

    await toast.present();
  }

  getHinoSeguinte(hinoId: string | undefined){
    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      const hinoResult = resp.find(x => x.id === hinoId);
      const indexHino = resp.indexOf( hinoResult!);
      const hinoAseguir = resp[indexHino + 1];
      this.router.navigate(['/hino-read/', hinoAseguir.id])

    })
  }
  getHinoAnterior(hinoId: string | undefined){
    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      const hinoResult = resp.find(x => x.id === hinoId);
      const indexHino = resp.indexOf( hinoResult!);
      const hinoAnterior = resp[indexHino - 1];
      this.router.navigate(['/hino-read/', hinoAnterior.id])

    })
  }

  aumentarTamanhoFonte() {
    const paragrafos = document.querySelectorAll("p");
    const titulo = document.querySelectorAll("h4");

    paragrafos.forEach(p => {
      const tamanhoActual = window.getComputedStyle(p).fontSize;
      const novoTamanhoDeFont = parseFloat(tamanhoActual) + 1;

      p.style.fontSize = novoTamanhoDeFont + "px"
    });

    titulo.forEach(h => {
      const tamanhoActual = window.getComputedStyle(h).fontSize;
      const novoTamanhoDeFont = parseFloat(tamanhoActual) + 1;

      h.style.fontSize = novoTamanhoDeFont + "px"
    });
    
  }


  diminuirTamanhoFonte() {
    const paragrafos = document.querySelectorAll("p");
    const titulo = document.querySelectorAll("h4");
    paragrafos.forEach(p => {
      const tamanhoActual = window.getComputedStyle(p).fontSize;
      const novoTamanhoDeFont = parseFloat(tamanhoActual) - 1;

      p.style.fontSize = novoTamanhoDeFont + "px"
    });

    titulo.forEach(h => {
      const tamanhoActual = window.getComputedStyle(h).fontSize;
      const novoTamanhoDeFont = parseFloat(tamanhoActual) - 1;

      h.style.fontSize = novoTamanhoDeFont + "px"
    });
    
  }
}
