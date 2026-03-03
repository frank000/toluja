import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Item, NovoItem, Pedido, PedidoItem, SubitemCategoria, TenantSummary } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  listarItens() {
    return this.http.get<Item[]>('/api/itens');
  }

  criarItem(payload: NovoItem) {
    return this.http.post<Item>('/api/itens', payload);
  }

  atualizarItem(itemId: number, payload: { nome: string; preco: number }) {
    return this.http.put<Item>(`/api/itens/${itemId}`, payload);
  }

  excluirItem(itemId: number) {
    return this.http.delete<void>(`/api/itens/${itemId}`);
  }

  listarCategoriasSubitens() {
    return this.http.get<SubitemCategoria[]>('/api/subitens/categorias');
  }

  criarCategoriaSubitem(nome: string) {
    return this.http.post<SubitemCategoria>('/api/subitens/categorias', { nome });
  }

  criarSubitem(categoriaId: number, payload: { nome: string; preco: number }) {
    return this.http.post<SubitemCategoria>(`/api/subitens/categorias/${categoriaId}/subitens`, payload);
  }

  excluirSubitem(categoriaId: number, subitemId: number) {
    return this.http.delete<SubitemCategoria>(`/api/subitens/categorias/${categoriaId}/subitens/${subitemId}`);
  }

  criarPedido(itens: PedidoItem[], observacao?: string) {
    return this.http.post<Pedido>('/api/pedidos', { itens, observacao });
  }

  listarPedidos() {
    return this.http.get<Pedido[]>('/api/pedidos');
  }

  listarTenantsPainel() {
    return this.http.get<TenantSummary[]>('/api/superadmin/tenants');
  }

  criarTenant(tenantId: string, nome: string) {
    return this.http.post<TenantSummary>('/api/superadmin/tenants', { tenantId, nome });
  }

  criarUsuarioPainel(payload: {
    tenantId: string;
    username: string;
    password: string;
    nomeExibicao: string;
    role: 'ADMIN' | 'ATENDENTE';
  }) {
    return this.http.post<void>('/api/superadmin/users', payload);
  }
}
