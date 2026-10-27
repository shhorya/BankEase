# BankEase: Net Banking Platform

Full-stack net banking app with JWT auth, ACID money transfers, loans, bill payments, investments and a security audit log.

## Tech Stack
| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, JPA/Hibernate, Flyway |
| Database | MySQL 8 |
| Auth | JWT (HS256), BCrypt, account lockout |
| Frontend | Angular 21, TypeScript, custom CSS design system |
| DevOps | GitHub Actions CI (build + tests), Swagger/OpenAPI |

## Features
- Register and login with 24h JWT and a 15-minute lockout after 5 failed logins
- Multiple accounts, deposit, withdraw and transfer with pessimistic row locking (no double-spend)
- Ownership checks so users can only access their own accounts
- Bill payments, loans (10.5% p.a., instant approval up to ₹5,00,000, admin approval above that), investments (FD, SIP, Mutual Fund)
- Audit log of every login and money action
- Dashboard with balance chart, stats, filters, transaction details and CSV export
- Dark mode (saved), responsive mobile layout, animated UI

## Run Locally
**Prerequisites:** Java 21, Node 22, MySQL 8

1. Create the database:
   ```sql
   CREATE DATABASE bankease_db;
   ```
2. Create `bankease-backend/src/main/resources/secrets.properties` (git-ignored):
   ```
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   app.jwt.secret=ANY_RANDOM_STRING_AT_LEAST_32_CHARACTERS_LONG
   ```
3. Start the backend (Flyway creates the tables automatically):
   ```bash
   cd bankease-backend
   ./mvnw spring-boot:run
   ```
4. Start the frontend:
   ```bash
   cd bankease-frontend
   npm install
   npm start
   ```
5. Open http://localhost:4200

API docs: http://localhost:8080/swagger-ui.html

## Make an Admin (to approve loans)
```sql
UPDATE users SET role='ADMIN' WHERE email='you@email.com';
```

## Testing
```bash
cd bankease-backend && ./mvnw verify
```

## Screenshots
| Login | Dashboard |
|---|---|
| ![login](docs/login.png) | ![dashboard](docs/dashboard.png) |

| Transfer drawer | Mobile |
|---|---|
| ![drawer](docs/drawer.png) | ![mobile](docs/mobile.png) |

## Architecture Notes
- Transfers lock both accounts in a fixed order, which prevents deadlocks.
- Schema is owned by Flyway migrations and Hibernate runs in `validate` mode.
- A global exception handler returns consistent JSON errors.