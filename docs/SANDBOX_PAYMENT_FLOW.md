# Sandbox Payment Flow: Midtrans Integration

## 1. Sequence Diagram (Simplified)
1. **Client:** Klik "Bayar" di aplikasi.
2. **Client:** `POST /api/v1/orders/place`.
3. **Backend:** Simpan Order, Reservasi Stok.
4. **Backend:** Panggil Midtrans Snap API.
5. **Backend:** Kembalikan `snapToken` & `redirectUrl` ke Client.
6. **Client:** Buka Midtrans Snap Overlay (Web/Mobile SDK).
7. **Midtrans:** Proses pembayaran (Sandbox).
8. **Midtrans:** Kirim Webhook ke Backend `/api/v1/webhooks/midtrans`.
9. **Backend:** Update status Order menjadi `PAID`.

## 2. Simulasi Webhook (Local Dev)
Gunakan `curl` untuk mensimulasikan notifikasi sukses dari Midtrans:
```bash
curl -X POST http://localhost:8080/api/v1/webhooks/midtrans \
-H "Content-Type: application/json" \
-d '{
  "transaction_status": "settlement",
  "order_id": "PASTE_ORDER_UUID_HERE",
  "transaction_id": "MID-TEST-123",
  "gross_amount": "100000.00",
  "signature_key": "SHA512_OF_ORDERID_STATUS_AMOUNT_SERVERKEY"
}'
```

## 3. Catatan Penting
- **Idempotency:** Jangan khawatir jika webhook terkirim dua kali. Backend sudah diproteksi.
- **Expiry:** Link pembayaran (Snap Token) biasanya berlaku 24 jam di sandbox.
- **Simulator:** Gunakan [Midtrans Sandbox Simulator](https://simulator.sandbox.midtrans.com/) untuk mencoba berbagai skenario (Success, Deny, Cancel).
