# Gayakini API Endpoint Registry & REST Analysis

## 🎯 RESTful API Analysis
The Gayakini API follows a mature, versioned REST architecture (`v1`) with a strong emphasis on observability and standardized communication.

### Design Patterns
*   **Uniform Interface**: Standard `ApiResponse<T>` and `PaginatedResponse<T>` wrappers ensure consistent parsing logic for frontends.
*   **HATEOAS-Lite**: While not fully HATEOAS, the use of `ProblemDetails` (RFC 7807) provides machine-readable error types and instances.
*   **Separation of Concerns**: Controllers are strictly divided by domain and actor (`Admin*` for backoffice, `Me*` for profile, Public for catalog).
*   **Idempotency**: Implemented in critical paths like payments and order cancellations.
*   **Observability**: Every response includes a `requestId` from MDC, allowing direct correlation with server logs.

---

## 🚀 Endpoint Registry (v1)

### 🔐 Authentication & Identity
| Method | Path | Description | Roles |
| :--- | :--- | :--- | :--- |
| POST | `/v1/auth/login` | Authenticate user and return JWT | PUBLIC |
| POST | `/v1/auth/register` | Register new customer account | PUBLIC |
| POST | `/v1/auth/refresh` | Refresh expired JWT | PUBLIC |
| GET | `/v1/me` | Get current user profile | AUTHENTICATED |
| PATCH | `/v1/me` | Update current user profile | AUTHENTICATED |
| GET | `/v1/me/addresses` | List saved addresses | AUTHENTICATED |
| POST | `/v1/me/addresses` | Add new address | AUTHENTICATED |

### 📦 Product Catalog
| Method | Path | Description | Roles |
| :--- | :--- | :--- | :--- |
| GET | `/v1/products` | List products (paginated, filtered) | PUBLIC |
| GET | `/v1/products/{id}` | Get product details | PUBLIC |
| GET | `/v1/products/categories` | List product categories | PUBLIC |
| POST | `/v1/admin/products` | Create new product | ADMIN |
| PUT | `/v1/admin/products/{id}` | Update product | ADMIN |
| DELETE | `/v1/admin/products/{id}` | Soft delete product | ADMIN |

### 🛒 Cart & Checkout
| Method | Path | Description | Roles |
| :--- | :--- | :--- | :--- |
| GET | `/v1/cart` | View current cart | AUTHENTICATED |
| POST | `/v1/cart/items` | Add item to cart | AUTHENTICATED |
| PATCH | `/v1/cart/items/{id}` | Update item quantity | AUTHENTICATED |
| DELETE | `/v1/cart/items/{id}` | Remove item from cart | AUTHENTICATED |
| POST | `/v1/checkout` | Initialize checkout process | AUTHENTICATED |

### 🧾 Orders & Payments
| Method | Path | Description | Roles |
| :--- | :--- | :--- | :--- |
| GET | `/v1/orders` | List my orders | AUTHENTICATED |
| GET | `/v1/orders/{id}` | Get order details | AUTHENTICATED |
| POST | `/v1/orders/{id}/cancellations` | Cancel order | AUTHENTICATED |
| POST | `/v1/payments/charge` | Create payment charge (Midtrans) | AUTHENTICATED |
| POST | `/v1/webhook/midtrans` | Midtrans payment notification | PUBLIC (SECURED) |

---

## 🛠️ Frontend Integration Hardening (DX)
To ensure the best experience for frontend developers:
1.  **Strict Typing**: Use the generated TypeScript types from OpenAPI.
2.  **Error Interceptors**: Centralized handling of `requestId` for error reporting.
3.  **Loading States**: All paginated responses include `totalElements` for proper UI skeletons.
4.  **Security**: JWT must be sent in `Authorization: Bearer <token>` header.
