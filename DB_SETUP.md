# MariaDB Setup Guide

---

## Step 1 — Install MariaDB

### macOS (Homebrew)
```bash
brew install mariadb
brew services start mariadb
sudo mariadb-secure-installation   # set root password
```

### Windows
Download the MSI installer from https://mariadb.org/download/
Run it, set a root password, keep port 3306.

### Ubuntu / Debian
```bash
sudo apt update && sudo apt install mariadb-server -y
sudo systemctl start mariadb
sudo mysql_secure_installation
```

---

## Step 2 — Run the Schema Script

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

This creates:
- The `balance_portal` database
- `customers`, `invoices`, `pending_registrations` tables
- An app user `portaluser` with least-privilege access (SELECT, INSERT, UPDATE, DELETE only)

---

## Step 3 — Load Seed Data

```bash
mysql -u portaluser -p balance_portal < src/main/resources/sql/seed.sql
```

This inserts the three demo customers and their invoices.

---

## Step 4 — Set Environment Variables

The app reads DB credentials from env vars — never hard-coded.

### macOS / Linux
```bash
export DB_HOST="localhost"
export DB_PORT="3306"
export DB_NAME="balance_portal"
export DB_USERNAME="portaluser"
export DB_PASSWORD="PortalPass123!"
```

Add to `~/.zshrc` or `~/.bashrc` for persistence.

### Windows (cmd)
```cmd
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=balance_portal
set DB_USERNAME=portaluser
set DB_PASSWORD=PortalPass123!
```

### IntelliJ IDEA (recommended for dev)
Run → Edit Configurations → Tomcat Server →
Startup/Connection tab → Environment variables field →
Add all DB_* keys there alongside the SMTP_* keys.

---

## Step 5 — Build and Deploy

```bash
mvn clean package
cp target/balance-portal-db.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

Watch the Tomcat console. You should see:

```
[ConnectionPool]   Connected to MariaDB at localhost:3306/balance_portal
[RepositoryFactory] Using MariaDB repositories.
[AppLifecycleListener] Data layer ready: MariaDB
```

If the DB is not reachable you will see:

```
[ConnectionPool]   Could not connect to MariaDB: ... Falling back to MockDataStore.
[RepositoryFactory] Using in-memory Mock repositories (fallback).
```

The app still starts and runs normally with the fallback.

---

## Verify It's Working

```sql
-- Connect as portaluser and check the tables
mysql -u portaluser -p balance_portal

SELECT username, account_number, email FROM customers;
SELECT invoice_number, status, amount FROM invoices ORDER BY account_number;
```

After registering a new user through the app:
```sql
-- Watch pending registrations
SELECT username, email, expires_at FROM pending_registrations;

-- After verification, the row moves to customers
SELECT username, account_number, verified FROM customers ORDER BY id DESC LIMIT 5;

-- And invoices are created
SELECT invoice_number, description, status FROM invoices
WHERE account_number NOT IN ('ACC-1001','ACC-1002','ACC-1003');
```

---

## Full Environment Variables Reference

| Variable       | Required | Default          | Description                        |
|----------------|----------|------------------|------------------------------------|
| DB_HOST        | No       | localhost        | MariaDB host                       |
| DB_PORT        | No       | 3306             | MariaDB port                       |
| DB_NAME        | No       | balance_portal   | Database name                      |
| DB_USERNAME    | No       | portaluser       | App DB user                        |
| DB_PASSWORD    | **Yes**  | —                | App DB password                    |
| SMTP_HOST      | No       | smtp.mail.com    | SMTP server                        |
| SMTP_PORT      | No       | 587              | SMTP port                          |
| SMTP_USERNAME  | **Yes**  | —                | Full email address                 |
| SMTP_PASSWORD  | **Yes**  | —                | Email password                     |
| SMTP_FROM      | No       | SMTP_USERNAME    | From display name + address        |
| APP_BASE_URL   | No       | localhost:8080/… | Base URL for verify link in email  |
