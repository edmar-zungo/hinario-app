import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { HinoModel } from "../hino-model";
import { HinoService } from "../service/hino.service";
import { Router } from "@angular/router";
import { Linguas } from "../linguas";

@Component({
  selector: "app-hino-create",
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: "./hino-create.component.html",
  styleUrl: "./hino-create.component.css",
})
export class HinoCreateComponent implements OnInit {
  hinoForm!: FormGroup;
  hino!: HinoModel;
  hinoId?: string;
  linguaValue = Object.keys(Linguas);

  constructor(
    private formBuider: FormBuilder,
    private hinoService: HinoService,
    private router: Router
  ) {}
  ngOnInit(): void {
    this.cresteForm();
  }

  cresteForm() {
    this.hinoForm = this.formBuider.group({
      titulo: ["", Validators.required],
      pagina: ["", Validators.required],
      numero: ["", Validators.required],
      autor: [""],
      dataCriacao: [""],
      dataActualizacao: [""],
      comentario: [""],
      isFavorito: [false],
      lingua: ["", Validators.required],
    });
  }

  onSave() {
    if (this.hinoForm.valid) {
      this.hino = this.hinoForm.value;

      this.hinoService.createHino(this.hino).subscribe((resp) => {
        this.hinoService.getAll().subscribe(() => {
          location.reload();
        });

        this.hinoForm.reset();
        this.router.navigate(["/hino/visualizar/", resp.id]);
      });
    }
  }

  onCancel() {
    this.router.navigate(["/hino"]);
  }
}
