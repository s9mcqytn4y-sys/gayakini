# Sandbox Payment Flow: Midtrans Integration

## 1. Sequence Diagram (Simplified)
1. **Client:** Klik "Bayar" di aplikasi.
2. **Client:** `POST /v1/checkouts/{checkoutId}/orders`.
3. **Backend:** Simpan Order, Reservasi Stok.
4. **Client:** `POST /v1/orders/{orderId}/payments` dengan `Idempotency-Key` dan `X-Order-Token` untuk guest order.
5. **Backend:** Panggil Midtrans Snap API.
6. **Backend:** Kembalikan `snapToken` & `redirectUrl` ke Client.
7. **Client:** Buka Midtrans Snap Overlay (Web/Mobile SDK).
8. **Midtrans:** Proses pembayaran (Sandbox).
9. **Midtrans:** Kirim Webhook ke Backend `/v1/webhooks/midtrans`.
10. **Backend:** Rekonsiliasi status pembayaran lalu update Order menjadi `PAID`.

## 2. Simulasi Webhook (Local Dev)
Gunakan `curl` untuk mensimulasikan notifikasi sukses dari Midtrans:
```bash
curl -X POST http://localhost:8080/v1/webhooks/midtrans \
-H "Content-Type: application/json" \
-d '{
  "transaction_status": "settlement",
  "order_id": "PASTE_PROVIDER_ORDER_ID_HERE",
  "transaction_id": "MID-TEST-123",
  "status_code": "200",
  "gross_amount": "100000.00",
  "signature_key": "SHA512_OF_ORDERID_STATUS_AMOUNT_SERVERKEY"
}'
```

## 3. Catatan Penting
- **Idempotency:** Endpoint create payment mewajibkan `Idempotency-Key`, tetapi webhook reconciliation di backend belum menyimpan receipt terpisah. Duplicate delivery masih perlu hardening lanjutan.
- **Expiry:** Link pembayaran (Snap Token) biasanya berlaku 24 jam di sandbox.
- **Simulator:** Gunakan [Midtrans Sandbox Simulator](https://simulator.sandbox.midtrans.com/) untuk mencoba berbagai skenario (Success, Deny, Cancel).
