import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { HinoService } from '../service/hino.service';
import { HinoModel } from '../hino-model';
import { EstrofeListComponent } from '../../estrofe/estrofe-list/estrofe-list.component';
import { EstrofeModel } from '../../estrofe/estrofe-model';
import { EstrofeService } from '../../estrofe/service/estrofe.service';

@Component({
  selector: 'app-hino-details',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, EstrofeListComponent],
  templateUrl: './hino-details.component.html',
  styleUrl: './hino-details.component.css'
})
export class HinoDetailsComponent implements OnInit{


  hino?: HinoModel;
  estrofes: EstrofeModel[] = [];
  

  constructor(private hinoService: HinoService, private activetedRoute: ActivatedRoute, private estrofeService: EstrofeService){}

  ngOnInit(): void {

    this.loadHino();
    this.loadEstrofesPorHino()
  }


  back() {
    window.history.back();
  }

  loadHino(){
    const hinoId = this.activetedRoute.snapshot.params['hinoId'];
 
    this.hinoService.getHinoById(hinoId).subscribe(resp => {
     this.hino = resp;
    })
  }

  loadEstrofesPorHino(){
    const hinoId = this.activetedRoute.snapshot.params['hinoId']
    
      this.estrofeService.getAll().subscribe((resp) => {
        this.estrofes = resp.filter(x => x.hino?.id === hinoId ) ?? [];
      } )

  }


}
