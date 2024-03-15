import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoListComponent } from './hino-list.component';

describe('HinoListComponent', () => {
  let component: HinoListComponent;
  let fixture: ComponentFixture<HinoListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
