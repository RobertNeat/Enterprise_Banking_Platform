# Symulacja niedostępności mocka → incydent ICT (DORA)

Wzorzec: gdy `card_processor_mock` (F4) lub `external_institutions_gateway_mock` (F6/F7) zwraca
`503` (albo nie odpowiada wcale), `banking_backend` **PRZED** zwróceniem błędu do przeglądarki
woła `governance_mock`'s `POST /ict-incidents`, zapisując incydent (`source`, `severity='HIGH'`).
Oryginalny `503` jest zawsze zwracany do frontendu, nawet jeśli samo zgłoszenie incydentu też się
nie powiedzie (podwójna awaria nie maskuje pierwotnego błędu).

`banking_web` **nie ma przycisku do włączenia symulacji** (strona NestJS pod `/` jest statycznym
placeholderem) — trzeba wywołać endpoint REST ręcznie.

## Scenariusz 1 — niedostępność `card_processor_mock` → incydent ICT przy blokadzie karty

1. Włącz symulację awarii procesora (PowerShell, osobne okno/terminal — nie przez agenta):

   ```powershell
   Invoke-RestMethod -Uri http://localhost:10521/processor/simulate-outage -Method Post `
     -ContentType 'application/json' -Body '{"enabled": true}'
   ```

2. W `banking_web` (`contact`/`admin`), na `/cards`, spróbuj zablokować/odblokować dowolną aktywną
   kartę (np. `CRD-5521`) albo zgłoś chargeback.

**Oczekiwany wynik**: akcja w UI zwraca błąd `503` ("Procesor niedostępny"). Na `/dashboard`,
karta "Alerty ICT" pokazuje nowy incydent `source='CARD_PROCESSOR'`, `severity='HIGH'`,
`status='OPEN'`.

3. Wyłącz symulację po teście:

   ```powershell
   Invoke-RestMethod -Uri http://localhost:10521/processor/simulate-outage -Method Post `
     -ContentType 'application/json' -Body '{"enabled": false}'
   ```

   (Można też sprawdzić aktualny stan: `Invoke-RestMethod -Uri http://localhost:10521/processor/status`.)

**Uwaga**: flaga dotyczy RÓWNIEŻ `POST /authorizations` — ale `banking_web` nie ma formularza
tworzenia autoryzacji (decyzja F4: to transakcja klienta, nie akcja pracownika), więc w praktyce
jedyne ścieżki do przetestowania z UI to blokada/odblokowanie/chargeback.

## Scenariusz 2 — niedostępność `external_institutions_gateway_mock` → incydent ICT przy rejestracji zajęcia

`external_institutions_gateway_mock` nie ma przełącznika REST do symulacji awarii — jedyny sposób
to zatrzymanie jego kontenera. **Wykonaj to sam** (zgodnie z zasadą projektu, że kontenerami
zarządza użytkownik, nie agent):

```powershell
docker stop external-institutions-gateway-mock
```

1. Przejdź do `/enforcement`, spróbuj zarejestrować nowe zajęcie (`enforcement_f6.md` scenariusz
   1) albo wykonać transfer na zajęciu z zatwierdzonym approvalem.

**Oczekiwany wynik**: `503 Service Unavailable` (connection refused tłumaczone na
`RegulatoryGatewayUnavailableException`), nowy incydent ICT `source='EXTERNAL_INSTITUTIONS_GATEWAY'`
na pulpicie. Ten sam mechanizm obsługuje `/reporting` (F7) — próba "Złóż"/"Sprawdź status" z
zatrzymaną bramą da ten sam wzorzec.

2. Podnieś kontener z powrotem po teście:

   ```powershell
   docker start external-institutions-gateway-mock
   ```

## Weryfikacja incydentu bez UI (opcjonalnie)

Historia audytowa i rejestr ICT żyją w `governance_mock`, który nie ma portu na hoście — nie da
się ich podglądnąć bezpośrednio przez przeglądarkę poza kartą "Alerty ICT" na pulpicie
`banking_web`. Pełna lista (nie tylko TOP 5 `OPEN`) jest dostępna tylko przez wywołanie
`GET http://localhost:10520/dashboard/ict-incidents` (przez `banking_backend`, wymaga JWT) albo
pośrednio przez `GET /audit-events` w kodzie — nie ma do tego osobnego ekranu w `banking_web`.
