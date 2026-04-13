# Gayakini Midtrans Snap Integration Guide

This guide outlines how to integrate the Gayakini Payment API with the Midtrans Snap frontend SDK.

Use `GET /api-docs` or `GET /swagger-ui.html` as the canonical contract if this guide and runtime ever differ.

## 1. Get Midtrans Client Key
Before initializing Snap, you need the `Client Key`. Fetch it from the backend:

```http
GET /v1/payments/config
```

Response:
```json
{
  "message": "Konfigurasi pembayaran berhasil diambil.",
  "data": {
    "clientKey": "SB-Mid-client-xxxxxxxx",
    "isProduction": false
  }
}
```

## 2. Load Midtrans Snap Script
Include the Snap script in your HTML header or via a script tag:

- **Sandbox**: `https://app.sandbox.midtrans.com/snap/snap.js`
- **Production**: `https://app.midtrans.com/snap/snap.js`

Use the `data-client-key` attribute with the key obtained from Step 1.

## 3. Create Payment Session
When the user clicks "Pay", call the Gayakini API to get a `snapToken`.

```http
POST /v1/payments/orders/{orderId}
Header: Idempotency-Key: <unique-uuid>
Header: X-Order-Token: <token-from-checkout> (for guests)
Content-Type: application/json

{
  "preferredChannel": "BCA_VA",
  "enabledChannels": ["BCA_VA", "GOPAY", "QRIS"]
}
```

Response:
```json
{
  "data": {
    "snapToken": "xxxx-xxxx-xxxx",
    "snapRedirectUrl": "https://app.sandbox.midtrans.com/snap/v2/vtweb/xxxx",
    "transactionNumber": "PAY-20231027-ABC123"
  }
}
```

## 4. Trigger Snap Popup
Use the `snap.pay()` method with the `snapToken`.

```javascript
window.snap.pay(snapToken, {
  onSuccess: function(result) {
    /* UI Transition only! Do not update status via API here. */
    window.location.href = '/order/success?id=' + orderId;
  },
  onPending: function(result) {
    window.location.href = '/order/pending?id=' + orderId;
  },
  onError: function(result) {
    window.location.href = '/order/error?id=' + orderId;
  },
  onClose: function() {
    console.log('User closed the popup without finishing the payment');
  }
});
```

## 5. Important Security Rules
1. **Never update order status from the frontend.** The backend handles status updates via Midtrans Webhooks and periodic status polling.
2. **Client Key is public**, but **Server Key is strictly confidential**. Never expose the Server Key in frontend code.
3. **CORS** is restricted to authorized origins (e.g., `http://localhost:3000`).
