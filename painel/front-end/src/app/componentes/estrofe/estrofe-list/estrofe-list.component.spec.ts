import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeListComponent } from './estrofe-list.component';

describe('EstrofeListComponent', () => {
  let component: EstrofeListComponent;
  let fixture: ComponentFixture<EstrofeListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
