import { Routes } from '@angular/router';
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'loader',
    pathMatch: 'full',
  },
  {
    path: 'loader',
    loadComponent: () => import('./pages/loader/loader.page').then(m => m.LoaderPage)
  },
  {
    path: 'hinos-list',
    loadComponent: () => import('./pages/hino-list/hino-list.page').then(m => m.HinoListPage)
  },
  {
    path: 'searchPage',
    loadComponent: () => import('./pages/pesquisa/pesquisa.page').then( m => m.PesquisaPage)
  },
  {
    path: 'hinos-favoritos',
    loadComponent: () => import('./pages/hinos-favoritos/hinos-favoritos.page').then( m => m.HinosFavoritosPage)
  },
  {
    path: 'hino-read/:hinoId',
    loadComponent: () => import('./pages/hino-read/hino-read.page').then( m => m.HinoReadPage)
  },
  {
    path: 'dez-mandamentos',
    loadComponent: () => import('./pages/dez-mandamentos/dez-mandamentos.page').then( m => m.DezMandamentosPage)
  },
  {
    path: 'credo-apostolico',
    loadComponent: () => import('./pages/credo-apostolico/credo-apostolico.page').then( m => m.CredoApostolicoPage)
  },
  {
    path: 'oracao-dominical',
    loadComponent: () => import('./pages/oracao-dominical/oracao-dominical.page').then( m => m.OracaoDominicalPage)
  },
  {
    path: 'salmos23',
    loadComponent: () => import('./pages/salmos23/salmos23.page').then( m => m.Salmos23Page)
  },
];
