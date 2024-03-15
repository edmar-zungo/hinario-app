import { Component, OnInit } from "@angular/core";
import { HinoService } from "../service/hino.service";
import { HinoModel } from "../hino-model";
import { RouterLink, RouterLinkActive } from "@angular/router";
@Component({
  selector: "app-hino-list",
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: "./hino-list.component.html",
  styleUrl: "./hino-list.component.css",
})
export class HinoListComponent implements OnInit {

  hinos: HinoModel[] = [];

  constructor(protected hinoService: HinoService) {}

  ngOnInit(): void {
    this.getAll();
  }

  getAll() {
    this.hinoService.getAll().subscribe((resp) => {
      this.hinos = resp ?? [];
    });
    
  }

  onDelete(hinoId: string | undefined) {
    this.hinoService.deleteHino(hinoId!).subscribe(() => {
      this.getAll();
    });

    location.reload();
    
  }

  addFavoritos(hinoId: string | undefined) {
    this.hinoService.addFavoritos(hinoId).subscribe(() =>{
      this.hinoService.getAllFavoritos();
      this.getAll();
      console.log("Adicionado!");
    })
  }
}
