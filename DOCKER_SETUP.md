# Docker Compose Setup

One command starts everything. MariaDB and Tomcat spin up together,
the schema is created automatically, and the app is live at `localhost:8080`.

---

## Prerequisites

Install Docker Desktop: https://www.docker.com/products/docker-desktop/

That's it. No Java, no Maven, no MariaDB installation needed on your machine.
Docker handles all of it inside containers.

---

## Step 1 — Configure Credentials

```bash
cp .env.example .env
```

Open `.env` and fill in your real SMTP credentials:

```env
SMTP_USERNAME=yourname@mail.com
SMTP_PASSWORD=yourpassword
SMTP_FROM=Balance Portal <yourname@mail.com>
```

The DB passwords are pre-filled with defaults that work out of the box.
Change them if you're deploying anywhere public.

---

## Step 2 — Start Everything

```bash
docker-compose up --build
```

What happens in order:

1. Docker builds the Tomcat image (Maven compiles the WAR inside the container)
2. MariaDB container starts
3. MariaDB runs `01-schema.sql` then `02-seed.sql` automatically
4. MariaDB health check passes (confirms InnoDB is initialized)
5. Tomcat container starts
6. HikariCP connects to MariaDB
7. App is live

First run takes 2–3 minutes (Maven downloads dependencies).
Subsequent runs take ~20 seconds (dependencies are cached).

---

## Step 3 — Open the App

```
http://localhost:8080/balance-portal-db/
```

Demo accounts:

| Username  | Password  |
|-----------|-----------|
| jsmith    | pass123   |
| mjohnson  | secure99  |
| bdavis    | letmein   |

---

## Day-to-Day Commands

```bash
# Start (first time or after code changes)
docker-compose up --build

# Start (no code changes — uses cached image)
docker-compose up

# Start in background (detached)
docker-compose up -d

# View logs
docker-compose logs -f

# View only Tomcat logs
docker-compose logs -f tomcat

# View only MariaDB logs
docker-compose logs -f mariadb

# Stop containers (data is preserved)
docker-compose down

# Stop and DELETE all data (full reset)
docker-compose down -v

# Rebuild just the Tomcat image after code changes
docker-compose build tomcat
docker-compose up
```

---

## Connect to MariaDB Directly

MariaDB port 3306 is exposed to your host machine.
Connect with DBeaver, MySQL Workbench, HeidiSQL, or the CLI:

```bash
# CLI
mysql -h 127.0.0.1 -P 3306 -u portaluser -p balance_portal

# Check registered users
SELECT username, account_number, email, created_at FROM customers;

# Check invoices
SELECT invoice_number, account_number, status, amount FROM invoices;

# Watch pending registrations
SELECT username, email, expires_at FROM pending_registrations;
```

---

## How the Two Containers Talk to Each Other

Docker Compose creates a private network between the containers.
Each service can reach the other using its **service name as a hostname**.

In `docker-compose.yml`:
```yaml
DB_HOST: mariadb   # ← "mariadb" is the service name
```

Docker's internal DNS resolves `mariadb` to the MariaDB container's IP.
This is why `DB_HOST=mariadb` works inside Docker but `DB_HOST=localhost`
would not (localhost inside the Tomcat container refers to the Tomcat container itself).

---

## What the Health Check Does

```yaml
healthcheck:
  test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
  interval: 5s
  retries: 10
```

MariaDB takes a few seconds to initialize after the process starts.
Without the health check, Tomcat would start immediately, try to connect
before MariaDB is ready, and fall back to MockDataStore.

`depends_on: condition: service_healthy` makes Tomcat wait until
MariaDB passes the health check before starting. This guarantees
the app always connects to the real database on startup.

---

## Data Persistence

MariaDB data lives in a Docker **named volume** (`mariadb_data`).

```bash
docker-compose down       # containers stop — DATA IS KEPT
docker-compose down -v    # containers stop — DATA IS DELETED
```

Use `down -v` when you want a clean reset (re-runs schema + seed on next `up`).

---

## Production Checklist

Before deploying publicly:

- [ ] Change all passwords in `.env`
- [ ] Set `APP_BASE_URL` to your real domain
- [ ] Add HTTPS (nginx reverse proxy in front of Tomcat)
- [ ] Change `restart: unless-stopped` to `restart: always`
- [ ] Move `.env` values to a secrets manager (AWS Secrets Manager, Vault)
- [ ] Set `DB_HOST` to your managed DB service (e.g. Amazon RDS)
