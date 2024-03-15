import { Component } from '@angular/core';
import { HinoListComponent } from '../hino/hino-list/hino-list.component';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppComponent } from '../../app.component';
import { EstrofeListComponent } from '../estrofe/estrofe-list/estrofe-list.component';
import { HinoFavoritoListComponent } from '../hino/hino-favorito-list/hino-favorito-list.component';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [HinoListComponent,HinoFavoritoListComponent,RouterLink, RouterLinkActive, AppComponent],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css'
})
export class NavbarComponent {

}
