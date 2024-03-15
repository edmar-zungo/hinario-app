import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoDetailsComponent } from './hino-details.component';

describe('HinoDetailsComponent', () => {
  let component: HinoDetailsComponent;
  let fixture: ComponentFixture<HinoDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoDetailsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
