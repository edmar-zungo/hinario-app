import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HinoListPage } from './hino-list.page';

describe('HinoListPage', () => {
  let component: HinoListPage;
  let fixture: ComponentFixture<HinoListPage>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(HinoListPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
