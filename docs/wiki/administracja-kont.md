---
layout: default
title: Administracja kont
summary: Zatwierdzanie samodzielnych rejestracji pracowników i przypisanie im roli systemowej.
---

## Trasa

`/accounts`

## Kto ma dostęp

**Tylko** Administrator.

## Funkcje

| Funkcja | Opis |
|---|---|
| Lista kont oczekujących | konta ze statusem „czeka na zatwierdzenie”, utworzone przez [formularz rejestracji](../logowanie-i-rejestracja/) |
| Lista kont rozpatrzonych | konta ze statusem „aktywne” albo „odrzucone” |
| Zatwierdź | otwiera dialog przypisania roli (patrz niżej) |
| Odrzuć | odrzuca konto bez dodatkowych pól — pojedynczy przycisk potwierdzenia |

## Dialog zatwierdzenia

### Pole formularza

| Pole | Typ | Wymagane | Opcje |
|---|---|---|---|
| Przypisana rola | lista wyboru | tak | Administrator systemu, Compliance / AML, Doradca kredytowy, Analityk ryzyka, Windykator, Konsultant kanału kontaktu |

### Obsługa błędów

| Sytuacja | Komunikat |
|---|---|
| Brak uprawnień | 403 |
| Konto nie jest już w stanie oczekującym | 409 |

<div class="media-block">
  <img src="{{ '/media/1_login_registration/admin_accept_registration.png' | relative_url }}" alt="Zatwierdzenie rejestracji pracownika z przypisaniem roli">
  <figcaption>Zatwierdzenie konta z przypisaniem roli</figcaption>
</div>
