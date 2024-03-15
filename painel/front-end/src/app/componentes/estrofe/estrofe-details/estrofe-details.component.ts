import { Component } from '@angular/core';
import { EstrofeModel } from '../estrofe-model';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { EstrofeService } from '../service/estrofe.service';

@Component({
  selector: 'app-estrofe-details',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './estrofe-details.component.html',
  styleUrl: './estrofe-details.component.css'
})
export class EstrofeDetailsComponent {

  estrofe!: EstrofeModel;

  constructor(private activatedRoute: ActivatedRoute, private estrofeService: EstrofeService){}

  ngOnInit(): void {
    const estrofeId = this.activatedRoute.snapshot.params['estrofeId'];
    console.log(estrofeId);
 
    this.estrofeService.getEstrofeById(estrofeId).subscribe(resp => {
     this.estrofe = resp;
    })
   }
 
 
   back() {
     window.history.back();
   }
 

}
