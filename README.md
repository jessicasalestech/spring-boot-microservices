# Spring Boot Microservices

[![CI](https://github.com/jessicasalestech/spring-boot-microservices/actions/workflows/ci.yml/badge.svg)](https://github.com/jessicasalestech/spring-boot-microservices/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=spring&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)

A professional **microservices** portfolio project built with **Java 21**, **Spring Boot 3.4**
and **Spring Cloud**. Three independent services communicate over HTTP and are exposed
through a single API gateway — no service discovery, no Kafka, no unnecessary machinery —
so the architecture stays easy to read, test and run.

The goal of this project is to demonstrate the core patterns that make up
microservices architecture:

- **Service ownership** — every service owns its own REST API, domain model and database schema.
- **Inter-service communication** — `order-service` calls `product-service` over HTTP with a
  configurable base URL and explicit error handling.
- **API composition edge** — Spring Cloud Gateway exposes a single public endpoint.
- **Testability** — every layer is tested (persistence slice, unit, web slice and an
  end-to-end gateway routing test) with stubbed downstream services.

---

## Architecture

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

`api-gateway` is the **single entry point** on port `8080`. It forwards `/api/products/**`
to `product-service` and `/api/orders/**` to `order-service` using static routes. When an
order is placed, `order-service` first calls `product-service` to validate that the product
exists (and is active and in stock) before persisting the order.

---

## Services and endpoints

| Context path | Service | Port | Description |
| --- | --- | --- | --- |
| `/` | `api-gateway` | **8080** | Spring Cloud Gateway edge |
| `/api/products` | `product-service` | **8081** | Product catalogue (REST + JPA) |
| `/api/orders` | `order-service` | **8082** | Orders, validates products over HTTP |

### product-service — `GET /api/products`

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| GET | `/api/products` | List all products | `200` | — |
| GET | `/api/products/{id}` | Get one product | `200` | `404` |
| POST | `/api/products` | Create a product | `201` | `400`, `409` |
| PUT | `/api/products/{id}` | Update a product | `200` | `400`, `404`, `409` |
| DELETE | `/api/products/{id}` | Delete a product | `204` | `404` |

### order-service — `GET /api/orders`

| Method | Path | Description | Success | Errors |
| --- | --- | --- | --- | --- |
| GET | `/api/orders` | List all orders | `200` | — |
| GET | `/api/orders/{id}` | Get one order | `200` | `404` |
| POST | `/api/orders` | Place an order (validates the product) | `201` | `400`, `404`, `409`, `503` |

> All error responses follow **RFC 7807 `ProblemDetail`** and always carry a human readable
> `title`, a resolvable `type` URI and, for validation failures, a `violations` map.

### Example: create a product

```bash
curl -s http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard","description":"Hot-swappable, 75% layout","price":429.90,"stockQuantity":25}'
```

### Example: place an order through the gateway

```bash
curl -s http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"productId":1,"quantity":2}'
```

The gateway routes the request to `order-service`, which fetches the product from
`product-service` and stores the order with a computed `totalPrice` of `859.80`.

---

## Project layout

```text
spring-boot-microservices/
├── pom.xml                     # parent POM (dependency management, build plugins)
├── docker-compose.yml          # runs all three services + healthchecks
├── .github/workflows/ci.yml    # CI: checkout -> JDK 21 -> mvn verify
├── product-service/            # REST + Spring Data JPA + H2   (port 8081)
├── order-service/              # REST + JPA + HTTP client      (port 8082)
└── api-gateway/                # Spring Cloud Gateway routes   (port 8080)
```

The build is a standard Maven **multi-module reactor**: `mvn verify` at the root builds and
tests every module in dependency order.

---

## Requirements

- **Java 21** (we use [Temurin], LTS)
- **Maven 3.9+**
- **Docker** + **Docker Compose** (optional, only for the container path)

[Temurin]: https://adoptium.net/

---

## Build and test with Maven

```bash
mvn -B clean verify
```

This compiles every module, packages runnable jars, and runs **all tests** (persistence slice,
web slice, unit, contract and end-to-end gateway routing). A successful run ends with
`BUILD SUCCESS`.

### Run the services locally (no Docker)

```bash
# terminal 1
mvn -pl product-service spring-boot:run

# terminal 2
mvn -pl order-service spring-boot:run

# terminal 3
mvn -pl api-gateway spring-boot:run
```

`product-service` seeds four demo products when the `demo` profile is active
(`--spring.profiles.active=demo`). The HTTP console for each H2 database is available at
`http://localhost:<port>/h2-console` while a service is running.

Then open the API through the gateway at **http://localhost:8080/api/products**.

---

## Run with Docker Compose

Build and start the whole stack behind the gateway:

```bash
docker compose up --build
```

Healthchecks are wired so `order-service` waits until `product-service` reports healthy, and
the gateway waits for both. The `demo` profile is active so the catalogue comes pre-seeded.

| Service | URL |
| --- | --- |
| API gateway | http://localhost:8080 |
| product-service | http://localhost:8081 |
| order-service | http://localhost:8082 |
| product-service health | http://localhost:8081/actuator/health |

Stop everything with:

```bash
docker compose down
```

---

## How the services were tested

| Test | Strategy | What it proves |
| --- | --- | --- |
| `ProductRepositoryTest` / `OrderRepositoryTest` | `@DataJpaTest` | JPA mapping, derived queries, auditing timestamps |
| `ProductControllerTest` | `@SpringBootTest` + MockMvc | Full CRUD, `400` validation, `404`, `409` duplicate name |
| `OrderControllerTest` | `@SpringBootTest` + MockMvc + `MockRestServiceServer` | Order REST contract with a **stubbed** product-service |
| `ProductCatalogClientTest` | `MockRestServiceServer` (no context) | HTTP error translation: `404`→domain, `5xx`/timeout→`503` |
| `OrderServiceTest` | Mockito unit | Total-price math, stock rules, inactive-product rule |
| `GatewayRoutesTest` | context + route definitions | Routes bound to the right URI + `Path` predicate |
| `GatewayRoutingTest` | `RANDOM_PORT` + real HTTP stub | Requests are actually proxied end-to-end |

`order-service` never touches a real `product-service` during the test suite: the downstream
call is stubbed with `MockRestServiceServer` so the suite is deterministic and needs no running
services.

---

## What this project demonstrates

- **JDK 21 & modern Java** — records for DTOs, text blocks, pattern matching for `instanceof`.
- **Spring Boot 3 & Spring Cloud** — auto-configuration, Spring Data JPA, Bean Validation
  (`@Valid` records), `RestClient`, Spring Cloud Gateway.
- **Microservices architecture** — service-per-database boundaries, HTTP-based inter-service
  communication, a single API-composition gateway, and env-var-driven configuration
  (`PRODUCT_SERVICE_URL`, `ORDER_SERVICE_URL`).
- **Resilient inter-service calls** — explicit mapping of downstream failures to meaningful
  status codes (`404`/`409`/`503`) instead of bubbling raw transport errors.
- **Consistent HTTP errors** — RFC 7807 `ProblemDetail` across services.
- **Containerisation** — multi-stage, non-root Dockerfiles and a wired `docker-compose.yml`.
- **CI** — GitHub Actions runs `mvn -B verify` on every push/PR to `main`.

---

## License

Released under the **MIT License** — free to use for learning, portfolios and experiments.

<p align="center">
  Built with Spring Boot &middot; Maintained by <a href="https://github.com/jessicasalestech">Jessica Sales</a>
</p>