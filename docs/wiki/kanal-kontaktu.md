---
layout: default
title: Kanał kontaktu
summary: Widok 360 klienta, dyspozycje serwisowe, historia kontaktów i symulacja czatu.
---

## Trasa

`/contact-center`

## Kto ma dostęp

Konsultant kanału kontaktu, Administrator. **Nie mają dostępu**: Doradca kredytowy, Analityk
ryzyka, Windykator, Compliance/AML.

## Wybór klienta

### Pole

| Pole | Typ | Wymagane |
|---|---|---|
| Klient | lista wyboru (z listy klientów) | tak |

Wybór klienta odświeża wszystkie poniższe sekcje.

## Widok 360 klienta

Tylko do odczytu: imię i nazwisko, segment, liczba kredytów, liczba kart, rating ryzyka,
liczba otwartych spraw.

## Lista zleceń serwisowych (dyspozycji)

### Funkcje

| Funkcja | Opis |
|---|---|
| Powiązanie ze sprawą | przy każdej dyspozycji można wpisać identyfikator sprawy i kliknąć „Połącz” |

### Pole formularza

| Pole | Typ | Wymagane | Walidacja |
|---|---|---|---|
| Identyfikator sprawy | tekst | tak | sprawa musi istnieć i należeć do tego samego klienta (inaczej błąd 404/400) |

## Historia kontaktów

Tylko do odczytu — lista według kanału komunikacji.

## Czat z klientem (symulacja)

### Pole

| Pole | Typ | Wymagane |
|---|---|---|
| Treść wiadomości | tekst | tak (do wysłania) |

<div class="callout warning">
Czat jest wyłącznie symulacją frontendową — wysłane wiadomości nie są zapisywane po stronie
serwera. Zob. <a href="../audyt-i-kreatory/">znane ograniczenia</a>.
</div>
