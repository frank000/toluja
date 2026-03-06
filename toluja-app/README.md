# Toluja App Monorepo

Monorepo com frontend Angular e backend Spring Boot para autenticação JWT, gestão de itens e pedidos.

## Estrutura

- `frontend/`: aplicação Angular (login, itens, pedido e lista de pedidos).
- `backend/`: API Spring Boot com JWT, Validation, Security, JPA, Flyway e PostgreSQL.

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

Banco PostgreSQL padrão:

- URL: `jdbc:postgresql://localhost:5432/toluja`
- usuário: `toluja`
- senha: `toluja`

Também é possível sobrescrever por variáveis:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Schema criado pelo Flyway (`V1__init.sql`) com tabelas:

- `users`
- `items`
- `orders`
- `order_items`
- `subitem_categories`
- `subitems`
- `item_subitem_categories`
- `order_item_subitems`

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

- `GET /api/itens`: autenticado (`ADMIN` ou `ATENDENTE`), retorna item com categorias/subitens permitidos.
- `POST /api/itens`: somente `ADMIN`, valida `nome`, `preco > 0` e categorias opcionais permitidas para o item.
- `GET /api/subitens/categorias`: autenticado (`ADMIN` ou `ATENDENTE`), retorna categorias com subitens.
- `POST /api/subitens/categorias`: somente `ADMIN`, cria categoria de subitens.
- `POST /api/subitens/categorias/{categoriaId}/subitens`: somente `ADMIN`, cria subitem na categoria.

### Pedidos

- `POST /api/pedidos`: autenticado; recebe `itens[{itemId, quantidade, subitemIds[]}]` e `observacao` opcional, valida subitens permitidos por item, soma adicionais no subtotal/total, gera `codigo` único de 5 caracteres e `senhaChamada` incremental.
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
- senha de chamada
- data/hora
- nomeExibicao do usuário
- itens (nome, quantidade, preço base, subitens selecionados e subtotal)
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
- `GET /api/subitens/categorias`
- `POST /api/subitens/categorias`
- `POST /api/subitens/categorias/{categoriaId}/subitens`
- `GET /api/pedidos`
- `POST /api/pedidos`

CORS configurado para `http://localhost:4200`.


## Docker (front + back + db)

Para subir tudo de uma vez (frontend, backend e PostgreSQL):

```bash
cd toluja-app
docker compose up --build
```

Serviços:

- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- DB (PostgreSQL): serviço `db` em `localhost:5432`, com dados persistidos no volume Docker `db_data`.

Para parar:

```bash
docker compose down
```

Para apagar também o volume do banco:

```bash
docker compose down -v
```

## Deploy na Contabo com imagem gerada no GitHub

Este repositório já possui workflow para build/push de imagens Docker no GHCR:

- Workflow: `.github/workflows/docker-images.yml`
- Imagens publicadas:
  - `ghcr.io/<owner>/toluja-backend:latest`
  - `ghcr.io/<owner>/toluja-frontend:latest`

### 1) Build automático no GitHub

Ao fazer push na branch `main` (ou rodar manualmente no Actions), o GitHub gera e publica as imagens.

### 2) Preparar VPS (Contabo)

Na VPS:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
```

Faça logout/login novamente para aplicar grupo docker.

### 3) Subir containers na VPS

Copie para a VPS:

- `docker-compose.prod.yml`
- `.env.prod.example` (renomeie para `.env.prod`)

Edite `.env.prod` com seus valores reais (imagens, senha do banco, JWT e domínio).

Login no GHCR com token do GitHub (`read:packages`):

```bash
echo "<SEU_GHCR_TOKEN>" | docker login ghcr.io -u <SEU_USUARIO_GITHUB> --password-stdin
```

Suba os containers:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

Para atualizar depois de novo push:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

## Nginx Proxy Manager em compose separado

Arquivo pronto: `docker-compose.npm.yml`.

### 1) Criar rede compartilhada

```bash
docker network create proxy
```

### 2) Subir NPM

```bash
docker compose -f docker-compose.npm.yml up -d
```

Painel NPM: `http://SEU_IP:81`

- Email inicial: `admin@example.com`
- Senha inicial: `changeme`

### 3) Subir app

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

### 4) Criar Proxy Host no NPM

- Domain Names: seu domínio
- Forward Hostname / IP: `toluja-frontend`
- Forward Port: `80`
- SSL: solicitar Let's Encrypt e habilitar Force SSL
