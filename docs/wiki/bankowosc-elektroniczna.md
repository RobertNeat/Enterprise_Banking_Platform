---
layout: default
title: Bankowość elektroniczna
summary: Odblokowanie dostępu klienta i reset zarejestrowanego urządzenia, chronione kodem jednorazowym (PSD2/SCA).
---

## Trasa

`/ebanking`

## Kto ma dostęp

Doradca kredytowy, Konsultant kanału kontaktu, Administrator. **Nie mają dostępu**: Analityk
ryzyka, Windykator, Compliance/AML.

## Lista użytkowników e-banking

### Kolumny

login, kanał (web/mobilny), limit dzienny, ostatnie logowanie, uprawnienia, status dostępu.

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Odblokowanie dostępu | status dostępu ≠ „aktywny” | uruchamia dwuetapowy dialog potwierdzenia OTP |
| Reset urządzenia | tylko kanał mobilny | uruchamia dwuetapowy dialog potwierdzenia OTP, poprzedzony pobraniem listy urządzeń klienta |

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/doblokowanie_dostępu_1.png' | relative_url }}" alt="Lista użytkowników e-banking">
  <figcaption>Lista użytkowników e-banking</figcaption>
</div>

## Dialog potwierdzenia OTP (wspólny dla obu akcji)

| Krok | Pole | Typ | Wymagane | Uwagi |
|---|---|---|---|---|
| 1. Żądanie kodu | Urządzenie | lista wyboru | tak (tylko reset urządzenia) | automatycznie wybrane, jeśli klient ma tylko jedno zarejestrowane urządzenie |
| 2. Potwierdzenie kodu | Kod OTP | tekst | tak | błędny/wygasły kod pokazuje błąd bez zamykania dialogu |

Krok 1 dla odblokowania dostępu nie wymaga żadnego pola — od razu żąda wysłania kodu.

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/doblokowanie_dostępu_kod.png' | relative_url }}" alt="Dialog potwierdzenia kodu OTP przy odblokowaniu dostępu">
  <figcaption>Potwierdzenie kodu OTP — odblokowanie dostępu</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/doblokowanie_dostępu_2.png' | relative_url }}" alt="Potwierdzone odblokowanie dostępu">
  <figcaption>Potwierdzone odblokowanie dostępu</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/doblokowanie_dostępu_4.png' | relative_url }}" alt="Status dostępu po odblokowaniu">
  <figcaption>Status dostępu po odblokowaniu</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/reset_urządzenia_1.png' | relative_url }}" alt="Wybór urządzenia do resetu">
  <figcaption>Wybór urządzenia do resetu</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/reset_urządzenia_kod.png' | relative_url }}" alt="Potwierdzenie kodu OTP przy resecie urządzenia">
  <figcaption>Potwierdzenie kodu OTP — reset urządzenia</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/reset_urządzenia_2.png' | relative_url }}" alt="Reset urządzenia w trakcie">
  <figcaption>Reset urządzenia w trakcie</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/reset_urządzenia_4.png' | relative_url }}" alt="Reset urządzenia zakończony">
  <figcaption>Reset urządzenia zakończony</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/reset_urządzenia_5.png' | relative_url }}" alt="Zarejestrowane urządzenie po resecie">
  <figcaption>Zarejestrowane urządzenie po resecie</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/5_odblokowanie_dostępu_i_reset_urządzenia/odblokowanie_dostępu_i_reset_urządzenia.gif' | relative_url }}" alt="Pełny przepływ odblokowania dostępu i resetu urządzenia">
  <figcaption>Pełny przepływ: odblokowanie dostępu i reset urządzenia z OTP</figcaption>
</div>
