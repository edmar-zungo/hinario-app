import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoUpdateComponent } from './hino-update.component';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('HinoUpdateComponent', () => {
  let component: HinoUpdateComponent;
  let fixture: ComponentFixture<HinoUpdateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoUpdateComponent]
    ,
      providers: [provideHttpClient(), provideRouter([])]})
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoUpdateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
