import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HinarioService } from './hinario.service';

describe('HinarioService', () => {
  let service: HinarioService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HinarioService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should remove accents and lowercase query', () => {
    expect(HinarioService.removerAcentos('Jezú Ô Amém Ção')).toBe('jezu o amem cao');
  });
});
