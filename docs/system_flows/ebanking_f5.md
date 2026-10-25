# F5 — e-banking: odblokowanie dostępu i reset urządzenia (OTP/SCA)

Konto: `loans` / `PRC-10234` lub `contact` / `PRC-40890` (obie mają permission `ebanking`).

**Kod OTP nigdy nie pojawia się w `banking_web`** — otwórz drugą kartę na
`http://localhost:10525/otp` (panel operatorski `identity_access_mock`), żeby odczytać kod
wygenerowany dla danego `otpId`.

## Dane z seeda

| Użytkownik | Klient | `accessStatus` |
| --- | --- | --- |
| `EB-1001` | CUST-001 | `locked` |
| `EB-1002` | CUST-002 | `active` |
| `EB-1004` | CUST-004 | `pendingReset` |
| `EB-1005` | CUST-006 | `locked` |

## Scenariusz 1 — odblokowanie dostępu (`EB-1001`, `locked`)

1. Przejdź do `/ebanking`, znajdź `EB-1001`, kliknij "Odblokuj".
2. Krok 1 (Wyślij kod) — zatwierdź. Zapamiętaj `otpId` z odpowiedzi (widoczny na ekranie dialogu).
3. Przejdź na kartę `http://localhost:10525/otp`, znajdź wygenerowany kod dla tego `otpId`.
4. Wróć do `banking_web`, krok 2: wpisz odczytany kod w pole `otpCode`, kliknij "Potwierdź".

**Oczekiwany wynik**: `accessStatus→'active'`.

## Scenariusz 2 — reset urządzenia (`EB-1004`, `pendingReset`)

1. Na `EB-1004` kliknij "Resetuj urządzenie".
2. Backend pobiera listę urządzeń (`GET /ebanking/EB-1004/devices`) — jeśli jest więcej niż jedno,
   wybierz jedno z listy (`deviceId`); jeśli jedno, zostanie wybrane automatycznie.
3. Krok 1 (Wyślij kod) → zapisz `otpId`.
4. Odczytaj kod z `http://localhost:10525/otp`.
5. Krok 2: wpisz kod, "Potwierdź".

**Oczekiwany wynik**: urządzenie zresetowane, `accessStatus→'active'` (zakładając że reset kończy
flow dostępu — zweryfikuj rzeczywisty status po akcji na liście).

## Scenariusz negatywny 1 — odblokowanie konta już `active`

1. Na `EB-1002` (`active`) spróbuj kliknąć "Odblokuj" (przycisk powinien być zablokowany/skryty po
   stanie; jeśli dostępny, backend i tak odrzuci).

**Oczekiwany wynik**: `409 Conflict` — unlock wymaga `accessStatus` w `{'locked','pendingReset'}`.

## Scenariusz negatywny 2 — niepoprawny kod OTP

1. Powtórz scenariusz 1 na `EB-1005`, ale w kroku 2 wpisz losowy błędny kod (np. `000000`).

**Oczekiwany wynik**: `identity_access_mock` zwraca `200 {"verified": false}` (nie kod błędu) —
`banking_backend` tłumaczy to na `403 Forbidden` lokalnie (`OtpNotVerifiedException`). Dialog
pokazuje błąd, `accessStatus` pozostaje niezmieniony, OTP nie jest skonsumowane (można spróbować
ponownie poprawnym kodem).

## Scenariusz negatywny 3 — reset cudzego urządzenia

1. Spróbuj wywołać reset z `deviceId` należącym do innego użytkownika niż ten z path (wymaga
   bezpośredniego wywołania API, bo UI zawsze pobiera listę dla właściwego `userId`).

**Oczekiwany wynik**: `404 Not Found` — backend weryfikuje, że `deviceId` jest na liście
`GET /ebanking/{userId}/devices` PRZED jakąkolwiek akcją.
