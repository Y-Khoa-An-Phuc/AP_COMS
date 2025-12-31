import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDividerModule } from '@angular/material/divider';


@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatInputModule,
    MatFormFieldModule,
    MatDividerModule
  ],
  template: `
    
  `,
  // keep external css but add local overrides to constrain size
  styleUrls: ['./menu.css'],
  styles: [
    `


    
    `
  ]
})
export class Menu {}