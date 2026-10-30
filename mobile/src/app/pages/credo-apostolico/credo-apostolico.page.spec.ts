import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideIonicAngular } from '@ionic/angular';
import { CredoApostolicoPage } from './credo-apostolico.page';

describe('CredoApostolicoPage', () => {
  let component: CredoApostolicoPage;
  let fixture: ComponentFixture<CredoApostolicoPage>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideIonicAngular()],
    });
    fixture = TestBed.createComponent(CredoApostolicoPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
