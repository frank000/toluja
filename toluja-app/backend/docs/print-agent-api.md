# Print Agent API Contract

Base path: `/api/print-agent`  
Auth header (required): `X-Print-Key: <tenant_print_key>`

## 1) Pull next print job

`GET /api/print-agent/jobs/next?deviceId={deviceId}`

### Responses

- `200 OK` with a job
- `204 No Content` when queue is empty
- `401 Unauthorized` when print key is invalid

### 200 body

```json
{
  "jobId": "f3bf5c4a-3e26-4a27-b729-3f6f469b7480",
  "tenantId": "default",
  "storeId": "store-001",
  "deviceId": "agent-store-001",
  "orderId": "order-abc123",
  "payloadType": "TEXT",
  "payloadBase64": "Li4uYmFzZTY0Li4u",
  "createdAt": "2026-03-03T16:05:01.442Z",
  "deliveries": [
    {
      "deliveryId": "9cb59326-c237-4b3a-a20d-f40dfd3a7e89",
      "printerId": "balcao-usb-1",
      "printerName": "Balcao USB",
      "channel": "CUPS",
      "destination": "EPSON_TM_T20X",
      "copies": 1
    },
    {
      "deliveryId": "5ed245db-3753-4bf1-9950-2de18f12f3b9",
      "printerId": "cozinha-usb-1",
      "printerName": "Cozinha USB",
      "channel": "CUPS",
      "destination": "BEMATECH_MP4200",
      "copies": 1
    }
  ]
}
```

## 2) Ack delivery results

`POST /api/print-agent/jobs/{jobId}/ack`

### Request body

```json
{
  "deliveries": [
    {
      "deliveryId": "9cb59326-c237-4b3a-a20d-f40dfd3a7e89",
      "status": "SUCCESS",
      "errorMessage": null,
      "printedAt": "2026-03-03T16:05:04.001Z"
    },
    {
      "deliveryId": "5ed245db-3753-4bf1-9950-2de18f12f3b9",
      "status": "ERROR",
      "errorMessage": "Fila USB indisponível",
      "printedAt": "2026-03-03T16:05:04.058Z"
    }
  ]
}
```

`status` accepts only: `SUCCESS`, `ERROR`.

### Response body

```json
{
  "jobId": "f3bf5c4a-3e26-4a27-b729-3f6f469b7480",
  "status": "PARTIAL_OR_ERROR",
  "receivedDeliveries": 2
}
```

## 3) Optional enqueue endpoint (dev/reference)

`POST /api/print-agent/jobs`

Uses the same payload as the `GET /jobs/next` response body.
