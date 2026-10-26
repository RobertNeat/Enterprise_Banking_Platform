---
layout: default
title: Raportowanie regulacyjne
summary: Cykl życia raportów regulacyjnych do KNF/NBP/BFG oraz eksportów podatkowych FATCA/CRS.
---

## Trasy

`/reporting` (lista), `/reporting/dashboard` (KPI)

## Kto ma dostęp

Compliance/AML, Administrator. **Nie mają dostępu**: Doradca kredytowy, Analityk ryzyka,
Windykator, Konsultant kanału kontaktu.

## Lista raportów

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Filtr odbiorcy | zawsze | wszystkie / KNF / NBP / BFG / FATCA-CRS (filtr działa lokalnie, bez zapytania do serwera) |
| Złóż raport | status = „roboczy” | przekazuje raport do symulowanej bramy sprawozdawczej |
| Sprawdź status | status = „złożony” | odpytuje bramę o wynik: w toku / odrzucony (z błędami walidacji) / zaakceptowany |
| Historia złożeń | zawsze | lista wcześniejszych prób złożenia raportu |
| Eksport do CSV | zawsze | generuje plik CSV po stronie przeglądarki (`raporty-regulacyjne.csv`) |
| Przejdź do dashboardu | zawsze | otwiera widok KPI raportowania |

### Pole formularza

| Pole | Typ | Wymagane |
|---|---|---|
| Filtr odbiorcy | lista wyboru | nie (domyślnie: wszystkie) |

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/raport_regulacyjny-robocze.png' | relative_url }}" alt="Raporty w statusie roboczym">
  <figcaption>Raporty w statusie „roboczy”</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/raport_regulacyjny-złożone.png' | relative_url }}" alt="Raporty złożone">
  <figcaption>Raporty w statusie „złożony”</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/raport_regulacyjny-zaakceptowane.png' | relative_url }}" alt="Raporty zaakceptowane">
  <figcaption>Raporty zaakceptowane</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/eksport CSV.png' | relative_url }}" alt="Eksport raportów do CSV">
  <figcaption>Eksport listy raportów do CSV</figcaption>
</div>

<div class="callout warning">
Eksport CSV jest ogólnym plikiem generowanym w przeglądarce — nie jest to format zgodny ze
standardem wymiany FATCA/CRS, mimo że backend bramy zewnętrznej posiada generator takiego
formatu. Zob. <a href="../audyt-i-kreatory/">znane ograniczenia</a>.
</div>

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/raportowanie.gif' | relative_url }}" alt="Pełny przepływ raportowania regulacyjnego">
  <figcaption>Pełny przepływ: złożenie raportu i sprawdzenie statusu</figcaption>
</div>

## Dashboard raportowania

Tylko do odczytu, bez formularzy:

| Element | Opis |
|---|---|
| Kafelki KPI | łączna liczba raportów, liczba przeterminowanych, najbliższy termin, liczba zaakceptowanych |
| Wykres słupkowy | raporty według odbiorcy |
| Wykres kołowy | raporty według statusu (roboczy / złożony / zaakceptowany) |

<div class="media-block">
  <img src="{{ '/media/8_raportowanie/raport_regulacyjny-dashboard.png' | relative_url }}" alt="Dashboard raportowania regulacyjnego">
  <figcaption>Dashboard raportowania — KPI i wykresy</figcaption>
</div>
