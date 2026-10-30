import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HinoReadPage } from './hino-read.page';

describe('HinoReadPage', () => {
  let component: HinoReadPage;
  let fixture: ComponentFixture<HinoReadPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideIonicAngular(),
        {
          provide: ActivatedRoute,
          useValue: { params: of({ hinoId: 'test-id' }), snapshot: { params: { hinoId: 'test-id' } } },
        },
      ],
    });
    fixture = TestBed.createComponent(HinoReadPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
