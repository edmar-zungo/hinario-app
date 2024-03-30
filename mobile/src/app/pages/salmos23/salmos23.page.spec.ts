import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Salmos23Page } from './salmos23.page';

describe('Salmos23Page', () => {
  let component: Salmos23Page;
  let fixture: ComponentFixture<Salmos23Page>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(Salmos23Page);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
