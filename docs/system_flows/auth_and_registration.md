# Logowanie i rejestracja

## Scenariusz 1 — logowanie poprawnymi danymi

1. Otwórz `http://localhost:10500/login`.
2. Wpisz `employeeId=PRC-00001`, `password=Admin12345` (albo kliknij konto demo "admin" w
   popoverze, jeśli formularz go udostępnia).
3. Zatwierdź.

**Oczekiwany wynik**: przekierowanie na `/dashboard`, widoczne wszystkie sekcje menu.

## Scenariusz 2 — logowanie błędnym hasłem

1. `employeeId=PRC-00001`, `password=zlehaslo123`.

**Oczekiwany wynik**: backend `401` (`identity_access_mock` → `banking_backend` 1:1), komunikat
błędu logowania, brak przekierowania.

## Scenariusz 3 — logowanie z niepoprawnym formatem `employeeId`

1. `employeeId=12345` (bez prefiksu `PRC-`), `password=Admin12345`.

**Oczekiwany wynik**: walidacja klienta (`Validators.pattern(/^PRC-\d{5}$/)`) blokuje submit przed
wysłaniem żądania — pole oznaczone jako błędne, przycisk logowania nieaktywny/formularz nie wysyła
żądania.

## Scenariusz 4 — rejestracja nowego konta i zatwierdzenie przez administratora

1. Z `/login` przejdź do `/register`.
2. Wypełnij formularz:
   - **Dane osobowe**: `firstName=Ewa`, `lastName=Testowa`, `email=ewa.testowa@bank.test`,
     `phone=+48 600 000 000`
   - **Zatrudnienie**: `employeeId=PRC-99999`, `department` = wybierz z listy (`IT` / `Ryzyko
     Kredytowe` / `Obsługa Klienta` / `Compliance` / `Operacje`), `branch=Oddział Testowy`,
     `position=Tester`, `supervisor=PRC-00001`, `startDate` = dowolna data z kalendarza
   - **Profil branżowy**: `businessLines` — zaznacz co najmniej jedną opcję (`retail`/`corporate`/
     `risk`/`collections`/`compliance`/`it`), pola `certifications`/`licenseNumber` opcjonalne
   - **Konto**: `password=Testowe123`, `confirmPassword=Testowe123`, zaznacz `acceptPolicy`
3. Zatwierdź.

**Oczekiwany wynik**: toast o powodzeniu (informujący o konieczności zatwierdzenia przez
administratora), przekierowanie na `/login`. `identity_access_mock` zapisuje konto ze statusem
`PENDING_APPROVAL` i `role=null`. Próba zalogowania się nowym kontem (`PRC-99999`/`Testowe123`)
skończy się `403 Forbidden` ("Account is not active: PENDING_APPROVAL") do momentu zatwierdzenia.

Dalej jako administrator:

1. Zaloguj się jako `admin` (`PRC-00001`/`Admin12345`), przejdź do `/accounts`.
2. W sekcji "Oczekujące na zatwierdzenie" znajdź `PRC-99999`, kliknij "Zatwierdź", wybierz rolę
   (np. "Doradca kredytowy") i potwierdź.

**Oczekiwany wynik**: konto przechodzi do `ACTIVE` z wybraną rolą, przenosi się do sekcji
"Zatwierdzone i odrzucone". Logowanie `PRC-99999`/`Testowe123` teraz się udaje, zwraca wybraną
rolę i odpowiadające jej uprawnienia.

## Scenariusz 4b — odrzucenie zarejestrowanego konta

1. Powtórz rejestrację ze scenariusza 4 (inny `employeeId`, np. `PRC-99998`).
2. Jako `admin` na `/accounts` kliknij "Odrzuć" przy tym koncie.

**Oczekiwany wynik**: konto przechodzi do `REJECTED`. Logowanie tym kontem zawsze kończy się
`403 Forbidden` ("Account is not active: REJECTED"), z komunikatem o odrzuceniu rejestracji
(a nie "czeka na zatwierdzenie"). `employeeId` pozostaje zajęte — ponowna rejestracja tym samym
identyfikatorem zwraca `409 Conflict`.

## Scenariusz 5 — rejestracja z zajętym `employeeId`

1. Powtórz formularz ze scenariusza 4, ale `employeeId=PRC-00001` (już istnieje).

**Oczekiwany wynik**: `409 Conflict` z `identity_access_mock` ("employeeId already registered"),
komunikat błędu na formularzu, brak przekierowania.

## Scenariusz 6 — rejestracja z niezgodnymi hasłami

1. `password=Testowe123`, `confirmPassword=Inne12345`.

**Oczekiwany wynik**: walidacja cross-field (`passwordsMatchValidator`) blokuje submit po stronie
klienta, zanim żądanie trafi do backendu.
