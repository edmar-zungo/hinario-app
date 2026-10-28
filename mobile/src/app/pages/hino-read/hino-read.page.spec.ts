import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular';

import { HinoReadPage } from './hino-read.page';

describe('HinoReadPage', () => {
  let component: HinoReadPage;
  let fixture: ComponentFixture<HinoReadPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([]), provideIonicAngular()],
    });
    fixture = TestBed.createComponent(HinoReadPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
