import { Component, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { HinoService } from "../service/hino.service";
import { HinoModel } from "../hino-model";

@Component({
  selector: "app-hino-update",
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: "./hino-update.component.html",
  styleUrl: "./hino-update.component.css",
})
export class HinoUpdateComponent implements OnInit {
  hinoForm!: FormGroup;
  hino!: HinoModel;
  isReadOnly: boolean = true;

  constructor(
    private activatedRoute: ActivatedRoute,
    private hinoService: HinoService,
    private formBuilder: FormBuilder,
    private router: Router
  ) {}

  ngOnInit(): void {

    this.criarFormulario();

    this.activatedRoute.params.subscribe((param) => {
      const hinoId = param["hinoId"];
      this.hinoService.getHinoById(hinoId).subscribe((resp) => {
        this.hinoForm.patchValue({
          id: resp.id,
          titulo: resp.titulo,
          pagina: resp.pagina,
          numero: resp.numero,
          autor: resp.autor,
          dataCriacao: resp.dataCriacao,
          dataActualizacao: resp.dataActualizacao,
          comentario: resp.comentario,
          isFavorito: resp.isFavorito
        });
      });
    });
  }

  criarFormulario(){
    this.hinoForm = this.formBuilder.group({
      id: [""],
      titulo: [""],
      pagina: [""],
      numero: [""],
      autor: [""],
      dataCriacao: [""],
      dataActualizacao: [""],
      comentario: [""],
      isFavorito: [false],
    });
  }

  onCancel() {
    window.history.back();
  }

  onSave() {
    this.hino = this.hinoForm.value;
    this.hinoService.updateHino(this.hino.id, this.hino).subscribe(() => {
      this.hinoService.getAll().subscribe(() => {
        location.reload();
      });
    });

    this.router.navigate(['/hino']);
  }
}
