export interface AuthUser {
  id: number;
  username: string;
  nomeExibicao: string;
  role: 'ADMIN' | 'ATENDENTE' | string;
  deveTrocarSenha: boolean;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface Item {
  id: number;
  nome: string;
  preco: number;
  ativo?: boolean;
}

export interface NovoItem {
  nome: string;
  preco: number;
}

export interface PedidoItem {
  itemId: number;
  quantidade: number;
}

export interface Pedido {
  id: number;
  codigo: string;
  criadoEm: string;
  status: string;
  total: number;
  observacao?: string;
  itens: Array<{ id?: number; itemId: number; nomeSnapshot?: string; nomeItem?: string; quantidade: number; precoSnapshot?: number; precoUnitario?: number; subtotal?: number }>;
}
