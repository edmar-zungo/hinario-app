import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeUpdateComponent } from './estrofe-update.component';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('EstrofeUpdateComponent', () => {
  let component: EstrofeUpdateComponent;
  let fixture: ComponentFixture<EstrofeUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeUpdateComponent]
    ,
      providers: [provideHttpClient(), provideRouter([])]})
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
