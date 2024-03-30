import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OracaoDominicalPage } from './oracao-dominical.page';

describe('OracaoDominicalPage', () => {
  let component: OracaoDominicalPage;
  let fixture: ComponentFixture<OracaoDominicalPage>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(OracaoDominicalPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
