import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';
import { EstrofeModel } from 'src/app/model/estrofe-model';

@Component({
  selector: 'app-hino-read',
  templateUrl: './hino-read.page.html',
  styleUrls: ['./hino-read.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule, RouterLink, RouterLinkActive]
})
export class HinoReadPage implements OnInit {

  hino?: HinoModel;

  estrofes: EstrofeModel[] = [];
  liked: boolean = false;

  constructor(private activatedRoute: ActivatedRoute, private hinarioService: HinarioService) { }

  ngOnInit() {
    this.loadHino();
    this.loadEstrofesPorHino();
  }

  loadHino(){
    const hinoId = this.activatedRoute.snapshot.params['hinoId'];
 
    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      this.hino = resp.find(x => x.id === hinoId);
    })
  }

  loadEstrofesPorHino(){
    const hinoId = this.activatedRoute.snapshot.params['hinoId']
    
      this.hinarioService.getAllEstrofesByJSON().subscribe(resp => {
        this.estrofes = resp.filter(x => x.hino?.id === hinoId ) ?? [];
      } )

  }

  // addOuRemoveDosFavoritos(hinoId: string | undefined) {
  //   this.hinarioService.getAllHinoByJSON().subscribe((resp) => {
  //     const hino = resp.find((x) => x.id === hinoId);
  //     this.hinarioService.getAllHinoByJSON();
  //     if (hino?.isFavorito) {
  //       hino.isFavorito = false;
  //       this.hinarioService.actualizaHinoOnJSON(hino!);
  //       this.hinarioService.addFavoritos(hino);
        
  //       this.liked = false;
  //     } else {
  //       hino!.isFavorito = true;
  //       this.hinarioService.actualizaHinoOnJSON(hino!);
  //       this.liked = true;
  //       this.hinosFavoritos.push(hino!);
  //     }
  //   });
  // }

  addOuRemoveDosFavoritos(hinoId: string | undefined) {

    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      const hinoResult = resp.find(x => x.id === hinoId)!;
    })
    
  }

}
