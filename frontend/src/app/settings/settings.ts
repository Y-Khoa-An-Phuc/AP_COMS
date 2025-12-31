import { Component } from '@angular/core';
import { Navbar } from "../navbar/navbar";
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-settings',
  imports: [Navbar, RouterOutlet],
  template: `
    <app-navbar></app-navbar>
    <div>
      <router-outlet></router-outlet>
    </div>
  `,
  styleUrl: './settings.css',
})
export class Settings {

}
