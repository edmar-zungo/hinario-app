import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaginaRevisaoComponent } from './pagina-revisao.component';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('PaginaRevisaoComponent', () => {
  let component: PaginaRevisaoComponent;
  let fixture: ComponentFixture<PaginaRevisaoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginaRevisaoComponent],
      providers: [provideHttpClient(withXhr()), provideRouter([])]})
    .compileComponents();

    fixture = TestBed.createComponent(PaginaRevisaoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
