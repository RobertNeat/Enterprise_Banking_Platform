---
layout: default
title: Pulpit (Dashboard)
summary: Strona startowa po zalogowaniu — kluczowe wskaźniki banku, wykresy portfela i alerty.
---

## Trasa

`/dashboard`

## Kto ma dostęp

Widoczny dla **wszystkich ról** (Administrator, Compliance/AML, Doradca kredytowy, Analityk
ryzyka, Windykator, Konsultant kanału kontaktu).

## Funkcje

Ekran jest w całości tylko do odczytu, z jednym wyjątkiem (rozwiązywanie incydentów ICT).

| Funkcja | Opis |
|---|---|
| Kafelki KPI | aktywne kredyty, portfel kredytowy (mln PLN), wydane karty, wskaźnik NPL, LCR, NSFR, otwarte sprawy, zaległości windykacyjne |
| Wykres trendu portfela | portfel kredytowy w czasie |
| Wykres portfela według produktu | agregacja wniosków kredytowych po typie produktu |
| Lista ostatnich spraw klientów | podgląd najnowszych spraw |
| Lista alertów regulacyjnych | ostrzeżenia dotyczące zgodności |
| Karta incydentów ICT (DORA) | TOP 5 najnowszych incydentów o statusie „otwarty” |
| **Rozwiązanie incydentu ICT** | przycisk „Rozwiąż” przy incydencie — zamyka wpis, który następnie znika z listy (widoczne są tylko incydenty otwarte) |

## Pola / dane wejściowe

Brak formularzy — jedyna interakcja to przycisk „Rozwiąż” przy incydencie ICT, bez dodatkowych
pól.

<div class="callout">
Różnica skali między migawką KPI a liczbą wniosków kredytowych widocznych na ekranie
„Kredyty” jest zamierzona — migawka KPI pochodzi z niezależnego zestawu danych startowych, a
nie z bieżącego przeliczenia wniosków.
</div>
