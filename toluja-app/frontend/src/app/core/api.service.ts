import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { HttpParams } from '@angular/common/http';
import { CreatedTenant, Item, NovoItem, PagedResponse, Pedido, PedidoGuestPayload, PedidoItem, Segmento, SubitemCategoria, TenantConfig, TenantSummary } from './models';

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
    if (payload.imagem) {
      const formData = new FormData();
      const body = {
        nome: payload.nome,
        preco: payload.preco,
        categoriaIds: payload.categoriaIds,
        segmentoId: payload.segmentoId
      };
      formData.append('payload', new Blob([JSON.stringify(body)], { type: 'application/json' }));
      formData.append('imagem', payload.imagem);
      return this.http.post<Item>('/api/itens', formData);
    }
    const body = {
      nome: payload.nome,
      preco: payload.preco,
      categoriaIds: payload.categoriaIds,
      segmentoId: payload.segmentoId
    };
    return this.http.post<Item>('/api/itens', body);
  }

  atualizarItem(itemId: number, payload: { nome: string; preco: number; categoriaIds?: number[]; imagem?: File | null }) {
    if (payload.imagem) {
      const formData = new FormData();
      const body = {
        nome: payload.nome,
        preco: payload.preco,
        categoriaIds: payload.categoriaIds
      };
      formData.append('payload', new Blob([JSON.stringify(body)], { type: 'application/json' }));
      formData.append('imagem', payload.imagem);
      return this.http.put<Item>(`/api/itens/${itemId}`, formData);
    }
    return this.http.put<Item>(`/api/itens/${itemId}`, {
      nome: payload.nome,
      preco: payload.preco,
      categoriaIds: payload.categoriaIds
    });
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

  excluirSegmento(segmentoId: number) {
    return this.http.delete<void>(`/api/segmentos/${segmentoId}`);
  }

  atualizarOrdemSegmentos(segmentoIds: number[]) {
    return this.http.put<void>('/api/segmentos/ordem', { segmentoIds });
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

  criarPedidoGuest(tenantId: string, payload: PedidoGuestPayload) {
    return this.http.post<Pedido>(`/api/public/tenants/${encodeURIComponent(tenantId)}/pedidos`, payload);
  }

  listarPedidos() {
    return this.http.get<Pedido[]>('/api/pedidos');
  }

  reimprimirPedido(orderId: number) {
    return this.http.post<void>(`/api/pedidos/${orderId}/imprimir`, {});
  }

  listarItensGuest(tenantId: string, params?: { nome?: string; page?: number; size?: number }) {
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
    return this.http.get<PagedResponse<Item>>(`/api/public/tenants/${encodeURIComponent(tenantId)}/itens`, {
      params: queryParams
    });
  }

  listarSegmentosGuest(tenantId: string) {
    return this.http.get<Segmento[]>(`/api/public/tenants/${encodeURIComponent(tenantId)}/segmentos`);
  }

  obterConfiguracaoGuest(tenantId: string) {
    return this.http.get<TenantConfig>(`/api/public/tenants/${encodeURIComponent(tenantId)}/configuracao`);
  }

  obterConfiguracaoTenant() {
    return this.http.get<TenantConfig>('/api/configuracao/tenant');
  }

  salvarConfiguracaoTenant(payload: TenantConfig) {
    return this.http.put<TenantConfig>('/api/configuracao/tenant', payload);
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
