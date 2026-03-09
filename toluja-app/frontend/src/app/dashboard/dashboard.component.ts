import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Pedido } from '../core/models';

type PeriodoFiltro = '24h' | '48h' | '1w' | '1m' | '3m' | '6m' | '12m';

type SeriePonto = {
  label: string;
  valor: number;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  pedidos: Pedido[] = [];
  erro = '';

  periodoSelecionado: PeriodoFiltro = '24h';
  readonly opcoesPeriodo: Array<{ value: PeriodoFiltro; label: string }> = [
    { value: '24h', label: '24h' },
    { value: '48h', label: '48h' },
    { value: '1w', label: '1 semana' },
    { value: '1m', label: '1 mês' },
    { value: '3m', label: '3 meses' },
    { value: '6m', label: '6 meses' },
    { value: '12m', label: '12 meses' }
  ];

  serieVendasDia: SeriePonto[] = [];
  seriePedidosDia: SeriePonto[] = [];

  totalPedidos = 0;
  totalPedidosWhatsapp = 0;
  totalVendas = 0;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listarPedidos().subscribe({
      next: (pedidos) => {
        this.pedidos = pedidos;
        this.recalcularDashboard();
      },
      error: () => {
        this.erro = 'Falha ao carregar dados do dashboard.';
      }
    });
  }

  onPeriodoChange(): void {
    this.recalcularDashboard();
  }

  alturaBarra(serie: SeriePonto[], valor: number): number {
    const max = Math.max(...serie.map((p) => p.valor), 0);
    if (max <= 0) return 0;
    return (valor / max) * 100;
  }

  private recalcularDashboard(): void {
    const inicio = this.inicioPeriodo(this.periodoSelecionado);
    const fim = new Date();
    const pedidosFiltrados = this.pedidos.filter((pedido) => {
      const criado = new Date(pedido.criadoEm);
      return criado >= inicio && criado <= fim;
    });

    this.totalPedidos = pedidosFiltrados.length;
    this.totalPedidosWhatsapp = pedidosFiltrados.filter((pedido) => (pedido.status || '').toUpperCase() === 'WHATSAPP').length;
    this.totalVendas = pedidosFiltrados.reduce((acc, pedido) => acc + (pedido.total || 0), 0);

    const mapaPedidos = new Map<string, number>();
    const mapaVendas = new Map<string, number>();

    pedidosFiltrados.forEach((pedido) => {
      const criado = new Date(pedido.criadoEm);
      const chaveDia = this.chaveData(criado);
      mapaPedidos.set(chaveDia, (mapaPedidos.get(chaveDia) || 0) + 1);
      mapaVendas.set(chaveDia, (mapaVendas.get(chaveDia) || 0) + (pedido.total || 0));
    });

    const dias = this.listarDiasPeriodo(inicio, fim);
    this.seriePedidosDia = dias.map((dia) => ({
      label: this.labelData(dia),
      valor: mapaPedidos.get(dia) || 0
    }));
    this.serieVendasDia = dias.map((dia) => ({
      label: this.labelData(dia),
      valor: mapaVendas.get(dia) || 0
    }));
  }

  private inicioPeriodo(periodo: PeriodoFiltro): Date {
    const agora = new Date();
    const inicio = new Date(agora);

    if (periodo === '24h') {
      inicio.setHours(inicio.getHours() - 24);
      return inicio;
    }
    if (periodo === '48h') {
      inicio.setHours(inicio.getHours() - 48);
      return inicio;
    }
    if (periodo === '1w') {
      inicio.setDate(inicio.getDate() - 7);
      return inicio;
    }
    if (periodo === '1m') {
      inicio.setMonth(inicio.getMonth() - 1);
      return inicio;
    }
    if (periodo === '3m') {
      inicio.setMonth(inicio.getMonth() - 3);
      return inicio;
    }
    if (periodo === '6m') {
      inicio.setMonth(inicio.getMonth() - 6);
      return inicio;
    }

    inicio.setMonth(inicio.getMonth() - 12);
    return inicio;
  }

  private listarDiasPeriodo(inicio: Date, fim: Date): string[] {
    const dias: string[] = [];
    const cursor = new Date(inicio.getFullYear(), inicio.getMonth(), inicio.getDate());
    const fimDia = new Date(fim.getFullYear(), fim.getMonth(), fim.getDate());

    while (cursor <= fimDia) {
      dias.push(this.chaveData(cursor));
      cursor.setDate(cursor.getDate() + 1);
    }
    return dias;
  }

  private chaveData(data: Date): string {
    const y = data.getFullYear();
    const m = String(data.getMonth() + 1).padStart(2, '0');
    const d = String(data.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private labelData(chave: string): string {
    const [ano, mes, dia] = chave.split('-');
    return `${dia}/${mes}`;
  }
}
