# card_processor_mock — stan bieżący

Serwis NestJS (TypeScript ESM), eksponowany na hoście na porcie `10521` (env `PORT`, patrz `deploy/compose.yml`). Wywoływany bezpośrednio z przeglądarki oraz przez `banking_backend`.

Konsoliduje dwa mocki: procesor płatności (autoryzacje kart w konwencji zbliżonej do ISO 8583) i rejestr reklamacji kartowych (chargeback z kodami przyczyn Visa/Mastercard).

Stan przechowywany wyłącznie w pamięci procesu (`Map`/`Set` w singletonach `@Injectable()`) — brak bazy danych, brak ORM. Dane nie przetrwają restartu procesu.

Serwis jest czystym REST API — brak UI/panelu operatorskiego (Thymeleaf jest technologią Spring/Java, niekompatybilną z NestJS; decyzja użytkownika).

## Zaimplementowane endpointy

### Autoryzacje kart (ISO 8583-style)

- `POST /authorizations` — przyjmuje `{ cardId, merchant, amount, mcc, processor? }`. `processor` (`Visa`|`Mastercard`) jest opcjonalny — jeśli nie podany, wyznaczany deterministycznie z hasha `cardId`. Reguła decyzji: `amount <= 5000` → zatwierdzona (`responseCode: "00"`), `amount > 5000` → odrzucona (`responseCode: "05"`). Zwraca pełny rekord `CardAuthorization` rozszerzony o `mcc`, `responseCode`, `processorId`.
- `GET /authorizations?cardId=...` — lista autoryzacji, filtr po `cardId` opcjonalny.
- `GET /authorizations/{id}` — pojedyncza autoryzacja; 404 jeśli nieznana.
- Gdy symulowana jest niedostępność procesora (patrz niżej), `POST /authorizations` zwraca `503` —
  jak również `POST /cards/{cardId}/block`/`unblock` i `POST /chargebacks`/`{id}/advance` (R5): te
  cztery są akcje, które `banking_backend` faktycznie wywołuje dla F4, więc ścieżka 503→incydent ICT
  musi być osiągalna przez nie, nie tylko przez `/authorizations` (którego `banking_backend` nie woła).

### Blokada karty

- `POST /cards/{cardId}/block`, `POST /cards/{cardId}/unblock` — przełączają flagę blokady w pamięci dla danego `cardId`. Serwis przechowuje tylko flagę, nie całą encję `PaymentCard` (ta żyje w `banking_backend`/`banking_web`).
- `GET /cards/{cardId}/block-status` — `{ cardId, blocked }`.

### Chargeback (rejestr reklamacji kartowych)

- `POST /chargebacks` — przyjmuje `{ authorizationId, reasonCode, description }`. `reasonCode` to jeden z kodów Visa/Mastercard: `4855` (towar niedostarczony), `4837` (transakcja nieautoryzowana), `4853` (towar/usługa niezgodna z opisem), `4863` (transakcja sporna — karta obecna, nieuznana przez posiadacza). Waliduje istnienie `authorizationId` (404 jeśli nieznane). Nowy wpis ma status `FILED`.
- `POST /chargebacks/{id}/advance` — przyjmuje `{ status }`. Dozwolone przejścia cyklu: `FILED → UNDER_REVIEW`, `UNDER_REVIEW → RESOLVED_CUSTOMER | RESOLVED_MERCHANT`. Każde inne przejście (w tym pominięcie `UNDER_REVIEW` lub zmiana ze stanu rozstrzygniętego) → `409 Conflict`.
- `GET /chargebacks?authorizationId=...` — lista, filtr opcjonalny.
- `GET /chargebacks/{id}` — pojedynczy wpis; 404 jeśli nieznany.

### Symulacja niedostępności procesora

- `POST /processor/simulate-outage` — `{ enabled: boolean }`, przełącza globalną flagę w pamięci.
- `GET /processor/status` — `{ outageSimulated: boolean }`.
- Służy do testowania przez `banking_backend` ścieżki alternatywnej F4 (procesor niedostępny → rejestracja incydentu ICT w `governance_mock`). Ten serwis **nie** wywołuje `governance_mock` — tylko ułatwia test tej ścieżki. Flaga wpływa na block/unblock/chargeback (nie tylko `/authorizations`, patrz wyżej).

Pełny kontrakt request/response: `docs/card_processor_mock_contract/api.md`.

### Seedowanie startowe

Zmienna `CARD_PROCESSOR_SEED_MOCK_DATA` (domyślnie `true`, patrz `.env.example`/`deploy/compose.yml`) kontroluje, czy przy starcie procesu (w konstruktorach serwisów, odczyt `process.env` — bez nowej zależności typu `@nestjs/config`) zasiewane są dane zgodne z seedem `banking_backend`:

- 4 autoryzacje z identyfikatorami UUID zgodnymi z `card_authorization_ref` w `db/changelog/seed/003-card.sql`: `dfd92a8d-976a-4601-97be-13ab2e09c7b1` (CRD-5521, Empik), `fc936c4c-ffaf-4dd1-9fdd-c5b4af746a98` (CRD-5521, Orlen), `449084f7-77b5-4cab-a790-e680763a6572` (CRD-5530, Amazon EU), `c78bf1a6-6d59-4a54-8de4-f5a3bc862310` (CRD-5522, Biedronka).
- Chargeback `d9a633a4-1223-48cd-b470-217a69610114` na autoryzacji `449084f7-...`, `reasonCode=4837`, status `FILED`.
- Karta `CRD-5522` oznaczona jako zablokowana (zgodnie z `payment_card.status='blocked'` w seedzie backendu).

Ustawienie `CARD_PROCESSOR_SEED_MOCK_DATA=false` startuje serwis z pustym stanem (jak przed R1.5). Stan nadal żyje tylko w pamięci procesu — restart zawsze resetuje do seeda (lub do pustego stanu, zależnie od flagi).

## Walidacja

Walidacja ręczna w kontrolerach (bez `class-validator`/`class-transformer` — nie są zależnościami projektu). Błędy walidacji → `400 Bad Request` z `{ "message": "..." }`. Brak zasobu → `404 Not Found`. Niedozwolone przejście chargeback → `409 Conflict`. Symulowana niedostępność procesora → `503 Service Unavailable`.

## Decyzje projektowe

- Autoryzacja nie blokuje transakcji na karcie oznaczonej jako zablokowana — blokada i autoryzacja to niezależne mechanizmy w tej iteracji (do rozważenia w kolejnej sesji, patrz niżej).
- Brak CORS — serwis jest wywoływany z przeglądarki przez `banking_web`; jeśli w kolejnej iteracji pojawią się problemy z CORS, do rozważenia `app.enableCors()` (bez nowej zależności).

## Do uzupełnienia w innych częściach aplikacji

- Potrzebny trwały storage (baza danych + seeder) w kolejnej iteracji — obecnie cały stan (autoryzacje, blokady, chargebacki, flaga outage) ginie po restarcie procesu.
- Rozważyć w kolejnej iteracji, czy autoryzacja na zablokowanej karcie powinna być automatycznie odrzucana (obecnie blokada i autoryzacja są niezależne).

## CORS

Serwis jest eksponowany na hoście i wołany bezpośrednio z przeglądarki — dodano `app.enableCors()` w `main.ts` (origin konfigurowalny przez `CARD_PROCESSOR_CORS_ALLOWED_ORIGIN`, domyślnie `http://localhost:10500`, metody `GET`/`POST`), analogicznie do `identity_access_mock`/`external_institutions_gateway_mock`.
