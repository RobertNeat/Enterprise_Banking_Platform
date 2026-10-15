# banking_backend

Moduł Spring Boot (Spring Boot 4.1, Java 25, Maven) — jedyny moduł zapisujący do PostgreSQL.
Zawiera warstwę danych (encje JPA, repozytoria Spring Data, schemat bazy zarządzany przez Liquibase,
opcjonalny seed danymi mockowymi zgodnymi z `apps/banking_web/src/app/features/**/*.data.ts`) oraz
warstwę REST (kontrolery GET-only + DTO, konsumowaną na żywo przez `banking_web` — zob. sekcja
"Warstwa REST" niżej).

Szczegółowy opis kolumn, typów i ograniczeń: `docs/banking_backend/data_model.md`.

## Encje i tabele

Pakiet bazowy: `robert_neat.banking_backend`. Każda encja ma repozytorium `<Encja>Repository`
(`JpaRepository`) w tym samym pakiecie.

| pakiet          | encje (tabela)                                                                                                         |
| --------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `customer`      | `Customer` (`customer`), `Employee` (`employee`, self-FK `supervisor_id`), `BankAccount` (`bank_account`)              |
| `loan`          | `LoanApplication` (`loan_application`), `LoanStageTransition` (`loan_stage_transition`), `Installment` (`installment`) |
| `card`          | `PaymentCard` (`payment_card`), `CardAuthorizationRef` (`card_authorization_ref`), `ChargebackRef` (`chargeback_ref`)  |
| `risk`          | `CollateralEntry` (`collateral_entry`)                                                                                 |
| `ebanking`      | `EbankingUser` (`ebanking_user`)                                                                                       |
| `cases`         | `CustomerCase` (`customer_case`)                                                                                       |
| `contactcenter` | `ServiceDisposition` (`service_disposition`), `ContactHistoryEntry` (`contact_history_entry`)                          |
| `collections`   | `CollectionsCase` (`collections_case`), `CollectionsAction` (`collections_action`)                                     |
| `enforcement`   | `EnforcementCase` (`enforcement_case`)                                                                                 |
| `reporting`     | `RegulatoryReport` (`regulatory_report`), `ReportSubmission` (`report_submission`)                                     |
| `dashboard`     | `DashboardKpiSnapshot` (`dashboard_kpi_snapshot`), `PortfolioTrendPoint` (`portfolio_trend_point`)                     |

Konwencje:

- Statusy i wartości słownikowe to `String` (nie enumy Java) z `CHECK` w bazie — literały identyczne
  z frontendem.
- Kwoty `BigDecimal` (`NUMERIC(19,2)`), daty `LocalDate` (`DATE`), znaczniki czasu `Instant`
  (`TIMESTAMP WITH TIME ZONE`).
- Klucze biznesowe (`LN-80231`, `CUST-001`, `PRC-00001`) jako PK `String`; tabele historyczne
  (`loan_stage_transition`, `installment`, `collections_action`, `report_submission`,
  `dashboard_kpi_snapshot`, `portfolio_trend_point`) mają PK `Long` z `IDENTITY`.
- Relacje `@ManyToOne(fetch = LAZY)`; brak strony `@OneToMany`.
- Hibernate działa z `spring.jpa.hibernate.ddl-auto=validate` — schemat tworzy wyłącznie Liquibase,
  a Hibernate tylko weryfikuje zgodność encji ze schematem przy starcie.

## Liquibase

- Changelog główny: `src/main/resources/db/changelog/db.changelog-master.yaml` — jawna lista
  `include` w kolejności zależności FK: najpierw `schema/*.sql`, potem `seed/*.sql`.
- Format: Liquibase formatted SQL, autor `robert_neat`, id changesetu `<tabela>-NNN`
  (seed: `seed-<tabela>-NNN`).
- SQL jest przenośny między PostgreSQL 17 a H2 (`MODE=PostgreSQL`, używany w testach).

Szerokości kolumn wymagające uzasadnienia:

- `regulatory_report.id` (i FK `report_submission.regulatory_report_id`) to `VARCHAR(15)` — identyfikator `RPT-FATCA-009` ma 13 znaków.
- `regulatory_report.period` to `VARCHAR(20)` — wartości typu `Wrzesień 2026` wymagają zapasu.

## Seedowanie danymi mockowymi

Zmienna środowiskowa `BANKING_SEED_MOCK_DATA` (`true`/`false`, domyślnie `false`) jest mapowana na
właściwość `banking.seed-mock-data`. Każdy changeset w `seed/` ma w nagłówku `context:mock-data`.

`MockDataContextConfig` rejestruje `BeanPostProcessor`, który przed inicjalizacją beana
`SpringLiquibase` ustawia kontekst:

- `true` → `mock-data` — wykonywany jest schemat i seed,
- `false`/brak → `schema-only` — kontekst niepasujący do żadnego seeda, wykonywany jest tylko schemat.

Proste `spring.liquibase.contexts=${...}` nie wystarcza: Liquibase traktuje pustą/nieustawioną listę
kontekstów jako „uruchom wszystko”, więc przy wyłączonym seedzie i tak załadowałby dane. Dlatego przy
wyłączonym seedzie ustawiana jest jawna, niepasująca wartość.

Zawartość seeda: 12 klientów (unikalne nazwy z danych mockowych frontendu), 9 pracowników
(6 kont `DEMO_ACCOUNTS` z `login.page.ts` bez haseł + 3 opiekunów spraw z `cases.data.ts`), po jednym
rachunku na klienta, 13 wniosków kredytowych (8 z `loans.data.ts` + 5 zaślepek dla pożyczek
występujących tylko w `collections.data.ts`), przykładowe przejścia etapów, częściowe harmonogramy
rat (12 rat dla `LN-80231` i `LN-80203`), karty i autoryzacje, jeden przykładowy chargeback, zabezpieczenia,
użytkownicy e-bankingu, sprawy klientów, dyspozycje i historia kontaktu, sprawy i akcje windykacyjne,
zajęcia komornicze, raporty regulacyjne z dwoma zgłoszeniami do bramy, migawka KPI pulpitu oraz
9 punktów trendu portfela.

Testy `SeedMockDataEnabledTest` i `SeedMockDataDisabledTest` weryfikują oba warianty (liczności
wszystkich 21 tabel), ustawiając `BANKING_SEED_MOCK_DATA` na osobnych bazach H2.

## Dodawanie nowej tabeli

1. Changeset schematu w `src/main/resources/db/changelog/schema/` (nowy plik lub nowy changeset
   `<tabela>-NNN`; istniejących, już wdrożonych changesetów nie modyfikuje się) + wpis `include`
   w changelogu głównym we właściwej kolejności FK.
2. Encja JPA zgodna z kolumnami (walidowana przez `ddl-auto=validate`) i repozytorium w pakiecie domeny.
3. Opcjonalnie seed w `seed/` z nagłówkiem `--changeset robert_neat:seed-<tabela>-NNN context:mock-data`
   — bez `context:mock-data` changeset wykona się zawsze.
4. Uzupełnienie liczności w `SeedMockDataEnabledTest.EXPECTED_COUNTS`.

## Uwagi operacyjne

Jeśli deweloperski wolumen PostgreSQL zawiera tabele utworzone wcześniej przez Hibernate
(`ddl-auto=update`), należy go usunąć i utworzyć ponownie przed pierwszym uruchomieniem — changesety
`CREATE TABLE` Liquibase kolidują z istniejącymi tabelami.

## Weryfikacja integralności modelu danych (zidentyfikowane problemy)

Weryfikacja objęła: 21 tabel (schemat Liquibase ↔ encje JPA ↔ `docs/banking_backend/data_model.md`),
integralność referencyjną i statusową 11 plików seed, zgodność z danymi źródłowymi
`apps/banking_web/src/app/features/**/*.data.ts` i `login.page.ts`, zgodność typów referencji z
kontraktami 5 serwisów mockowych oraz zgodność liczności w `SeedMockDataEnabledTest`/
`SeedMockDataDisabledTest` z rzeczywistą liczbą wierszy w seedach.

