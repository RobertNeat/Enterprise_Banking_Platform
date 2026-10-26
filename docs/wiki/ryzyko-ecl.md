---
layout: default
title: Ryzyko kredytowe (ECL)
summary: Rejestr zabezpieczeń i wycen oraz ręczne przeliczenie koszyka IFRS 9 i rezerwy na straty.
---

## Trasa

`/risk`

## Kto ma dostęp

Analityk ryzyka, Administrator. **Nie mają dostępu**: Doradca kredytowy, Windykator, Konsultant
kanału kontaktu, Compliance/AML.

## Lista zabezpieczeń i wycen

### Kolumny

klient, typ zabezpieczenia (hipoteka / zastaw / gwarancja), wycena, wskaźnik LTV, rating
klienta, koszyk IFRS 9 (stage 1/2/3), rezerwa (kwota odpisu).

<div class="media-block">
  <img src="{{ '/media/6_przeliczenie_ryzyka_ecl/ryzyko_ECL.png' | relative_url }}" alt="Lista zabezpieczeń i wycen">
  <figcaption>Lista zabezpieczeń i wycen z koszykiem IFRS 9</figcaption>
</div>

## Przeliczenie koszyka / rezerwy ECL

### Funkcje

| Funkcja | Opis |
|---|---|
| Reklasyfikacja zabezpieczenia | ręczne przeliczenie koszyka IFRS 9 i rezerwy na podstawie dni opóźnienia spłaty |

### Pole formularza

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Dni opóźnienia (DPD) | liczba | tak | minimum 0 |

Jest to **jedyny mechanizm wyzwalający** przeliczenie — system nie posiada procesu
cyklicznego działającego w tle; dopóki pracownik nie wprowadzi liczby dni opóźnienia i nie
zatwierdzi reklasyfikacji, koszyk i rezerwa pozostają niezmienione.

<div class="media-block">
  <img src="{{ '/media/6_przeliczenie_ryzyka_ecl/before.png' | relative_url }}" alt="Zabezpieczenie przed przeliczeniem">
  <figcaption>Przed przeliczeniem</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/6_przeliczenie_ryzyka_ecl/change.png' | relative_url }}" alt="Dialog przeliczenia ryzyka ECL">
  <figcaption>Dialog wprowadzenia dni opóźnienia</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/6_przeliczenie_ryzyka_ecl/after.png' | relative_url }}" alt="Zabezpieczenie po przeliczeniu">
  <figcaption>Po przeliczeniu — zaktualizowany koszyk IFRS 9 i rezerwa</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/6_przeliczenie_ryzyka_ecl/przeliczanie_ryzyka.gif' | relative_url }}" alt="Pełny przepływ przeliczenia ryzyka ECL">
  <figcaption>Pełny przepływ: przeliczenie koszyka IFRS 9 i rezerwy</figcaption>
</div>

<div class="callout">
Przeliczenie rezerwy nie odświeża migawki KPI widocznej na <a href="../pulpit/">pulpicie</a> —
to niezależny zestaw danych, odświeżany odrębnie.
</div>
