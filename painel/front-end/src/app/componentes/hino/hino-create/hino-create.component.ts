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
      isFavorito: new FormControl(false),
    });
  }

  onSave() {
    if (this.hinoForm.valid) {
      this.hino = this.hinoForm.value;

      this.hinoService.createHino(this.hino).subscribe(() => {
        this.hinoService.getAll()
        this.hinoForm.reset();
      });
    }

    this.router.navigate(["/hino"]);
  }

  onCancel() {
    window.history.back();
  }
}
