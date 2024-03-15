import { Component, Input, OnInit } from '@angular/core';
import { EstrofeModel } from '../estrofe-model';
import { EstrofeService } from '../service/estrofe.service';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-estrofe-list',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './estrofe-list.component.html',
  styleUrl: './estrofe-list.component.css'
})
export class EstrofeListComponent implements OnInit{

  @Input() estrofes?: EstrofeModel[] = [];
  @Input() hinoId? = '';

  constructor(private estrofeService: EstrofeService){}

  ngOnInit(): void {
    
  }

  getAll() {
    this.estrofeService.getAll().subscribe((resp) => {
      this.estrofes = resp ?? [];
    });
  }

  onDelete(estrofeId: string | undefined) {
    this.estrofeService.deleteEstrofe(estrofeId!).subscribe(() => {
      this.getAll();
    });

    location.reload();
  }

}
