---
layout: default
title: Ślad audytowy i kreatory
summary: Wzorce UI powtarzające się na wielu ekranach systemu — warto je rozpoznać raz, zamiast opisywać osobno na każdej stronie.
---

## Dialog „Historia” (ślad audytowy)

Ekrany [Kredyty](../kredyty/), [Karty płatnicze](../karty/), [Sprawy klientów](../sprawy-klientow/)
oraz [Zajęcia komornicze](../zajecia-komornicze/) udostępniają przycisk **„Historia”**,
otwierający dialog z listą zdarzeń audytowych dla danego rekordu (kto, kiedy, jaką akcję
wykonał). Dane pochodzą z `GET /audit-events?entityType=<TYP_ENCJI>`, gdzie `<TYP_ENCJI>` to
odpowiednio `LOAN_APPLICATION`, `PAYMENT_CARD`, `CUSTOMER_CASE` albo `ENFORCEMENT_CASE`.

Dialog jest tylko do odczytu — nie ma pól do wypełnienia.

## Kreator wieloetapowy (wizard)

Trzy ekrany używają tego samego komponentu kreatora wieloetapowego do rejestracji nowego
rekordu, z walidacją każdego kroku przed przejściem do następnego:

| Ekran | Liczba kroków | Kroki |
|---|---|---|
| [Kredyty](../kredyty/) | 4 | wnioskodawca i segment → produkt i kwota → scoring i DSTI (odczyt) → podsumowanie |
| [Sprawy klientów](../sprawy-klientow/) | 3 | dane sprawy → priorytet i terminy → podsumowanie |
| [Zajęcia komornicze](../zajecia-komornicze/) | 3 | komornik i sprawa → kwoty → podsumowanie |

Każdy krok blokuje przejście dalej, dopóki wymagane pola nie są poprawnie wypełnione. Krok
podsumowania jest zawsze tylko do odczytu — ostateczne przesłanie następuje po jego
zatwierdzeniu.

## Znane ograniczenia (świadomie niepodłączone funkcje)

Poniższe elementy interfejsu istnieją i działają wizualnie, ale nie mają pełnego pokrycia
backendowego — nie są to usterki, tylko świadome decyzje projektowe bieżącej iteracji:

- **Czat z klientem** (Kanał kontaktu) — czysto frontendowa symulacja komunikatora, bez
  zapisu wiadomości po stronie serwera.
- **Eksport CSV raportów** (Raportowanie) — generuje ogólny plik CSV po stronie przeglądarki;
  nie jest to format zgodny ze standardem wymiany FATCA/CRS, mimo że taki generator istnieje
  jako osobny mock.
- **Harmonogram spłat dla starszych wniosków kredytowych** — gdy wniosek nie ma własnych
  wierszy harmonogramu w bazie (dane startowe/seed), frontend generuje harmonogram tymczasowo
  w czasie działania aplikacji jako rozwiązanie zastępcze.
