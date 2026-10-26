---
layout: default
title: Role i uprawnienia
summary: Pełna matryca ról, modułów nawigacji i poziomów decyzyjności (seniority) w systemie.
---

## Role demo

System definiuje 6 ról (wartości są jednocześnie etykietami wyświetlanymi w UI):

| Rola | Identyfikator konta demo | Poziom seniority |
|---|---|---|
| Administrator systemu | `PRC-00001` / `Admin12345` | 3 (najwyższy) |
| Compliance / AML | `PRC-50123` / `Compliance1` | 2 |
| Doradca kredytowy | `PRC-10234` / `Loans12345` | 1 |
| Analityk ryzyka | `PRC-20456` / `Risk123456` | 1 |
| Windykator | `PRC-30678` / `Collect1234` | 1 |
| Konsultant kanału kontaktu | `PRC-40890` / `Contact1234` | 1 |

Poziom **seniority** decyduje o tym, kto może zatwierdzić decyzję podjętą przez kogo innego —
zob. [Zasada maker-checker](../maker-checker/). Role o seniority 1 są równorzędne między sobą
(brak uzasadnienia biznesowego do hierarchii wewnątrz tej grupy).

## Matryca: rola → dostępne moduły nawigacji

Widoczność linku w menu nie jest jedynym zabezpieczeniem — każda operacja zmieniająca stan jest
dodatkowo weryfikowana po stronie backendu.

| Moduł | Admin | Compliance | Doradca kredytowy | Analityk ryzyka | Windykator | Konsultant kontaktu |
|---|---|---|---|---|---|---|
| Pulpit | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Kredyty | ✅ | — | ✅ | ✅ | — | — |
| Ryzyko (ECL) | ✅ | — | — | ✅ | — | — |
| Windykacja | ✅ | — | — | — | ✅ | — |
| Kanał kontaktu | ✅ | — | — | — | — | ✅ |
| Compliance / Raportowanie | ✅ | ✅ | — | — | — | — |
| Bankowość elektroniczna | ✅ | — | ✅ | — | — | ✅ |
| Karty płatnicze | ✅ | — | — | — | — | ✅ |
| Administracja kont | ✅ | — | — | — | — | — |
| Audyt | ✅ | ✅ | — | — | — | — |
| Zatwierdzenia (kolejka) | ✅ | ✅ | — | — | — | — |
| Zajęcia komornicze | ✅ | — | — | — | ✅ | — |
| Sprawy klientów | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

Sprawy klientów nie mają dedykowanego uprawnienia — ekran jest widoczny dla każdego
zalogowanego użytkownika niezależnie od roli.

## Uwaga dotycząca bezpieczeństwa

Mechanizm uprawnień filtruje **wyłącznie widoczność menu** w interfejsie. Trasy (URL-e) w
`banking_web` są strzeżone jedynie wymogiem bycia zalogowanym, nie przypisaniem do modułu —
rzeczywistą warstwą kontroli dostępu są endpointy `banking_backend`, które odrzucają żądania
niezgodne z rolą lub poziomem seniority niezależnie od tego, czy link był widoczny w UI.
