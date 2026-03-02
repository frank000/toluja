# Toluja App Monorepo

Monorepo com frontend Angular e backend Spring Boot para autenticação JWT, gestão de itens e pedidos.

## Estrutura

- `frontend/`: aplicação Angular (login, itens, pedido e lista de pedidos).
- `backend/`: API Spring Boot com JWT, Validation, Security, JPA, Flyway e SQLite.

## Pré-requisitos

- Node.js 20+
- npm 10+
- Java 17+
- Maven 3.9+

## Backend

```bash
cd backend
mvn spring-boot:run
```

API disponível em `http://localhost:8080`.

Banco SQLite em arquivo: `backend/data/pedido.db` (`jdbc:sqlite:./data/pedido.db`).

Schema criado pelo Flyway (`V1__init.sql`) com tabelas:

- `users`
- `items`
- `orders`
- `order_items`

### Usuário seed (Flyway)

O Flyway cria usuário administrador padrão:

- username: `admin`
- senha: `admin123`
- role: `ADMIN`
- `deveTrocarSenha = true`

Após o primeiro login, **é obrigatório trocar a senha** via endpoint:

- `POST /api/auth/change-password`

Body exemplo:

```json
{
  "senhaAtual": "admin123",
  "novaSenha": "NovaSenha@123"
}
```

## Regras dos endpoints

### Itens

- `GET /api/itens`: autenticado (`ADMIN` ou `ATENDENTE`).
- `POST /api/itens`: somente `ADMIN`, valida `nome` obrigatório e `preco > 0`.

### Pedidos

- `POST /api/pedidos`: autenticado; recebe `itens[{itemId, quantidade}]` e `observacao` opcional, cria pedido para usuário logado, calcula subtotal/total e gera `codigo` único de 5 caracteres.
- `GET /api/pedidos`:
  - `ADMIN` vê todos os pedidos.
  - `ATENDENTE` vê somente os próprios pedidos.

## Padronização de erros JSON

- `400` (validação): retorna JSON com `status`, `message`, `path`, `fields`, `timestamp`.
- `401` / `403`: retorna JSON com `status`, `message`, `path`.
- `500`: retorna JSON com `status`, `message`, `path`, `timestamp`.


## Módulo de impressão (Raspberry / USB)

Na criação do pedido, o backend gera um cupom e envia para **duas impressoras** configuradas:

- `printers.printer1`
- `printers.printer2`

Formato suportado:

- TCP raw: `tcp://IP:9100` (recomendado para impressoras térmicas)
- CUPS (stub): `cups://nome-da-fila` (usa comando `lp`)

Se a impressão falhar em qualquer uma das duas:

- status do pedido é atualizado para `ERRO_IMPRESSAO`
- a API retorna HTTP 500 com mensagem clara

Cupom contém:

- código do pedido
- data/hora
- nomeExibicao do usuário
- itens (nome, quantidade, preço, subtotal)
- total e observação

## Frontend

```bash
cd frontend
npm install
npm start
```

Aplicação disponível em `http://localhost:4200`.

## JWT e segurança

- Login em `POST /api/auth/login` com `username/password`.
- Resposta retorna `token` + dados do usuário.
- Passwords validadas com BCrypt.
- Rotas `/api/**` protegidas, exceto `/api/auth/login`.
- Roles suportadas: `ADMIN` e `ATENDENTE`.

## Rotas backend

- `POST /api/auth/login`
- `POST /api/auth/change-password`
- `GET /api/itens`
- `POST /api/itens`
- `GET /api/pedidos`
- `POST /api/pedidos`

CORS configurado para `http://localhost:4200`.


## Docker (front + back + db)

Para subir tudo de uma vez (frontend, backend e volume de banco SQLite):

```bash
cd toluja-app
docker compose up --build
```

Serviços:

- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- DB (volume SQLite): serviço `db` mantendo o arquivo `pedido.db` em volume Docker `db_data`.

Para parar:

```bash
docker compose down
```

Para apagar também o volume do banco:

```bash
docker compose down -v
```
