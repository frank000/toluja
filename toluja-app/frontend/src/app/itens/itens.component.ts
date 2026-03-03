import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Item, SubitemCategoria } from '../core/models';

@Component({
  selector: 'app-itens',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './itens.component.html'
})
export class ItensComponent implements OnInit {
  itens: Item[] = [];
  categorias: SubitemCategoria[] = [];
  erro = '';
  sucesso = '';
  categoriasExpandidas = new Set<number>();

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
    preco: [0, [Validators.required, Validators.min(0.01)]]
  });

  edicaoForm = this.fb.group({
    nome: ['', Validators.required],
    preco: [0, [Validators.required, Validators.min(0.01)]]
  });

  categoriaIdsSelecionadas = new Set<number>();
  itemEmEdicaoId: number | null = null;

  constructor(private fb: FormBuilder, private api: ApiService, public auth: AuthService) {}

  ngOnInit(): void {
    this.recarregarTela();
  }

  cadastrarCategoria(): void {
    if (this.categoriaForm.invalid || !this.auth.isAdmin()) return;
    this.sucesso = '';
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

  cadastrarItem(): void {
    if (this.itemForm.invalid || !this.auth.isAdmin()) return;
    this.sucesso = '';

    const value = this.itemForm.getRawValue();
    this.api.criarItem({
      nome: value.nome || '',
      preco: value.preco || 0,
      categoriaIds: Array.from(this.categoriaIdsSelecionadas)
    }).subscribe({
      next: () => {
        this.itemForm.reset({ nome: '', preco: 0 });
        this.categoriaIdsSelecionadas.clear();
        this.sucesso = 'Item cadastrado com sucesso.';
        this.carregarItens();
      },
      error: () => (this.erro = 'Não foi possível cadastrar item.')
    });
  }

  iniciarEdicao(item: Item): void {
    if (!this.auth.isAdmin()) return;
    this.erro = '';
    this.sucesso = '';
    this.itemEmEdicaoId = item.id;
    this.edicaoForm.reset({ nome: item.nome, preco: item.preco });
  }

  cancelarEdicao(): void {
    this.itemEmEdicaoId = null;
    this.edicaoForm.reset({ nome: '', preco: 0 });
  }

  salvarEdicao(itemId: number): void {
    if (!this.auth.isAdmin() || this.edicaoForm.invalid) return;
    this.erro = '';
    this.sucesso = '';
    const value = this.edicaoForm.getRawValue();

    this.api.atualizarItem(itemId, { nome: value.nome || '', preco: value.preco || 0 }).subscribe({
      next: () => {
        this.sucesso = 'Item atualizado com sucesso.';
        this.cancelarEdicao();
        this.carregarItens();
      },
      error: () => (this.erro = 'Não foi possível atualizar item.')
    });
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
  }

  private carregarItens(): void {
    this.api.listarItens().subscribe((itens) => (this.itens = itens));
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
}
