import { TestBed } from '@angular/core/testing';

import { EstrofeService } from './estrofe.service';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('EstrofeService', () => {
  let service: EstrofeService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])]
    });
    service = TestBed.inject(EstrofeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
