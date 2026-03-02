import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Item, NovoItem, Pedido, PedidoItem } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  listarItens() {
    return this.http.get<Item[]>('/api/itens');
  }

  criarItem(payload: NovoItem) {
    return this.http.post<Item>('/api/itens', payload);
  }

  criarPedido(itens: PedidoItem[], observacao?: string) {
    return this.http.post<Pedido>('/api/pedidos', { itens, observacao });
  }

  listarPedidos() {
    return this.http.get<Pedido[]>('/api/pedidos');
  }
}
