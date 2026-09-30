import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  mode: 'login' | 'register' = 'login';
  loading = false;
  error = '';

  fullName = ''; email = ''; password = ''; phoneNumber = '';

  constructor(private auth: AuthService, private router: Router) {}

  toggleMode() { this.mode = this.mode === 'login' ? 'register' : 'login'; this.error = ''; }

  submit() {
    this.error = '';
    if (this.mode === 'login') {
      if (!this.email || !this.password) { this.error = 'Email and password are required'; return; }
      this.loading = true;
      this.auth.login({ email: this.email, password: this.password }).subscribe({
        next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
        error: (err) => { this.loading = false; this.error = err.error?.message || 'Invalid email or password'; }
      });
    } else {
      if (!this.fullName || !this.email || this.password.length < 8 || !/^\d{10}$/.test(this.phoneNumber)) {
        this.error = 'Check all fields — password needs 8+ characters, phone needs 10 digits';
        return;
      }
      this.loading = true;
      this.auth.register({ fullName: this.fullName, email: this.email, password: this.password, phoneNumber: this.phoneNumber }).subscribe({
        next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
        error: (err) => { this.loading = false; this.error = err.error?.message || 'Registration failed'; }
      });
    }
  }
}