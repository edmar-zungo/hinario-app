import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoFavoritoListComponent } from './hino-favorito-list.component';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('HinoFavoritoListComponent', () => {
  let component: HinoFavoritoListComponent;
  let fixture: ComponentFixture<HinoFavoritoListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoFavoritoListComponent]
    ,
      providers: [provideHttpClient(withXhr()), provideRouter([])]})
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoFavoritoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
