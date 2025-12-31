import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Navbar } from "../navbar/navbar";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, Navbar, RouterOutlet],
  template: `
    <app-navbar></app-navbar>
    <div class="home-content">
      <router-outlet></router-outlet>
    </div>
  `,
  styleUrls: ['./home.css'],
})
export class Home {}