# Release Candidate 2 (RC2) Notes: gayakini Backend

## Ringkasan Perubahan
RC2 berfokus pada kesiapan integrasi tim frontend dan mobile di lingkungan sandbox.

### 1. Hardening Kontrak Integrasi
- Menambahkan **Frontend Sandbox Integration Pack** di folder `docs/`.
- Menstandarisasi response error menggunakan **RFC 7807 (Problem Detail)** untuk kemudahan parsing di sisi client.
- Menyiapkan **Sandbox Test Token** (`sandbox-test-token`) untuk mempermudah development tanpa harus login berulang kali.

### 2. Keamanan & Identitas
- Rute `/api/v1/orders/**` sekarang sepenuhnya terproteksi (401 jika tanpa token).
- Ekstraksi `UserPrincipal` dari token sudah stabil untuk digunakan dalam alur pembuatan pesanan.

### 3. Simulasi & Webhook
- Menyiapkan runbook simulasi webhook untuk Midtrans dan Biteship tanpa harus menunggu trigger dari server pihak ketiga.

## Daftar Artefak Integrasi
1. `docs/FRONTEND_SANDBOX_INTEGRATION.md`: Panduan umum.
2. `docs/API_ERROR_REFERENCE.md`: Kamus error.
3. `docs/SANDBOX_PAYMENT_FLOW.md`: Alur Midtrans.
4. `docs/SANDBOX_SHIPPING_FLOW.md`: Alur Biteship.

## Status Verifikasi
- **Build & Test:** Lulus (`./gradlew build`).
- **Idempotency:** Teruji untuk alur Place Order.
- **Stock Lock:** Teruji untuk alur Place Order.
- **Webhook Signature:** Teruji untuk alur Midtrans Callback.

## Known Limitations
- Modul Registrasi/Login belum diimplementasikan secara UI-facing, masih bergantung pada pre-generated tokens untuk testing.
- Webhook Biteship baru mendukung event `order.status` dasar.
