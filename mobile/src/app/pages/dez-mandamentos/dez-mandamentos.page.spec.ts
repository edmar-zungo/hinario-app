import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideIonicAngular } from '@ionic/angular';
import { DezMandamentosPage } from './dez-mandamentos.page';

describe('DezMandamentosPage', () => {
  let component: DezMandamentosPage;
  let fixture: ComponentFixture<DezMandamentosPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideIonicAngular()],
    });
    fixture = TestBed.createComponent(DezMandamentosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
