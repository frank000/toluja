import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../core/api.service';
import { Item, PedidoItem, Segmento, Subitem } from '../core/models';

type CarrinhoEntry = {
  key: string;
  item: Item;
  quantidade: number;
  subitens: Subitem[];
};

type PedidoResumo = {
  codigo: string;
  senhaChamada: number;
  criadoEm: string;
  total: number;
  clienteNome: string;
  clienteTelefone: string;
  tipoAtendimento: 'RETIRADA' | 'ENTREGA';
  observacao?: string;
  itens: Array<{
    nome: string;
    quantidade: number;
    subitens: string[];
    subtotal: number;
  }>;
};

@Component({
  selector: 'app-guest-pedido',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './guest-pedido.component.html',
  styleUrl: './guest-pedido.component.css'
})
export class GuestPedidoComponent implements OnInit {
  tenantId = '';
  itens: Item[] = [];
  segmentos: Segmento[] = [];
  carrinho: Record<string, CarrinhoEntry> = {};
  observacao = '';
  mensagemSucesso = '';
  erro = '';
  entregaAtiva = false;
  informacaoTelaResumo = '';
  whatsappNumeroEmpresa = '';
  modalFechamentoAberto = false;
  etapaModalResumo: 'FORMULARIO' | 'RESULTADO' = 'FORMULARIO';
  erroModalResumo = '';
  enviandoPedido = false;
  clienteNome = '';
  clienteTelefone = '';
  atendimentoEntrega = false;
  resumoPedidoAtual: PedidoResumo | null = null;
  abaSegmentoAtiva: number | 'sem-segmento' | null = null;

