import { Component, OnInit } from "@angular/core";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { EstrofeModel } from "../estrofe-model";
import { EstrofeService } from "../service/estrofe.service";
import { ActivatedRoute, Router } from "@angular/router";
import { HinoModel } from "../../hino/hino-model";
import { HinoService } from "../../hino/service/hino.service";

@Component({
  selector: "app-estrofe-create",
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: "./estrofe-create.component.html",
  styleUrl: "./estrofe-create.component.css",
})
export class EstrofeCreateComponent implements OnInit {
  estrofeForm!: FormGroup;
  estrofe!: EstrofeModel;
  hinos: HinoModel[] = [];

  constructor(
    private estrofeService: EstrofeService,
    private formBuilder: FormBuilder,
    private hinoService: HinoService,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.loadHinos();
    this.cresteForm();

    this.addHinoToForm();
  }

  cresteForm() {
    this.estrofeForm = this.formBuilder.group({
      numero: [""],
      descricao: ["", Validators.required],
      hino: [""],
      refrao: [false],
    });
  }

  onSave() {
    if (this.estrofeForm.valid) {
      this.estrofe = this.estrofeForm.value;

      this.estrofeService.createEstrofe(this.estrofe).subscribe(() => {
        this.estrofeService.getAll();
        this.estrofeForm.reset();
      });
    }

    window.history.back();
  }

  onCancel() {
    window.history.back();
  }

  loadHinos() {
    this.hinoService.getAll().subscribe((resp) => {
      this.hinos = resp ?? [];
    });
  }

  addHinoToForm() {
    this.activatedRoute.queryParams.subscribe((resp) => {
      const hinoId = resp["hinoID"];
      this.hinoService.getHinoById(hinoId).subscribe((param) => {
        this.estrofeForm.patchValue({
          hino: param,
        });
      });
    });
  }
}
