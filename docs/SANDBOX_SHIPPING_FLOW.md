# Sandbox Shipping Flow: Biteship Integration

## 1. Sequence Diagram
1. **Client:** Pilih Alamat & Kurir.
2. **Client:** `POST /api/v1/orders/place`.
3. **Backend:** Simpan Order, Reservasi Stok.
4. **Backend:** Panggil Biteship Order API (Booking).
5. **Biteship:** Konfirmasi Booking & Alokasi Driver (Simulator).
6. **Biteship:** Kirim Webhook ke Backend `/api/v1/webhooks/biteship`.
7. **Backend:** Simpan Waybill ID & Update status Order menjadi `SHIPPED`.

## 2. Simulasi Webhook (Local Dev)
Gunakan `curl` untuk mensimulasikan notifikasi status kurir:
```bash
curl -X POST http://localhost:8080/api/v1/webhooks/biteship \
-H "Content-Type: application/json" \
-d '{
  "event": "order.status",
  "order_id": "BITESHIP_ORDER_ID_HERE",
  "status": "shipped",
  "waybill_id": "GAYA-SHIP-001"
}'
```

## 3. Catatan Penting
- **Area ID:** Pastikan menggunakan Area ID yang valid dari Biteship (misal: `IDN_AREA_ID`).
- **Kurir:** Gunakan kurir yang didukung sandbox (misal: `jne`, `sicepat`, `jnt`).
- **Tracking:** Waybill ID baru muncul setelah status berubah menjadi `picked` atau `shipped`.
- **Simulator:** Gunakan [Biteship Sandbox Console](https://biteship.com/dashboard/developers/api) untuk memantau trafik.
