import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Item, Segmento, SubitemCategoria } from '../core/models';

type ModalCadastro = 'itens' | 'subitens' | 'segmentos' | null;

@Component({
  selector: 'app-itens',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './itens.component.html',
  styleUrl: './itens.component.css'
})
export class ItensComponent implements OnInit {
  itens: Item[] = [];
  categorias: SubitemCategoria[] = [];
  segmentos: Segmento[] = [];
  erro = '';
  sucesso = '';
  filtroNome = '';
  categoriasExpandidas = new Set<number>();
  secaoItensExpandida = true;

  paginaAtual = 0;
  tamanhoPagina = 10;
  totalPaginas = 0;
  totalItens = 0;
  primeiraPagina = true;
  ultimaPagina = true;

  modalCadastroAberta: ModalCadastro = null;

  categoriaForm = this.fb.group({
    nome: ['', Validators.required]
  });

  subitemForm = this.fb.group({
    categoriaId: [null as number | null, Validators.required],
    nome: ['', Validators.required],
    preco: [0, [Validators.required, Validators.min(0)]]
  });

  itemForm = this.fb.group({
    nome: ['', Validators.required],
    preco: [0, [Validators.required, Validators.min(0.01)]],
    segmentoId: [null as number | null]
  });

  segmentoForm = this.fb.group({
    nome: ['', Validators.required],
    cor: ['#1A73E8', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    icone: ['bi-tag', Validators.required]
  });

  edicaoForm = this.fb.group({
    nome: ['', Validators.required],
    preco: [0, [Validators.required, Validators.min(0.01)]]
  });

  categoriaIdsSelecionadas = new Set<number>();
  imagemItemSelecionada: File | null = null;
  salvandoItem = false;

  itemEmEdicaoId: number | null = null;
  edicaoModalAberta = false;
  imagemEdicaoSelecionada: File | null = null;
  categoriaIdsEdicaoSelecionadas = new Set<number>();
  salvandoEdicao = false;
  excluindoSegmentoId: number | null = null;
  salvandoOrdemSegmentos = false;

  readonly iconesBasicos = [
    { value: 'bi-cup-straw', label: 'Bebidas' },
    { value: 'bi-cup-hot', label: 'Café/Chá' },
    { value: 'bi-egg-fried', label: 'Refeições' },
    { value: 'bi-basket2', label: 'Mercado' },
    { value: 'bi-ice-cream', label: 'Sobremesas' },
    { value: 'bi-star', label: 'Destaques' },
    { value: 'bi-tag', label: 'Geral' }
  ];

  constructor(private fb: FormBuilder, private api: ApiService, public auth: AuthService) {}

  ngOnInit(): void {
    this.recarregarTela();
  }

  abrirModalCadastro(tipo: Exclude<ModalCadastro, null>): void {
    if (!this.auth.isAdmin()) return;
    this.erro = '';
    this.sucesso = '';
    this.modalCadastroAberta = tipo;
  }

  fecharModalCadastro(): void {
    this.modalCadastroAberta = null;
  }

  modalAberta(tipo: Exclude<ModalCadastro, null>): boolean {
    return this.modalCadastroAberta === tipo;
  }

  toggleSecaoItens(): void {
    this.secaoItensExpandida = !this.secaoItensExpandida;
  }

  aplicarFiltro(): void {
    this.paginaAtual = 0;
    this.carregarItens();
  }

  limparFiltro(): void {
    this.filtroNome = '';
    this.paginaAtual = 0;
    this.carregarItens();
  }

  irParaPagina(page: number): void {
    if (page < 0 || page >= this.totalPaginas || page === this.paginaAtual) {
      return;
    }
    this.paginaAtual = page;
    this.carregarItens();
  }

  paginasVisiveis(): number[] {
    if (this.totalPaginas <= 0) {
      return [];
    }
    const maxPaginas = 5;
    const inicio = Math.max(0, this.paginaAtual - Math.floor(maxPaginas / 2));
    const fim = Math.min(this.totalPaginas, inicio + maxPaginas);
    const inicioAjustado = Math.max(0, fim - maxPaginas);
    return Array.from({ length: fim - inicioAjustado }, (_, i) => inicioAjustado + i);
  }

  cadastrarCategoria(): void {
    if (this.categoriaForm.invalid || !this.auth.isAdmin()) return;
    this.sucesso = '';
    this.erro = '';
    const nome = this.categoriaForm.getRawValue().nome?.trim();
    if (!nome) return;

    this.api.criarCategoriaSubitem(nome).subscribe({
      next: () => {
        this.categoriaForm.reset({ nome: '' });
        this.sucesso = 'Categoria cadastrada com sucesso.';
        this.carregarCategorias();
      },
      error: () => (this.erro = 'Não foi possível cadastrar categoria.')
    });
  }

  cadastrarSubitem(): void {
    if (this.subitemForm.invalid || !this.auth.isAdmin()) return;
    this.sucesso = '';
    this.erro = '';
    const value = this.subitemForm.getRawValue();
    if (!value.categoriaId) return;

    this.api.criarSubitem(value.categoriaId, { nome: value.nome || '', preco: value.preco || 0 }).subscribe({
      next: () => {
        this.subitemForm.reset({ categoriaId: null, nome: '', preco: 0 });
        this.sucesso = 'Subitem cadastrado com sucesso.';
        this.carregarCategorias();
      },
      error: () => (this.erro = 'Não foi possível cadastrar subitem.')
    });
  }

  cadastrarSegmento(): void {
    if (this.segmentoForm.invalid || !this.auth.isAdmin()) return;
    this.sucesso = '';
    this.erro = '';
    const value = this.segmentoForm.getRawValue();
    const nome = value.nome?.trim();
    const cor = value.cor?.trim();
    const icone = value.icone?.trim();

    if (!nome || !cor || !icone) return;

    this.api.criarSegmento({ nome, cor, icone }).subscribe({
      next: () => {
        this.segmentoForm.reset({ nome: '', cor: '#1A73E8', icone: 'bi-tag' });
        this.sucesso = 'Segmento cadastrado com sucesso.';
        this.carregarSegmentos();
      },
      error: () => (this.erro = 'Não foi possível cadastrar segmento.')
    });
  }

  excluirSegmento(segmentoId: number): void {
    if (!this.auth.isAdmin()) return;
    if (!confirm('Deseja realmente excluir este segmento?')) return;
    if (this.excluindoSegmentoId !== null || this.salvandoOrdemSegmentos) return;

    this.sucesso = '';
    this.erro = '';
    this.excluindoSegmentoId = segmentoId;

    this.api.excluirSegmento(segmentoId).subscribe({
      next: () => {
        this.excluindoSegmentoId = null;
        this.sucesso = 'Segmento excluído com sucesso.';
        this.carregarSegmentos();
        this.carregarItens();
      },
      error: () => {
        this.excluindoSegmentoId = null;
        this.erro = 'Não foi possível excluir segmento.';
      }
    });
  }

  moverSegmentoParaCima(segmentoId: number): void {
    if (this.salvandoOrdemSegmentos || this.excluindoSegmentoId !== null) return;
    const index = this.segmentos.findIndex((segmento) => segmento.id === segmentoId);
    if (index <= 0) return;
    this.reordenarSegmentos(index, index - 1);
  }

  moverSegmentoParaBaixo(segmentoId: number): void {
    if (this.salvandoOrdemSegmentos || this.excluindoSegmentoId !== null) return;
    const index = this.segmentos.findIndex((segmento) => segmento.id === segmentoId);
    if (index < 0 || index >= this.segmentos.length - 1) return;
    this.reordenarSegmentos(index, index + 1);
  }

  cadastrarItem(): void {
    if (this.itemForm.invalid || !this.auth.isAdmin()) return;
    if (this.salvandoItem) return;

    this.sucesso = '';
    this.erro = '';
    this.salvandoItem = true;

    const value = this.itemForm.getRawValue();
    this.api
      .criarItem({
        nome: value.nome || '',
        preco: value.preco || 0,
        categoriaIds: Array.from(this.categoriaIdsSelecionadas),
        segmentoId: value.segmentoId,
        imagem: this.imagemItemSelecionada
      })
      .subscribe({
        next: () => {
          this.salvandoItem = false;
          this.itemForm.reset({ nome: '', preco: 0, segmentoId: null });
          this.categoriaIdsSelecionadas.clear();
          this.imagemItemSelecionada = null;
          this.sucesso = 'Item cadastrado com sucesso.';
          this.carregarItens();
        },
        error: () => {
          this.salvandoItem = false;
          this.erro = 'Não foi possível cadastrar item.';
        }
      });
  }

  selecionarImagemItem(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.imagemItemSelecionada = file;
  }

  iniciarEdicao(item: Item): void {
    if (!this.auth.isAdmin()) return;
    this.erro = '';
    this.sucesso = '';
    this.itemEmEdicaoId = item.id;
    this.edicaoForm.reset({ nome: item.nome, preco: item.preco });
    this.categoriaIdsEdicaoSelecionadas = new Set(item.categorias.map((categoria) => categoria.id));
    this.imagemEdicaoSelecionada = null;
    this.edicaoModalAberta = true;
  }

  cancelarEdicao(): void {
    this.edicaoModalAberta = false;
    this.itemEmEdicaoId = null;
    this.imagemEdicaoSelecionada = null;
    this.categoriaIdsEdicaoSelecionadas.clear();
    this.edicaoForm.reset({ nome: '', preco: 0 });
  }

  salvarEdicao(): void {
    if (this.itemEmEdicaoId == null) return;
    if (!this.auth.isAdmin() || this.edicaoForm.invalid) return;
    if (this.salvandoEdicao) return;

    this.erro = '';
    this.sucesso = '';
    this.salvandoEdicao = true;
    const value = this.edicaoForm.getRawValue();

    this.api
      .atualizarItem(this.itemEmEdicaoId, {
        nome: value.nome || '',
        preco: value.preco || 0,
        categoriaIds: Array.from(this.categoriaIdsEdicaoSelecionadas),
        imagem: this.imagemEdicaoSelecionada
      })
      .subscribe({
        next: () => {
          this.salvandoEdicao = false;
          this.sucesso = 'Item atualizado com sucesso.';
          this.cancelarEdicao();
          this.carregarItens();
        },
        error: () => {
          this.salvandoEdicao = false;
          this.erro = 'Não foi possível atualizar item.';
        }
      });
  }

  selecionarImagemEdicao(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.imagemEdicaoSelecionada = file;
  }

  toggleCategoriaEdicao(categoriaId: number, checked: boolean): void {
    if (checked) {
      this.categoriaIdsEdicaoSelecionadas.add(categoriaId);
      return;
    }
    this.categoriaIdsEdicaoSelecionadas.delete(categoriaId);
  }

  excluirItem(itemId: number): void {
    if (!this.auth.isAdmin()) return;
    if (!confirm('Deseja realmente excluir este item?')) return;
    this.erro = '';
    this.sucesso = '';

    this.api.excluirItem(itemId).subscribe({
      next: () => {
        this.sucesso = 'Item excluído com sucesso.';
        this.carregarItens();
      },
      error: () => (this.erro = 'Não foi possível excluir item.')
    });
  }

  excluirSubitem(categoriaId: number, subitemId: number): void {
    if (!this.auth.isAdmin()) return;
    if (!confirm('Deseja realmente excluir este subitem?')) return;
    this.erro = '';
    this.sucesso = '';

    this.api.excluirSubitem(categoriaId, subitemId).subscribe({
      next: () => {
        this.sucesso = 'Subitem excluído com sucesso.';
        this.carregarCategorias();
      },
      error: () => (this.erro = 'Não foi possível excluir subitem.')
    });
  }

  toggleCategoriaItem(categoriaId: number, checked: boolean): void {
    if (checked) {
      this.categoriaIdsSelecionadas.add(categoriaId);
      return;
    }
    this.categoriaIdsSelecionadas.delete(categoriaId);
  }

  categoriasDoItem(item: Item): string {
    const nomes = item.categorias.map((categoria) => categoria.nome);
    return nomes.length ? nomes.join(', ') : '-';
  }

  private recarregarTela(): void {
    this.carregarItens();
    this.carregarCategorias();
    this.carregarSegmentos();
  }

  private carregarItens(): void {
    this.api
      .listarItens({
        nome: this.filtroNome,
        page: this.paginaAtual,
        size: this.tamanhoPagina
      })
      .subscribe((response) => {
        this.itens = response.itens;
        this.paginaAtual = response.page;
        this.tamanhoPagina = response.size;
        this.totalPaginas = response.totalPages;
        this.totalItens = response.totalElements;
        this.primeiraPagina = response.first;
        this.ultimaPagina = response.last;

        if (this.paginaAtual > 0 && this.itens.length === 0 && this.totalItens > 0) {
          this.paginaAtual -= 1;
          this.carregarItens();
        }
      });
  }

  private carregarCategorias(): void {
    this.api.listarCategoriasSubitens().subscribe((categorias) => {
      this.categorias = categorias;
      const idsAtuais = new Set(categorias.map((categoria) => categoria.id));
      this.categoriasExpandidas.forEach((id) => {
        if (!idsAtuais.has(id)) {
          this.categoriasExpandidas.delete(id);
        }
      });
    });
  }

  private carregarSegmentos(): void {
    this.api.listarSegmentos().subscribe((segmentos) => {
      this.segmentos = segmentos;
    });
  }

  toggleCategoriaSubitens(categoriaId: number): void {
    if (this.categoriasExpandidas.has(categoriaId)) {
      this.categoriasExpandidas.delete(categoriaId);
      return;
    }
    this.categoriasExpandidas.add(categoriaId);
  }

  categoriaExpandida(categoriaId: number): boolean {
    return this.categoriasExpandidas.has(categoriaId);
  }

  private reordenarSegmentos(indexOrigem: number, indexDestino: number): void {
    const segmentosAnteriores = [...this.segmentos];
    const segmentosReordenados = [...this.segmentos];
    const [movido] = segmentosReordenados.splice(indexOrigem, 1);
    segmentosReordenados.splice(indexDestino, 0, movido);

    this.segmentos = segmentosReordenados;
    this.salvandoOrdemSegmentos = true;
    this.erro = '';
    this.sucesso = '';

    this.api.atualizarOrdemSegmentos(segmentosReordenados.map((segmento) => segmento.id)).subscribe({
      next: () => {
        this.salvandoOrdemSegmentos = false;
        this.sucesso = 'Ordem dos segmentos atualizada com sucesso.';
      },
      error: () => {
        this.salvandoOrdemSegmentos = false;
        this.segmentos = segmentosAnteriores;
        this.erro = 'Não foi possível atualizar a ordem dos segmentos.';
      }
    });
  }
}
