# URL Shortener

A REST service that turns long URLs into short links, with custom aliases, link expiry, and click analytics.
Built with Java 21, Spring Boot 3, Spring Data JPA, and PostgreSQL, and packaged with Docker.

## Features

- Shorten any http/https URL to a random 7-character Base62 code (SecureRandom, collision-safe)
- Optional custom alias (`/my-link`) with reserved-word protection
- Optional expiry time; expired links return `410 Gone`
- Click counting with an atomic database increment (no lost updates under concurrency)
- Input validation and consistent RFC 7807 error responses (`ProblemDetail`)
- Rejects unsafe schemes such as `javascript:` and `ftp:`
- Health endpoint at `/actuator/health`
- Integration tests with MockMvc

## API

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| POST | `/api/urls` | Create a short link | 201 |
| GET | `/api/urls/{code}` | Link details and click count | 200 |
| DELETE | `/api/urls/{code}` | Delete a link | 204 |
| GET | `/{code}` | Redirect to the original URL | 302 |

Errors: `400` invalid input, `404` unknown code, `409` alias taken, `410` expired.

### Example

```
curl -X POST localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/a/very/long/path", "alias": "demo", "expiresAt": "2030-01-01T00:00:00Z"}'

curl -i localhost:8080/demo          # 302 redirect
curl localhost:8080/api/urls/demo    # shows clickCount
```

## Run locally (in-memory H2 database)

```
mvn spring-boot:run
```

## Run with Docker and PostgreSQL

```
docker compose up --build
```

## Test

```
mvn test
```

## Project layout

```
src/main/java/com/example/shortener/
├── UrlShortenerApplication.java
├── web/         UrlController (REST + redirect)
├── service/     UrlService (code generation, validation, expiry, click tracking)
├── repository/  ShortUrlRepository (Spring Data JPA)
├── model/       ShortUrl (entity)
├── dto/         request/response records
└── exception/   domain exceptions + global handler
```

## Design notes

- `create` is deliberately not wrapped in one transaction, so a unique-constraint collision can be caught and retried instead of poisoning the transaction.
- The unique index on `code` is the source of truth for uniqueness; the `existsByCode` check is only a fast path.
- Redirects are `302` with `Cache-Control: no-store` so every click reaches the server and is counted.

## Ideas to extend it

- Rate limiting per IP (Bucket4j or a Redis counter)
- Redis cache for hot redirects
- Flyway migrations instead of `ddl-auto: update`
- API keys or user accounts, with per-user link lists
- Click analytics by day, referrer, and country
- QR code generation endpoint
- GitHub Actions CI running `mvn test`

## Resume bullets

- Built a URL shortener REST API in Java 21 and Spring Boot with custom aliases, link expiry, and click analytics, backed by PostgreSQL via Spring Data JPA.
- Designed collision-safe short code generation (SecureRandom Base62 with a unique-index fallback) and atomic click counting to stay correct under concurrent requests.
- Added request validation, RFC 7807 error handling, and URL scheme filtering to block unsafe redirects.
- Wrote MockMvc integration tests covering create, redirect, expiry, conflict, and deletion flows; containerized the app and database with Docker Compose.
