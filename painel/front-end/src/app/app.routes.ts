import { Routes } from '@angular/router';
import { HinoListComponent } from './componentes/hino/hino-list/hino-list.component';
import { HinoUpdateComponent } from './componentes/hino/hino-update/hino-update.component';
import { HinoDetailsComponent } from './componentes/hino/hino-details/hino-details.component';
import { HinoCreateComponent } from './componentes/hino/hino-create/hino-create.component';
import { EstrofeListComponent } from './componentes/estrofe/estrofe-list/estrofe-list.component';
import { EstrofeCreateComponent } from './componentes/estrofe/estrofe-create/estrofe-create.component';
import { EstrofeUpdateComponent } from './componentes/estrofe/estrofe-update/estrofe-update.component';
import { EstrofeDetailsComponent } from './componentes/estrofe/estrofe-details/estrofe-details.component';
import { HinoFavoritoListComponent } from './componentes/hino/hino-favorito-list/hino-favorito-list.component';
import { PaginaListComponent } from './componentes/pagina/pagina-list/pagina-list.component';
import { PaginaRevisaoComponent } from './componentes/pagina/revisao/pagina-revisao.component';
import { ExportarComponent } from './componentes/exportacao/exportar/exportar.component';
import { CompararComponent } from './componentes/exportacao/comparar/comparar.component';

export const routes: Routes = [
     {path: '', redirectTo: 'hino', pathMatch: 'full'},
     {path: 'hino', component: HinoListComponent},
     {path: 'hino/create', component: HinoCreateComponent},
     {path: 'hino/favoritos', component: HinoFavoritoListComponent},
     {path: 'hino/visualizar/:hinoId', component: HinoDetailsComponent},
     {path: 'hino/editar/:hinoId', component: HinoUpdateComponent},
     {path: 'estrofe', component: EstrofeListComponent},     
     {path: 'estrofe/create', component: EstrofeCreateComponent},     
     {path: 'estrofe/visualizar/:estrofeId', component: EstrofeDetailsComponent},     
     {path: 'estrofe/editar/:estrofeId', component: EstrofeUpdateComponent},
     {path: 'paginas', component: PaginaListComponent},
     {path: 'paginas/revisar/:numero', component: PaginaRevisaoComponent},
     {path: 'exportar', component: ExportarComponent},
     {path: 'comparar', component: CompararComponent}

];
