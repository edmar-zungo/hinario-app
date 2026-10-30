import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideIonicAngular } from '@ionic/angular';
import { ConstituicaoPage } from './constituicao.page';

describe('ConstituicaoPage', () => {
  let component: ConstituicaoPage;
  let fixture: ComponentFixture<ConstituicaoPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideIonicAngular()],
    });
    fixture = TestBed.createComponent(ConstituicaoPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
