import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  erro = '';
  loading = false;
  carregandoTenants = false;
  painelMode = false;
  tenants: Array<{ tenantId: string; nome: string }> = [];

  form = this.fb.group({
    tenantId: ['default', Validators.required],
    username: ['admin', Validators.required],
    password: ['admin123', Validators.required]
  });

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router) {
    this.carregarTenants();
  }

  togglePainelMode(): void {
    this.painelMode = !this.painelMode;
    this.erro = '';
    if (this.painelMode) {
      this.form.patchValue({ tenantId: 'default', username: 'superadmin', password: '' });
    } else {
      this.form.patchValue({ username: 'admin' });
    }
  }

  submit(): void {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    this.erro = '';
    const { tenantId, username, password } = this.form.getRawValue();

    this.authService.login(tenantId!, username!, password!).subscribe({
      next: () => {
        if (this.painelMode) {
          if (!this.authService.isSuperadmin()) {
            this.authService.logout();
            this.loading = false;
            this.erro = 'Somente superadmin pode acessar o painel.';
            return;
          }
          this.loading = false;
          this.router.navigate(['/painel-superadmin']);
          return;
        }
        this.loading = false;
        this.router.navigate([this.authService.mustChangePassword() ? '/trocar-senha' : '/pedido']);
      },
      error: () => {
        this.loading = false;
        this.erro = 'Usuário ou senha inválidos.';
      }
    });
  }

  private carregarTenants(): void {
    this.carregandoTenants = true;
    this.authService.listarTenantsPublicos().subscribe({
      next: (tenants) => {
        this.tenants = tenants.length > 0 ? tenants : [{ tenantId: 'default', nome: 'Tenant Padrão' }];
        const tenantSelecionado = this.form.value.tenantId;
        if (!tenantSelecionado && this.tenants.length > 0) {
          this.form.patchValue({ tenantId: this.tenants[0].tenantId });
        }
        this.carregandoTenants = false;
      },
      error: () => {
        this.tenants = [{ tenantId: 'default', nome: 'Tenant Padrão' }];
        this.carregandoTenants = false;
      }
    });
  }
}
