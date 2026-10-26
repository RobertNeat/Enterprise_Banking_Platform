---
layout: default
title: Enterprise Banking Platform — Wiki
summary: Dokumentacja funkcjonalna ekranów systemu banking_web — kto, co i jakimi danymi.
---

Enterprise Banking Platform (EBP) to modułowy system core-bankingowy obsługujący cały cykl
operacji bankowych: zarządzanie klientem, kredyty, karty płatnicze, ryzyko kredytowe,
bankowość elektroniczną, egzekucję komorniczą, raportowanie regulacyjne oraz obsługę spraw
klientów — z wymuszoną zasadą podwójnej kontroli (maker-checker) dla operacji o istotnym
ryzyku.

Ta wiki opisuje **każdy ekran aplikacji `banking_web`** z trzech perspektyw:

1. **Jakie funkcje można wykonać** z danej lokalizacji (ekranu/strony).
2. **Jaka rola użytkownika** może wykonać daną funkcję.
3. **Jakie dane/pola** są wprowadzane na danym ekranie (typ, wymagalność, walidacja).

## Zacznij tutaj

- [Role i uprawnienia](wiki/role-i-uprawnienia/) — pełna matryca: rola → moduły → poziom
  decyzyjności (seniority).
- [Zasada maker-checker](wiki/maker-checker/) — jak działa rozdzielenie wykonawcy i
  zatwierdzającego.
- [Ślad audytowy i kreatory](wiki/audyt-i-kreatory/) — wspólne wzorce UI powtarzające się na
  wielu ekranach.

## Ekrany systemu

| Ekran | Skrót opisu |
|---|---|
| [Logowanie i rejestracja](wiki/logowanie-i-rejestracja/) | Uwierzytelnienie pracownika, samodzielna rejestracja konta |
| [Pulpit (Dashboard)](wiki/pulpit/) | KPI banku, wykresy portfela, alerty regulacyjne, incydenty ICT |
| [Kredyty i pożyczki](wiki/kredyty/) | Wnioski kredytowe, scoring, cykl życia, harmonogram spłat |
| [Sprawy klientów](wiki/sprawy-klientow/) | Reklamacje, terminy ustawowe, eskalacja SLA |
| [Karty płatnicze](wiki/karty/) | Blokada/odblokowanie, autoryzacje, chargeback |
| [Bankowość elektroniczna](wiki/bankowosc-elektroniczna/) | Odblokowanie dostępu i reset urządzenia z OTP (PSD2/SCA) |
| [Ryzyko kredytowe (ECL)](wiki/ryzyko-ecl/) | Zabezpieczenia, koszyki IFRS 9, rezerwy |
| [Kanał kontaktu](wiki/kanal-kontaktu/) | Widok 360 klienta, dyspozycje, historia kontaktów |
| [Zajęcia komornicze](wiki/zajecia-komornicze/) | Rejestracja zajęć, przekazanie środków organowi |
| [Windykacja](wiki/windykacja/) | Wezwania do zapłaty, ugody, przekazanie zewnętrzne |
| [Raportowanie regulacyjne](wiki/raportowanie/) | Raporty do KNF/NBP/BFG/FATCA-CRS, eksport CSV |
| [Zatwierdzenia (kolejka)](wiki/zatwierdzenia/) | Decyzje maker-checker dla kredytów i egzekucji |
| [Administracja kont](wiki/administracja-kont/) | Zatwierdzanie rejestracji pracowników i przypisanie roli |

## Źródła

Treść tej wiki jest oparta na katalogu formularzy i uprawnień systemu
(`docs/development_docs/banking_web_functions(concise).md`, `RolePermissions.java`,
`RoleCode.java`, `nav.model.ts`) oraz na nagraniach i zrzutach ekranu w `docs/media/`.
