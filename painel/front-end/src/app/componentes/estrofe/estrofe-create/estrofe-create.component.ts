import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EstrofeModel } from '../estrofe-model';
import { EstrofeService } from '../service/estrofe.service';
import { Router } from '@angular/router';
import { HinoModel } from '../../hino/hino-model';
import { HinoService } from '../../hino/service/hino.service';

@Component({
  selector: 'app-estrofe-create',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './estrofe-create.component.html',
  styleUrl: './estrofe-create.component.css'
})
export class EstrofeCreateComponent implements OnInit{

  estrofeForm!: FormGroup;
  estrofe!: EstrofeModel;
  hinos: HinoModel[] = [];


  constructor(private estrofeService: EstrofeService, private formBuilder: FormBuilder, private router: Router, private hinoService: HinoService){}

  ngOnInit(): void {
    this.loadHinos();
    this.cresteForm();
  }

  cresteForm() {
    this.estrofeForm = this.formBuilder.group({
      numero: ["", Validators.required],
      descricao: ["", Validators.required],
      hino: [, Validators.required],
      refrao: [false]
    });
  }

  onSave() {
    if (this.estrofeForm.valid) {
      this.estrofe = this.estrofeForm.value;

      console.log(this.estrofe)

      this.estrofeService.createEstrofe(this.estrofe).subscribe(() => {
        this.estrofeService.getAll()
        this.estrofeForm.reset();
      });
    }

    this.router.navigate(["/estrofe"]);
  }

  onCancel() {
    window.history.back();
  }

  loadHinos(){
    this.hinoService.getAll().subscribe(resp => {
      this.hinos = resp ?? [];
    })
  }
}
