---
layout: default
title: Zajęcia komornicze
summary: Rejestracja zajęć egzekucyjnych, blokowanie środków i przekazanie należności organowi pod kontrolą maker-checker.
---

## Trasa

`/enforcement`

## Kto ma dostęp

Windykator, Administrator. **Nie mają dostępu**: Doradca kredytowy, Analityk ryzyka, Konsultant
kanału kontaktu, Compliance/AML.

Przycisk „Zgłoś do zatwierdzenia przekazania” jest dodatkowo ukryty dla Administratora — nie
może on inicjować żądania w ramach zasady [maker-checker](../maker-checker/).

## Lista zajęć komorniczych

### Kolumny

komornik, sygnatura sprawy, klient, rodzaj zajęcia, kwota zablokowana, kwota wolna od zajęcia,
status (zarejestrowane / aktywne / przekazane).

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Zgłoś do zatwierdzenia przekazania | status = „zarejestrowane” | otwiera żądanie maker-checker |
| Przekaż środki | status = „aktywne” (zatwierdzenie już udzielone) | otwiera dialog przekazania (patrz niżej) |
| Historia | zawsze | ślad audytowy zajęcia |
| Nowe zajęcie | zawsze | otwiera kreator 3-etapowy (patrz niżej) |

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/before.png' | relative_url }}" alt="Lista zajęć komorniczych">
  <figcaption>Lista zajęć komorniczych</figcaption>
</div>

## Nowe zajęcie — kreator 3-etapowy

| Krok | Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|---|
| 1. Komornik i sprawa | Klient | lista wyboru | tak | — |
| | Komornik | tekst | tak | — |
| | Sygnatura sprawy | tekst | tak | minimalna długość 5 znaków |
| 2. Kwoty | Rodzaj zajęcia | lista wyboru: niealimentacyjne / alimentacyjne | tak | domyślnie niealimentacyjne |
| | Kwota zablokowana | liczba | tak | minimum 0 |
| | Kwota wolna od zajęcia | liczba | tak | minimum 0; nie może przekroczyć limitu dla danego rodzaju zajęcia |
| 3. Podsumowanie | — | tylko do odczytu, przycisk „Prześlij” | — | — |

<div class="callout warning">
<strong>Kwota wolna od zajęcia</strong> jest ograniczona ustawowo: dla zajęć
<strong>niealimentacyjnych</strong> limit wynosi 3604,50 PLN (75% minimalnego wynagrodzenia
brutto na 2026 r., art. 54 §1 Prawa bankowego); dla zajęć <strong>alimentacyjnych</strong>
limit wynosi 0,00 PLN — brak zwolnienia w tym module demonstracyjnym. Przekroczenie limitu w
formularzu jest odrzucane także po stronie backendu (kod 400).
</div>

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/zaj_komornicze_0.png' | relative_url }}" alt="Krok 1 kreatora zajęcia">
  <figcaption>Krok 1 — komornik i sprawa</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/zaj_komornicze_1.png' | relative_url }}" alt="Krok 2 kreatora zajęcia">
  <figcaption>Krok 2 — kwoty</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/zaj_komornicze_2.png' | relative_url }}" alt="Krok 3 kreatora zajęcia">
  <figcaption>Krok 3 — podsumowanie</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/zaj_komornicze_zarejestrow.png' | relative_url }}" alt="Zarejestrowane zajęcie komornicze">
  <figcaption>Zarejestrowane zajęcie</figcaption>
</div>

## Przekazanie środków

### Pole formularza

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Kwota przekazana | liczba | tak | minimum 0,01 |

Przekazanie jest możliwe tylko po zatwierdzeniu żądania na ekranie
[Zatwierdzenia](../zatwierdzenia/); próba przekazania bez zatwierdzenia lub bez powiązania z
bramą zewnętrzną zwraca błąd 409 z komunikatem „transfer odrzucony”.

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/zaj_komornicze_zatwierdzone.png' | relative_url }}" alt="Zatwierdzone i przekazane zajęcie komornicze">
  <figcaption>Zajęcie po zatwierdzeniu i przekazaniu środków</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/7_rejestracja_zajęcia_komorniczego/rejestracja_zajęcia_komorniczego.gif' | relative_url }}" alt="Pełny przepływ rejestracji zajęcia komorniczego">
  <figcaption>Pełny przepływ: rejestracja → zatwierdzenie → przekazanie środków</figcaption>
</div>
