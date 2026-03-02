import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  erro = '';
  loading = false;

  form = this.fb.group({
    username: ['admin', Validators.required],
    password: ['admin123', Validators.required]
  });

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.erro = '';
    const { username, password } = this.form.getRawValue();

    this.authService.login(username!, password!).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/itens']);
      },
      error: () => {
        this.loading = false;
        this.erro = 'Usuário ou senha inválidos.';
      }
    });
  }
}
