<p align="center">
  <img src="apps/banking_web/public/colloseum.png" width="150" alt="HIS logo">
</p>

<h1 align="center">Enterprise Banking Platform</h1>

Enterprise Banking Platform (EBP) is a modular core-banking system that models the back office of a retail bank together with mock simulations of the external institutions it depends on. It covers the full lifecycle of a bank's day-to-day operations — customer and account management, lending, card processing, credit risk, e-banking security, debt enforcement, regulatory reporting, and customer case handling — while enforcing dual-control (maker-checker) approval on every sensitive operation.

> 🇵🇱 Polska wersja tego dokumentu: [docs/readme/README_pl.md](docs/readme/README_pl.md)

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
- **Maker-checker approvals** — every sensitive operation (loan approval, fund transfer, enforcement payout) must be approved by a different user than the one who initiated it, enforced server-side.
- **Loan origination** — application intake, credit-bureau scoring, approval workflow, and annuity repayment schedule generation.
- **Credit risk management** — IFRS 9 staging and provisioning for collateral, plus collections case tracking and actions.
- **Card management** — card block/unblock and chargeback filing and resolution, with incident escalation when the card processor is unavailable.
- **E-banking security** — OTP/SCA-protected flows for unlocking customer access and resetting registered devices.
- **Debt enforcement (bailiff proceedings)** — registration of asset seizures, fund freezing, and maker-checker-controlled settlement transfers.
- **Regulatory reporting** — submission of reports to the KNF/NBP/BFG gateways and FATCA/CRS tax exports.
- **Customer case management** — complaint handling with statutory deadline calculation and SLA-based automatic or manual escalation.
- **Operations dashboard** — key liquidity and portfolio indicators, regulatory alerts, and top ICT incidents at a glance.
