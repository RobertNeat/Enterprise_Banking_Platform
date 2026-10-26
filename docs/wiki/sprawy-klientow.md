---
layout: default
title: Sprawy klientów
summary: Obsługa reklamacji i spraw klientów z terminem ustawowym i monitorowaniem SLA.
---

## Trasa

`/cases`

## Kto ma dostęp

**Każda zalogowana rola** — ekran nie ma dedykowanego uprawnienia, w przeciwieństwie do
większości innych modułów.

## Lista spraw

### Kolumny

priorytet, termin ustawowy, pozostały czas SLA, status.

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Rozpocznij | status = „nowa” | przechodzi sprawę do realizacji; 409 jeśli status inny niż „nowa” |
| Eskaluj | status inny niż „zamknięta”/„eskalowana” | ostrzeżenie przy ≤24h do końca SLA; backend sam wykrywa, czy termin SLA już minął (eskalacja automatyczna vs. ręczna) |
| Zamknij | status inny niż „zamknięta” | dostępne z etapów „nowa” / „w toku” / „eskalowana” |
| Historia | zawsze | ślad audytowy sprawy |

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/sprawa klienta w toku.png' | relative_url }}" alt="Sprawa klienta w toku">
  <figcaption>Sprawa w toku</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/eskalacja sprawy klienta.png' | relative_url }}" alt="Eskalacja sprawy klienta">
  <figcaption>Eskalacja sprawy</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/zamknięcie sprawy klienta.png' | relative_url }}" alt="Zamknięcie sprawy klienta">
  <figcaption>Zamknięcie sprawy</figcaption>
</div>

## Nowa sprawa — kreator 3-etapowy

| Krok | Pole | Typ | Wymagane |
|---|---|---|---|
| 1. Dane sprawy | Temat | tekst | tak |
| | Klient | lista wyboru (z listy klientów) | tak |
| | Typ sprawy | lista wyboru: reklamacja / wniosek o restrukturyzację / zapytanie | tak |
| 2. Priorytet i terminy | Priorytet | lista wyboru: niski / średni / wysoki | tak |
| 3. Podsumowanie | — | tylko do odczytu, przycisk „Prześlij” | — |

Termin ustawowy i pozostały czas SLA są wyliczane automatycznie przez backend — nie są polami
wprowadzanymi ręcznie.

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/sprawa_klienta-krok_0.png' | relative_url }}" alt="Krok 1 kreatora sprawy">
  <figcaption>Krok 1 — dane sprawy</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/sprawa_klienta-krok_1.png' | relative_url }}" alt="Krok 2 kreatora sprawy">
  <figcaption>Krok 2 — priorytet i terminy</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/sprawa_klienta-krok_2.png' | relative_url }}" alt="Krok 3 kreatora sprawy">
  <figcaption>Krok 3 — podsumowanie</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/rozpoczęcie sprawy klienta.png' | relative_url }}" alt="Rozpoczęcie sprawy klienta">
  <figcaption>Rozpoczęcie nowej sprawy</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/3_rozpoczęcie_sprawy_klienta/rozpoczęcie_sprawy_klienta.gif' | relative_url }}" alt="Pełny przepływ obsługi sprawy klienta">
  <figcaption>Pełny przepływ: rejestracja → realizacja → eskalacja/zamknięcie</figcaption>
</div>

## Powiązanie z Kanałem kontaktu

Na ekranie [Kanał kontaktu](../kanal-kontaktu/) każda dyspozycja serwisowa może zostać
powiązana z istniejącą sprawą poprzez wpisanie jej identyfikatora — zob. sekcja „Lista zleceń
serwisowych” w tamtym opisie.
