import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Pedido } from '../core/models';

@Component({
  selector: 'app-pedidos-lista',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedidos-lista.component.html',
  styleUrl: './pedidos-lista.component.css'
})
export class PedidosListaComponent implements OnInit {
  pedidos: Pedido[] = [];
  filtroCodigo = '';
  filtroSenha = '';
  filtroStatus = '';
  filtroTexto = '';
  filtroTotalMinimo: number | null = null;
  filtroTotalMaximo: number | null = null;
  filtroDataInicio = '';
  filtroDataFim = '';

  paginaAtual = 1;
  tamanhoPagina = 10;
  readonly opcoesTamanhoPagina = [10, 20, 50];
  imprimindoPedidoId: number | null = null;
  modalVisualizacaoAberta = false;
  pedidoVisualizacao: Pedido | null = null;
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
    return Math.max(1, Math.ceil(this.pedidosFiltrados.length / this.tamanhoPagina));
  }

  get pedidosPaginados(): Pedido[] {
    const inicio = (this.paginaAtual - 1) * this.tamanhoPagina;
    return this.pedidosFiltrados.slice(inicio, inicio + this.tamanhoPagina);
  }

  get pedidosFiltrados(): Pedido[] {
    const codigo = this.filtroCodigo.trim().toLowerCase();
    const senha = this.filtroSenha.trim();
    const status = this.filtroStatus.trim().toUpperCase();
    const texto = this.filtroTexto.trim().toLowerCase();
    const dataInicio = this.filtroDataInicio ? new Date(`${this.filtroDataInicio}T00:00:00`) : null;
    const dataFim = this.filtroDataFim ? new Date(`${this.filtroDataFim}T23:59:59.999`) : null;

    return this.pedidos.filter((pedido) => {
      if (codigo && !pedido.codigo.toLowerCase().includes(codigo)) return false;
      if (senha && !String(pedido.senhaChamada).includes(senha)) return false;
      if (status && (pedido.status || '').toUpperCase() !== status) return false;

      if (this.filtroTotalMinimo != null && Number.isFinite(this.filtroTotalMinimo) && pedido.total < this.filtroTotalMinimo) {
        return false;
      }
      if (this.filtroTotalMaximo != null && Number.isFinite(this.filtroTotalMaximo) && pedido.total > this.filtroTotalMaximo) {
        return false;
      }

      const criadoEm = new Date(pedido.criadoEm);
      if (dataInicio && criadoEm < dataInicio) return false;
      if (dataFim && criadoEm > dataFim) return false;

      if (texto) {
        const itensTexto = pedido.itens
          .map((item) => `${item.nomeSnapshot || item.nomeItem || ''} ${(item.subitens || []).map((s) => s.nomeSnapshot).join(' ')}`)
          .join(' ')
          .toLowerCase();
        const observacao = (pedido.observacao || '').toLowerCase();
        const alvo = `${pedido.codigo} ${pedido.status} ${pedido.senhaChamada} ${observacao} ${itensTexto}`.toLowerCase();
        if (!alvo.includes(texto)) return false;
      }

      return true;
    });
  }

  get statusesDisponiveis(): string[] {
    return [...new Set(this.pedidos.map((pedido) => (pedido.status || '').toUpperCase()).filter(Boolean))].sort();
  }

  get inicioRegistro(): number {
    if (this.pedidosFiltrados.length === 0) return 0;
    return (this.paginaAtual - 1) * this.tamanhoPagina + 1;
  }

  get fimRegistro(): number {
    return Math.min(this.paginaAtual * this.tamanhoPagina, this.pedidosFiltrados.length);
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

  aplicarFiltros(): void {
    this.paginaAtual = 1;
  }

  limparFiltros(): void {
    this.filtroCodigo = '';
    this.filtroSenha = '';
    this.filtroStatus = '';
    this.filtroTexto = '';
    this.filtroTotalMinimo = null;
    this.filtroTotalMaximo = null;
    this.filtroDataInicio = '';
    this.filtroDataFim = '';
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

  visualizar(pedido: Pedido): void {
    this.pedidoVisualizacao = pedido;
    this.modalVisualizacaoAberta = true;
  }

  fecharVisualizacao(): void {
    this.modalVisualizacaoAberta = false;
    this.pedidoVisualizacao = null;
  }
}
