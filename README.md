<p align="center">
  <img src="apps/banking_web/public/colloseum.png" width="150" alt="HIS logo">
</p>

<h1 align="center">Enterprise Banking Platform</h1>

Enterprise Banking Platform (EBP) is a modular core-banking system that models the back office of a retail bank together with mock simulations of the external institutions it depends on. It covers the full lifecycle of a bank's day-to-day operations — customer and account management, lending, card processing, credit risk, e-banking security, debt enforcement, regulatory reporting, and customer case handling — while enforcing dual-control (maker-checker) approval on every sensitive operation.

<p align="center">
  <a href="https://robertneat.github.io/Enterprise_Banking_Platform/">
    <img src="https://img.shields.io/badge/Documentation-white?style=for-the-badge&labelColor=white&color=white&logoColor=black" alt="Documentation">
  </a>
  <br>
  <a href="https://github.com/RobertNeat/Enterprise_Banking_Platform/blob/main/docs/readme/README_pl.md">
    <img src="https://img.shields.io/badge/Polska_wersja_dokumentu-white?style=for-the-badge" alt="Polska wersja">
  </a>
</p>

## Enterprise banking standards used

The platform implements or simulates the following banking and financial regulatory standards:

- **IFRS 9** — classifies credit risk exposures into stage 1/2/3 and computes loss provisions for loan collateral.
- **PSD2 / SCA (Strong Customer Authentication)** — requires one-time-password verification before sensitive e-banking actions (device unlock, device reset) are executed.
- **FATCA (Foreign Account Tax Compliance Act)** — exports XML reports on US tax-resident accounts to the simulated tax authority gateway.
- **CRS (Common Reporting Standard)** — exports the equivalent OECD cross-border tax information reports, using the same reporting mechanism as FATCA.
- **DORA (Digital Operational Resilience Act)** — maintains an ICT incident registry with severity levels, surfaced on the operations dashboard.
- **KNF (Polish Financial Supervision Authority) reporting** — submits regulatory filings to the simulated national financial supervisor gateway.
- **NBP (National Bank of Poland) reporting** — submits filings to the central bank, including a sample liquidity report.
- **BFG (Bank Guarantee Fund) reporting** — submits filings to the deposit guarantee fund via the same regulatory gateway.
- **LCR / NSFR (Basel III liquidity ratios)** — referenced in the sample liquidity report submitted to the NBP gateway.
- **AML / Compliance** — dedicated "Compliance / AML" user role participating in the maker-checker approval workflow.
- **Card scheme chargeback rules (Visa/Mastercard)** — standard chargeback reason codes (e.g. 4855, 4837, 4853, 4863) used in the card dispute lifecycle.

## What EBP does

- **Authentication & authorization** — role-based access control with seniority levels (Admin, Compliance, Loans, Risk, Collections, Contact), backed by a dedicated identity service.

<p align="center">
  <img src="docs/media/1_login_registration/register_and_login.png" width="80%" alt="login and register">
</p>

- **Maker-checker approvals** — every sensitive operation (loan approval, fund transfer, enforcement payout) must be approved by a different user than the one who initiated it, enforced server-side.
- **Loan origination** — application intake, credit-bureau scoring, approval workflow, and annuity repayment schedule generation.

<p align="center">
  <img src="docs/media/2_wniosek_kredytowy/wniosek_kredytory.gif" width="80%" alt="loan application">
</p>

- **Credit risk management** — IFRS 9 staging and provisioning for collateral, plus collections case tracking and actions.

<p align="center">
  <img src="docs/media/6_przeliczenie_ryzyka_ecl/przeliczanie_ryzyka.gif" width="80%" alt="ECL risk recalculation">
</p>

- **Card management** — card block/unblock and chargeback filing and resolution, with incident escalation when the card processor is unavailable.

<p align="center">
  <img src="docs/media/4_operacja_na_kartach_kredytowych/operacje_na_kartach_kredytwoych.gif" width="80%" alt="credit card operations">
</p>

- **E-banking security** — OTP/SCA-protected flows for unlocking customer access and resetting registered devices.

<p align="center">
  <img src="docs/media/5_odblokowanie_dostępu_i_reset_urządzenia/odblokowanie_dostępu_i_reset_urządzenia.gif" width="80%" alt="access unlock and device reset">
</p>

- **Debt enforcement (bailiff proceedings)** — registration of asset seizures, fund freezing, and maker-checker-controlled settlement transfers.

<p align="center">
  <img src="docs/media/7_rejestracja_zajęcia_komorniczego/rejestracja_zajęcia_komorniczego.gif" width="80%" alt="bailiff seizure registration">
</p>

- **Regulatory reporting** — submission of reports to the KNF/NBP/BFG gateways and FATCA/CRS tax exports.
- **Customer case management** — complaint handling with statutory deadline calculation and SLA-based automatic or manual escalation.

<p align="center">
  <img src="docs/media/3_rozpoczęcie_sprawy_klienta/rozpoczęcie_sprawy_klienta.gif" width="80%" alt="customer case handling">
</p>

- **Operations dashboard** — key liquidity and portfolio indicators, regulatory alerts, and top ICT incidents at a glance.
