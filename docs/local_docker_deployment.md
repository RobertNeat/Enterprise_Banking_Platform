# Lokalne budowanie i uruchamianie (Docker Compose)

Instrukcja budowania i uruchamiania całego stosu lokalnie za pomocą Docker Compose.
Zakłada uruchomiony Docker Desktop oraz PowerShell jako powłokę.

## 1. Zbuduj `banking_web` (Angular) przed obrazem Dockera

Dockerfile dla `banking-web` w wariancie dev (`.github/docker/angular.local.Dockerfile`)
**nie buduje Angulara wewnątrz kontenera** — kopiuje już gotowy katalog `dist/`. Dlatego
build frontendowy trzeba wykonać ręcznie przed `docker compose ... build`/`up --build`:

```powershell
pnpm install --frozen-lockfile
pnpm --filter banking-web run build
```

Wynik trafia do `apps\banking_web\dist\banking_web\browser` — to ten katalog
Dockerfile następnie kopiuje do obrazu.

## 2. Zbuduj obrazy

Z katalogu głównego repo:

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env build
```

Zbuduje to wszystkie 7 serwisów aplikacyjnych: `banking-web`, `banking-backend`,
`card-processor-mock`, `credit-risk-mock`, `external-institutions-gateway-mock`,
`governance-mock`, `identity-access-mock`.

Żeby zbudować pojedynczy serwis (przydatne przy debugowaniu problemów builda — łatwiej
zobaczyć, który krok zawodzi), dopisz jego nazwę na końcu:

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env build banking-backend
```

## 3. Podnieś cały stos

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env up -d
```

`up -d --build` łączy krok 2. i 3. w jedną komendę, jeśli wolisz (ale wciąż wymaga
wcześniejszego `pnpm run build` dla `banking_web`, patrz krok 1).

## 4. Sprawdź stan kontenerów

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env ps
```

Każdy kontener powinien po chwili (do ok. 60 s, zależnie od `start_period` w
`compose.yml`) pokazać status `healthy`.

## 5. Adresy serwisów po starcie

| Serwis                             | URL                                    | Uwagi                                      |
| ---------------------------------- | -------------------------------------- | ------------------------------------------ |
| banking_web                        | http://localhost:10500                 | Frontend Angular                           |
| banking_backend                    | http://localhost:10520/actuator/health | REST API                                   |
| card_processor_mock                | http://localhost:10521/                | Panel operatorski (Bootstrap)              |
| credit_risk_mock                   | http://localhost:10522/                | Panel operatorski (Thymeleaf, placeholder) |
| external_institutions_gateway_mock | http://localhost:10523/                | Panel operatorski (Thymeleaf)              |
| governance_mock                    | http://localhost:10524/                | Panel operatorski (Thymeleaf, placeholder) |
| identity_access_mock               | http://localhost:10525/                | Panel operatorski (Thymeleaf)              |
| postgres                           | `localhost:10526`                      | Tylko dla banking_backend, bez UI          |

Wszystkie porty są publikowane wyłącznie na `127.0.0.1` (loopback), zgodnie z konfiguracją
w `deploy/compose.dev.yml`. Domyślne numery portów można nadpisać przez zmienne
`*_HOST_PORT` w `.env` (np. `BANKING_WEB_HOST_PORT`, `CREDIT_RISK_MOCK_HOST_PORT`).

## 6. Podgląd logów

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env logs -f banking-backend
```

Zamień nazwę serwisu na dowolną z listy z kroku 5 (nazwy serwisów w Compose używają
myślników, np. `credit-risk-mock`, nie podkreśleń).

## 7. Zatrzymanie i restart

Zatrzymanie bez usuwania danych (wolumen Postgresa zostaje, dane przetrwają do
następnego `up`):

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env down
```

Ponowne uruchomienie po zmianie kodu w jednym serwisie (np. po edycji
`identity_access_mock`) — przebuduj tylko ten obraz i podmień kontener:

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env build identity-access-mock
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env up -d identity-access-mock
```

(Dla `banking-web` pamiętaj najpierw o `pnpm run build`, patrz krok 1 — sam rebuild
obrazu Dockera nie przebuduje Angulara.)

## 8. Pełne czyszczenie (usunięcie wolumenu Postgresa)

**Destrukcyjne — usuwa dane w bazie.** Używać tylko świadomie, np. żeby zacząć
od czystego seeda:

```powershell
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env down -v
```
