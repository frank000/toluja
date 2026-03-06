import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { CreatedTenant, Item, NovoItem, PagedResponse, Pedido, PedidoItem, Segmento, SubitemCategoria, TenantSummary } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  listarItens(params?: { nome?: string; page?: number; size?: number }) {
    let queryParams = new HttpParams();
    if (params?.nome && params.nome.trim()) {
      queryParams = queryParams.set('nome', params.nome.trim());
    }
    if (params?.page !== undefined) {
      queryParams = queryParams.set('page', params.page);
    }
    if (params?.size !== undefined) {
      queryParams = queryParams.set('size', params.size);
    }
    return this.http.get<PagedResponse<Item>>('/api/itens', { params: queryParams });
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

  listarSegmentos() {
    return this.http.get<Segmento[]>('/api/segmentos');
  }

  criarSegmento(payload: { nome: string; cor: string; icone: string }) {
    return this.http.post<Segmento>('/api/segmentos', payload);
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
    return this.http.post<CreatedTenant>('/api/superadmin/tenants', { tenantId, nome });
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
