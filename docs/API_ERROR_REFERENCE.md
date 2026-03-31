# API Error Reference: gayakini

Backend `gayakini` menggunakan standar **RFC 7807 (Problem Details for HTTP APIs)** untuk semua response error.

## 1. Format Umum
Semua error response akan memiliki format berikut:
```json
{
  "type": "https://gayakini.com/probs/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Token tidak valid atau kedaluwarsa.",
  "timestamp": "2023-10-27T10:00:00Z"
}
```

## 2. Daftar Kode Status & Tipe Error
| HTTP Status | Title | Type | Deskripsi |
|-------------|-------|------|-----------|
| 400 | Kesalahan Validasi | `.../validation-error` | Data input tidak sesuai skema (misal: email salah format). Lihat field `errors`. |
| 401 | Unauthorized | `.../unauthorized` | Token `Authorization` tidak ada atau salah. |
| 403 | Forbidden | `.../forbidden` | Token benar, tapi tidak memiliki hak akses (misal: customer akses API admin). |
| 404 | Data Tidak Ditemukan | `.../not-found` | Resource (Product/Order/Cart) tidak ada di database. |
| 409 | Konflik Bisnis | `.../conflict` | Operasi ditolak karena aturan bisnis (misal: stok habis). |
| 500 | Kesalahan Server | `.../internal-server-error` | Terjadi bug/crash di backend. |

## 3. Detail Kesalahan Validasi (400)
Jika status 400 karena validasi, field `errors` akan muncul:
```json
"errors": [
  { "field": "cartId", "message": "Harus diisi" },
  { "field": "shippingAddress.email", "message": "Email tidak valid" }
]
```
