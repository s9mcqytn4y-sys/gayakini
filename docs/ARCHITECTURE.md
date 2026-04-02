# Gayakini Architecture Overview

Modular Monolith with Domain-Driven Design (DDD) principles.

## Package Structure
- `com.gayakini.app`: Bootstrap and global configuration.
- `com.gayakini.common`: Shared utilities, base DTOs, and cross-cutting concerns (idempotency, UUIDv7).
- `com.gayakini.infrastructure`: Technical concerns (Security, DB Config, JWT).
- `com.gayakini.[domain]`: Domain boundaries.
  - `api`: Controllers and DTOs.
  - `application`: Services (Orchestration).
  - `domain`: Entities and Repository interfaces.

## Domain Boundaries
- **Catalog**: Products, Variants, Categories.
- **Customer**: Identity, Profiles, Addresses.
- **Cart**: Transient shopping sessions.
- **Checkout**: Order preparation and shipping calculation.
- **Order**: Permanent purchase records and status tracking.
- **Payment**: Integration with Midtrans.
- **Shipping**: Integration with Biteship.
- **Inventory**: Stock management and reservations.
- **Webhook**: External event ingestion.

## Resilience & Patterns
- **Idempotency**: Handled at the application layer using `IdempotencyService`.
- **UUIDv7**: Used as primary keys for sorting and distributed systems compatibility.
- **Problem Details (RFC 7807)**: Standardized error responses.
- **Flyway**: Database versioning.
