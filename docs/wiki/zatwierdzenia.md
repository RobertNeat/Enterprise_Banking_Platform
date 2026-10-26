---
layout: default
title: Zatwierdzenia (kolejka)
summary: Centralna kolejka decyzji maker-checker dla żądań z modułów Kredyty i Zajęcia komornicze.
---

## Trasa

`/approvals`

## Kto ma dostęp

**Tylko** Administrator i Compliance/AML — to jedyne role o poziomie seniority wystarczającym,
by cokolwiek zdecydować. Zob. [Zasada maker-checker](../maker-checker/).

## Funkcje

| Funkcja | Opis |
|---|---|
| Filtr statusu | wszystkie / oczekujące / zatwierdzone / odrzucone (domyślnie: oczekujące) |
| Zatwierdź | podejmuje pozytywną decyzję dla żądania |
| Odrzuć | podejmuje negatywną decyzję dla żądania |

### Pole formularza

| Pole | Typ | Wymagane |
|---|---|---|
| Filtr statusu | lista wyboru | nie (domyślnie: oczekujące) |

Nie ma żadnych innych pól tekstowych — ekran służy wyłącznie do podejmowania decyzji
przyciskami.

### Kolumny listy

identyfikator żądania, osoba wnioskująca, poziom seniority wnioskującego, status.

### Warunki pokazania przycisków decyzji

Przyciski „Zatwierdź”/„Odrzuć” są widoczne tylko, gdy **wszystkie** warunki są spełnione:

1. status żądania = „oczekujące”,
2. osoba decydująca nie jest autorem żądania,
3. poziom seniority osoby decydującej jest **ściśle wyższy** niż poziom seniority wnioskującego.

Próba decyzji niespełniającej tych warunków jest odrzucana przez backend kodem 403.
