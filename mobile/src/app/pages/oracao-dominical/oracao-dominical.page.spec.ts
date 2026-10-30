import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideIonicAngular } from '@ionic/angular';
import { OracaoDominicalPage } from './oracao-dominical.page';

describe('OracaoDominicalPage', () => {
  let component: OracaoDominicalPage;
  let fixture: ComponentFixture<OracaoDominicalPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideIonicAngular()],
    });
    fixture = TestBed.createComponent(OracaoDominicalPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
