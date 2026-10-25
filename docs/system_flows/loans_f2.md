# F2 — wniosek kredytowy: scoring → zatwierdzenie → decyzja → wypłata

Konto: `loans` / `PRC-10234` / `Loans12345` (albo `admin`, ale `admin` NIE widzi przycisku "Nowy" —
`seniorityLevel=3` blokuje inicjowanie maker-checker, zob. `README.md`).

## Scenariusz 1 — nowy wniosek: złożenie → scoring → wysłanie do zatwierdzenia

1. Zaloguj się jako `loans`, przejdź do `/loans`, kliknij "Nowy".
2. Krok 1: `customerId` = `CUST-002` (Katarzyna Zając), `segment=retail`.
3. Krok 2: `product=cash`, `requestedAmount=15000`.
4. Krok 3 (odczyt): backend woła `POST /loans` — scoring jest **deterministyczny po
   `applicantId`**, wyświetli się `scoreResult` (300-850) i `dsti` (rosnące z kwotą). Nie trzeba
   zgadywać dokładnej wartości — sprawdź tylko, że `scoreResult` leży w `300-850` i `dsti` w
   `0.00-0.99`.
5. Krok 4: zatwierdź podsumowanie — woła `POST /loans/{id}/submit-for-approval`.

**Oczekiwany wynik**: nowy wniosek pojawia się na liście `/loans` ze `stage=decision`. W
`/approvals` (konto `compliance`/`admin`) pojawia się nowy wpis `PENDING`, `requestedBy=PRC-10234`.

## Scenariusz 2 — awans etapu po zatwierdzeniu (wymaga najpierw scenariusza z `approvals_maker_checker.md`)

Wymaga wniosku ze `stage='decision'` i powiązanym approvalem `APPROVED` — użyj wniosku z seeda
`LN-80255` (`CUST-003`, `stage='decision'`, approval `22222222-...-002` **seedowany jako
`PENDING`** — najpierw zdecyduj go w `/approvals`, zob. `approvals_maker_checker.md` scenariusz 2)
albo wniosku stworzonego w scenariuszu 1 powyżej po jego zatwierdzeniu.

1. Zaloguj się jako `loans`, otwórz `/loans/LN-80255` (po zatwierdzeniu approvala).
2. Kliknij "Awansuj etap".

**Oczekiwany wynik**: `stage→'contract'`, backend generuje harmonogram rat (annuity,
`interestRate=0.0790`), widoczny w sekcji harmonogramu na stronie szczegółów. Wpis "Historia"
pokazuje `LOAN_STAGE_CHANGED`.

## Scenariusz 3 — wypłata po kontrakcie

1. Na wniosku ze `stage='contract'` (np. `LN-80261`, `CUST-004`, `overdraft`, już `schedule` —
   użyj wniosku z poprzedniego scenariusza po awansie do `contract`) kliknij "Wypłać".

**Oczekiwany wynik**: `stage→'disbursement'`.

## Scenariusz negatywny 1 — awans etapu bez zatwierdzonego approvala

1. Otwórz wniosek wciąż `stage='decision'` z approvalem `PENDING` (jeśli nie zdecydowano w
   `approvals_maker_checker.md`) i kliknij "Awansuj etap".

**Oczekiwany wynik**: `409 Conflict` — approval nie jest `APPROVED`.

## Scenariusz negatywny 2 — odrzucony approval wraca wniosek do `application`

1. W `/approvals` odrzuć ("Odrzuć") approval powiązany z jakimś wnioskiem `stage='decision'`.
2. Wróć na `/loans/{id}` tego wniosku i kliknij "Awansuj etap".

**Oczekiwany wynik**: frontend pokazuje toast ostrzegawczy (nie błąd) `loans.rejected`, wniosek
wraca do `stage='application'` — widoczny przycisk "Złóż do zatwierdzenia" (ponowne wysłanie), nie
"Awansuj etap".

## Scenariusz negatywny 3 — niedostępność `credit_risk_mock` przy tworzeniu wniosku

Patrz `outage_and_ict_incidents.md` — ten scenariusz dotyczy `card_processor_mock`/
`external_institutions_gateway_mock` (mają przełącznik `simulate-outage`); `credit_risk_mock` nie
ma takiego przełącznika REST, jedyny sposób zasymulowania 503 to zatrzymanie jego kontenera
(`docker stop <nazwa_kontenera_credit_risk_mock>` — wykonaj to sam, zgodnie z zasadą projektu o
niepodnoszeniu/niezatrzymywaniu kontenerów przez agenta). Oczekiwany wynik: `POST /loans` zwraca
`503`, **żaden wiersz `loan_application` nie zostaje zapisany** (scoring jest wołany przed
zapisem).
