import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Pedido } from '../core/models';

@Component({
  selector: 'app-pedidos-lista',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pedidos-lista.component.html',
  styleUrl: './pedidos-lista.component.css'
})
export class PedidosListaComponent implements OnInit {
  pedidos: Pedido[] = [];
  paginaAtual = 1;
  tamanhoPagina = 10;
  readonly opcoesTamanhoPagina = [10, 20, 50];
  imprimindoPedidoId: number | null = null;
  erro = '';
  sucesso = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listarPedidos().subscribe((pedidos) => {
      this.pedidos = [...pedidos].sort((a, b) => {
        return new Date(b.criadoEm).getTime() - new Date(a.criadoEm).getTime();
      });
      if (this.paginaAtual > this.totalPaginas) {
        this.paginaAtual = this.totalPaginas;
      }
    });
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.pedidos.length / this.tamanhoPagina));
  }

  get pedidosPaginados(): Pedido[] {
    const inicio = (this.paginaAtual - 1) * this.tamanhoPagina;
    return this.pedidos.slice(inicio, inicio + this.tamanhoPagina);
  }

  get inicioRegistro(): number {
    if (this.pedidos.length === 0) return 0;
    return (this.paginaAtual - 1) * this.tamanhoPagina + 1;
  }

  get fimRegistro(): number {
    return Math.min(this.paginaAtual * this.tamanhoPagina, this.pedidos.length);
  }

  irParaPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) return;
    this.paginaAtual = pagina;
  }

  alterarTamanhoPagina(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    if (!Number.isFinite(value) || value < 1) return;
    this.tamanhoPagina = value;
    this.paginaAtual = 1;
  }

  statusClass(status: string): string {
    const normalized = (status || '').toUpperCase();
    if (normalized === 'ABERTO') return 'status-badge status-badge--aberto';
    if (normalized === 'ERRO_IMPRESSAO') return 'status-badge status-badge--erro-impressao';
    if (normalized === 'IMPRESSO') return 'status-badge status-badge--impresso';
    if (normalized === 'CANCELADO') return 'status-badge status-badge--cancelado';
    return 'status-badge status-badge--default';
  }

  reimprimir(pedido: Pedido): void {
    this.imprimindoPedidoId = pedido.id;
    this.erro = '';
    this.sucesso = '';
    this.api.reimprimirPedido(pedido.id).subscribe({
      next: () => {
        this.imprimindoPedidoId = null;
        this.sucesso = `Pedido ${pedido.codigo} enviado para impressão novamente.`;
      },
      error: () => {
        this.imprimindoPedidoId = null;
        this.erro = `Falha ao reimprimir o pedido ${pedido.codigo}.`;
      }
    });
  }
}
