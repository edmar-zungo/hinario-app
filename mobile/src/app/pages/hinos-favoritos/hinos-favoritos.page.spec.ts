import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HinosFavoritosPage } from './hinos-favoritos.page';

describe('HinosFavoritosPage', () => {
  let component: HinosFavoritosPage;
  let fixture: ComponentFixture<HinosFavoritosPage>;

  beforeEach(async(() => {
    fixture = TestBed.createComponent(HinosFavoritosPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
