---
layout: default
title: Kredyty i pożyczki
summary: Cykl życia wniosku kredytowego — od złożenia, przez scoring, zatwierdzenie, umowę, aż po uruchomienie i harmonogram spłat.
---

## Trasy

`/loans` (lista), `/loans/:id` (szczegóły)

## Kto ma dostęp

Doradca kredytowy, Analityk ryzyka, Administrator. **Nie mają dostępu**: Windykator, Konsultant
kanału kontaktu, Compliance/AML.

Przycisk „Nowy wniosek” jest dodatkowo ukryty dla Administratora — nie może on inicjować
żądania w ramach zasady [maker-checker](../maker-checker/).

## Lista wniosków kredytowych

### Funkcje

| Funkcja | Opis |
|---|---|
| Wyszukiwanie | filtr tekstowy po nazwie/ID wnioskodawcy |
| Filtr segmentu | wszystkie / detaliczny / biznesowy |
| Podgląd szczegółów | kliknięcie wiersza przenosi do widoku szczegółowego |
| Nowy wniosek | otwiera kreator 4-etapowy (patrz niżej) — niedostępny dla Administratora |

### Kolumny listy

id, wnioskodawca, segment, produkt, etap, kwota wnioskowana, kwota przyznana, wynik scoringu,
DSTI, data utworzenia.

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-historia.png' | relative_url }}" alt="Historia wniosku kredytowego">
  <figcaption>Dialog „Historia” — ślad audytowy wniosku</figcaption>
</div>

## Nowy wniosek kredytowy — kreator 4-etapowy

| Krok | Pola | Typ | Wymagane |
|---|---|---|---|
| 1. Wnioskodawca i segment | Klient | lista wyboru (z listy klientów) | tak |
| | Segment | lista wyboru: detaliczny / biznesowy | tak |
| 2. Produkt i kwota | Produkt | lista wyboru: gotówkowy / hipoteczny / limit w koncie / inwestycyjny / linia kredytowa | tak |
| | Kwota wnioskowana | liczba | tak, min. 0,01 |
| 3. Scoring i DSTI | Wynik scoringu, DSTI | tylko do odczytu | — |
| 4. Podsumowanie | — | tylko do odczytu, przycisk „Prześlij” | — |

Krok 3 jest wyliczany automatycznie — po przejściu do tego kroku system wywołuje scoring
zewnętrzny i liczy wskaźnik DSTI (dług do dochodu); użytkownik nie wpisuje tych wartości
ręcznie. Przesłanie w kroku 4 otwiera żądanie zatwierdzenia w ramach zasady maker-checker.

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_0.png' | relative_url }}" alt="Krok 1 kreatora wniosku">
  <figcaption>Krok 1 — wnioskodawca i segment</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_1.png' | relative_url }}" alt="Krok 2 kreatora wniosku">
  <figcaption>Krok 2 — produkt i kwota</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_2.png' | relative_url }}" alt="Krok 3 kreatora wniosku">
  <figcaption>Krok 3 — wynik scoringu i DSTI</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_3.png' | relative_url }}" alt="Krok 4 kreatora wniosku">
  <figcaption>Krok 4 — podsumowanie i przesłanie</figcaption>
</div>

## Szczegóły wniosku

### Funkcje

| Funkcja | Dostępność | Opis |
|---|---|---|
| Prześlij do zatwierdzenia | etap = „wniosek złożony” | otwiera żądanie maker-checker; używane też przy ponownym złożeniu po odrzuceniu |
| Przesuń etap | etap = „decyzja” | wymaga wcześniej zatwierdzonej decyzji; odrzucenie zwraca wniosek do etapu „wniosek złożony” |
| Uruchom (wypłata) | etap = „umowa” | uruchamia środki i generuje harmonogram spłat |
| Historia | zawsze | ślad audytowy wniosku |

### Etapy cyklu życia

wniosek → scoring (automatyczny, w tle) → decyzja → umowa → uruchomienie → harmonogram → windykacja (jeśli dotyczy)

### Dane tylko do odczytu

id, wnioskodawca, segment, produkt, kwota wnioskowana/przyznana, wynik scoringu, DSTI,
oprocentowanie, okres kredytowania.

### Harmonogram spłat

Tabela: numer raty, data spłaty, kapitał, odsetki, status raty (spłacona / nadchodząca /
zaległa).

<div class="callout warning">
Dla części wniosków z danych startowych, które nie mają własnych wierszy harmonogramu w
bazie, harmonogram jest generowany tymczasowo w przeglądarce jako rozwiązanie zastępcze —
zob. <a href="../audyt-i-kreatory/">znane ograniczenia</a>.
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-decyzja.png' | relative_url }}" alt="Etap decyzji wniosku">
  <figcaption>Etap decyzji</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/zatwierdzenie_wniosku_kredytowego.png' | relative_url }}" alt="Zatwierdzenie wniosku kredytowego">
  <figcaption>Zatwierdzenie wniosku na ekranie Zatwierdzenia (inna rola, wyższe seniority)</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-podgląd_decyzji.png' | relative_url }}" alt="Podgląd decyzji po zatwierdzeniu">
  <figcaption>Podgląd wniosku po podjęciu decyzji</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_4.png' | relative_url }}" alt="Etap umowy">
  <figcaption>Etap umowy i uruchomienia</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytowy-krok_6.png' | relative_url }}" alt="Harmonogram spłat">
  <figcaption>Harmonogram spłat</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/compliance_logowanie.png' | relative_url }}" alt="Logowanie jako Compliance/AML do zatwierdzenia">
  <figcaption>Logowanie na rolę Compliance/AML w celu zatwierdzenia wniosku</figcaption>
</div>

<div class="media-block">
  <img src="{{ '/media/2_wniosek_kredytowy/wniosek_kredytory.gif' | relative_url }}" alt="Pełny przepływ wniosku kredytowego">
  <figcaption>Pełny przepływ: od złożenia do zatwierdzenia wniosku</figcaption>
</div>
