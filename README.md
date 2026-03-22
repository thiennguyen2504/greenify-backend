# Greenify

Spring Boot backend application for Greenify project.

## Tech Stack

- Java 21
- Spring Boot 3.5.9
- PostgreSQL 16 (Docker)
- Redis 7 (cache)
- H2 (local development)

---

## Quick Start with Docker

### 1. Clone the repository

```bash
git clone <repository-url>
cd greenify
```

### 2. Create environment file

```bash
cp .env.example .env
```

Edit `.env` and fill in your values:
- `DB_PASSWORD` - database password
- `JWT_SECRET` - your 256-bit secret key
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` - from Google Cloud Console
- `MAIL_USERNAME` / `MAIL_PASSWORD` - Gmail app password

### 3. Start all services

```bash
docker compose up --build
```

This will start:
- PostgreSQL database (port 5432)
- Redis cache
- Spring Boot application (port 8080)

### 4. Verify

Open [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## Common Docker Commands

| Command | Description |
|---------|-------------|
| `docker compose up --build` | Build and start all services |
| `docker compose up -d` | Start in background |
| `docker compose down` | Stop all services |
| `docker compose down -v` | Stop and delete all data |
| `docker compose logs -f app` | View app logs |
| `docker compose up --build app` | Rebuild only the app |

---

## Local Development (without Docker)

Run with H2 in-memory database:

```bash
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

---

## FAQ

**Q: Data persists after restart?**
Yes. Uses Docker named volumes (`pg_data`, `redis_data`).

**Q: How to reset the database?**
```bash
docker compose down -v
docker compose up --build
```

**Q: How to connect to PostgreSQL?**
```bash
docker exec -it greenify-db psql -U greenify_user -d greenify_db
```
