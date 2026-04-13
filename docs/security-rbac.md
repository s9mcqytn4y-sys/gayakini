# Security & Role-Based Access Control (RBAC)

Dokumen ini menjelaskan kontrak security untuk HTTP surface yang aktif dan dipublikasikan melalui generated OpenAPI.
Jika ada drift antara dokumen ini dan runtime, `/api-docs` adalah sumber kebenaran utama.

## Published Roles

Kontrak endpoint yang aktif saat ini dipublikasikan untuk dua role utama:

1.  **`ADMIN`**:
    - **Scope**: Omnipotent system operator.
    - **Access**: Full access to administrative endpoints, finance, audit, dan actuator.
    - **Identity**: Usually assigned to the store owner or authorized staff.

2.  **`CUSTOMER`**:
    - **Scope**: Authenticated end-user.
    - **Access**: Resource pribadi, upload proof tertentu, dan alur JWT customer.
    - **Identity**: Created via registration or social login.

## Access Control Matrix

| Resource Group | Endpoint Pattern | Role Required | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| **Public API** | `/v1/products/**` | None (Guest) | `permitAll()` in SecurityConfig |
| **Guest Flow** | `/v1/carts/**`, `/v1/checkouts/**` | None (Guest) | Token-based (X-Cart-Token) |
| **Guest Flow** | `POST /v1/checkouts/{checkoutId}/orders` | None (Guest) | Checkout token + idempotency |
| **Orders (Guest)** | `GET /v1/orders/{orderId}`, `POST /v1/orders/{orderId}/cancellations` | None (Guest) | Order token matching |
| **Payment Session** | `GET /v1/payments/config`, `POST /v1/payments/orders/{orderId}` | None (Guest) | Public config + order token matching in service |
| **Profile** | `/v1/me/**` | `ROLE_CUSTOMER` | `hasRole('CUSTOMER')` & User ID filter |
| **Admin Operations** | `/v1/admin/**` | `ROLE_ADMIN` | `hasRole('ADMIN')` |
| **Payment Proof** | `/v1/admin/payments/**` | JWT (`ADMIN`/`CUSTOMER`) | `authenticated()` + `@PreAuthorize` |
| **Secure Media** | `/v1/media/secure/**` | JWT (`ADMIN`/owner) | `authenticated()` + ownership predicate |
| **Webhooks** | `/v1/webhooks/**` | None (Public) | Provider signature verification |
| **System Health** | `/actuator/**` | `ROLE_ADMIN` | `hasRole('ADMIN')` |

## Resource Ownership (ID Matching)

For `CUSTOMER` endpoints, the system enforces strict resource ownership. Authentication is not enough; the requested resource must belong to the authenticated user.

### Enforcement Strategy:
- **Automatic Scoping**: Repositories or Services filter by `customerId` retrieved from `SecurityUtils.getCurrentUserId()`.
- **Manual Validation**: In `OrderService`, every access to `/v1/me/orders/{id}` is validated via `validateOrderAccess(orderId, customerId)`.
- **Address Management**: The `AddressController` uses the authenticated `UserPrincipal.id` to scope all CRUD operations.

## JWT Implementation

- **Subject**: Contains the `UUID` of the Customer.
- **Claims**:
    - `email`: User's primary email.
    - `role`: Either `ADMIN` or `CUSTOMER`.
- **Validation**: Every request is intercepted by `JwtAuthenticationFilter`, which populates the `SecurityContext` with a `UserPrincipal`.

## Secure Webhook Pattern

While `/v1/webhooks/**` endpoints are marked as `permitAll()` in Spring Security to allow provider ingress, they are **not unprotected**. Each controller performs provider-specific signature or token validation before mutating state.
