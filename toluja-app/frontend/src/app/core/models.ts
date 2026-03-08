export interface AuthUser {
  id: number;
  tenantId: string;
  username: string;
  nomeExibicao: string;
  role: 'ADMIN' | 'ATENDENTE' | string;
  deveTrocarSenha: boolean;
}

export interface LoginResponse {
  token: string;
  user: AuthUser;
}

export interface TenantSummary {
  tenantId: string;
  nome: string;
}

export interface CreatedTenant {
  id: number;
  tenantId: string;
  nome: string;
  ativo: boolean;
  hasPrintKey: boolean;
  printKey: string;
}

export interface Item {
  id: number;
  nome: string;
  preco: number;
  imagemUrl?: string | null;
  ativo?: boolean;
  segmento?: Segmento | null;
  categorias: SubitemCategoria[];
}

export interface PagedResponse<T> {
  itens: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface NovoItem {
  nome: string;
  preco: number;
  categoriaIds?: number[];
  segmentoId?: number | null;
  imagem?: File | null;
}

export interface Segmento {
  id: number;
  nome: string;
  cor: string;
  icone: string;
}

export interface PedidoItem {
  itemId: number;
  quantidade: number;
  subitemIds?: number[];
}

export interface Pedido {
  id: number;
  codigo: string;
  senhaChamada: number;
  criadoEm: string;
  status: string;
  total: number;
  observacao?: string;
  itens: Array<{
    id?: number;
    itemId: number;
    nomeSnapshot?: string;
    nomeItem?: string;
    quantidade: number;
    precoSnapshot?: number;
    precoUnitario?: number;
    subtotal?: number;
    subitens?: Array<{
      id?: number;
      subitemId: number;
      categoriaNomeSnapshot: string;
      nomeSnapshot: string;
      precoSnapshot: number;
    }>;
  }>;
}

export interface SubitemCategoria {
  id: number;
  nome: string;
  subitens: Subitem[];
}

export interface Subitem {
  id: number;
  nome: string;
  preco: number;
}
