import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Item, PedidoItem } from '../core/models';

@Component({
  selector: 'app-pedido',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedido.component.html',
  styleUrl: './pedido.component.css'
})
export class PedidoComponent implements OnInit {
  itens: Item[] = [];
  carrinho: Record<number, { item: Item; quantidade: number }> = {};
  observacao = '';
  mensagemSucesso = '';
  erro = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listarItens().subscribe((itens) => (this.itens = itens));
  }

  adicionar(item: Item): void {
    this.mensagemSucesso = '';
    const atual = this.carrinho[item.id];
    this.carrinho[item.id] = { item, quantidade: (atual?.quantidade ?? 0) + 1 };
  }

  aumentar(itemId: number): void {
    const entry = this.carrinho[itemId];
    if (entry) entry.quantidade += 1;
  }

  diminuir(itemId: number): void {
    const entry = this.carrinho[itemId];
    if (!entry) return;
    entry.quantidade -= 1;
    if (entry.quantidade <= 0) this.remover(itemId);
  }

  remover(itemId: number): void {
    delete this.carrinho[itemId];
  }

  enviarPedido(): void {
    this.erro = '';
    this.mensagemSucesso = '';

    const itens: PedidoItem[] = Object.values(this.carrinho).map((c) => ({
      itemId: c.item.id,
      quantidade: c.quantidade
    }));

    if (!itens.length) {
      this.erro = 'Adicione itens ao carrinho.';
      return;
    }

    this.api.criarPedido(itens, this.observacao || undefined).subscribe({
      next: (pedido) => {
        this.carrinho = {};
        this.observacao = '';
        this.mensagemSucesso = `Pedido enviado com sucesso. Código: ${pedido.codigo}`;
      },
      error: () => (this.erro = 'Falha ao enviar pedido.')
    });
  }

  get carrinhoEntries() {
    return Object.values(this.carrinho);
  }

  get total(): number {
    return this.carrinhoEntries.reduce((acc, c) => acc + c.item.preco * c.quantidade, 0);
  }
}
