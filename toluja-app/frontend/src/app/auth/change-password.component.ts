import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password.component.html'
})
export class ChangePasswordComponent {
  erro = '';
  loading = false;

  form = this.fb.group({
    senhaAtual: ['', Validators.required],
    novaSenha: ['', [Validators.required, Validators.minLength(6)]],
    confirmarSenha: ['', Validators.required]
  });

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {}

  submit(): void {
    if (this.form.invalid || this.loading) return;

    const { senhaAtual, novaSenha, confirmarSenha } = this.form.getRawValue();
    if (novaSenha !== confirmarSenha) {
      this.erro = 'A confirmação da nova senha não confere.';
      return;
    }

    this.loading = true;
    this.erro = '';

    this.authService.changePassword(senhaAtual!, novaSenha!).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/itens']);
      },
      error: () => {
        this.loading = false;
        this.erro = 'Não foi possível trocar a senha. Verifique a senha atual.';
      }
    });
  }
}
