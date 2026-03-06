import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { Item, PedidoItem, Subitem } from '../core/models';

type CarrinhoEntry = {
  key: string;
  item: Item;
  quantidade: number;
  subitens: Subitem[];
};

@Component({
  selector: 'app-pedido',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pedido.component.html',
  styleUrl: './pedido.component.css'
})
export class PedidoComponent implements OnInit {
  itens: Item[] = [];
  carrinho: Record<string, CarrinhoEntry> = {};
  observacao = '';
  mensagemSucesso = '';
  erro = '';

  itemEmConfiguracao: Item | null = null;
  subitemIdsSelecionados = new Set<number>();

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listarItens({ page: 0, size: 100 }).subscribe((response) => (this.itens = response.itens));
  }

  selecionarItem(item: Item): void {
    this.mensagemSucesso = '';
    this.erro = '';

    if (!this.temSubitensDisponiveis(item)) {
      this.adicionarComSubitens(item, []);
      return;
    }

    this.itemEmConfiguracao = item;
    this.subitemIdsSelecionados = new Set<number>();
  }

  confirmarConfiguracao(): void {
    if (!this.itemEmConfiguracao) return;
    const subitens = this.subitensDisponiveis(this.itemEmConfiguracao).filter((subitem) =>
      this.subitemIdsSelecionados.has(subitem.id)
    );
    this.adicionarComSubitens(this.itemEmConfiguracao, subitens);
    this.cancelarConfiguracao();
  }

  cancelarConfiguracao(): void {
    this.itemEmConfiguracao = null;
    this.subitemIdsSelecionados.clear();
  }

  toggleSubitem(subitemId: number, checked: boolean): void {
    if (checked) {
      this.subitemIdsSelecionados.add(subitemId);
      return;
    }
    this.subitemIdsSelecionados.delete(subitemId);
  }

  aumentar(key: string): void {
    const entry = this.carrinho[key];
    if (entry) entry.quantidade += 1;
  }

  diminuir(key: string): void {
    const entry = this.carrinho[key];
    if (!entry) return;
    entry.quantidade -= 1;
    if (entry.quantidade <= 0) this.remover(key);
  }

  remover(key: string): void {
    delete this.carrinho[key];
  }

  enviarPedido(): void {
    this.erro = '';
    this.mensagemSucesso = '';

    const itens: PedidoItem[] = Object.values(this.carrinho).map((entry) => ({
      itemId: entry.item.id,
      quantidade: entry.quantidade,
      subitemIds: entry.subitens.map((subitem) => subitem.id)
    }));

    if (!itens.length) {
      this.erro = 'Adicione itens ao carrinho.';
      return;
    }

    this.api.criarPedido(itens, this.observacao || undefined).subscribe({
      next: (pedido) => {
        this.carrinho = {};
        this.observacao = '';
        this.mensagemSucesso = `Pedido enviado com sucesso. Senha: ${this.formatarSenha(pedido.senhaChamada)} | Código: ${pedido.codigo}`;
      },
      error: () => (this.erro = 'Falha ao enviar pedido.')
    });
  }

  get carrinhoEntries(): CarrinhoEntry[] {
    return Object.values(this.carrinho);
  }

  get total(): number {
    return this.carrinhoEntries.reduce((acc, entry) => acc + this.totalEntry(entry), 0);
  }

  totalEntry(entry: CarrinhoEntry): number {
    const totalSubitens = entry.subitens.reduce((acc, subitem) => acc + subitem.preco, 0);
    return (entry.item.preco + totalSubitens) * entry.quantidade;
  }

  subitensDoEntry(entry: CarrinhoEntry): string {
    return entry.subitens.length ? entry.subitens.map((subitem) => subitem.nome).join(', ') : '-';
  }

  subitensAgrupados(item: Item): Array<{ categoriaNome: string; subitens: Subitem[] }> {
    return item.categorias
      .filter((categoria) => categoria.subitens?.length)
      .map((categoria) => ({
        categoriaNome: categoria.nome,
        subitens: categoria.subitens
      }));
  }

  private temSubitensDisponiveis(item: Item): boolean {
    return this.subitensDisponiveis(item).length > 0;
  }

  private subitensDisponiveis(item: Item): Subitem[] {
    return item.categorias.flatMap((categoria) => categoria.subitens ?? []);
  }

  private adicionarComSubitens(item: Item, subitens: Subitem[]): void {
    const key = this.gerarKey(item.id, subitens.map((subitem) => subitem.id));
    const atual = this.carrinho[key];
    if (atual) {
      atual.quantidade += 1;
      return;
    }
    this.carrinho[key] = { key, item, quantidade: 1, subitens };
  }

  private gerarKey(itemId: number, subitemIds: number[]): string {
    const ids = [...subitemIds].sort((a, b) => a - b).join('-');
    return `${itemId}:${ids}`;
  }

  private formatarSenha(senha: number): string {
    return senha.toString().padStart(2, '0');
  }
}
