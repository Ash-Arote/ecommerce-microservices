# E-commerce Microservices

Spring Boot 4 / Spring Cloud 2025.1 microservices version of `../ecommerce-lite`.

```
client ──► gateway :8080
             ├─ /api/products/** ─► product-service :8081  (db: products)
             ├─ /api/cart/**     ─► cart-service    :8082  (db: carts)
             └─ /api/orders/**   ─► order-service   :8083  (db: orders)

cart-service  ──HTTP──► product-service   (price, stock check)
order-service ──HTTP──► cart-service, product-service
```

## Run

Everything in Docker:

```bash
docker compose up --build -d
```

Or run Mongo in Docker and the services locally (one terminal each):

```bash
docker compose up -d mongo
./mvnw -pl product-service spring-boot:run
./mvnw -pl cart-service spring-boot:run
./mvnw -pl order-service spring-boot:run
./mvnw -pl gateway spring-boot:run
```

Mongo Express (DB browser): http://localhost:8090

## APIs (call through the gateway on :8080)

| # | Method | Path | Service |
|---|--------|------|---------|
| 1 | POST   | `/api/products` | product |
| 2 | GET    | `/api/products?category=` | product |
| 3 | GET    | `/api/products/{id}` | product |
| 4 | PUT    | `/api/products/{id}` | product |
| 5 | DELETE | `/api/products/{id}` | product |
| 6 | POST   | `/api/cart/{userId}/items` | cart |
| 7 | GET    | `/api/cart/{userId}` | cart |
| 8 | DELETE | `/api/cart/{userId}/items/{productId}` | cart |
| 9 | POST   | `/api/orders` (body `{"userId": "..."}`) | order |
| 10 | GET   | `/api/orders/{id}` | order |

Internal endpoints used between services: `PATCH /api/products/{id}/stock` and `DELETE /api/cart/{userId}`.

## Example

```bash
curl -X POST localhost:8080/api/products -H 'Content-Type: application/json' \
  -d '{"name":"Laptop","price":999.99,"category":"electronics","stock":5}'
curl -X POST localhost:8080/api/cart/u1/items -H 'Content-Type: application/json' \
  -d '{"productId":"<id>","quantity":2}'
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' -d '{"userId":"u1"}'
```

## Design notes

- Each service owns its own database; services never read another service's data directly.
- Checkout spans services with no shared transaction. If reserving stock fails partway, order-service
  releases what it already reserved (a compensating action).
- If a downstream service is down, callers return `503`.
