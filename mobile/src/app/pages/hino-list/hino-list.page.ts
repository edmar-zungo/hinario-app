import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IonHeader, IonToolbar, IonButtons, IonTitle, IonContent, IonList, IonLabel, IonFab, IonIcon, IonMenuButton, IonItem } from '@ionic/angular/standalone';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HinarioService } from 'src/app/services/hinario.service';
import { HinoModel } from 'src/app/model/hino-model';

@Component({
  selector: 'app-hino-list',
  templateUrl: './hino-list.page.html',
  styleUrls: ['./hino-list.page.scss'],
  standalone: true,
  imports: [IonIcon, IonFab, IonLabel, IonList, IonContent, IonTitle, IonButtons, IonToolbar, CommonModule, FormsModule, RouterLink, RouterLinkActive, IonHeader, IonMenuButton, IonItem, ]
})
export class HinoListPage implements OnInit {

  
  hinosByJSON: HinoModel[] = [];

  constructor(private router: Router, private hinarioService: HinarioService) { }

  ngOnInit() {
    // this.getAllHinos();
    this.getAllhinosByJSON();
  
  }

  // getAllHinos(){
  //   this.hinarioService.getAllHinos().subscribe(resp => {
  //     this.hinos = resp ?? [];
    
  //   })
  // }

  getAllhinosByJSON(){
    this.hinarioService.getAllHinoByJSON().subscribe(resp => {
      this.hinosByJSON = resp ?? [];
    })
  }

  goToSearchPage() {
    this.router.navigate(['/searchPage']);
    }

}
