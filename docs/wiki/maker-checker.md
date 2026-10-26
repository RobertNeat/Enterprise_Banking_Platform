---
layout: default
title: Zasada maker-checker
summary: Rozdzielenie roli wykonawcy (maker) i zatwierdzającego (checker) dla operacji o istotnym ryzyku.
---

## Zasada

Operacje o istotnym ryzyku finansowym — przejście wniosku kredytowego do etapu decyzji oraz
przekazanie należności z zajęcia komorniczego do organu egzekucyjnego — wymagają zatwierdzenia
przez **inną osobę** niż ta, która zainicjowała żądanie, i o **wyższym poziomie seniority**.
Reguła jest wymuszona po stronie serwera (`banking_backend`), nie tylko w interfejsie.

## Kto może inicjować (maker)

Każda rola o seniority **1** (Doradca kredytowy, Analityk ryzyka, Windykator, Konsultant kanału
kontaktu) może zainicjować żądanie zatwierdzenia. **Administrator systemu nie może** —
posiadając najwyższe seniority (3), nie istniałaby żadna rola zdolna zatwierdzić jego żądanie,
więc przycisk inicjujący jest dla Administratora ukryty, a backend i tak odrzuciłby takie
żądanie kodem 403.

## Kto może zatwierdzać (checker)

Decyzję (zatwierdzenie/odrzucenie) może podjąć osoba, której seniority jest **ściśle wyższe**
niż seniority roli wnioskującego, i która **nie jest** autorem żądania. W praktyce:

- Żądania od ról seniority 1 (Doradca, Analityk, Windykator, Konsultant) może zatwierdzić
  Compliance / AML (2) albo Administrator (3).
- Ekran [Zatwierdzenia](../zatwierdzenia/) jest widoczny tylko dla Administratora i
  Compliance/AML — to jedyne role, które mogą cokolwiek zdecydować.

## Gdzie ta zasada występuje w UI

| Ekran | Akcja inicjująca (maker) | Decyzja (checker) |
|---|---|---|
| [Kredyty](../kredyty/) | „Prześlij do zatwierdzenia” / „Przesuń etap” | [Zatwierdzenia](../zatwierdzenia/) |
| [Zajęcia komornicze](../zajecia-komornicze/) | „Zgłoś do zatwierdzenia przekazania” | [Zatwierdzenia](../zatwierdzenia/) |

## Co się dzieje po odrzuceniu

W przypadku kredytów odrzucona decyzja zwraca wniosek do etapu `wniosek złożony`, umożliwiając
ponowne złożenie do zatwierdzenia po korekcie danych.
