import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrofeDetailsComponent } from './estrofe-details.component';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('EstrofeDetailsComponent', () => {
  let component: EstrofeDetailsComponent;
  let fixture: ComponentFixture<EstrofeDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstrofeDetailsComponent]
    ,
      providers: [provideHttpClient(), provideRouter([])]})
    .compileComponents();
    
    fixture = TestBed.createComponent(EstrofeDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
