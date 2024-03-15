import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeUpdateComponent } from './estrofe-update.component';

describe('EstrofeUpdateComponent', () => {
  let component: EstrofeUpdateComponent;
  let fixture: ComponentFixture<EstrofeUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeUpdateComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
