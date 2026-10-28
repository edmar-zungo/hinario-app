import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PaginaListComponent } from './pagina-list.component';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('PaginaListComponent', () => {
  let component: PaginaListComponent;
  let fixture: ComponentFixture<PaginaListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaginaListComponent]
    ,
      providers: [provideHttpClient(withXhr()), provideRouter([])]})
    .compileComponents();

    fixture = TestBed.createComponent(PaginaListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
