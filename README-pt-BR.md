# Spring Boot Microservices

[![Português](https://img.shields.io/badge/Portugu%C3%AAs-green?style=plastic&logo=openbadges&logoColor=white)](README-pt-BR.md) [![English](https://img.shields.io/badge/English-blue?style=plastic&logo=openbadges&logoColor=white)](README.md)

[![CI](https://github.com/jessicasalestech/spring-boot-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/jessicasalestech/spring-boot-microservices/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=spring&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)

Um projeto de portfólio **profissional de microservices** construído com **Java 21**, **Spring Boot 3.4**
e **Spring Cloud**. Três serviços independentes se comunicam por HTTP e são expostos
através de um único gateway de API — sem service discovery, sem Kafka, sem maquinário desnecessário —
para que a arquitetura continue fácil de ler, testar e executar.

O objetivo deste projeto é demonstrar os padrões centrais que compõem a
arquitetura de microservices:

- **Posse de serviço (service ownership)** — cada serviço é dono da sua própria API REST, modelo de domínio e esquema de banco de dados.
- **Comunicação entre serviços** — `order-service` chama `product-service` por HTTP com uma
  URL base configurável e tratamento explícito de erros.
- **Edge de composição de API** — Spring Cloud Gateway expõe um único endpoint público.
- **Testabilidade** — todas as camadas são testadas (fatia de persistência, unidade, fatia web e um
  teste ponta a ponta de roteamento do gateway) com serviços downstream simulados (stubs).

---

## Arquitetura

```mermaid
flowchart LR
    Client[Client / cURL] -->|GET/POST /api/products/**| GW[API Gateway<br/>port 8080]

    subgraph Edge
        GW[Spring Cloud Gateway<br/>api-gateway]
    end

    subgraph Backing Services
        PS[product-service<br/>port 8081<br/>Spring Data JPA + H2]
        OS[order-service<br/>port 8082<br/>Spring Data JPA + H2]
    end

    GW -->|/api/products/**| PS
    GW -->|/api/orders/**| OS

    OS -.HTTP GET /api/products/{id}.-> PS

    PS --- PDB[(products]
    OS --- ODB[(orders]
```

O `api-gateway` é o **ponto único de entrada (single entry point)** na porta `8080`. Ele encaminha `/api/products/**`
para o `product-service` e `/api/orders/**` para o `order-service` usando rotas estáticas. Quando um
pedido é feito, o `order-service` primeiro chama o `product-service` para validar se o produto
existe (e está ativo e em estoque) antes de persistir o pedido.

---

## Serviços e endpoints

| Context path | Service | Port | Description |
| --- | --- | --- | --- |
| `/` | `api-gateway` | **8080** | Spring Cloud Gateway edge |
| `/api/products` | `product-service` | **8081** | Catálogo de produtos (REST + JPA) |
| `/api/orders` | `order-service` | **8082** | Pedidos, valida produtos por HTTP |

### product-service — `GET /api/products`

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| GET | `/api/products` | Listar todos os produtos | `200` | — |
| GET | `/api/products/{id}` | Obter um produto | `200` | `404` |
| POST | `/api/products` | Criar um produto | `201` | `400`, `409` |
| PUT | `/api/products/{id}` | Atualizar um produto | `200` | `400`, `404`, `409` |
| DELETE | `/api/products/{id}` | Excluir um produto | `204` | `404` |

### order-service — `GET /api/orders`

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| GET | `/api/orders` | Listar todos os pedidos | `200` | — |
| GET | `/api/orders/{id}` | Obter um pedido | `200` | `404` |
| POST | `/api/orders` | Fazer um pedido (valida o produto) | `201` | `400`, `404`, `409`, `503` |

> Todas as respostas de erro seguem **RFC 7807 `ProblemDetail`** e sempre carregam um `title` legível
> por humanos, um URI `type` resolvível e, para falhas de validação, um mapa de `violations`.

### Exemplo: criar um produto

```bash
curl -s http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard","description":"Hot-swappable, 75% layout","price":429.90,"stockQuantity":25}'
```

### Exemplo: fazer um pedido através do gateway

```bash
curl -s http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
```

O gateway roteia a solicitação para o `order-service`, que busca o produto do
`product-service` e armazena o pedido com um `totalPrice` calculado de `859.80`.

---

## Estrutura do projeto

```text
spring-boot-microservices/
├── pom.xml                     # POM pai (gerenciamento de dependências, plugins de build)
├── docker-compose.yml          # executa os três serviços + healthchecks
├── .github/workflows/ci.yml    # CI: checkout -> JDK 21 -> mvn verify
├── product-service/            # REST + Spring Data JPA + H2   (porta 8081)
├── order-service/              # REST + JPA + cliente HTTP      (porta 8082)
└── api-gateway/                # rotas do Spring Cloud Gateway (porta 8080)
```

O build é um **reator multi-módulo** padrão do Maven: `mvn verify` na raiz compila e
testa cada módulo em ordem de dependência.

---

## Requisitos

- **Java 21** (usamos o [Temurin], LTS)
- **Maven 3.9+**
- **Docker** + **Docker Compose** (opcional, apenas para o caminho de contêineres)

[Temurin]: https://adoptium.net/

---

## Build e teste com Maven

```bash
mvn -B clean verify
```

Isso compila cada módulo, empacota jars executáveis e executa **todos os testes** (fatia de persistência,
fatia web, unidade, contrato e roteamento ponta a ponta do gateway). Uma execução bem-sucedida termina com
`BUILD SUCCESS`.

### Executar os serviços localmente (sem Docker)

```bash
# terminal 1
mvn -pl product-service spring-boot:run

# terminal 2
mvn -pl order-service spring-boot:run

# terminal 3
mvn -pl api-gateway spring-boot:run
```

O `product-service` semeia quatro produtos de demonstração quando o perfil `demo` está ativo
(`--spring.profiles.active=demo`). O console HTTP de cada banco H2 está disponível em
`http://localhost:<port>/h2-console` enquanto um serviço está em execução.

Então abra a API através do gateway em **http://localhost:8080/api/products**.

---

## Executar com Docker Compose

Compile e inicie toda a stack atrás do gateway:

```bash
docker compose up --build
```

Os healthchecks estão configurados para que o `order-service` aguarde o `product-service` reportar saudável, e
o gateway aguarda ambos. O perfil `demo` está ativo para que o catálogo venha pré-semeado.

| Service | URL |
| --- | --- |
| API gateway | http://localhost:8080 |
| product-service | http://localhost:8081 |
| order-service | http://localhost:8082 |
| product-service health | http://localhost:8081/actuator/health |

Pare tudo com:

```bash
docker compose down
```

---

## Como os serviços foram testados

| Test | Strategy | What it proves |
| --- | --- | --- |
| `ProductRepositoryTest` / `OrderRepositoryTest` | `@DataJpaTest` | Mapeamento JPA, consultas derivadas, timestamps de auditoria |
| `ProductControllerTest` | `@SpringBootTest` + MockMvc | CRUD completo, validação `400`, `404`, `409` nome duplicado |
| `OrderControllerTest` | `@SpringBootTest` + MockMvc + `MockRestServiceServer` | Contrato REST de pedidos com um product-service **simulado (stub)** |
| `ProductCatalogClientTest` | `MockRestServiceServer` (sem contexto) | Tradução de erros HTTP: `404`→domínio, `5xx`/timeout→`503` |
| `OrderServiceTest` | Teste de unidade Mockito | Cálculo do preço total, regras de estoque, regra de produto inativo |
| `GatewayRoutesTest` | contexto + definições de rotas | Rotas vinculadas à URI correta + predicado `Path` |
| `GatewayRoutingTest` | `RANDOM_PORT` + stub HTTP real | As solicitações são realmente redirecionadas ponta a ponta |

O `order-service` nunca toca um `product-service` real durante a suíte de testes: a chamada
downstream é simulada com `MockRestServiceServer` para que a suíte seja determinística e não precise de
serviços em execução.

---

## O que este projeto demonstra

- **JDK 21 & Java moderno** — records para DTOs, text blocks, pattern matching para `instanceof`.
- **Spring Boot 3 & Spring Cloud** — auto-configuração, Spring Data JPA, Bean Validation
  (`@Valid` records), `RestClient`, Spring Cloud Gateway.
- **Arquitetura de microservices** — fronteiras service-per-database, comunicação entre serviços via HTTP,
  um único gateway de composição de API e configuração orientada a variáveis de ambiente
  (`PRODUCT_SERVICE_URL`, `ORDER_SERVICE_URL`).
- **Chamadas resilientes entre serviços** — mapeamento explícito de falhas downstream para códigos
  de status significativos (`404`/`409`/`503`) em vez de propagar erros de transporte brutos.
- **Erros HTTP consistentes** — RFC 7807 `ProblemDetail` entre os serviços.
- **Containerização** — Dockerfiles multi-estágio sem root e um `docker-compose.yml` conectado.
- **CI** — GitHub Actions executa `mvn -B verify` em todo push/PR para `main`.

---

## Licença

Liberado sob a **MIT License** — livre para uso em aprendizado, portfólios e experimentos.

<p align="center">
  Built with Spring Boot &middot; Mantido por <a href="https://github.com/jessicasalestech">Jessica Sales</a>
</p>