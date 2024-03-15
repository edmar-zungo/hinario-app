import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeDetailsComponent } from './estrofe-details.component';

describe('EstrofeDetailsComponent', () => {
  let component: EstrofeDetailsComponent;
  let fixture: ComponentFixture<EstrofeDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeDetailsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
