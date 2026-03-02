import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Pedido } from '../core/models';

@Component({
  selector: 'app-pedidos-lista',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pedidos-lista.component.html'
})
export class PedidosListaComponent implements OnInit {
  pedidos: Pedido[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.listarPedidos().subscribe((pedidos) => (this.pedidos = pedidos));
  }
}
