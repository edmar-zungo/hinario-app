import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ConstituicaoReadPage } from './constituicao-read.page';

describe('ConstituicaoReadPage', () => {
  let component: ConstituicaoReadPage;
  let fixture: ComponentFixture<ConstituicaoReadPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideIonicAngular(),
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ unidadeId: 'I-art-1' }),
            snapshot: { params: { unidadeId: 'I-art-1' } },
          },
        },
      ],
    });
    fixture = TestBed.createComponent(ConstituicaoReadPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
