import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { HinoService } from '../service/hino.service';
import { HinoModel } from '../hino-model';

@Component({
  selector: 'app-hino-details',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './hino-details.component.html',
  styleUrl: './hino-details.component.css'
})
export class HinoDetailsComponent implements OnInit{


  hino?: HinoModel;
  

  constructor(private hinoService: HinoService, private activetedRoute: ActivatedRoute){}

  ngOnInit(): void {
   const hinoId = this.activetedRoute.snapshot.params['hinoId'];
   console.log(hinoId);

   this.hinoService.getHinoById(hinoId).subscribe(resp => {
    this.hino = resp;
   })
  }


  back() {
    window.history.back();
  }


}