### Potwierdzone jako poprawne (bez problemu)

- Wszystkie kolumny/typy/`NOT NULL`/`CHECK` w 11 plikach `schema/*.sql` są zgodne z odpowiadającymi
  polami `@Entity` (długości `VARCHAR`, `NUMERIC(precyzja, skala)`, `@Column(name=...)`) — brak
  rozjazdów na poziomie silnika SQL.
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` jest użyty konsekwentnie na wszystkich 6
  tabelach z PK `BIGINT GENERATED BY DEFAULT AS IDENTITY` (`loan_stage_transition`, `installment`,
  `collections_action`, `report_submission`, `dashboard_kpi_snapshot`, `portfolio_trend_point`), a
  żaden seed nie wstawia własnej wartości `id` do tych kolumn — sekwencja IDENTITY nie zostanie
  zdublowana przy pierwszym realnym zapisie z aplikacji.
- Wszystkie znaczniki czasu `TIMESTAMP WITH TIME ZONE` mapowane są na `java.time.Instant` (nie
  `LocalDateTime`) — brak utraty strefy czasowej.
- `spring.jpa.hibernate.ddl-auto=validate` w `application.properties` i `test/resources/application.properties`,
  `MODE=PostgreSQL` w URL H2 testowego — testy faktycznie weryfikują zgodność ze składnią PostgreSQL,
  nie tylko z dialektem H2.
- Literały `CHECK` w schemacie odpowiadają 1:1 unijnym typom TS we wszystkich sprawdzonych
  `*.model.ts` (`LoanStage` 8 wartości, `CollectionsStage` 6 wartości, `CaseStatus`, `CardStatus`,
  `CardNetwork`, `AccessStatus`, `ReportStatus`, `EnforcementStatus`, `CollateralType`, `Ifrs9Stage`).
- Brak osieroconych FK i duplikatów PK w żadnym z 21 seedów. Wszystkie kolumny referencyjne do
  mocków (`bureau_inquiry_id`, `approval_id`, `governance_audit_event_id`, `gateway_enforcement_id`,
  `gateway_submission_id`, `card_authorization_ref_id`/`card_authorization_ref.id`,
  `chargeback_ref.id`) to `VARCHAR(36)` — wystarczające dla UUID (36 znaków), zgodnie z kontraktami
  `credit_risk_mock`, `governance_mock`, `external_institutions_gateway_mock`, `card_processor_mock`.
- Mapowania `customer_id`/`holder`/`applicant` → nazwa klienta sprawdzone krzyżowo dla `loan_application`,
  `payment_card`, `collateral_entry`, `ebanking_user`, `customer_case`, `enforcement_case`,
  `collections_case` (łącznie z 5 „zaślepkami" pożyczek referencjonowanych tylko z
  `collections.data.ts`) — wszystkie rozwiązują się do poprawnego klienta i segmentu
  (`retail`/`business` vs forma prawna nazwy).
- Zaślepki `loan_application` dla `LN-80180/80175/80162/80120/80099` (pożyczki występujące tylko w
  `collections.data.ts`, nie w `loans.data.ts`) mają sensowne wartości: `stage='collections'`,
  `approved_amount` wyższy niż powiązane `collections_case.outstanding_amount` (częściowa spłata —
  logiczne), `segment`/`product` zgodne z przypisanym klientem.
- `SeedMockDataEnabledTest.EXPECTED_COUNTS` zgadza się dokładnie z rzeczywistą liczbą wierszy we
  wszystkich 21 tabelach (suma 138 wierszy = `Liquibase: Rows affected: 138` z realnego przebiegu).
  `SeedMockDataDisabledTest` poprawnie asercjonuje `count=0` dla wszystkich 21 tabel i mechanizm
  przełączania kontekstu Liquibase (`mock-data` vs `schema-only` w `MockDataContextConfig`) faktycznie
  gwarantuje ten efekt (żadny seed nie ma wspólnego, "dopasowującego się domyślnie" kontekstu).

### Problemy naprawione automatycznie

1. **`escalation_source='AUTO_SLA'` niemożliwe czasowo** (`seed/006-cases.sql`, sprawy `CS-10231` i
   `CS-10233`). Obie sprawy miały `escalated_at` wcześniejszy niż `sla_due_at` o 18h i 6h — zgodnie z
   kontraktem `governance_mock` (`POST /cases/{caseId}/sla-check`) auto-eskalacja następuje tylko gdy
   `slaRemainingHours <= 0`. Dane te nie pochodzą z `cases.data.ts` (model `CustomerCase` we frontendzie
   nie ma pola `escalationSource`), więc nie jest to odziedziczona niezgodność z frontendem — poprawiono
   `escalation_source` na `'MANUAL'` dla obu wierszy (eskalacja ręczna przed terminem SLA jest sensowna
   biznesowo; `governance_audit_event_id` pozostaje `NULL`, co dla `MANUAL` jest poprawne).
2. **`bank_account.blocked_amount` niezgodny z własną definicją w `data_model.md`**
   (`seed/001-customer.sql`, konta `ACC-0004` i `ACC-0005`). Definicja: „suma aktywnych zajęć/blokad”.
   `ACC-0004` miało `blocked_amount=4200.00` przy powiązanym `EN-2198` ze statusem `settled` (rozliczone,
   nieaktywne); `ACC-0005` miało `blocked_amount=128000.00` przy `EN-2190` ze statusem `closed`
   (zamknięte). Pola te nie pochodzą z `enforcement.data.ts` (frontend nie ma modelu `bank_account`) —
   poprawiono obie wartości na `0.00`.
3. **Zaokrąglenie harmonogramu `LN-80203`** (`seed/002-loan.sql`, `installment_no=12`). Suma 12 rat
   kapitałowych po `1833.33` dawała `21999.96` zamiast `22000.00` (`approved_amount`). Poprawiono
   ostatnią ratę na `1833.37` (typowa "rata wyrównawcza"), żeby suma dokładnie odtwarzała kwotę kredytu.
4. **`report_submission` dla `RPT-NBP-014` sprzeczny wewnętrznie** (`seed/010-reporting.sql`).
   `regulatory_report.status='submitted'` (zgodne 1:1 z `reporting.data.ts`), ale powiązany
   `report_submission.gateway_status` był `'ACCEPTED'` z wypełnionym `resolved_at` — brama już by
   zaakceptowała zgłoszenie, co jest niezgodne ze statusem raportu „w toku”. To pole nie pochodzi z
   `reporting.data.ts` (frontend nie ma modelu `report_submission`) — poprawiono na
   `gateway_status='PENDING'`, `resolved_at=NULL`.
5. **`enforcement_case.transferred_amount` dla `EN-2205`** (`seed/009-enforcement.sql`). Przy
   `status='registered'` (zajęcie zarejestrowane, transfer jeszcze nie nastąpił) kolumna miała
   `0.00` zamiast `NULL`. Kontrakt `external_institutions_gateway_mock` zwraca `transferredAmount: null`
   dla nowo zarejestrowanych zajęć; `data_model.md` opisuje pole jako nullable „do rozliczenia”.
   Frontend (`enforcement.data.ts`) ma `transferredAmount: number` bez `null` tylko z powodu typu TS —
   backend może i powinien używać poprawnej semantyki `NULL`. Poprawiono na `NULL`.

Po poprawkach uruchomiono `./mvnw.cmd test` w `apps/banking_backend` — **BUILD SUCCESS, 5/5 testów
przechodzi**, Liquibase nadal zgłasza 138 wierszy (liczba wierszy niezmieniona, tylko wartości).

**Uwaga operacyjna**: powyższe poprawki (w tym poprawki z sekcji "Rozbieżności frontend vs kontrakt
mocka" poniżej) zmieniają treść już zaimplementowanych changesetów Liquibase (`001-customer.sql`,
`002-loan.sql`, `003-card.sql`, `004-risk.sql`, `006-cases.sql`, `007-contactcenter.sql`,
`009-enforcement.sql`, `010-reporting.sql`), co zmienia ich checksumę. Na środowisku lokalnym z
PostgreSQL, na którym te changesety zostały już wcześniej zastosowane, kolejny `./mvnw.cmd` zgłosi
błąd walidacji checksumy — należy użyć `liquibase clearCheckSums` albo odtworzyć wolumen bazy od zera.
Testy jednostkowe (H2, baza zawsze od zera) nie są tym dotknięte.

### Rozbieżności frontend vs kontrakt mocka — rozstrzygnięte decyzją użytkownika

Poniższe wartości były zgodne 1:1 z odpowiadającym plikiem `*.data.ts`, ale sprzeczne z deterministyczną
regułą lub zakresem zdefiniowanym w kontrakcie serwisu mockowego. Użytkownik podjął decyzję dla każdego
punktu; status poniżej odzwierciedla wynik.

6. **NAPRAWIONE**: `loan_application.score_result` vs skala `credit_risk_mock`. Zmieniono kontrakt
   mocka (nie dane frontendu/seeda): `credit_risk_mock` generuje teraz `scoreResult` w skali FICO/BIK
   (300-850) — `CreditBureauService.deterministicScore` przeliczony na
   `300 + Math.floorMod(applicantId.hashCode(), 551)` (deterministyczny, jak dotychczas). Zaktualizowano
   `docs/credit_risk_mock_contract/api.md`, `docs/services/credit_risk_mock.md` i test
   `CreditBureauServiceTest`. Seed `banking_backend` (wartości 589-782) nie wymagał zmian — już leży w
   nowym zakresie.
7. **NAPRAWIONE**: `card_authorization_ref` `AUTH-901..904` i `chargeback_ref.id='CB-0001'` zamienione
   na realne UUID v4 w `seed/003-card.sql` (`dfd92a8d-...`, `fc936c4c-...`, `449084f7-...`,
   `c78bf1a6-...` dla autoryzacji; `d9a633a4-...` dla chargebacku), zgodnie z kontraktem
   `card_processor_mock` (`id` jako `"string (UUID)"`). FK `chargeback_ref.card_authorization_ref_id`
   zaktualizowany do nowego UUID `AUTH-903`. Ten seed teraz świadomie odbiega od krótkich ID w
   `cards.data.ts` (frontend nie został zmieniony, poza zakresem tej sesji).
8. **NAPRAWIONE**: `AUTH-903` (`CRD-5530`, Amazon EU, `amount=1340.20`) zmieniony na `approved=TRUE` —
   zgodne z regułą kontraktu `card_processor_mock` (`amount <= 5000 → approved: true`). Tabela
   `card_authorization_ref` nie ma kolumny `response_code` (potwierdzone w schemacie), więc nie ma
   dodatkowego pola do skorygowania. Ten wiersz teraz odbiega od `cards.data.ts:87` (`approved: false`)
   — świadoma decyzja zgodności z kontraktem mocka.
9. **NAPRAWIONE**: `CB-0001` (teraz `d9a633a4-...`) ma `reason_code='4837'` ("transakcja
   nieautoryzowana, karta nieobecna") zamiast `'4863'` — zgodne kontekstowo z zakupem online (Amazon
   EU, karta nieobecna) i z `AUTH-903` teraz zatwierdzonym (`approved=TRUE`, punkt 8).
10. **NAPRAWIONE**: `collateral_entry.provision` przeliczony według reguły IFRS9/ECL kontraktu
    `credit_risk_mock` (ekspozycja = `valuation × ltv`, rezerwa = 1%/5%/25% dla stage1/2/3) w
    `seed/004-risk.sql`: `COL-3301`=4588.00, `COL-3302`=110000.00, `COL-3303`=7150.00,
    `COL-3304`=104550.00, `COL-3305`=22420.00 (przeliczenia zweryfikowane: `COL-3301`
    620000×0.74×1%=4588, `COL-3304` 410000×1.02×25%=104550 — zgodne z raportem weryfikacyjnym).
    `ecl_classified_at` ustawione na `2026-10-05 09:00:00+02:00` dla wszystkich wierszy (zamiast
    `NULL`). Seed teraz odbiega od `risk.data.ts` (świadoma decyzja zgodności z kontraktem mocka).
11. **ODSTĄPIONO OD PLANU**: `RPT-FATCA-009` `gateway_status='ACCEPTED'` pozostaje niezmienione.
    Zweryfikowano wykonalność opcji z zadania: `report_submission` NIE MA osobnej kolumny
    przechowującej wartość `reportId` użytą w wywołaniu `POST /regulatory-gateway/submit` — istnieje
    tylko `regulatory_report_id` (FK = `regulatory_report.id`) i `gateway_submission_id` (to
    `submissionId` z ODPOWIEDZI bramy, inna wartość, generowana przez mock). `reportId` przekazywany do
    bramy to bezpośrednio `regulatory_report.id='RPT-FATCA-009'` — potwierdzono też w kodzie
    `external_institutions_gateway_mock` (`RegulatoryGatewayService.resolve`: `Math.floorMod(reportId.hashCode(), 10) == 0`
    hashuje dokładnie ten string). Zmiana tego ID złamałaby FK i referencje w `reporting.data.ts`
    (zmiana PK, poza zakresem; `dashboard.data.ts` nie referencjonuje `RPT-FATCA-009`, zweryfikowano).
    `floorMod("RPT-FATCA-009".hashCode(), 10)` policzone
    emulacją algorytmu `String.hashCode()` = `0` — potwierdzone, że kontrakt wymagałby `REJECTED`.
    Pozostaje jako znana rozbieżność demo bez prostego rozwiązania bez zmiany PK.
12. **NAPRAWIONE (częściowo)**: `service_disposition` ↔ `customer_case`. `DSP-771`
    (`cards.limitDaily`, `occurred_at='2026-10-05'`) przelinkowany z `CS-10231` (klient `CUST-009`) na
    `CS-10229` — potwierdzono, że dyspozycja faktycznie należy do `CUST-001` (Jan Wiśniewski): zgadza
    się z czatem (`INITIAL_CHAT`, prośba o limit karty) i `CH-4401` (`contact_history_entry`,
    `occurred_at='2026-10-05'`, "Prośba o podniesienie limitu karty") powiązanymi z
    `CUSTOMER_PROFILE`/`CUST-001` w `contact-center.data.ts`. `CS-10229` jest jedyną sprawą `CUST-001`
    w seedzie (brak dedykowanej sprawy o limicie karty) — najbliższe dostępne dopasowanie.
    `DSP-765` (`ebanking.unlock`, `occurred_at='2026-10-02'`, `status='resolved'`) **pozostawiony przy
    `CS-10219` bez zmian** — po weryfikacji ustalono, że dyspozycja faktycznie dotyczy `CUST-002`
    (Katarzyna Zając), NIE `CUST-001`: `CS-10219` to sprawa `CUST-002` (`resolved`,
    `statutory_deadline='2026-10-02'`, dokładna zgodność dat), `ebanking_user` `EB-1002` (`CUST-002`)
    jest `active` z `last_login='2026-10-05'` (zgodne z odblokowaniem 10-02), a `CUST-001` nie ma
    żadnego wpisu `contact_history_entry` z 10-02 (`EB-1001` pozostaje `locked`, co byłoby sprzeczne z
    "resolved" odblokowaniem). Obecność `DSP-765` na tym samym ekranie co `CUSTOMER_PROFILE`
    (`CUST-001`) w `contact-center.data.ts` NIE oznacza, że dyspozycja dotyczy tego klienta — podobnie
    jak `DSP-758` (patrz niżej) odnosi się do innego klienta. `CS-10219` ("Reklamacja transakcji
    kartowej") jest jedyną sprawą `CUST-002` w seedzie i nie jest tematycznie e-bankingowa — **pełna
    naprawa wymaga dodania dedykowanej sprawy e-bankingowej dla `CUST-002` w przyszłej iteracji**
    (zmiana liczności, poza zakresem tej sesji). `DSP-771` teraz odbiega od `contact-center.data.ts`
    (`linkedCase: 'CS-10231'`), co jest świadomą decyzją zgodności tematycznej wewnątrz
    `banking_backend`. `DSP-758` (`loans.decisionMove` → `CS-10225`, klient `CUST-003`) nie był w
    zakresie tej sesji — pozostaje jako znana obserwacja do przyszłej iteracji.
13. **`card_processor_mock` nie zna ID z `banking_backend`** — to generyczny, bezstanowy mock bez
    własnego seeda, więc `GET /authorizations/{id}` i `GET /chargebacks/{id}` dla ID z seeda (teraz
    realne UUID, patrz punkt 7) zwrócą `404 Not Found` w runtime, mimo że `banking_backend` traktuje je
    jako referencje do „source of truth” w tym serwisie (`data_model.md`). **Potwierdzono decyzję
    użytkownika**: referencje w `banking_backend` pozostają ilustracyjne/offline w tej iteracji — nie
    dodano seeda w `card_processor_mock` (poza zakresem tej sesji, dotyka tylko `apps/credit_risk_mock`
    i `apps/banking_backend`).

### Pozostałe uwagi (bez naprawy — kwestia doprecyzowania dokumentacji lub architektury, poza zakresem tej sesji)

14. **`data_model.md`, sekcja "Raportowanie regulacyjne"**: zdanie „`banking_backend` trzyma tylko że
    eksport nastąpił, poprzez `report_submission` z `recipient='FATCA/CRS'`” jest nieprecyzyjne —
    tabela `report_submission` NIE MA kolumny `recipient` (jest tylko na `regulatory_report`); odczyt
    wymaga JOIN `report_submission` → `regulatory_report`. Dodatkowo kontrakt `POST /fatca-crs/export`
    nie zwraca `submissionId` — wpis w `report_submission` dla FATCA musi logicznie pochodzić z
    `POST /regulatory-gateway/submit`, a nie z `/fatca-crs/export`. Wymaga decyzji użytkownika, czy
    poprawić wyłącznie opis w `data_model.md` (dozwolone w ramach tej sesji tylko dla literówek — to
    jest doprecyzowanie, nie literówka, więc pozostawiono do osobnej decyzji).
15. **`loan_stage_transition.from_stage NOT NULL`** nie pozwala zapisać przejścia „z niczego” do
    pierwszego etapu `application` — pierwsze realne przejście w cyklu życia pożyczki nie jest
    reprezentowalne. Wymaga decyzji architektonicznej (np. `from_stage` nullable albo sentinel
    `'none'`), poza zakresem drobnych poprawek.
16. **Niekompletny łańcuch `loan_stage_transition` dla `LN-80203`**: przejścia `application→scoring`
    (16.03), `contract→disbursement` (28.03), `schedule→collections` (06.07) — brakuje ogniw
    `scoring→affordability→decision→contract` i `disbursement→schedule`. Dla `LN-80231` łańcuch jest
    kompletny do etapu `contract`. Naprawa wymagałaby dodania nowych wierszy (zmiana liczności seeda i
    `EXPECTED_COUNTS`), więc pozostawiono jako obserwację.
17. **`case_type` bez `CHECK`** (`schema/006-cases.sql`) i brak faktycznego zróżnicowania terminu
    30 vs 60 dni w seedzie — wszystkie 5 wierszy używają 30 dni niezależnie od `case_type`
    (`complaint`/`restructuring_request`/`inquiry`), mimo że `data_model.md` opisuje `case_type` jako
    "steruje terminem ustawowym 30/60 dni". Wymaga decyzji: dodać `CHECK` z listą wartości i tabelę
    dni w `data_model.md`, oraz zróżnicować przykładowe dane.
18. **`contact_history_entry.occurred_at DATE`** ucina komponent godziny obecny w źródle
    (`contact-center.data.ts` ma np. `'2026-10-05 09:12'`) — zgodne z typem kolumny w schemacie, ale
    traci informację względem źródła. Do rozważenia: `TIMESTAMP WITH TIME ZONE`, jeśli godzina kontaktu
    ma znaczenie biznesowe (SLA kanału).
19. **`ebanking_user.username` bez `UNIQUE`** (`schema/005-ebanking.sql`) — pole służy jako login,
    obecne dane są unikalne, ale brak wymuszenia na poziomie schematu.
20. **Żadne przejście `loan_stage_transition` nie ma wypełnionego `approval_id`**, mimo że
    `data_model.md` opisuje przejście do etapu `decision` jako referencjonujące zatwierdzenie z
    `governance_mock`. Niekrytyczne (pole nullable), ale warte uzupełnienia przy integracji z
    governance_mock.
21. Nazewnictwo w danych demo bywa niejednoznaczne (nie błąd integralności): klientka „Maria Lis”
    (`CUST-006`) i pracownik „Marek Lis” (`PRC-60003`, skrót w `cases.data.ts`: „M. Lis”) oraz klientka
    „Anna Kowalska” (`CUST-008`) i pracowniczka „Anna Kowalska” (`PRC-60001`, ta sama pisownia) — w
    obu przypadkach mapowanie na właściwy byt w seedzie jest poprawne, ale podobieństwo nazw może być
    źródłem pomyłek przy przyszłych zmianach danych demo.
22. Skala migawki `dashboard_kpi_snapshot`/`portfolio_trend_point` (dziesiątki tysięcy kredytów/kart,
    setki milionów PLN) jest o rzędy wielkości większa niż liczba wierszy w pozostałych seedach
    (13 wniosków kredytowych, 5 kart) — zgodne 1:1 z `dashboard.page.html`/`.ts` i z opisem w
    `data_model.md` ("migawka niewyliczalna z innych tabel w tej iteracji"), czyli świadoma decyzja
    architektoniczna, nie błąd. Odnotowane, żeby nikt później nie próbował "naprawiać" sumowania.

### Podsumowanie

- **Znaleziono: 22 problemy/obserwacje.**
- **Naprawiono automatycznie: 5** (pozycje 1-5 — literówki/niekonsekwencje danych nieodziedziczone
  z frontendu, niewymagające zmiany architektury).
- **Rozstrzygnięte decyzją użytkownika: 8** (pozycje 6-13) — **naprawione: 6** (6, 7, 8, 9, 10, 12
  częściowo — patrz uwaga o brakującej dedykowanej sprawie w punkcie 12), **odstąpiono od planu: 1**
  (11 — `RPT-FATCA-009`, zmiana wymagałaby złamania PK `regulatory_report`), **potwierdzono bez zmian
  kodu: 1** (13 — pozostaje ilustracyjne/offline, decyzja użytkownika).
- **Odnotowane do rozważenia (dokumentacja/architektura, bez naprawy): 9** (pozycje 14-22).
- Status testów po poprawkach tej sesji: `apps/credit_risk_mock` **`./mvnw.cmd test` → BUILD SUCCESS,
  25/25 testów przechodzi**; `apps/banking_backend` **`./mvnw.cmd test` → BUILD SUCCESS, 5/5 testów
  przechodzi**, Liquibase: 138 wierszy (bez zmiany liczności, tylko wartości pól w
  `003-card.sql`, `004-risk.sql`, `007-contactcenter.sql`).

## Warstwa REST

Kontrolery `@RestController` (pakiet domenowy) wystawiają dane frontendowi przez DTO (Java
records) — encje JPA nigdy nie są serializowane bezpośrednio. Większość endpointów jest GET-only
(R1); od R1.5/R2 część kontrolerów (`approvals/`, `loan/`, `cases/`) dodała też mutujące `POST`
(maker-checker approvals, F2 wniosek kredytowy, F8 sprawa klienta — zob. sekcje niżej). Service
layer jest `@Transactional(readOnly = true)`, mapuje encje na DTO w ramach transakcji (unika
`LazyInitializationException`; `spring.jpa.open-in-view=false`). Pełny kontrakt (endpointy, DTO,
macierz pokrycia pól widok ↔ backend, udokumentowane GAP-y): `docs/banking_backend_contract/api.md`.

Testowanie kontrolerów wymaga dodatkowego startera `spring-boot-starter-webmvc-test` (test-scoped) —
Spring Boot 4.1 wymodularyzował `@AutoConfigureMockMvc` poza `spring-boot-starter-test`, analogicznie
do `spring-boot-starter-liquibase` wyżej; pakiet adnotacji też się przeniósł, do
`org.springframework.boot.webmvc.test.autoconfigure`.

## Security (JWT / Spring Security)

Stateless resource-server: `config/SecurityConfig.java`. CSRF disabled, sesje
`SessionCreationPolicy.STATELESS`. CORS skonfigurowany WEWNĄTRZ security filter chain
(`CorsConfigurationSource`, `/**`, metody `GET/POST/PUT/PATCH/DELETE/OPTIONS`, nagłówki
`Authorization, Content-Type`, origin z `app.cors.allowed-origin`/`BANKING_WEB_ORIGIN`) — NIE przez
`WebMvcConfigurer` (ta klasa z poprzedniej rundy, `config/CorsConfig.java`, została usunięta), bo
inaczej preflight `OPTIONS` dla POST z nagłówkiem `Authorization` dostaje 401 przed dojściem do CORS.

`permitAll`: `/actuator/health` (healthcheck w `deploy/compose.yml`), `POST /auth/login`,
`POST /auth/register`. Wszystko inne wymaga poprawnego JWT.

JWT HS256: `config/JwtConfig.java` (`JwtEncoder`/`JwtDecoder` z Nimbus, część
`spring-security-oauth2-jose`, bez bibliotek trzeciej strony). Sekret `banking.jwt.secret`
(`BANKING_JWT_SECRET`, wymagany, min. 32 bajty, walidowany przy starcie) — bez defaultu w kodzie, tylko
w `.env.example`/`application.properties` lokalnie. Role→kod: `auth/RoleCode.java` (enum, jedno
miejsce). `auth/AuthService.java` woła `identity_access_mock` przez `auth/IdentityAccessClient.java`
(`RestClient`, adres `banking.identity-access.base-url`/`BANKING_IDENTITYACCESS_BASEURL`, domyślnie
`http://localhost:10525`) i wydaje własny token; `/auth/logout` i `/auth/register` to proxy 1:1.

Pełny opis claimów/endpointów: `docs/banking_backend_contract/api.md`, sekcja "Auth (JWT)".

### Hierarchia ról (seniorityLevel) i kolejka zatwierdzeń (maker-checker, R1.5)

`auth/RoleCode.java` dodaje `seniorityLevel` (int) — hierarchia NIE istnieje w `identity_access_mock`
(jego `RolePermissions` jest świadomie "no persistence, no hierarchy"), więc żyje wyłącznie tutaj:
`ADMIN=3`, `COMPLIANCE=2`, `LOANS=RISK=COLLECTIONS=CONTACT=1`. Decyzja: `COMPLIANCE` jest jedyną rolą
pośrednią (zatwierdza 4 role liniowe, zatwierdzana tylko przez `ADMIN`); role liniowe są płaskie
między sobą — nie znaleziono powodu biznesowego, by np. analityk ryzyka miał wyższy poziom niż
doradca kredytowy. Zwracane jako claim JWT `seniorityLevel` oraz pole `LoginResponse.seniorityLevel`
(frontend go potrzebuje bez dekodowania JWT ręcznie).

`approvals/` — proxy do `governance_mock`'s `/approvals` (`GovernanceClient`, `RestClient`, adres
`banking.governance.base-url`/`BANKING_GOVERNANCE_BASEURL`, domyślnie `http://localhost:10524`).
Endpointy: `GET /approvals` (filtr `status`), `GET /approvals/{id}`, `POST /approvals/{id}/decide`.
`GovernanceClient` ma też `recordAuditEvent(...)` i `slaCheck(...)` — wywoływane od R2 przez
`loan/LoanActionsService` (F2) i `cases/CaseActionsService` (F8), zob. niżej.

Reguła maker-checker wymuszana PO STRONIE BACKENDU (nie tylko UI), w `ApprovalsService.decide()`:
1. `requestedBy` approvala (zawsze `employeeId` typu `PRC-xxxxx`) jest tą samą osobą co wołający →
   403 (self-approval), bez wołania `governance_mock`.
2. Rola wnioskodawcy jest rozwiązywana z `Employee.role` (tabela `employee` w `banking_backend`, NIE
   wołanie HTTP do `identity_access_mock`) → `RoleCode.fromDisplayName()` → `seniorityLevel`. Działa,
   bo `employee.id` i `identity_access_mock`'s demo-konta używają tych samych `PRC-xxxxx` (patrz
   `db/changelog/seed/001-customer.sql`). Jeśli rola wnioskodawcy nie mapuje się na `RoleCode` (np.
   `PRC-6000x`, "Doradca klienta") → fail closed, 403.
3. `checker.seniorityLevel <= requesterRole.seniorityLevel()` → 403.
4. W przeciwnym razie: `decidedBy` ustawiany z JWT `sub` (NIE z ciała żądania klienta — frontend
   wysyła tylko `{decision}`), forward do `governance_mock`.

`GET /approvals` wzbogaca każdy wpis o `requestedByRole`/`requestedBySeniorityLevel` (ta sama
rezolucja co wyżej), żeby frontend mógł pokazać/schować przycisk zatwierdź/odrzuć bez duplikowania
mapowania employeeId→rola.

### F2 — wniosek kredytowy: scoring → approval → decyzja → harmonogram (R2)

`loan/LoanActionsService` rozszerza istniejący `LoanController`/`LoanService` (GET-only z R1) o trzy
mutujące endpointy — zob. `docs/banking_backend_contract/api.md`, sekcja "Loans", dla kształtu
request/response. Przepływ:

1. `POST /loans` — `CreditRiskClient.score(customerId, requestedAmount, segment)` (nowy klient,
   pakiet `creditrisk/`, wzorzec identyczny z `GovernanceClient`: `RestClient`, adres
   `banking.creditrisk.base-url`/`BANKING_CREDITRISK_BASEURL`, domyślnie `http://localhost:10522`,
   mapowanie błędów 4xx→`CreditRiskException`/connection-refused→`CreditRiskUnavailableException`→503).
   Scoring woła się PRZED zapisem — outage `credit_risk_mock` nie zostawia częściowego wiersza w
   `loan_application` (sprawdzone testem `createReturns503WhenCreditRiskMockUnavailable...`).
   `scoreResult`/`dsti`/`bureauInquiryId` z odpowiedzi, `interestRate` = stała `0.0790` (seed używa
   tej samej stawki dla każdego produktu, brak tabeli stawek per-produkt w tej iteracji), `stage='application'`.
2. `POST /loans/{id}/submit-for-approval` — `GovernanceClient.createApproval(type=LOAN_STAGE_TRANSITION,
   requestedBy=<employeeId z JWT>, payload=<opis>)`, zapisuje `LoanStageTransition` z `approvalId`
   wypełnionym (kolumna istniała od R1, nieużywana do teraz), `stage→'decision'`,
   `recordAuditEvent(LOAN_STAGE_CHANGED)`. `409` jeśli wniosek nie jest w `application`.
3. `POST /loans/{id}/advance-stage` — **decyzja projektowa R2**: endpoint wołany RĘCZNIE przez
   pracownika (nie automatyczny callback z `ApprovalsService.decide()`). Powód: `ApprovalRecord`
   (kontrakt `governance_mock`) niesie tylko `payload` jako wolny tekst — `ApprovalsService` (pakiet
   `approvals/`) nie ma żadnej strukturalnej wiedzy "to jest kredyt, awansuj go po APPROVED"; żeby to
   zrobić automatycznie, trzeba by parsować tekst payloadu albo wprowadzić sprzężenie
   `approvals/`→`loan/` (import pakietu domenowego z generycznego proxy). Trzymanie `approvalId` na
   `LoanStageTransition` (R1 zostawił tę kolumnę pod to) i odczytanie go z powrotem w
   `advance-stage` (`GovernanceClient.getApproval(approvalId)`, wymaga `status=APPROVED`, inaczej
   `409`) jest prostsze i bliższe dzisiejszemu UI, gdzie pracownik klika akcje jawnie (wzorowane na
   istniejącym przycisku "Przesuń do kolejnego etapu" w `loan-detail.page.ts`). Generuje
   `Installment` (rata równa — annuity: `r = interestRate/12`, `payment = amount*r*(1+r)^n/((1+r)^n-1)`,
   ostatnia rata domyka różnicę zaokrągleń tak, by suma kapitału = `approvedAmount`; to nie jest
   kalkulator finansowy klasy produkcyjnej, tylko wypełnienie encji dla UI), `approvedAmount=requestedAmount`,
   `stage→'contract'`, `recordAuditEvent(LOAN_STAGE_CHANGED)`.

### F8 — sprawa klienta: termin ustawowy, SLA, eskalacja (R2)

`cases/CaseActionsService` rozszerza `CaseController`/`CaseService` o dwa mutujące endpointy:

1. `POST /cases` — **termin ustawowy i SLA są wyliczane przez backend, nie przyjmowane z
   formularza** (zgodnie z `docs/banking_web_functions(concise).md`, sekcja "Sprawy klientów i
   reklamacje": "automatyczne wyliczenie terminu ustawowego (30 dni od zgłoszenia, z możliwością
   wydłużenia do 60 dni w sprawach szczególnie złożonych) na podstawie typu sprawy"). Reguła
   zaimplementowana: `caseType='restructuring_request'` → 60 dni (restrukturyzacja wymaga wielu
   stron/komitetu kredytowego — uznana za "szczególnie złożoną"), każdy inny `caseType` (`complaint`,
   `inquiry`) → 30 dni. SLA do eskalacji to OSOBNY, krótszy zegar, kluczowany priorytetem — reguła
   wewnętrzna (nie z żadnego kontraktu/ustawy): `high`=24h, `medium`=72h, `low`=120h. `ownerId`
   opcjonalny, domyślnie employeeId z JWT (pracownik rejestrujący sprawę staje się jej właścicielem,
   jeśli nie wskaże innego). Status startowy `new`.
2. `POST /cases/{id}/escalate` — woła `GovernanceClient.slaCheck(caseId, hoursRemaining)` z aktualnym
   odliczeniem do `sla_due_at` (liczone na nowo z `Instant.now()`, NIE z zaokrąglonego
   `slaRemainingHours` z DTO). Jeśli `escalated=true` (SLA już minęło) — `governance_mock` sam dopisał
   audit event `AUTO_ESCALATED`, backend tylko zapisuje `escalationSource='AUTO_SLA'` i
   `governanceAuditEventId` z odpowiedzi, bez drugiego wywołania `/audit-events`. Jeśli
   `escalated=false` (pracownik eskaluje mimo że SLA jeszcze nie minęło — manualna decyzja,
   `governance_mock` nie audytuje tego przypadku) — backend woła
   `recordAuditEvent(actor=<employeeId>, action=CASE_ESCALATED_MANUALLY, ...)` sam, zapisuje
   `escalationSource='MANUAL'`. Oba przypadki ustawiają `status='escalated'`. `409` jeśli sprawa jest
   już `escalated` lub `resolved` (governance_mock nie deduplikuje wpisów audytowych dla powtórnych
   wywołań tej samej sprawy — bez tego gate'u klik dwa razy na "Eskaluj" dopisałby drugi audit event).

**Wzorzec do ponownego użycia w R3/R4**: obsługa niedostępności mocka (connection refused →
dedykowany `*UnavailableException` → `503`; 4xx mocka → przekazanie 1:1 statusu/komunikatu) jest
teraz zduplikowana trzy razy (`GovernanceClient`, `CreditRiskClient`) z identyczną strukturą
`exchange()` + `extractMessage()`. R3 (karty, `card_processor_mock`, ścieżka 503→incydent ICT) i R4
(`external_institutions_gateway_mock`) powinny powielić ten sam wzorzec klienta, nie wymyślać
nowego — ale jeśli liczba klientów urośnie dalej, warto rozważyć wspólną klasę bazową/helper zamiast
kopiowania `exchange()` po raz czwarty.

### F4 — karty: blokada, odblokowanie, chargeback, 503→incydent ICT (R3)

`card/CardActionsService` rozszerza `CardController`/`CardService` (GET-only z R1) o cztery mutujące
endpointy — zob. `docs/banking_backend_contract/api.md`, sekcja "Cards", dla kształtu
request/response. Nowy `card/CardProcessorClient` (wzorzec identyczny z `CreditRiskClient`/
`GovernanceClient`: `RestClient`, adres `banking.cardprocessor.base-url`/`BANKING_CARDPROCESSOR_BASEURL`,
domyślnie `http://localhost:10521`) woła `card_processor_mock`.

1. Walidacja stanu PRZED wywołaniem mocka (ten sam wzorzec co `LoanActionsService`/`CaseActionsService`
   — żaden efekt uboczny u mocka dla encji w złym stanie): blokada wymaga `status='active'` (`409`
   inaczej), odblokowanie wymaga `status='blocked'`. Ta walidacja była dotąd TYLKO we frontendzie
   (`cards.page.ts`) — przeniesiona na backend, frontend zachowuje swój UX-guard jako dodatkową warstwę
   (nieszkodliwe duplikowanie, nie usunięte).
2. **Ścieżka 503 → incydent ICT** (F4's "Procesor niedostępny" z `docs/banking_web_functions(concise).md`):
   `CardProcessorUnavailableException` (connection refused LUB `503` realnie zwrócony przez mocka —
   dziś tylko `POST /authorizations` sprawdza flagę `simulate-outage`, `block`/`unblock`/`chargebacks`
   w `card_processor_mock` jej nie sprawdzają, więc tam 503 wystąpi tylko przy connection failure, nie
   przy przełączniku demo) jest łapany w `CardActionsService` PRZED propagacją do klienta: backend woła
   `GovernanceClient.createIctIncident(source="CARD_PROCESSOR", severity="HIGH", description=...)`,
   potem i tak zwraca `503`. Jeśli samo zgłoszenie incydentu zawiedzie (podwójna awaria, governance_mock
   też niedostępny) — błąd jest logowany i **nie maskuje** oryginalnego `503` z `card_processor_mock`
   (sprawdzone testem `blockReturns503EvenWhenGovernanceAlsoUnavailable`). Ten wzorzec (mock
   niedostępny → ICT incident → rethrow oryginału) jest reużywalny w R4 dla
   `external_institutions_gateway_mock` (F6/F7) — `GovernanceClient.createIctIncident(...)` jest już
   ogólnego przeznaczenia, nie specyficzne dla kart.
3. Chargeback shape: `POST /cards/{cardId}/chargebacks` (nie `/cards/chargebacks` z `cardId` w body) —
   wybrano REST shape z `cardId` w path, bo akcja jest inicjowana z widoku konkretnej karty
   (`cards.page.ts`); `authorizationId`/`reasonCode`/`description` w body. Backend sprawdza, że
   `authorizationId` istnieje LOKALNIE (w `card_authorization_ref`) i należy do `cardId` z path (`404`
   inaczej) PRZED wywołaniem mocka — podwójna referencja (lokalna FK + mock) musi się zgadzać.
   `POST /cards/chargebacks/{id}/advance` (bez `cardId` w path — chargeback ma jednoznaczny `id`
   niezależny od karty, podobnie jak `GET /cards/chargebacks/{id}` w kontrakcie mocka).
4. Brak endpointu `POST /cards/{cardId}/authorize` — **decyzja R3**: autoryzacja kartowa jest
   transakcją klienta (terminal płatniczy/e-commerce), nie akcją pracownika w tym UI.
   `cards.page.ts` nigdy nie miało formularza do ręcznego tworzenia autoryzacji (tylko listę
   read-only) — dodanie tego endpointu bez naturalnego miejsca w UI byłoby implementowaniem kontraktu
   mocka "na wszelki wypadek", nie realizacją zakresu F4.

### F5 — e-banking: odblokowanie dostępu i reset urządzenia (OTP/SCA, R3)

`ebanking/EbankingActionsService` rozszerza `EbankingController`/`EbankingService` o pięć endpointów
(dwuetapowy flow OTP dla unlock + dwuetapowy flow dla reset-device + proxy listy urządzeń) — zob.
`docs/banking_backend_contract/api.md`, sekcja "Ebanking". `auth/IdentityAccessClient` (już istniejący
od R1 dla `/auth/*`) rozszerzony o metody OTP/devices/ebanking-access, NIE osobny klient — ten sam
`identity_access_mock`, ten sam base-url/mapowanie błędów, rozdzielenie na dwa kliencki do jednego
serwisu dodałoby strukturę bez korzyści.

1. Walidacja `accessStatus` PRZED jakimkolwiek wywołaniem OTP (ten sam wzorzec co F4): unlock wymaga
   `accessStatus` w `{'locked','pendingReset'}` (`409` jeśli już `'active'`) — sprawdzana zarówno w
   `request-otp` jak i w `confirm` (frontend może wywołać confirm bez ponownego sprawdzenia stanu po
   czasie).
2. `POST /otp/verify` z identity_access_mock'a ZAWSZE zwraca `200` (`{"verified": boolean}`, nigdy
   kod błędu) — `verified:false` jest tłumaczone lokalnie na `OtpNotVerifiedException` → `403`, osobny
   typ wyjątku od `IdentityAccessException` (ten reprezentuje realny status HTTP z mocka, to nie jest
   taki przypadek).
3. OTP jest faktycznie konsumowany dopiero w `POST /ebanking-access/{userId}/unlock` lub
   `POST /devices/{deviceId}/reset` (nie w `/otp/verify` samym) — backend woła `verify` NAJPIERW (gate),
   potem dopiero `unlock`/`reset` (który realnie zużywa `otpId`), zgodnie z kontraktem
   `identity_access_mock`.
4. `GET /ebanking/{userId}/devices` i walidacja własności urządzenia w reset-flow: `deviceId` z path
   jest zawsze zweryfikowany przez `GET /devices/{userId}` PRZED jakąkolwiek akcją (`404` jeśli
   `deviceId` nie jest na liście tego `userId`) — bez tego sprawdzenia pracownik mógłby zresetować
   cudze urządzenie, podając poprawny `deviceId` innego właściciela.
5. Kod OTP nigdy nie przechodzi przez `banking_backend` ani przez przeglądarkę — widoczny wyłącznie w
   panelu Thymeleaf `identity_access_mock:10525/otp` (manualny test demo). Pracownik odbiera kod od
   klienta kanałem pozakanałowym (symulowane jako podpowiedź tekstowa w UI, nie realny SMS/telefon).

### F3 — ryzyko (ECL) i windykacja: akcje przeniesione na backend (R4)

`risk/RiskActionsService` dodaje `POST /risk/collateral/{id}/reclassify` — woła `credit_risk_mock`
`POST /ecl/classify` przez `creditrisk/CreditRiskClient.classifyEcl(...)` (rozszerzenie klienta z
F2, ten sam `RestClient`/adres). **Decyzja projektowa: proweniencja `daysPastDue`.**
`CollateralEntry` nie ma własnego pola opóźnienia, a łańcuch `CollateralEntry.customer` →
`LoanApplication` → `CollectionsCase` nie jest jednoznaczny (klient może mieć wiele kredytów i wiele
spraw windykacyjnych o różnym opóźnieniu) — automatyczne wyprowadzenie byłoby niejednoznaczne.
Wybrano opcję (a): `daysPastDue` jest polem formularza wpisywanym przez pracownika w dialogu
"Przelicz ECL" (`risk.page.ts`) w momencie przeliczania — prostsza i poprawna, operator znający
sprawę klienta wie, ile dni zwłoki ma uwzględnić. Wynik (`stage`/`provision`/`eclClassifiedAt`)
zapisywany na encji, audytowany jako `COLLATERAL_RECLASSIFIED`.

`collections/CollectionsActionsService` dodaje trzy endpointy przenoszące na backend walidacje, które
wcześniej żyły tylko w `collections.page.ts`: `POST /collections/{id}/remind` (bez zmiany `stage`,
`409` jeśli `stage='legal'`), `POST /collections/{id}/propose-settlement` (`stage→'settlement'`),
`POST /collections/{id}/transfer-external` (`stage→'external'`, `409` jeśli `stage` to
`'monitoring'`/`'reminder'`). Każda akcja dopisuje wiersz `CollectionsAction` — `action_type` jest
ograniczony przez `CHECK` w bazie do `REMINDER_SENT|SETTLEMENT_PROPOSED|EXTERNAL_TRANSFER` (zbiór
zdefiniowany w R1/seedzie, R4 go nie rozszerza); audyt w `governance_mock` używa osobnych, bardziej
opisowych nazw akcji (`COLLECTIONS_REMINDER_SENT` itd.) — te dwa słowniki są niezależne.

### F6 — zajęcia komornicze: rejestracja → maker-checker → transfer (R4)

`enforcement/EnforcementActionsService` rozszerza `EnforcementController` o trzy mutujące
endpointy — zob. `docs/banking_backend_contract/api.md`, sekcja "Enforcement". Nowy
`reporting/RegulatoryGatewayClient` (pakiet `reporting/`, bo woła tę samą bramę jak F7; wzorzec
identyczny z `CreditRiskClient`/`CardProcessorClient`: `RestClient`, adres
`banking.regulatorygateway.base-url`/`BANKING_REGULATORYGATEWAY_BASEURL`, domyślnie
`http://localhost:10523`) woła `external_institutions_gateway_mock`. Różnica względem innych
klientów: ten mock zwraca `ProblemDetail` (RFC 9457, pole `detail`), nie `{"message": ...}` —
`extractMessage()` czyta `detail` najpierw, wzorem `GovernanceClient`, nie `CreditRiskClient`.

1. `POST /enforcement` — woła `POST /enforcement/incoming`, zapisuje `gatewayEnforcementId` z
   odpowiedzi bramy. **Zamyka otwarty punkt z R1.5**: od teraz NOWO rejestrowane zajęcia mają
   realne powiązanie z bramą; zajęcia SEEDOWANE (`db/changelog/seed/009-enforcement.sql`) pozostają
   z `gateway_enforcement_id=NULL` — to świadome, TRWAŁE ograniczenie demo (seedy backendu i seed
   bramy nie mają wspólnych identyfikatorów, nigdy nie będą połączone retroaktywnie), nie zadanie do
   zrobienia.
2. `POST /enforcement/{id}/request-transfer-approval` — otwiera approval w `governance_mock`
   (`type="ENFORCEMENT_TRANSFER"`), zapisuje `approvalId` na kolumnie `enforcement_case.approval_id`
   (kolumna istniała od R1.5, nieużywana do teraz — bez potrzeby nowego Liquibase changesetu w R4).
   `409` jeśli `status != 'registered'` LUB `gatewayEnforcementId=NULL` (sprawa seedowana — bez
   sensu otwierać approval dla transferu, który i tak nie będzie mógł się wykonać).
3. `POST /enforcement/{id}/transfer` — wzorzec identyczny z `LoanActionsService.advanceStage()`
   (F2): wymaga `approvalId` zapisanego i `governance_mock`'s `getApproval(approvalId).status()
   == "APPROVED"`, inaczej `409`. Dodatkowo wymaga `gatewayEnforcementId` nie-NULL (ta sama logika
   co w punkcie 2.) — próba transferu zajęcia seedowanego dostaje `409` z komunikatem wyjaśniającym
   ograniczenie, zamiast wywołania bramy z `null`/pustym id.
4. **Ścieżka 503 → incydent ICT**: identyczny wzorzec z `CardActionsService.callProcessor()` (F4) —
   `RegulatoryGatewayUnavailableException` (connection refused) łapana PRZED propagacją do klienta,
   backend woła `GovernanceClient.createIctIncident(source="EXTERNAL_INSTITUTIONS_GATEWAY",
   severity="HIGH")`, potem i tak zwraca `503`; podwójna awaria (governance_mock też niedostępny)
   nie maskuje oryginalnego błędu. Ta sama `callGateway()` jest reużywana przez
   `ReportingActionsService` (F7) — jeden helper, dwa wołający.

### F7 — raport regulacyjny: złożenie → status przyjęcia/odrzucenia (R4)

`reporting/ReportingActionsService` rozszerza `ReportingController` o `POST
/reporting/reports/{id}/submit` i `POST /reporting/reports/{id}/refresh-submission-status` — zob.
`docs/banking_backend_contract/api.md`, sekcja "Reporting". Używa tego samego
`RegulatoryGatewayClient` co F6.

1. `submit` wymaga `status='draft'` (`409` inaczej), woła `POST /regulatory-gateway/submit`,
   zapisuje `ReportSubmission` (`gatewayStatus='PENDING'`), `status→'submitted'`.
2. `refresh-submission-status` wymaga `status='submitted'`, woła `GET
   /regulatory-gateway/submissions/{gatewaySubmissionId}` na najnowszym `ReportSubmission` tego
   raportu (`findFirstByRegulatoryReportIdOrderBySubmittedAtDesc`). `validationErrors` (lista z
   bramy) jest złączana `"; "` i przycinana do 1000 znaków (limit kolumny
   `report_submission.validation_errors`).
3. **Decyzja projektowa: status `RegulatoryReport` po `REJECTED`.** `reporting.model.ts` (frontend)
   ma union `'draft' | 'submitted' | 'accepted'` — brak `'rejected'`, zgodnie z `CHECK` w
   `regulatory_report.status`. `ACCEPTED` z bramy → `status='accepted'`. `REJECTED` → `status='draft'`
   (zamiast dodawać nową wartość do unii/CHECK bez wyraźnej potrzeby) — raport wraca do stanu
   edytowalnego, `validationErrors` z ostatniego `ReportSubmission` pozostają widoczne we
   frontendzie (`ReportSubmissionDto`/`RefreshSubmissionStatusResponse`) jako wyjaśnienie odrzucenia,
   mimo że status raportu formalnie wrócił do `'draft'`. Reguła odrzucenia bramy jest deterministyczna
   per `reportId` (`floorMod(reportId.hashCode(),10)==0`) — raport, który trafia na tę regułę, będzie
   odrzucany przy KAŻDYM ponownym złożeniu z tym samym `id` (świadoma właściwość mocka, nie błąd).
4. **Decyzja projektowa: `/fatca-crs/export` nie jest wołany w R4.** Kontrakt bramy wystawia osobny
   endpoint do generowania XML z rekordów `{customer, taxId, balance}`, ale `RegulatoryReport`/
   `reporting.page.ts` nie przechowują takich danych na poziomie rekordu, tylko metadane raportu
   (`recipient`/`name`/`period`/`dueDate`/`status`); `/regulatory-gateway/submit` akceptuje
   `recipient="FATCA/CRS"` tą samą ścieżką co KNF/NBP/BFG, więc jedna generyczna ścieżka `submit`
   obsługuje wszystkie cztery. Dodanie `/fatca-crs/export` bez modelu rekordów do wypełnienia byłoby
   implementowaniem kontraktu mocka bez realnego miejsca w UI.

### Pułapki Spring Boot 4.1 (do wiedzy przy kolejnych rundach)

- Startery: `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` (główny),
  `spring-boot-starter-security-test` (test-scoped, daje `SecurityMockMvcRequestPostProcessors.jwt()`).
  Potwierdzone empirycznie w BOM 4.1.1 — żadna biblioteka poza ekosystemem Spring Security.
- **Jackson 3**: `spring-boot-starter-web` w Boot 4.1 ciągnie `tools.jackson.core:jackson-databind`
  (pakiet `tools.jackson.databind.*`), NIE `com.fasterxml.jackson.databind.*` — tylko
  `jackson-annotations` zostało w starym pakiecie `com.fasterxml.jackson.annotation`. Kod korzystający
  z `JsonNode`/`ObjectMapper` musi importować z `tools.jackson.databind`.
- **`EndpointRequest` się przeniósł**: teraz w `spring-boot-security` (nie
  `spring-boot-actuator-autoconfigure`), pakiet
  `org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest`.
- **Brak auto-configured `RestClient.Builder`**: `spring-boot-starter-web` samo nie wystawia tego beana
  w tym układzie modułów — dodany ręcznie w `config/HttpClientConfig.java` (`@Bean RestClient.Builder`).
  Kolejne rundy (R2+) wołające inne mocki powinny reużyć tego beana, nie duplikować.
- **Dwa beany `RequestMappingHandlerMapping`** (`requestMappingHandlerMapping` +
  `controllerEndpointHandlerMapping` z Actuatora) — wstrzykiwanie wymaga
  `@Qualifier("requestMappingHandlerMapping")` (zob. `EndpointAuthorizationTest`).

## Stan bieżący — ograniczenia świadome (nie zadania do wykonania)

Wszystkie funkcje F2-F8 z `docs/banking_web_functions(concise).md` są podłączone do backendu
(R1-R4 kompletne). Poniższe punkty są trwałymi właściwościami tej iteracji demo, nie otwartymi
zadaniami:

- Harmonogram rat (`installment`) jest przechowywany tylko dla 2 z 13 wniosków kredytowych
  (`LN-80231`, `LN-80203`) — pozostałe wnioski zwracają `installments: []` z
  `GET /loans/{id}`, a `loan-detail.page.ts` spada na lokalnie liczony `buildSchedule()`.
- **Rozjazd nawigacji vs permissions `identity_access_mock`**: `RolePermissions.java` w
  `identity_access_mock` nie ma wartości `reporting` ani `cases` (żadna rola ich nie ma), a `admin` nie
  ma `enforcement` (mają je tylko `collections`). `banking_web`'s `nav.model.ts` dla R1 mapuje
  `reporting` → permission `compliance` (najbliższa dziedzina) i `cases` → brak gate (widoczne dla
  każdego zalogowanego) — do przeglądu, czy `identity_access_mock` powinien dostać te permissions
  jawnie w kolejnej rundzie.
- `AuthService.logout()` przekazuje JWT wydany przez `banking_backend` jako Bearer do
  `identity_access_mock` `/auth/logout` — ten serwis nie rozpoznaje tego tokenu (nieznany token = no-op
  wg jego kontraktu), więc logout tam nie ma żadnego efektu poza formalnym wywołaniem. JWT
  `banking_backend` nie jest rewokowalny (stateless) — wygasa tylko po `exp` (8h).
- **Pułapka**: filtr bearer-token Spring Security odrzuca niepoprawny/wygasły nagłówek
  `Authorization` z `401` ZANIM `permitAll` na `/auth/login`/`/auth/register` zostanie
  wyewaluowane — request z nieprawidłowym tokenem na te ścieżki dostaje 401, nie trafia do
  kontrolera. `banking_web`'s `authInterceptor` świadomie NIE dołącza przechowywanego tokenu do
  `/auth/login`/`/auth/register` z tego powodu (inaczej ponowne logowanie po wygaśnięciu tokenu by
  się nie udało). Zob. test `AuthControllerTest.loginWithAGarbageAuthorizationHeaderFailsBeforeReachingPermitAll`.
