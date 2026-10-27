import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { IonContent, IonImg } from '@ionic/angular/standalone';
import { Router } from '@angular/router';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.page.html',
  styleUrls: ['./loader.page.scss'],
  imports: [IonImg, IonContent, FormsModule],
})
export class LoaderPage implements OnInit {
  constructor(private router: Router) {}

  ngOnInit() {
    setTimeout(() => this.router.navigate(['/hinos-list']), 1500);
  }
}
