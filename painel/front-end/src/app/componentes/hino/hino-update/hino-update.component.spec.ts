import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoUpdateComponent } from './hino-update.component';

describe('HinoUpdateComponent', () => {
  let component: HinoUpdateComponent;
  let fixture: ComponentFixture<HinoUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoUpdateComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
