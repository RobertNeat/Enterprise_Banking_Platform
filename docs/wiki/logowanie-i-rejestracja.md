---
layout: default
title: Logowanie i rejestracja
summary: Uwierzytelnienie pracownika oraz samodzielna rejestracja nowego konta wymagająca zatwierdzenia administratora.
---

## Trasy

`/login`, `/register` — ekrany poza zalogowaną powłoką aplikacji, bez wymogu uwierzytelnienia.

## Kto ma dostęp

Dostępne dla każdego — to ekrany wejściowe przed zalogowaniem.

## Logowanie

### Funkcje

| Funkcja | Opis |
|---|---|
| Logowanie identyfikatorem i hasłem | Uwierzytelnienie względem `identity_access_mock`, wydanie tokenu JWT z rolą i uprawnieniami |
| Wybór konta demo | Lista 6 kont demonstracyjnych (po jednym na rolę) — wybór automatycznie wypełnia formularz |

### Pola formularza

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Identyfikator pracownika | tekst | tak | wzorzec `PRC-00000` (litery `PRC-` + 5 cyfr) |
| Hasło | pole hasła | tak | minimalna długość 8 znaków |

### Obsługa błędów

| Sytuacja | Komunikat |
|---|---|
| Błędne dane logowania | 401 — nieprawidłowy identyfikator lub hasło |
| Konto odrzucone przez administratora | 403 z treścią „REJECTED” |
| Konto czeka na zatwierdzenie | 403 (inna treść) — rejestracja jeszcze nie zatwierdzona |
| Usługa niedostępna | błąd 0/5xx — komunikat o niedostępności serwisu |

<div class="media-block">
  <img src="{{ '/media/1_login_registration/login.png' | relative_url }}" alt="Ekran logowania">
  <figcaption>Ekran logowania z listą kont demo</figcaption>
</div>

## Rejestracja

Nowe konto pracownika jest tworzone ze statusem „czeka na zatwierdzenie” i wymaga akceptacji
administratora na ekranie [Administracja kont](../administracja-kont/), zanim będzie można się
nim zalogować.

### Pola formularza (sekcje)

**Dane osobowe**

| Pole | Typ | Wymagane |
|---|---|---|
| Imię | tekst | tak |
| Nazwisko | tekst | tak |
| E-mail | tekst (format e-mail) | tak |
| Telefon | tekst | tak |

**Dane pracownicze**

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Identyfikator pracownika | tekst | tak | wzorzec `PRC-00000` |
| Departament | lista wyboru | tak | IT / Ryzyko Kredytowe / Obsługa Klienta / Compliance / Operacje |
| Oddział | tekst | tak | — |
| Stanowisko | tekst | tak | — |
| Przełożony | tekst | tak | — |
| Data rozpoczęcia | data | tak | zapisywana jako znacznik ISO 8601 (północ UTC) |

**Kompetencje branżowe**

| Pole | Typ | Wymagane |
|---|---|---|
| Linie biznesowe | lista wielokrotnego wyboru (retail / korporacyjna / ryzyko / windykacja / compliance / IT) | tak, minimum 1 |
| Certyfikaty | tekst | nie |
| Numer licencji | tekst | nie |

**Konto**

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Hasło | pole hasła | tak | minimalna długość 8 znaków |
| Powtórz hasło | pole hasła | tak | musi być identyczne z polem „Hasło” |
| Akceptacja regulaminu | checkbox | tak | musi być zaznaczone |

### Obsługa błędów

| Sytuacja | Komunikat |
|---|---|
| Identyfikator pracownika już istnieje | 409 |
| Niepoprawne dane | 400 |
| Usługa niedostępna | błąd 0/5xx |

<div class="media-block">
  <img src="{{ '/media/1_login_registration/register.png' | relative_url }}" alt="Formularz rejestracji">
  <figcaption>Formularz rejestracji nowego pracownika</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/1_login_registration/register_and_login.png' | relative_url }}" alt="Pełny przepływ rejestracji i logowania">
  <figcaption>Pełny przepływ: rejestracja → zatwierdzenie → logowanie</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/1_login_registration/admin_accept_registration.png' | relative_url }}" alt="Zatwierdzenie rejestracji przez administratora">
  <figcaption>Zatwierdzenie rejestracji przez administratora (zob. też Administracja kont)</figcaption>
</div>
