import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeCreateComponent } from './estrofe-create.component';

describe('EstrofeCreateComponent', () => {
  let component: EstrofeCreateComponent;
  let fixture: ComponentFixture<EstrofeCreateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeCreateComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
