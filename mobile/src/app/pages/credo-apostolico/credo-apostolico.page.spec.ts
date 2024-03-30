import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CredoApostolicoPage } from './credo-apostolico.page';

describe('CredoApostolicoPage', () => {
  let component: CredoApostolicoPage;
  let fixture: ComponentFixture<CredoApostolicoPage>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(CredoApostolicoPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
