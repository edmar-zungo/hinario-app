import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HinoReadPage } from './hino-read.page';

describe('HinoReadPage', () => {
  let component: HinoReadPage;
  let fixture: ComponentFixture<HinoReadPage>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(HinoReadPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
