import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaginaListComponent } from './pagina-list.component';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PaginaService } from '../service/pagina.service';
import { PaginaModel } from '../pagina-model';
import { HinoService } from '../../hino/service/hino.service';

describe('PaginaListComponent', () => {
  let component: PaginaListComponent;
  let fixture: ComponentFixture<PaginaListComponent>;
  let paginaService: jasmine.SpyObj<PaginaService>;
  let hinoService: jasmine.SpyObj<HinoService>;
  let router: Router;

  const paginaTeste: PaginaModel = {
    pagina: 42,
    hinarioId: 'kmb',
    imagem: 'imagens/original/0042.jpg',
    imagemProcessada: 'imagens/processada/0042.jpg',
    hash: 'sha256:teste',
    estado: 'estruturada',
    nomeOriginal: '0042.jpg',
    importadoEm: '2026-11-12T00:00:00Z',
    largura: 100,
    altura: 100
  };

  beforeEach(async () => {
    paginaService = jasmine.createSpyObj<PaginaService>(
      'PaginaService', ['listar', 'importar', 'ler']);
    paginaService.listar.and.returnValue(of([]));
    paginaService.importar.and.returnValue(of([]));
    paginaService.ler.and.returnValue(of(paginaTeste));

    hinoService = jasmine.createSpyObj<HinoService>('HinoService', ['getAll']);
    hinoService.getAll.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [PaginaListComponent],
      providers: [
        provideRouter([]),
        { provide: PaginaService, useValue: paginaService },
        { provide: HinoService, useValue: hinoService }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture = TestBed.createComponent(PaginaListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('imports, extracts synchronously and navigates to review when single page succeeds', () => {
    paginaService.importar.and.returnValue(of([paginaTeste]));
    paginaService.ler.and.returnValue(of(paginaTeste));
    component.ficheiros = [new File(['image'], '0042.jpg', { type: 'image/jpeg' })];

    component.importar();

    expect(paginaService.ler).toHaveBeenCalledOnceWith(42);
    expect(component.extraindo).toBeFalse();
    expect(component.modalExtracaoAberta).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/paginas/revisar', 42]);
  });

  it('keeps modal open with error detail when extraction fails', () => {
    paginaService.importar.and.returnValue(of([paginaTeste]));
    paginaService.ler.and.returnValue(throwError(() => ({
      error: { message: 'GEMINI_API_KEY não definida' }
    })));
    component.ficheiros = [new File(['image'], '0042.jpg', { type: 'image/jpeg' })];

    component.importar();

    expect(component.extraindo).toBeFalse();
    expect(component.modalExtracaoAberta).toBeTrue();
    expect(component.itensExtracao[0].estado).toBe('erro');
    expect(component.itensExtracao[0].detalhe).toContain('GEMINI_API_KEY não definida');
  });
});
