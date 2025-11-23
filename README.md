# Clearing Service

Spring Boot microservice scaffold for clearing/settlement workflows that mirrors the payment-flow-service conventions (stack, dependencies, and config layout).

**Stack:** Java 17 | Spring Boot 3.2.5 | PostgreSQL | jOOQ | JWT

## Usage

- `mvn clean install` to build
- `docker build -t clearing-service:latest .` to build the container image

## Project Layout

- `src/main/java/com/example/clearing` — application code (bootstrapped with a single entrypoint)
- `src/main/resources` — environment-specific configs and placeholders for SQL/templates

## Notes

- JWT, auditing, and RLS/security defaults follow the payment-flow-service setup; update the values to match clearing needs.
- Database settings reference env vars `CLEARING_DB_URL`, `CLEARING_DB_USER`, `CLEARING_DB_PASSWORD`.
- See `documentation/LBE/README.md` for the broader system overview and `copilot-instructions.md` for coding standards.
