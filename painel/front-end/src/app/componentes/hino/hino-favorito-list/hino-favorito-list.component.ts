import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { HinoModel } from '../hino-model';
import { HinoService } from '../service/hino.service';

@Component({
    selector: 'app-hino-favorito-list',
    imports: [RouterLink, RouterLinkActive],
    templateUrl: './hino-favorito-list.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './hino-favorito-list.component.css'
})
export class HinoFavoritoListComponent implements OnInit{

  hinos: HinoModel[] = [];

  constructor(protected hinoService: HinoService) {}
  
  
  ngOnInit(): void {
   this.getAllFavoritos();
  }

  getAllFavoritos(){
    this.hinoService.getAllFavoritos().subscribe(resp =>{
      this.hinos = resp;
    })
  }

}
