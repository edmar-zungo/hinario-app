import { Component } from '@angular/core';
import { EstrofeModel } from '../estrofe-model';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { EstrofeService } from '../service/estrofe.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NgSelectModule } from '@ng-select/ng-select';
import { HinoModel } from '../../hino/hino-model';
import { HinoService } from '../../hino/service/hino.service';

@Component({
  selector: 'app-estrofe-update',
  standalone: true,
  imports: [ReactiveFormsModule, NgSelectModule],
  templateUrl: './estrofe-update.component.html',
  styleUrl: './estrofe-update.component.css'
})
export class EstrofeUpdateComponent {

  estrofeForm!: FormGroup;
  estrofe!: EstrofeModel;
  hinos: HinoModel[] = [];
  isReadOnly: boolean = true;

  constructor(private estrofeService: EstrofeService, private activatedRoute: ActivatedRoute, private router: Router, private formBuilder: FormBuilder, private hinoService: HinoService){}


  ngOnInit(): void {

    this.loadHinos();
    this.criarFormulario();

    this.activatedRoute.params.subscribe((param) => {
      const estrofeId = param["estrofeId"];
      this.estrofeService.getEstrofeById(estrofeId).subscribe((resp) => {
        this.estrofeForm.patchValue({
          id: resp.id,
          numero: resp.numero,
          descricao: resp.descricao,
          hino: resp.hino,
          refrao: resp.refrao
          
        });
      });
    });
  }

  criarFormulario(){
    this.estrofeForm = this.formBuilder.group({
      id: [""],
      numero: [""],
      descricao: [""],
      hino: [""],
      refrao: [false]
    });
  }

  onCancel() {
    window.history.back();
  }

  onSave() {
    this.estrofe = this.estrofeForm.value;
    console.log(this.estrofe);
    this.estrofeService.updateEstrofe(this.estrofe.id, this.estrofe).subscribe(() => {
      this.estrofeService.getAll();
    });

    this.router.navigate(['/estrofe']);
  }

  loadHinos(){
    this.hinoService.getAll().subscribe(resp => {
      this.hinos = resp ?? [];
    })
  }

}
