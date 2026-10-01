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
  loading = false; error = ''; submitted = false;
  fullName = ''; email = ''; password = ''; phoneNumber = '';

  constructor(private auth: AuthService, private router: Router) {}

  get emailOk() { return /^\S+@\S+\.\S+$/.test(this.email); }
  get passOk() { return this.mode === 'login' ? this.password.length > 0 : this.password.length >= 8; }
  get phoneOk() { return /^\d{10}$/.test(this.phoneNumber); }
  get strengthPct() { return Math.min(this.password.length / 12, 1) * 100; }
  get strengthColor() { return this.strengthPct < 40 ? 'var(--red)' : this.strengthPct < 75 ? 'var(--amber)' : 'var(--green)'; }

  toggleMode() { this.mode = this.mode === 'login' ? 'register' : 'login'; this.error = ''; this.submitted = false; }

  submit() {
    this.error = ''; this.submitted = true;
    if (this.mode === 'login') {
      if (!this.emailOk || !this.passOk) return;
      this.loading = true;
      this.auth.login({ email: this.email, password: this.password }).subscribe({
        next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
        error: (e) => { this.loading = false; this.error = e.error?.message || 'Invalid email or password'; }
      });
    } else {
      if (!this.fullName.trim() || !this.emailOk || !this.passOk || !this.phoneOk) return;
      this.loading = true;
      this.auth.register({ fullName: this.fullName, email: this.email, password: this.password, phoneNumber: this.phoneNumber }).subscribe({
        next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
        error: (e) => { this.loading = false; this.error = e.error?.message || 'Registration failed'; }
      });
    }
  }
}
