# Security & RBAC Model

## Authentication
- **Mechanism**: JWT (JSON Web Token)
- **Header**: `Authorization: Bearer <token>`
- **Token Lifecycle**: Managed via `JwtService` and `RefreshTokenRepository`.

## Role-Based Access Control (RBAC)

The system enforces access control based on the following roles:

| Role | Description |
|------|-------------|
| `CUSTOMER` | Default role for registered users. Can manage their own profile, cart, and orders. |
| `ADMIN` | Full system access, including user management and system configuration. |
| `FINANCE` | Access to financial reports and payment audit logs (via `/v1/finance/**`). |
| `OPERATOR` | Access to warehouse, inventory, and fulfillment operations (via `/v1/operations/**`). |

## Route Boundaries

| Path | Access Rule |
|------|-------------|
| `/v1/auth/**`, `/v1/products/**` | `permitAll()` |
| `/v1/me/**` | `hasRole('CUSTOMER')` |
| `/v1/admin/**` | `hasRole('ADMIN')` |
| `/v1/finance/**` | `hasAnyRole('ADMIN', 'FINANCE')` |
| `/v1/operations/**` | `hasAnyRole('ADMIN', 'OPERATOR')` |
| `/actuator/health` | `permitAll()` |
| `/actuator/**` | `hasRole('ADMIN')` |

## Ownership Verification
For customer-facing resources like Orders and Carts, the system performs an additional check to ensure the resource belongs to the authenticated user, even if the user has the `CUSTOMER` role.
