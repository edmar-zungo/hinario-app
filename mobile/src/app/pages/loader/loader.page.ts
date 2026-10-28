import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { IonContent, IonImg } from '@ionic/angular';
import { Router } from '@angular/router';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.page.html',
  styleUrls: ['./loader.page.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [IonImg, IonContent, FormsModule],
})
export class LoaderPage implements OnInit {
  constructor(private router: Router) {}

  ngOnInit() {
    setTimeout(() => this.router.navigate(['/hinos-list']), 1500);
  }
}