  itemEmConfiguracao: Item | null = null;
  subitemIdsSelecionados = new Set<number>();

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.tenantId = (this.route.snapshot.paramMap.get('tenantId') || '').trim();
    if (!this.tenantId) {
      this.erro = 'Tenant ID inválido.';
      return;
    }
    this.carregarSegmentos();
    this.carregarTodosItens(0, []);
    this.carregarConfiguracaoTenant();
    this.carregarRascunhoLocal();
    this.carregarResumoLocal();
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
    this.salvarRascunhoLocal();
  }

  diminuir(key: string): void {
    const entry = this.carrinho[key];
    if (!entry) return;
    entry.quantidade -= 1;
    if (entry.quantidade <= 0) this.remover(key);
    this.salvarRascunhoLocal();
  }

  remover(key: string): void {
    delete this.carrinho[key];
    this.salvarRascunhoLocal();
  }

  enviarPedido(): void {
    this.erro = '';
    this.mensagemSucesso = '';
    if (!this.carrinhoEntries.length) {
      this.erro = 'Adicione itens ao carrinho.';
      return;
    }
    this.erroModalResumo = '';
    this.modalFechamentoAberto = true;
    this.etapaModalResumo = 'FORMULARIO';
    if (!this.entregaAtiva) {
      this.atendimentoEntrega = false;
    }
    this.salvarRascunhoLocal();
  }

  confirmarEnvioPedido(): void {
    this.erroModalResumo = '';
    this.mensagemSucesso = '';
    const nome = this.clienteNome.trim();
    const telefone = this.clienteTelefone.trim();
    if (!nome) {
      this.erroModalResumo = 'Informe o nome do cliente.';
      return;
    }
    if (!telefone) {
      this.erroModalResumo = 'Informe o telefone do cliente.';
      return;
    }
    const tipoAtendimento: 'RETIRADA' | 'ENTREGA' = this.atendimentoEntrega ? 'ENTREGA' : 'RETIRADA';
    if (tipoAtendimento === 'ENTREGA' && !this.entregaAtiva) {
      this.erroModalResumo = 'Entrega não disponível para este tenant.';
      return;
    }

    const itens: PedidoItem[] = Object.values(this.carrinho).map((entry) => ({
      itemId: entry.item.id,
      quantidade: entry.quantidade,
      subitemIds: entry.subitens.map((subitem) => subitem.id)
    }));
    if (!itens.length) {
      this.erroModalResumo = 'Adicione itens ao carrinho.';
      return;
    }

    const itensResumo = this.carrinhoEntries.map((entry) => ({
      nome: entry.item.nome,
      quantidade: entry.quantidade,
      subitens: entry.subitens.map((s) => s.nome),
      subtotal: this.totalEntry(entry)
    }));

    this.enviandoPedido = true;
    this.api.criarPedidoGuest(this.tenantId, {
      itens,
      observacao: this.observacao || undefined,
      clienteNome: nome,
      clienteTelefone: telefone,
      tipoAtendimento
    }).subscribe({
      next: (pedido) => {
        this.enviandoPedido = false;
        this.carrinho = {};
        this.etapaModalResumo = 'RESULTADO';
        this.resumoPedidoAtual = {
          codigo: pedido.codigo,
          senhaChamada: pedido.senhaChamada,
          criadoEm: pedido.criadoEm,
          total: pedido.total,
          clienteNome: nome,
          clienteTelefone: telefone,
          tipoAtendimento,
          observacao: this.observacao || undefined,
          itens: itensResumo
        };
        this.salvarResumoLocal();
        this.limparRascunhoLocal();
        this.observacao = '';
        this.clienteNome = '';
        this.clienteTelefone = '';
        this.atendimentoEntrega = false;
        this.mensagemSucesso = `Pedido enviado com sucesso. Senha: ${this.formatarSenha(pedido.senhaChamada)} | Código: ${pedido.codigo}`;
      },
      error: (err) => {
        this.enviandoPedido = false;
        this.erroModalResumo = err?.error?.message || 'Falha ao enviar pedido.';
      }
    });
  }

  fecharModalFechamento(): void {
    if (this.enviandoPedido) return;
    this.modalFechamentoAberto = false;
    this.etapaModalResumo = 'FORMULARIO';
    this.erroModalResumo = '';
    this.salvarRascunhoLocal();
  }

  acompanharNoWhatsapp(): void {
    const numero = (this.whatsappNumeroEmpresa || '').replace(/\D/g, '');
    if (!numero || !this.resumoPedidoAtual) return;
    const text = encodeURIComponent(this.mensagemWhatsapp(this.resumoPedidoAtual));
    const url = `https://wa.me/${numero}?text=${text}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  hasWhatsappEmpresa(): boolean {
    return !!(this.whatsappNumeroEmpresa || '').replace(/\D/g, '');
  }

  onDraftChange(): void {
    this.salvarRascunhoLocal();
  }

  abrirUltimoResumo(): void {
    if (!this.resumoPedidoAtual) return;
    this.modalFechamentoAberto = true;
    this.etapaModalResumo = 'RESULTADO';
    this.erroModalResumo = '';
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
      this.salvarRascunhoLocal();
      return;
    }
    this.carrinho[key] = { key, item, quantidade: 1, subitens };
    this.salvarRascunhoLocal();
  }

  private gerarKey(itemId: number, subitemIds: number[]): string {
    const ids = [...subitemIds].sort((a, b) => a - b).join('-');
    return `${itemId}:${ids}`;
  }

  private formatarSenha(senha: number): string {
    return senha.toString().padStart(2, '0');
  }

  get itensDaAbaAtiva(): Item[] {
    if (this.abaSegmentoAtiva === null) {
      return this.itens;
    }
    if (this.abaSegmentoAtiva === 'sem-segmento') {
      return this.itens.filter((item) => !item.segmento);
    }
    return this.itens.filter((item) => item.segmento?.id === this.abaSegmentoAtiva);
  }

  selecionarAbaSegmento(abaId: number | 'sem-segmento'): void {
    this.abaSegmentoAtiva = abaId;
  }

  hasItensSemSegmento(): boolean {
    return this.itens.some((item) => !item.segmento);
  }

  private carregarSegmentos(): void {
    this.api.listarSegmentosGuest(this.tenantId).subscribe({
      next: (segmentos) => {
        this.segmentos = segmentos;
        this.ajustarAbaAtiva();
      },
      error: () => (this.erro = 'Falha ao carregar segmentos.')
    });
  }

  private carregarConfiguracaoTenant(): void {
    this.api.obterConfiguracaoGuest(this.tenantId).subscribe({
      next: (config) => {
        this.entregaAtiva = !!config.entregaAtiva;
        this.informacaoTelaResumo = config.informacaoTelaResumo || '';
        this.whatsappNumeroEmpresa = config.whatsappNumero || '';
        if (!this.entregaAtiva) {
          this.atendimentoEntrega = false;
        }
      },
      error: () => {
        this.entregaAtiva = false;
        this.informacaoTelaResumo = '';
        this.whatsappNumeroEmpresa = '';
      }
    });
  }

  private carregarTodosItens(page: number, acumulado: Item[]): void {
    this.api.listarItensGuest(this.tenantId, { page, size: 100 }).subscribe({
      next: (response) => {
        const novosItens = [...acumulado, ...response.itens];
        if (response.last) {
          this.itens = novosItens;
          this.ajustarAbaAtiva();
          return;
        }
        this.carregarTodosItens(page + 1, novosItens);
      },
      error: () => (this.erro = 'Falha ao carregar itens.')
    });
  }

  private ajustarAbaAtiva(): void {
    const idsSegmentos = new Set(this.segmentos.map((segmento) => segmento.id));
    const existeSemSegmento = this.hasItensSemSegmento();

    if (typeof this.abaSegmentoAtiva === 'number' && idsSegmentos.has(this.abaSegmentoAtiva)) {
      return;
    }
    if (this.abaSegmentoAtiva === 'sem-segmento' && existeSemSegmento) {
      return;
    }
    if (this.segmentos.length > 0) {
      this.abaSegmentoAtiva = this.segmentos[0].id;
      return;
    }
    this.abaSegmentoAtiva = existeSemSegmento ? 'sem-segmento' : null;
  }

  private resumoStorageKey(): string {
    return `guest_order_summary_${this.tenantId}`;
  }

  private draftStorageKey(): string {
    return `guest_order_draft_${this.tenantId}`;
  }

  private salvarResumoLocal(): void {
    if (!this.resumoPedidoAtual) return;
    localStorage.setItem(this.resumoStorageKey(), JSON.stringify(this.resumoPedidoAtual));
  }

  private carregarResumoLocal(): void {
    const raw = localStorage.getItem(this.resumoStorageKey());
    if (!raw) return;
    try {
      this.resumoPedidoAtual = JSON.parse(raw) as PedidoResumo;
    } catch {
      this.resumoPedidoAtual = null;
    }
  }

  private salvarRascunhoLocal(): void {
    const draft = {
      carrinho: this.carrinho,
      observacao: this.observacao,
      clienteNome: this.clienteNome,
      clienteTelefone: this.clienteTelefone,
      atendimentoEntrega: this.atendimentoEntrega
    };
    localStorage.setItem(this.draftStorageKey(), JSON.stringify(draft));
  }

  private carregarRascunhoLocal(): void {
    const raw = localStorage.getItem(this.draftStorageKey());
    if (!raw) return;
    try {
      const parsed = JSON.parse(raw) as {
        carrinho: Record<string, CarrinhoEntry>;
        observacao: string;
        clienteNome: string;
        clienteTelefone: string;
        atendimentoEntrega: boolean;
      };
      this.carrinho = parsed.carrinho || {};
      this.observacao = parsed.observacao || '';
      this.clienteNome = parsed.clienteNome || '';
      this.clienteTelefone = parsed.clienteTelefone || '';
      this.atendimentoEntrega = !!parsed.atendimentoEntrega;
    } catch {
      this.carrinho = {};
    }
  }

  private limparRascunhoLocal(): void {
    localStorage.removeItem(this.draftStorageKey());
  }

  private mensagemWhatsapp(resumo: PedidoResumo): string {
    const itens = resumo.itens
      .map((i) => {
        const adicionais = i.subitens.length ? ` | adicionais: ${i.subitens.join(', ')}` : '';
        return `- ${i.nome} x${i.quantidade}${adicionais}`;
      })
      .join('\n');
    const senha = this.formatarSenha(resumo.senhaChamada);
    return `Novo pedido Toluja Go\nCódigo: ${resumo.codigo}\nSenha: ${senha}\nCliente: ${resumo.clienteNome}\nTelefone: ${resumo.clienteTelefone}\nTipo: ${resumo.tipoAtendimento}\nTotal: R$ ${resumo.total.toFixed(2)}\nItens:\n${itens}`;
  }
}
