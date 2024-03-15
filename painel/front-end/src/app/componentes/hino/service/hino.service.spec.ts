import { TestBed } from '@angular/core/testing';

import { HinoService } from './hino.service';

describe('HinoService', () => {
  let service: HinoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(HinoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
