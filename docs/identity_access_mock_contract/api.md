# Kontrakt REST API — identity_access_mock

Port domyślny: `10525` (env `SERVER_PORT`). Serwis wołany bezpośrednio z przeglądarki — CORS dozwolony
dla originu `http://localhost:10500` (właściwość `app.cors.allowed-origin`, nadpisywalna zmienną
środowiskową `APP_CORS_ALLOWEDORIGIN` — wymagane w środowiskach innych niż lokalne).

Panel operatorski Thymeleaf (`/`, `/accounts`, `/otp`, `/devices`) nie jest częścią tego kontraktu —
to wewnętrzne narzędzie do manualnego testowania, nieprzeznaczone do integracji.

## Auth

### POST /auth/login

Request:
```json
{ "employeeId": "PRC-00001", "password": "Admin12345" }
```

Response `200`:
```json
{
  "token": "uuid",
  "employeeId": "PRC-00001",
  "role": "Administrator systemu",
  "permissions": ["dashboard", "loans", "risk", "..."]
}
```

Błędy: `401` (niepoprawne dane logowania), `403` (konto `PENDING_APPROVAL`), `400` (brak pól).

### POST /auth/logout

Nagłówek `Authorization: Bearer <token>` (opcjonalny — nieznany/pusty token jest no-opem).

Response: `204 No Content`.

### POST /auth/register

Request (kształt zgodny z formularzem `register.page.ts`):
```json
{
  "personal": { "firstName": "", "lastName": "", "email": "", "phone": "" },
  "employment": {
    "employeeId": "PRC-00000",
    "department": "", "branch": "", "position": "", "supervisor": "",
    "startDate": "2026-10-14T00:00:00.000Z"
  },
  "industry": { "businessLines": ["retail"], "certifications": "", "licenseNumber": "" },
  "account": { "password": "", "confirmPassword": "", "acceptPolicy": true }
}
```

Response `201`:
```json
{
  "id": "uuid",
  "employeeId": "PRC-00000",
  "role": null,
  "status": "PENDING_APPROVAL",
  "firstName": "", "lastName": "", "email": ""
}
```

Błędy: `400` (format `employeeId` niezgodny z `PRC-\d{5}`, hasło krótsze niż 8 znaków, hasła niezgodne),
`409` (employeeId już istnieje). Rola jest `null` do momentu zatwierdzenia (poza zakresem tej iteracji).

### GET /auth/accounts

Response `200`: lista `AccountView` (bez pola `password`):
```json
[{ "id": "admin", "employeeId": "PRC-00001", "role": "Administrator systemu", "status": "ACTIVE",
   "firstName": null, "lastName": null, "email": null }]
```

## OTP / SCA (PSD2)

### POST /otp/request

Request:
```json
{ "employeeId": "PRC-30678", "purpose": "UNLOCK_ACCESS" }
```
`purpose`: `UNLOCK_ACCESS` | `RESET_DEVICE`.

Response `200` (kod NIE jest zwracany — widoczny tylko w panelu `/otp`):
```json
{ "otpId": "uuid", "expiresAt": "2026-10-14T18:05:00Z" }
```

TTL kodu: 5 minut.

### POST /otp/verify

Request:
```json
{ "otpId": "uuid", "code": "123456" }
```

Response `200`:
```json
{ "verified": true }
```

Nieznany `otpId`, błędny kod, wygasły kod lub już zużyty kod → `{ "verified": false }` (status `200`,
nie błąd). Pozytywna weryfikacja ustawia `verified`, ale kod jest faktycznie skonsumowany (nie może
odblokować dostępu / zresetować urządzenia drugi raz) dopiero w momencie użycia go w
`/devices/{id}/reset` lub `/ebanking-access/{id}/unlock` — każdy `otpId` można w ten sposób wykorzystać
tylko raz.

## Device registry

### GET /devices/{ownerId}

`{ownerId}` = employeeId (`PRC-xxxxx`) lub customerId/userId e-bankingu (np. `EB-1001`).

Response `200`:
```json
[{ "deviceId": "DEV-0001", "ownerId": "PRC-30678", "model": "iPhone 14",
   "lastSeenAt": "2026-10-14T17:00:00Z", "trusted": true }]
```

### POST /devices/{deviceId}/unbind

`{deviceId}` = identyfikator urządzenia (np. `DEV-0001`). Oznacza urządzenie jako `trusted: false`.

Response: `204 No Content`. Błąd: `404` (nieznane urządzenie).

### POST /devices/{deviceId}/reset

Request:
```json
{ "otpId": "uuid" }
```

Wymaga wcześniej zweryfikowanego OTP o celu `RESET_DEVICE`, inaczej `403`.

Response: `204 No Content`. Błędy: `403` (brak zweryfikowanego OTP), `404` (nieznane urządzenie).

## E-banking access (F5)

### POST /ebanking-access/{userId}/unlock

`{userId}` = identyfikator użytkownika e-bankingu (np. `EB-1001`), encja `EbankingUser` żyje w
`banking_backend` — ten serwis tylko certyfikuje silne uwierzytelnienie.

Request:
```json
{ "otpId": "uuid" }
```

Wymaga wcześniej zweryfikowanego OTP o celu `UNLOCK_ACCESS`, inaczej `403`.

Response `200`:
```json
{ "unlocked": true }
```

## Format błędów

Wszystkie błędy 400/401/403/404/409 mają kształt:
```json
{ "message": "opis błędu" }
```
