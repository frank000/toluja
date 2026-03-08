import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { TenantSummary } from '../core/models';

@Component({
  selector: 'app-superadmin-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './superadmin-panel.component.html',
  styleUrl: './superadmin-panel.component.css'
})
export class SuperadminPanelComponent implements OnInit {
  tenants: TenantSummary[] = [];
  tenantsCollapsed = false;
  tenantFilter = '';
  copyFeedbackByTenant: Record<string, string> = {};
  loadingTenants = false;
  savingTenant = false;
  savingUser = false;
  tenantError = '';
  userError = '';
  tenantSuccess = '';
  createdTenantId = '';
  createdTenantPrintKey = '';
  userSuccess = '';

  tenantForm = this.fb.group({
    tenantId: ['', Validators.required],
    nome: ['', Validators.required]
  });

  userForm = this.fb.group({
    tenantId: ['', Validators.required],
    username: ['', Validators.required],
    nomeExibicao: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]],
    role: ['ATENDENTE', Validators.required]
  });

  constructor(private fb: FormBuilder, private api: ApiService) {}

  ngOnInit(): void {
    this.carregarTenants();
  }

  get filteredTenants(): TenantSummary[] {
    const filtro = this.tenantFilter.trim().toLowerCase();
    if (!filtro) {
      return this.tenants;
    }
    return this.tenants.filter((tenant) => {
      return tenant.tenantId.toLowerCase().includes(filtro) || tenant.nome.toLowerCase().includes(filtro);
    });
  }

  carregarTenants(): void {
    this.loadingTenants = true;
    this.api.listarTenantsPainel().subscribe({
      next: (tenants) => {
        this.tenants = tenants;
        if (!this.userForm.value.tenantId && tenants.length > 0) {
          this.userForm.patchValue({ tenantId: tenants[0].tenantId });
        }
        this.loadingTenants = false;
      },
      error: () => {
        this.loadingTenants = false;
      }
    });
  }

  submitTenant(): void {
    if (this.tenantForm.invalid || this.savingTenant) return;

    this.savingTenant = true;
    this.tenantError = '';
    this.tenantSuccess = '';
    this.createdTenantId = '';
    this.createdTenantPrintKey = '';
    const { tenantId, nome } = this.tenantForm.getRawValue();

    this.api.criarTenant(tenantId!, nome!).subscribe({
      next: (createdTenant) => {
        this.savingTenant = false;
        this.tenantSuccess = 'Tenant cadastrado com sucesso. Guarde a print key e envie ao cliente.';
        this.createdTenantId = createdTenant.tenantId;
        this.createdTenantPrintKey = createdTenant.printKey;
        this.tenantForm.reset();
        this.carregarTenants();
      },
      error: (err) => {
        this.savingTenant = false;
        this.tenantError = err?.error?.message || 'Falha ao cadastrar tenant.';
      }
    });
  }

  submitUser(): void {
    if (this.userForm.invalid || this.savingUser) return;

    this.savingUser = true;
    this.userError = '';
    this.userSuccess = '';

    const payload = this.userForm.getRawValue() as {
      tenantId: string;
      username: string;
      nomeExibicao: string;
      password: string;
      role: 'ADMIN' | 'ATENDENTE';
    };

    this.api.criarUsuarioPainel(payload).subscribe({
      next: () => {
        this.savingUser = false;
        this.userSuccess = 'Usuário cadastrado com sucesso.';
        this.userForm.patchValue({ username: '', nomeExibicao: '', password: '', role: 'ATENDENTE' });
      },
      error: (err) => {
        this.savingUser = false;
        this.userError = err?.error?.message || 'Falha ao cadastrar usuário.';
      }
    });
  }

  toggleTenantsCollapse(): void {
    this.tenantsCollapsed = !this.tenantsCollapsed;
  }

  buildGuestOrderLink(tenantId: string): string {
    return `${window.location.origin}/guest/${encodeURIComponent(tenantId)}`;
  }

  async copyGuestLink(tenantId: string): Promise<void> {
    const link = this.buildGuestOrderLink(tenantId);
    const copiado = await this.writeClipboard(link);
    this.copyFeedbackByTenant[tenantId] = copiado ? 'Link copiado' : 'Falha ao copiar';
    setTimeout(() => {
      delete this.copyFeedbackByTenant[tenantId];
    }, 2500);
  }

  private async writeClipboard(value: string): Promise<boolean> {
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(value);
        return true;
      }
      const textarea = document.createElement('textarea');
      textarea.value = value;
      textarea.setAttribute('readonly', '');
      textarea.style.position = 'absolute';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      textarea.select();
      const success = document.execCommand('copy');
      document.body.removeChild(textarea);
      return success;
    } catch {
      return false;
    }
  }
}
