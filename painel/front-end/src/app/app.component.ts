import { Component, ChangeDetectionStrategy } from '@angular/core';

import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './componentes/navbar/navbar.component';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, NavbarComponent],
    templateUrl: './app.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'hinario-app';
}
