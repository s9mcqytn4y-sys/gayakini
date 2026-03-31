# Frontend Sandbox Integration Guide: gayakini

Panduan ini ditujukan bagi tim Frontend (Web/Mobile) untuk mengintegrasikan aplikasi dengan backend `gayakini` di lingkungan sandbox/staging.

## 1. Base URL & Authentication
- **Base URL:** `http://localhost:8080` (Local/Sandbox)
- **Auth Scheme:** Bearer Token (JWT)
- **Sandbox Test Token:** `sandbox-test-token` (Gunakan di header `Authorization: Bearer sandbox-test-token`)
- **Guest Token:** Beberapa endpoint (seperti `place order` tanpa login) memerlukan header `X-Guest-Token`.

## 2. Struktur Response
### Success
```json
{
  "success": true,
  "message": "Operasi berhasil",
  "data": { ... },
  "timestamp": "2023-10-27T10:00:00Z"
}
```

### Error (RFC 7807 Problem Detail)
```json
{
  "type": "https://gayakini.com/probs/validation-error",
  "title": "Kesalahan Validasi",
  "status": 400,
  "detail": "Data yang Anda kirimkan tidak valid.",
  "errors": [
    { "field": "email", "message": "Email tidak valid" }
  ],
  "timestamp": "2023-10-27T10:00:01Z"
}
```

## 3. Alur Utama (Happy Path)
1. **Cart:** Kelola keranjang di sisi client atau via API Cart.
2. **Checkout:** Hitung shipping via `Shipping API` (Biteship boundary).
3. **Place Order:** `POST /api/v1/orders/place`. Kembalikan `orderNumber`.
4. **Payment:** Backend akan memberikan `snapToken` dan `redirectUrl` untuk Midtrans Snap.
5. **Callback:** Midtrans akan memanggil Webhook backend. Frontend cukup melakukan polling status atau menunggu event.

## 4. Daftar Endpoint Penting
- `GET /api/v1/hello`: Cek konektivitas.
- `POST /api/v1/orders/place`: Membuat pesanan.
- `GET /api-docs`: OpenAPI JSON.
- `swagger-ui.html`: Dokumentasi interaktif.

## 5. Simulasi Webhook (Sandbox)
Gunakan tool seperti Postman untuk memicu webhook manual di lokal:
- **Midtrans:** `POST /api/v1/webhooks/midtrans`
- **Biteship:** `POST /api/v1/webhooks/biteship`
