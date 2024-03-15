import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoCreateComponent } from './hino-create.component';

describe('HinoCreateComponent', () => {
  let component: HinoCreateComponent;
  let fixture: ComponentFixture<HinoCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoCreateComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
