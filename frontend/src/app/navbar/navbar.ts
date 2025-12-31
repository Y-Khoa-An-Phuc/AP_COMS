import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDividerModule } from '@angular/material/divider';
import { MatSidenavModule } from '@angular/material/sidenav';


@Component({
  selector: 'app-navbar',
  imports: [
    CommonModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatInputModule,
    MatFormFieldModule,
    MatDividerModule,
    MatSidenavModule
  ],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css'],
})
export class Navbar {
  isMobileMenuOpen = false;

  constructor(private router: Router) {}

  toggleMobileMenu() {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  navigateToAddEmployee() {
    this.router.navigate(['/settings/employee/add']);
  }

  navigateToViewEmployee() {
    this.router.navigate(['/settings/employee/view']);
  }

  navigateToViewOccupation() {
    this.router.navigate(['/settings/occupation/view']);
  }

  logout() {
    // Clear all authentication data from localStorage
    localStorage.removeItem('authToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('email');
    localStorage.removeItem('role');

    // Navigate to login page
    this.router.navigateByUrl('/');
  }
}
