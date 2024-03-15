import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoFavoritoListComponent } from './hino-favorito-list.component';

describe('HinoFavoritoListComponent', () => {
  let component: HinoFavoritoListComponent;
  let fixture: ComponentFixture<HinoFavoritoListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoFavoritoListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoFavoritoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
