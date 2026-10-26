---
layout: default
title: Karty płatnicze
summary: Blokada i odblokowanie karty, przegląd autoryzacji oraz zgłaszanie chargebacków.
---

## Trasa

`/cards`

## Kto ma dostęp

Konsultant kanału kontaktu, Administrator. **Nie mają dostępu**: Doradca kredytowy, Analityk
ryzyka, Windykator, Compliance/AML.

## Lista kart

### Kolumny

posiadacz, numer karty (zamaskowany wg PCI DSS), organizacja płatnicza (Visa/Mastercard),
produkt, status (aktywna / zablokowana / oczekująca / wygasła), data ważności, limit dzienny,
limit w bankomacie.

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Blokada / odblokowanie karty | niedostępne dla kart o statusie „wygasła” lub „oczekująca” | przełącza stan aktywna ↔ zablokowana |
| Zgłoszenie chargebacku | — | otwiera dialog zgłoszenia (patrz niżej) |
| Historia | zawsze | ślad audytowy karty |

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/początkowe.png' | relative_url }}" alt="Lista kart płatniczych">
  <figcaption>Lista kart płatniczych</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/zablokowanie_karty.png' | relative_url }}" alt="Blokada karty">
  <figcaption>Blokada karty</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/odblokowanie karty.png' | relative_url }}" alt="Odblokowanie karty">
  <figcaption>Odblokowanie karty</figcaption>
</div>

## Lista autoryzacji karty

Tylko do odczytu: id autoryzacji, akceptant, kwota, data, procesor, status zatwierdzenia.
Dane wzorowane na polach wiadomości autoryzacyjnej ISO 8583.

## Zgłoszenie chargebacku

### Pola formularza

| Pole | Typ | Wymagane | Uwagi |
|---|---|---|---|
| Autoryzacja | lista wyboru | tak | zawężona do autoryzacji wybranej karty |
| Kod przyczyny | lista wyboru | tak | standardowe kody Visa/Mastercard: `4855`, `4837`, `4853`, `4863` |
| Opis | pole tekstowe wieloliniowe | tak | — |

Statusy cyklu chargebacku: zgłoszony → w trakcie rozpatrywania → rozstrzygnięty na korzyść
klienta / rozstrzygnięty na korzyść akceptanta.

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/zgłoszenie chargeback.png' | relative_url }}" alt="Formularz zgłoszenia chargebacku">
  <figcaption>Formularz zgłoszenia chargebacku</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/zapisanie chargeback.png' | relative_url }}" alt="Zapisany chargeback">
  <figcaption>Zapisany chargeback na liście</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/4_operacja_na_kartach_kredytowych/operacje_na_kartach_kredytwoych.gif' | relative_url }}" alt="Pełny przepływ operacji na kartach">
  <figcaption>Pełny przepływ: blokada karty i zgłoszenie chargebacku</figcaption>
</div>

<div class="callout warning">
Jeśli procesor kart jest niedostępny, operacja generuje automatycznie incydent ICT (DORA)
widoczny na <a href="../pulpit/">pulpicie</a> w liście alertów regulacyjnych.
</div>
