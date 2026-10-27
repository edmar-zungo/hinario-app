import { TestBed } from '@angular/core/testing';

import { HinoService } from './hino.service';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('HinoService', () => {
  let service: HinoService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])]
    });
    service = TestBed.inject(HinoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
