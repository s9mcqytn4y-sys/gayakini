# Security & Role-Based Access Control (RBAC)

Gayakini uses a simplified, strict RBAC model designed for a single-admin architecture. This document outlines the two available roles and how resource access is enforced.

## Roles

The system recognizes exactly two roles:

1.  **`ADMIN`**:
    - **Scope**: Omnipotent system operator.
    - **Access**: Full access to all administrative endpoints (`/v1/admin/**`) and management functions (Catalog, Orders, Inventory, Actuator).
    - **Identity**: Usually assigned to the store owner or authorized staff.

2.  **`CUSTOMER`**:
    - **Scope**: Authenticated end-user.
    - **Access**: Restricted to personal resources (`/v1/me/**`).
    - **Identity**: Created via registration or social login.

## Access Control Matrix

| Resource Group | Endpoint Pattern | Role Required | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| **Public API** | `/v1/products/**` | None (Guest) | `permitAll()` in SecurityConfig |
| **Guest Flow** | `/v1/carts/**`, `/v1/checkouts/**` | None (Guest) | Token-based (X-Cart-Token) |
| **Orders (Guest)** | `/v1/orders/{id}` | None (Guest) | Order Token matching |
| **Profile** | `/v1/me/**` | `ROLE_CUSTOMER` | `hasRole('CUSTOMER')` & User ID filter |
| **Admin Operations** | `/v1/admin/**` | `ROLE_ADMIN` | `hasRole('ADMIN')` |
| **Webhooks** | `/v1/webhooks/**` | None (Public) | Provider signature verification |
| **System Health** | `/v1/actuator/**` | `ROLE_ADMIN` | `hasRole('ADMIN')` |

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

While `/v1/webhooks/**` endpoints are marked as `permitAll()` in Spring Security to allow provider ingress, they are **not unprotected**. Each controller (e.g., `MidtransWebhookController`) performs a cryptographic signature check against the provider's public key or shared secret.
