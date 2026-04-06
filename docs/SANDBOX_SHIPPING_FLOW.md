# Sandbox Shipping Flow: Biteship Integration

## 1. Sequence Diagram
1. **Client:** Pilih Alamat & Kurir.
2. **Client:** `POST /v1/checkouts/{checkoutId}/orders`.
3. **Backend:** Simpan Order, reservasi stok, dan snapshot pilihan pengiriman.
4. **Backend:** Panggil Biteship Order API saat flow booking shipment diaktifkan.
5. **Biteship:** Konfirmasi booking & alokasi kurir.
6. **Biteship:** Kirim Webhook ke Backend `/v1/webhooks/biteship`.
7. **Backend:** Simpan waybill ID dan update status fulfillment/order.

## 2. Simulasi Webhook (Local Dev)
Gunakan `curl` untuk mensimulasikan notifikasi status kurir:
```bash
curl -X POST http://localhost:8080/v1/webhooks/biteship \
-H "Content-Type: application/json" \
-d '{
  "event": "order.status",
  "order_id": "BITESHIP_ORDER_ID_HERE",
  "status": "shipped",
  "courier_waybill_id": "GAYA-SHIP-001"
}'
```

## 3. Catatan Penting
- **Area ID:** Pastikan menggunakan Area ID yang valid dari Biteship (misal: `IDN_AREA_ID`).
- **Kurir:** Gunakan kurir yang didukung sandbox (misal: `jne`, `sicepat`, `jnt`).
- **Tracking:** Payload resmi Biteship memakai event `order.status` dan `order.waybill_id`; field waybill yang umum adalah `courier_waybill_id`.
- **Security:** Backend saat ini belum memverifikasi webhook secret Biteship secara eksplisit. Ini masih gap hardening.
- **Simulator:** Gunakan [Biteship Sandbox Console](https://biteship.com/dashboard/developers/api) untuk memantau trafik.
