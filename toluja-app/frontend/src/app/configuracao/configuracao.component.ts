import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'app-configuracao',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './configuracao.component.html'
})
export class ConfiguracaoComponent implements OnInit {
  entregaAtiva = false;
  informacaoTelaResumo = '';
  whatsappNumero = '';
  carregando = false;
  salvando = false;
  erro = '';
  sucesso = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.api.obterConfiguracaoTenant().subscribe({
      next: (config) => {
        this.entregaAtiva = !!config.entregaAtiva;
        this.informacaoTelaResumo = config.informacaoTelaResumo || '';
        this.whatsappNumero = this.maskWhatsapp(config.whatsappNumero || '');
        this.carregando = false;
      },
      error: () => {
        this.carregando = false;
        this.erro = 'Não foi possível carregar as configurações.';
      }
    });
  }

  salvar(): void {
    if (this.salvando) return;
    this.salvando = true;
    this.erro = '';
    this.sucesso = '';
    this.api.salvarConfiguracaoTenant({
      entregaAtiva: this.entregaAtiva,
      informacaoTelaResumo: this.informacaoTelaResumo,
      whatsappNumero: this.whatsappNumeroDigits()
    }).subscribe({
      next: () => {
        this.salvando = false;
        this.sucesso = 'Configurações salvas com sucesso.';
      },
      error: () => {
        this.salvando = false;
        this.erro = 'Não foi possível salvar as configurações.';
      }
    });
  }

  aoDigitarWhatsapp(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.whatsappNumero = this.maskWhatsapp(input.value);
  }

  private whatsappNumeroDigits(): string {
    return this.whatsappNumero.replace(/\D/g, '');
  }

  private maskWhatsapp(value: string): string {
    let digits = value.replace(/\D/g, '');
    if (digits.startsWith('55')) {
      digits = digits.slice(0, 13);
    } else {
      digits = ('55' + digits).slice(0, 13);
    }
    const country = digits.slice(0, 2);
    const ddd = digits.slice(2, 4);
    const local = digits.slice(4);
    const isMobile = local.length > 8;
    const middle = isMobile ? local.slice(0, 5) : local.slice(0, 4);
    const end = isMobile ? local.slice(5, 9) : local.slice(4, 8);

    let masked = `(${country}`;
    if (country.length === 2) masked += ')';
    if (ddd) masked += ` ${ddd}`;
    if (middle) masked += ` ${middle}`;
    if (end) masked += `-${end}`;
    return masked;
  }
}
