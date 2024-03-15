import { TestBed } from '@angular/core/testing';

import { EstrofeService } from './estrofe.service';

describe('EstrofeService', () => {
  let service: EstrofeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EstrofeService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
