## Uruchamianie projektu lokalnie w docker compose

```bash
# przy potrzebie usunięcia wolumenu bazy danych dodać na końcu flagę -v
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env down
# zbudowanie frontend (wygenerowanie dist)
pnpm --filter banking-web build
# uruchomienie z rebuildem obrazów
docker compose -f deploy/compose.yml -f deploy/compose.dev.yml --env-file .env up -d --build
```

## Zarządzanie projektem w docker-production w docker compose

```bash
# przy potrzebie usunięcia wolumenu bazy danych dodać na końcu flagę -v
docker compose -f compose.yml --env-file config.env down
docker compose -f compose.yml --env-file config.env up -d
```
