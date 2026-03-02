import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { authGuard } from './core/auth.guard';
import { ItensComponent } from './itens/itens.component';
import { PedidoComponent } from './pedido/pedido.component';
import { PedidosListaComponent } from './pedidos/pedidos-lista.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'itens', component: ItensComponent, canActivate: [authGuard] },
  { path: 'pedido', component: PedidoComponent, canActivate: [authGuard] },
  { path: 'pedidos', component: PedidosListaComponent, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'login' }
];
