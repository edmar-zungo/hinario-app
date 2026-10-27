import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HinoListComponent } from './hino-list.component';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideRouter } from '@angular/router';

describe('HinoListComponent', () => {
  let component: HinoListComponent;
  let fixture: ComponentFixture<HinoListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HinoListComponent]
    ,
      providers: [provideHttpClient(withXhr()), provideRouter([])]})
    .compileComponents();
    
    fixture = TestBed.createComponent(HinoListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
