import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Item } from '../core/models';

@Component({
  selector: 'app-itens',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './itens.component.html'
})
export class ItensComponent implements OnInit {
  itens: Item[] = [];
  erro = '';

  form = this.fb.group({
    nome: ['', Validators.required],
    preco: [0, [Validators.required, Validators.min(0.01)]]
  });

  constructor(private fb: FormBuilder, private api: ApiService, public auth: AuthService) {}

  ngOnInit(): void {
    this.carregarItens();
  }

  cadastrar(): void {
    if (this.form.invalid || !this.auth.isAdmin()) return;

    this.api.criarItem(this.form.getRawValue() as { nome: string; preco: number }).subscribe({
      next: () => {
        this.form.reset({ nome: '', preco: 0 });
        this.carregarItens();
      },
      error: () => (this.erro = 'Não foi possível cadastrar item.')
    });
  }

  private carregarItens(): void {
    this.api.listarItens().subscribe((itens) => (this.itens = itens));
  }
}
