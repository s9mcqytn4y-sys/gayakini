# Frontend Sandbox Integration Guide: gayakini

Panduan ini ditujukan bagi tim Frontend (Web/Mobile) untuk mengintegrasikan aplikasi dengan backend `gayakini` di lingkungan sandbox/staging.

## 1. Base URL & Authentication
- **Base URL:** `http://localhost:8080` (Local/Sandbox)
- **Auth Scheme:** Bearer Token (JWT)
- **Source of Truth API:** `GET /api-docs` dan `GET /swagger-ui.html`
- **Customer/Admin Token:** didapat dari `POST /v1/auth/login`
- **Guest Tokens:** alur guest memakai `X-Cart-Token`, `X-Checkout-Token`, dan `X-Order-Token` sesuai resource yang sedang diakses

## 2. Struktur Response
### Success
```json
{
  "success": true,
  "message": "Operasi berhasil",
  "data": { ... },
  "meta": {
    "requestId": "..."
  }
}
```

### Error
```json
{
  "success": false,
  "message": "Permintaan tidak valid.",
  "meta": {
    "requestId": "..."
  }
}
```

Catatan:
- Runtime error contract mengikuti bentuk `StandardResponse` tanpa RFC 7807.
- Jika butuh payload paling baru per endpoint, selalu cek Swagger UI.

## 3. Alur Utama (Happy Path)
1. **Cart:** `POST /v1/carts`, lalu simpan `id` dan `accessToken`.
2. **Isi cart:** `POST /v1/carts/{cartId}/items` dengan `X-Cart-Token` untuk guest.
3. **Checkout:** `POST /v1/checkouts`, lalu semua operasi checkout guest wajib mengirim `X-Checkout-Token`.
4. **Shipping:** `PUT /v1/checkouts/{checkoutId}/shipping-address`, `POST /v1/checkouts/{checkoutId}/shipping-quotes`, `PUT /v1/checkouts/{checkoutId}/shipping-selection`.
5. **Place order:** `POST /v1/checkouts/{checkoutId}/orders`.
6. **Payment:** `POST /v1/payments/orders/{orderId}`, guest wajib kirim `X-Order-Token`.
7. **Webhook:** Midtrans dan Biteship mengupdate status backend; frontend cukup refresh detail order atau list pesanan.

## 4. Endpoint Penting
- `GET /v1/hello`: Cek konektivitas.
- `POST /v1/auth/login`: Ambil access token customer/admin.
- `GET /v1/me`: Ambil profil customer yang sedang login.
- `GET /v1/products`: Catalog public.
- `GET /v1/carts/{cartId}`: Lihat cart guest/customer.
- `GET /v1/checkouts/{checkoutId}`: Lihat state checkout.
- `GET /v1/orders/{orderId}`: Lihat detail order.
- `GET /v1/me/orders`: Lihat order customer login.
- `POST /v1/payments/orders/{orderId}`: Buat sesi pembayaran Midtrans.
- `GET /api-docs`: OpenAPI JSON.
- `GET /swagger-ui.html`: Dokumentasi interaktif.

Untuk endpoint selain daftar ringkas di atas, jangan copy dari dokumen lama. Ambil langsung dari generated contract.

## 5. Simulasi Webhook (Sandbox)
Gunakan tool seperti Postman untuk memicu webhook manual di lokal:
- **Midtrans:** `POST /v1/webhooks/midtrans`
- **Biteship:** `POST /v1/webhooks/biteship`
