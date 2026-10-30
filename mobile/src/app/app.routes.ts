import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'tabs/hinos',
    pathMatch: 'full',
  },
  {
    path: 'tabs',
    loadComponent: () =>
      import('./pages/tabs/tabs.page').then((m) => m.TabsPage),
    children: [
      {
        path: '',
        redirectTo: 'hinos',
        pathMatch: 'full',
      },
      {
        path: 'hinos',
        loadComponent: () =>
          import('./pages/hino-list/hino-list.page').then((m) => m.HinoListPage),
      },
      {
        path: 'pesquisa',
        loadComponent: () =>
          import('./pages/pesquisa/pesquisa.page').then((m) => m.PesquisaPage),
      },
      {
        path: 'favoritos',
        loadComponent: () =>
          import('./pages/hinos-favoritos/hinos-favoritos.page').then(
            (m) => m.HinosFavoritosPage
          ),
      },
      {
        path: 'constituicao',
        loadComponent: () =>
          import('./pages/constituicao/constituicao.page').then(
            (m) => m.ConstituicaoPage
          ),
      },
    ],
  },
  {
    path: 'hino-read/:hinoId',
    loadComponent: () =>
      import('./pages/hino-read/hino-read.page').then((m) => m.HinoReadPage),
  },
  {
    path: 'dez-mandamentos',
    loadComponent: () =>
      import('./pages/dez-mandamentos/dez-mandamentos.page').then(
        (m) => m.DezMandamentosPage
      ),
  },
  {
    path: 'credo-apostolico',
    loadComponent: () =>
      import('./pages/credo-apostolico/credo-apostolico.page').then(
        (m) => m.CredoApostolicoPage
      ),
  },
  {
    path: 'oracao-dominical',
    loadComponent: () =>
      import('./pages/oracao-dominical/oracao-dominical.page').then(
        (m) => m.OracaoDominicalPage
      ),
  },
  {
    path: 'salmos23',
    loadComponent: () =>
      import('./pages/salmos23/salmos23.page').then((m) => m.Salmos23Page),
  },
  {
    path: 'loader',
    loadComponent: () =>
      import('./pages/loader/loader.page').then((m) => m.LoaderPage),
  },
  // Rotas de compatibilidade
  {
    path: 'hinos-list',
    redirectTo: 'tabs/hinos',
    pathMatch: 'full',
  },
  {
    path: 'searchPage',
    redirectTo: 'tabs/pesquisa',
    pathMatch: 'full',
  },
  {
    path: 'hinos-favoritos',
    redirectTo: 'tabs/favoritos',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: 'tabs/hinos',
  },
];
