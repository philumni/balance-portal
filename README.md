# Balance Portal — JWT + Ajax Version

Stateless authentication using **JSON Web Tokens (JWT)** and **Ajax fetch()**.  
No JSPs. No HttpSession. The server only speaks JSON.

---

## What Changed from the Session Version

| Concern          | Session Version              | JWT Version                              |
|------------------|------------------------------|------------------------------------------|
| Auth state       | `HttpSession` on server      | JWT stored in `localStorage` on client  |
| Views            | JSP server-rendered HTML     | Static HTML + JS builds the DOM         |
| Data format      | Full HTML page response      | JSON responses only                      |
| Protected routes | `session.getAttribute()`     | `Authorization: Bearer <token>` header  |
| Logout           | `session.invalidate()`       | Client deletes token from localStorage  |
| Scalability      | Sticky sessions / shared mem | Stateless — any server can verify       |

---

## Tech Stack

| Layer      | Technology                         |
|------------|------------------------------------|
| Server     | Apache Tomcat 10.x                 |
| Backend    | Java Servlets (Jakarta EE 6)       |
| JWT        | jjwt 0.12.5 (HMAC-SHA256 / HS256) |
| JSON       | Jackson 2.17                       |
| Frontend   | Static HTML + Vanilla JS (fetch)   |
| Build      | Maven (WAR packaging)              |

---

## Project Structure

```
balance-portal-jwt/
├── pom.xml
└── src/main/
    ├── java/com/portal/
    │   ├── model/
    │   │   ├── Customer.java           @JsonIgnore on password
    │   │   └── Invoice.java            @JsonProperty for formatted fields
    │   ├── data/
    │   │   └── MockDataStore.java      Same singleton as before
    │   ├── util/
    │   │   └── JwtUtil.java            generateToken() / validateToken()
    │   └── servlet/
    │       ├── BaseApiServlet.java     Shared JSON helpers + JWT extraction
    │       ├── LoginServlet.java       POST /api/login  → returns JWT
    │       ├── InvoiceServlet.java     GET  /api/invoices (protected)
    │       └── LogoutServlet.java      POST /api/logout (stateless)
    └── webapp/
        ├── index.html                  Login page (static)
        ├── dashboard.html              Dashboard (static)
        └── js/
            ├── login.js                Ajax login, stores JWT
            └── dashboard.js           Fetches invoices, builds DOM
```

---

## How JWT Auth Works Here

### Login
```
Browser                             Server
  |  POST /api/login                  |
  |  { username, password }  ──────>  |
  |                                   |  authenticate() → Customer
  |                                   |  JwtUtil.generateToken(...)
  |  { token: "eyJ...", ... }  <────  |
  |                                   |
  |  localStorage.setItem('bp_token') |
```

### Every Protected Request
```
Browser                             Server
  |  GET /api/invoices               |
  |  Authorization: Bearer eyJ... -> |
  |                                  |  JwtUtil.validateToken(token)
  |                                  |  extract accountNumber from claims
  |                                  |  load invoices from MockDataStore
  |  { invoices: [...] }  <────────  |
```

### Logout
```
Browser                             Server
  |  POST /api/logout  ──────────>  |  (nothing to destroy — stateless)
  |  200 OK  <─────────────────────  |
  |                                  |
  |  localStorage.removeItem(...)    |
  |  redirect to /index.html         |
```

---

## Build & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- Apache Tomcat 10.x

```bash
cd balance-portal-jwt
mvn clean package
cp target/balance-portal-jwt.war /path/to/tomcat/webapps/
# Start Tomcat, then visit:
http://localhost:8080/balance-portal-jwt/
```

---

## Demo Accounts

| Username  | Password  | Account  |
|-----------|-----------|----------|
| jsmith    | pass123   | ACC-1001 |
| mjohnson  | secure99  | ACC-1002 |
| bdavis    | letmein   | ACC-1003 |

---

## Security Notes

| Topic             | This Demo                        | Production Recommendation            |
|-------------------|----------------------------------|---------------------------------------|
| Secret key        | Hard-coded string                | Load from env var / secrets manager  |
| Token storage     | `localStorage`                   | HttpOnly cookie or in-memory          |
| Token expiry      | 1 hour                           | Short-lived (15 min) + refresh token |
| Logout blacklist  | None (stateless)                 | Redis JTI blacklist                   |
| Password storage  | Plain text                       | BCrypt                                |
| HTTPS             | Not configured                   | Required in production                |

---

## Extending the Project

1. **Refresh tokens** — issue a short-lived access token + a longer refresh token
2. **Redis blacklist** — on logout, store the token's `jti` claim in Redis until expiry
3. **Real database** — swap `MockDataStore` with JDBC calls
4. **Role-based access** — add a `role` claim to the JWT, check it in `BaseApiServlet`
5. **BCrypt passwords** — replace plain-text comparison with `BCrypt.checkpw()`
