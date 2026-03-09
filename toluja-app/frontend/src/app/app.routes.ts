import { Routes } from '@angular/router';
import { ChangePasswordComponent } from './auth/change-password.component';
import { LoginComponent } from './auth/login.component';
import { ConfiguracaoComponent } from './configuracao/configuracao.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { adminGuard } from './core/admin.guard';
import { authGuard } from './core/auth.guard';
import { superadminGuard } from './core/superadmin.guard';
import { GuestPedidoComponent } from './guest-pedido/guest-pedido.component';
import { ItensComponent } from './itens/itens.component';
import { PedidoComponent } from './pedido/pedido.component';
import { PedidosListaComponent } from './pedidos/pedidos-lista.component';
import { SuperadminPanelComponent } from './superadmin/superadmin-panel.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'guest/:tenantId', component: GuestPedidoComponent },
  { path: 'trocar-senha', component: ChangePasswordComponent, canActivate: [authGuard] },
  { path: 'itens', component: ItensComponent, canActivate: [authGuard] },
  { path: 'pedido', component: PedidoComponent, canActivate: [authGuard] },
  { path: 'pedidos', component: PedidosListaComponent, canActivate: [authGuard] },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard, adminGuard] },
  { path: 'configuracao', component: ConfiguracaoComponent, canActivate: [authGuard, adminGuard] },
  { path: 'painel-superadmin', component: SuperadminPanelComponent, canActivate: [authGuard, superadminGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
];
