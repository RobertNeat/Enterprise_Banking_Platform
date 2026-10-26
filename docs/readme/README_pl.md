<p align="center">
  <img src="../../apps/banking_web/public/colloseum.png" width="150" alt="HIS logo">
</p>

<h1 align="center">Enterprise Banking Platform</h1>

Enterprise Banking Platform (EBP) to modułowy system typu core-banking, modelujący back office banku detalicznego wraz z symulacjami instytucji zewnętrznych, z którymi się integruje. System obejmuje pełen cykl codziennych operacji bankowych — zarządzanie klientem i kontem, udzielanie kredytów, obsługę kart płatniczych, ocenę ryzyka kredytowego, bezpieczeństwo bankowości elektronicznej, egzekucję komorniczą, raportowanie regulacyjne oraz obsługę spraw klientów — wymuszając przy tym zasadę podwójnej kontroli (maker-checker) dla każdej istotnej operacji.


<p align="center">
  <a href="https://robertneat.github.io/Enterprise_Banking_Platform/">
    <img src="https://img.shields.io/badge/Documentation-white?style=for-the-badge&labelColor=white&color=white&logoColor=black" alt="Documentation">
  </a>
  <br>
  <a href="https://github.com/RobertNeat/Enterprise_Banking_Platform/blob/main/docs/readme/../../README.md">
    <img src="https://img.shields.io/badge/Angielska_wersja_dokumentu-white?style=for-the-badge" alt="Angielska wersja">
  </a>
</p>

## Zastosowane standardy bankowe

Platforma implementuje lub symuluje następujące standardy bankowe i regulacje finansowe:

- **IFRS 9** — klasyfikuje ekspozycje na ryzyko kredytowe do stage 1/2/3 oraz wylicza rezerwy na straty dla zabezpieczeń kredytowych.
- **PSD2 / SCA (Strong Customer Authentication)** — wymaga weryfikacji kodem jednorazowym przed wykonaniem wrażliwych operacji bankowości elektronicznej (odblokowanie dostępu, reset urządzenia).
- **FATCA (Foreign Account Tax Compliance Act)** — eksportuje raporty XML o kontach rezydentów podatkowych USA do symulowanej bramy urzędu skarbowego.
- **CRS (Common Reporting Standard)** — eksportuje analogiczne, międzynarodowe raporty podatkowe OECD, korzystając z tego samego mechanizmu co FATCA.
- **DORA (Digital Operational Resilience Act)** — prowadzi rejestr incydentów ICT z poziomami istotności, widoczny na panelu operacyjnym.
- **Nadzór KNF** — przekazuje zgłoszenia sprawozdawcze do symulowanej bramy krajowego nadzoru finansowego.
- **Raportowanie do NBP** — przekazuje sprawozdania do banku centralnego, w tym przykładowe sprawozdanie płynnościowe.
- **BFG** — przekazuje zgłoszenia do funduszu gwarantującego depozyty przez tę samą bramę sprawozdawczą.
- **LCR / NSFR (wskaźniki płynności Bazylea III)** — uwzględnione w treści przykładowego sprawozdania płynnościowego przekazywanego do NBP.
- **AML / Compliance** — dedykowana rola użytkownika „Compliance / AML" uczestnicząca w procesie zatwierdzania maker-checker.
- **Reguły schematów kartowych (Visa/Mastercard)** — standardowe kody przyczyn chargebacku (np. 4855, 4837, 4853, 4863) używane w procesie obsługi reklamacji kartowych.

## Co robi EBP

- **Uwierzytelnianie i uprawnienia** — kontrola dostępu oparta na rolach i poziomach seniorstwa (Administrator, Compliance, Kredyty, Ryzyko, Windykacja, Obsługa klienta), obsługiwana przez dedykowany serwis tożsamości.

<p align="center">
  <img src="../media/1_login_registration/register_and_login.png" width="80%" alt="logowanie i rejestracja">
</p>

- **Zatwierdzenia maker-checker** — każda istotna operacja (zatwierdzenie kredytu, transfer środków, wypłata z egzekucji) musi zostać zatwierdzona przez inną osobę niż ta, która ją zainicjowała — reguła wymuszona po stronie serwera.
- **Udzielanie kredytów** — przyjęcie wniosku, scoring w biurze informacji kredytowej, proces zatwierdzania oraz generowanie harmonogramu spłat rat annuitetowych.

<p align="center">
  <img src="../media/2_wniosek_kredytowy/wniosek_kredytory.gif" width="80%" alt="wniosek kredytowy">
</p>

- **Zarządzanie ryzykiem kredytowym** — klasyfikacja i tworzenie rezerw zgodnie z IFRS 9 dla zabezpieczeń, a także obsługa spraw i działań windykacyjnych.

<p align="center">
  <img src="../media/6_przeliczenie_ryzyka_ecl/przeliczanie_ryzyka.gif" width="80%" alt="przeliczanie ryzyka ECL">
</p>

- **Zarządzanie kartami** — blokowanie/odblokowywanie kart oraz zgłaszanie i rozstrzyganie chargebacków, z eskalacją incydentu przy niedostępności operatora kart.

<p align="center">
  <img src="../media/4_operacja_na_kartach_kredytowych/operacje_na_kartach_kredytwoych.gif" width="80%" alt="operacje na kartach kredytowych">
</p>

- **Bezpieczeństwo bankowości elektronicznej** — procesy chronione kodem jednorazowym (OTP/SCA) do odblokowywania dostępu klienta i resetowania zarejestrowanych urządzeń.

<p align="center">
  <img src="../media/5_odblokowanie_dostępu_i_reset_urządzenia/odblokowanie_dostępu_i_reset_urządzenia.gif" width="80%" alt="odblokowanie dostępu i reset urządzenia">
</p>

- **Egzekucja komornicza** — rejestracja zajęć majątkowych, blokowanie środków oraz rozliczenia kontrolowane zasadą maker-checker.

<p align="center">
  <img src="../media/7_rejestracja_zajęcia_komorniczego/rejestracja_zajęcia_komorniczego.gif" width="80%" alt="rejestracja zajęcia komorniczego">
</p>

- **Raportowanie regulacyjne** — przekazywanie sprawozdań do bram KNF/NBP/BFG oraz eksporty podatkowe FATCA/CRS.
- **Obsługa spraw klientów** — obsługa reklamacji z wyliczaniem ustawowych terminów oraz automatyczną lub manualną eskalacją na podstawie SLA.

<p align="center">
  <img src="../media/3_rozpoczęcie_sprawy_klienta/rozpoczęcie_sprawy_klienta.gif" width="80%" alt="rozpoczęcie sprawy klienta">
</p>

- **Panel operacyjny** — kluczowe wskaźniki płynności i portfela, alerty regulacyjne oraz najważniejsze incydenty ICT w jednym miejscu.
