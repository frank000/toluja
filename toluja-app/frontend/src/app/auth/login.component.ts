import { Component, ElementRef, HostListener } from '@angular/core';
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
  tenantSelectAberto = false;

  form = this.fb.group({
    tenantId: ['default', Validators.required],
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private elementRef: ElementRef<HTMLElement>
  ) {
    this.carregarTenants();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.tenantSelectAberto = false;
    }
  }

  get tenantsFiltrados(): Array<{ tenantId: string; nome: string }> {
    const termo = (this.form.value.tenantId || '').trim().toLowerCase();
    if (!termo) return this.tenants;
    return this.tenants.filter((tenant) =>
      tenant.tenantId.toLowerCase().includes(termo) || tenant.nome.toLowerCase().includes(termo)
    );
  }

  togglePainelMode(): void {
    this.painelMode = !this.painelMode;
    this.erro = '';
    if (this.painelMode) {
      this.form.patchValue({ tenantId: 'default', username: '', password: '' });
    } else {
      this.form.patchValue({ username: '', password: '' });
    }
  }

  onTenantInput(value: string): void {
    this.form.patchValue({ tenantId: value });
    this.tenantSelectAberto = true;
  }

  selecionarTenant(tenantId: string): void {
    this.form.patchValue({ tenantId });
    this.tenantSelectAberto = false;
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
