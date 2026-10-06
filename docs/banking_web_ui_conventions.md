# Konwencje UI — banking_web

## Identyfikatory HTML

Każdy znaczący element w szablonach HTML (`*.page.html`) — w tym każdy
przycisk, input, link i komponent — ma atrybut `id`, aby umożliwić łatwą
identyfikację elementu w strukturze DOM i odnalezienie go w kodzie źródłowym.
Elementy czysto dekoracyjne/wewnętrzne (np. `<td>`, ikony, ripple-effecty
PrimeNG) są pomijane.

### Schemat nazewnictwa

- Prefiks strony: kebab-case nazwa cechy, np. `cases-`, `loans-`, `dashboard-`.
- Sekcje statyczne: `<prefix>-page-header`, `<prefix>-title`, `<prefix>-table`,
  `<prefix>-toolbar` itd.
- Wiersze list/tabel renderowane w pętli: `<prefix>-row-{{ item.id }}`, gdzie
  `item.id` to unikalny identyfikator biznesowy rekordu (nigdy wolny tekst,
  np. nazwa klienta czy etykieta — może zawierać znaki łamiące selektory CSS).
- Jeśli model danych nie ma pola `id` (np. wiersze harmonogramu spłat,
  historia kontaktu, permission chipy), używany jest indeks z pętli
  (`row.no`, `let i = $index`, lub `let-i="rowIndex"` w `#body` tabeli
  `p-table`, bo `$index` nie jest tam bezpośrednio dostępne).
- Przyciski akcji w wierszu: `<prefix>-row-{{ item.id }}-<akcja>-btn`.

### Komponenty współdzielone (`app-kpi-card`, `app-status-tag`)

Przyjmują dedykowane wejścia `cardId` / `tagId` (nie `id`), aby wywołujący
mógł nadać identyfikator bez kolizji z natywnym atrybutem `id` Angulara.
Host komponentu (np. `<app-kpi-card>`) dostaje bazowe id (przez
`host: { '[attr.id]': 'cardId()' }`), a widoczny element wewnętrzny dostaje
to samo id z sufiksem (np. `-card`, `-tag`) — dzięki temu komponent da się
znaleźć zarówno po selektorze hosta, jak i po widocznym elemencie, bez
duplikatu id w DOM.

### PrimeNG: host vs. realny widoczny/interaktywny element

Część komponentów PrimeNG renderuje swój Angularowy host jako sekcję `root`
(`data-pc-section="root"`) — wtedy zwykłe `[id]` na hoście trafia na właściwy,
widoczny element (`p-tag`, `p-chip`, `p-select`, `p-table`, `p-chart`,
`p-progressbar`, `p-iconfield`, `p-inputicon`).

Inne komponenty renderują host jako sekcję `host`, a realny widoczny/
interaktywny element jest w środku (`p-button` → wewnętrzny `<button>`,
`p-step` → wewnętrzny `<button class="p-step-header">`). W takich wypadkach
samo `[id]` na hoście nie wystarcza — trzeba dodatkowo użyć pass-through
(`pt`), żeby ustawić id na realnym elemencie:

```html
<p-button
  id="cases-new-item-btn"
  [pt]="{ root: { id: 'cases-new-item-btn-root' } }"
  ...
/>
```

Sekcja `pt` do użycia zależy od komponentu — sprawdzaną wartość
`data-pc-section` danego elementu w żywym DOM (devtools) i tam kierować
`pt.<sekcja>.id`. Przykłady z tego projektu:
- `p-button` → `pt.root` (wewnętrzny `<button>`).
- `p-step` → `pt.header` (wewnętrzny `<button class="p-step-header">`).
- `p-select` → host ma już id na widocznym elemencie, ale dropdown-trigger
  (`div[role="button"]`) wymaga `pt.dropdown`.
- `p-toast` → host ma id na widocznym kontenerze, ale przycisk zamykania
  komunikatu (`<button data-pc-section="closebutton">`, widoczny tylko gdy
  aktywny jest komunikat) wymaga `pt.closeButton`.
- `p-splitbutton` → główny przycisk i przycisk-dropdown to osobne directivy
  `pButton` (nie komponenty), ich sekcja `pt` to zagnieżdżone
  `pcButton.root` / `pcDropdown.root` (nie `pcButton.id` wprost):
  `[pt]="{ pcButton: { root: { id: '...-btn' } }, pcDropdown: { root: { id: '...-dropdown-btn' } } }"`.
  Pozycje wewnętrznego menu (`MenuItem[]`) dostają id wprost przez pole
  `MenuItem.id` — trafia jako atrybut `id` na renderowany `<li>`.
- `p-datepicker` → `inputId` (jak `p-password`) na realnym `<input>`, ale
  przycisk-kalendarz to osobny realny `<button>` wymagający `pt.dropdown`.

Komponenty z dedykowanym wejściem `inputId` (`p-password`, `p-datepicker`,
`p-checkbox` z `[binary]="true"`) NIE wymagają `pt` dla samego inputu —
`inputId`/`binary`-checkbox input ustawia `[attr.id]` bezpośrednio na
realnym `<input>`. To prostsze niż `pt` i jest publicznym, zalecanym API tych
komponentów. `p-multiselect` i `p-select` przyjmują zwykły `[id]` na hoście,
bo ich host = widoczny combobox.

Zasada nazewnictwa: host/visible-root dostaje bazowe id, wewnętrzny element
z `pt` dostaje sufiks `-root` (lub nazwę sekcji, np. `-header`,
`-dropdown-btn`).

### CSS celujący w elementy wewnętrzne PrimeNG

CSS-owe custom properties (zmienne, np. `--p-button-primary-color`) dziedziczą
się przez drzewo DOM niezależnie od Angularowej Emulated encapsulation, więc
nadpisywanie wyglądu przez zmienne na selektorze hosta zawsze działa.
Zwykłe właściwości (`cursor`, `pointer-events`, `background` itd.) kierowane
na id realnego elementu wewnątrz szablonu PrimeNG NIE działają przez zwykły
scoped selektor — ten element jest poza zasięgiem encapsulation tego
komponentu. W takich przypadkach jedyne działające rozwiązanie w tym projekcie
to `::ng-deep` (potwierdzone empirycznie: reguła bez `::ng-deep` nie
zmieniała `getComputedStyle` w żywym DOM, z `::ng-deep` — zmieniała).
Używać `::ng-deep` oszczędnie, zawsze ze specyficznym selektorem id (nigdy
generycznym selektorem klasy PrimeNG), żeby nie przeciekało poza zamierzony
element.

### Proces z jednym aktywnym krokiem (lifecycle)

Do pokazywania procesu z dokładnie jednym aktywnym krokiem (np. cykl życia
wniosku kredytowego w `loan-detail.page.html`) używany jest `p-stepper`
(`p-step-list` + `p-step[value]`, `[disabled]="true"` na każdym kroku, żeby
był tylko wizualnym wskaźnikiem, nie nawigacją) zamiast ręcznie rysowanej
listy `<ol>` — unika to efektu nakładającej się linii łączącej na etykietach
kroków. Dla zwykłych długich formularzy (np. rejestracja pracownika) wybrano
inne podejście — patrz niżej.

### Routing: trasy auth (bez shella) vs. trasy w shellu

`/login` i `/register` są zdefiniowane jako samodzielne trasy top-level w
`app.routes.ts`, OBOK (nie wewnątrz) węzła `{ path: '', component: ShellPage,
children: [...] }`, który grupuje wszystkie pozostałe strony biznesowe. Dzięki
temu ekrany logowania/rejestracji renderują się bez topbara/sidebara. `p-toast`
jest zdefiniowany raz, globalnie w `app.ts` (nie w `shell.page.html`), żeby
działał identycznie na stronach auth i w shellu — `MessageService` jest
root-provided w `app.config.ts`, więc jedna instancja `p-toast` wystarcza
dla całej aplikacji.

### Logowanie demo — konta testowe bez prawdziwego backendu auth

Aplikacja nie ma prawdziwego backendu auth (patrz sekcja routingu powyżej —
`onSubmit()` w `login.page.ts`/`register.page.ts` tylko nawiguje, nic nie
zapisuje). Żeby nie trzeba było ręcznie wpisywać danych logowania przy
testowaniu różnych ról, `login.page.html` ma ikonkę `pi-info-circle` w
prawym górnym rogu panelu (`#login-demo-accounts-trigger`), która otwiera
`p-popover` z listą kont demo (`DEMO_ACCOUNTS` w `login.page.ts` — rola +
identyfikator + hasło). Kliknięcie pozycji wypełnia formularz
(`form.setValue(...)`) i zamyka popover; użytkownik nadal musi sam kliknąć
`login-submit-btn-root`, żeby się "zalogować" (przekierowanie na
`/dashboard`) — wybór z listy tylko uzupełnia pola, nie submituje automatycznie.

PrimeNG 21: `OverlayPanel` został przemianowany na `Popover`
(`primeng/popover`, selektor `p-popover`, API `toggle(event)`/`hide()` bez
zmian) — `OverlayPanel`/`OverlayPanelModule` już nie istnieją w tej wersji.

**Pułapka z `viewChild` + template reference variable**: jeśli pole klasy
(`protected readonly demoAccountsPopover = viewChild<Popover>('...')`) i
template-ref (`#demoAccountsPopover` w HTML) mają tę samą nazwę, Angular
myli się przy generowaniu kodu szablonu i w runtime rzuca
`TypeError: X is not a function` przy próbie wywołania. Rozwiązanie: albo
nazwać je różnie, albo (preferowane tutaj) wywoływać metody overlay
bezpośrednio przez template-ref jako lokalną zmienną w markupie
(`(click)="popoverRef.toggle($event)"` z `#popoverRef` na `<p-popover>`),
bez `viewChild` w ogóle, chyba że dostęp z TS jest faktycznie potrzebny
(np. `selectDemoAccount()` woła `hide()` programowo po wyborze — tam
`viewChild` zostaje, ale pod jednoznaczną nazwą).

### Długi formularz (rejestracja) — sekcje w karcie, nie wizard

`register.page.html` to jeden płaski `<form>` z kilkoma logicznymi sekcjami
(`formGroupName`), każda renderowana jako osobna karta (`.register-section`:
tło + border + radius, nie tylko linia-separator) z nagłówkiem i ikoną `pi`.
Pola krótkie (imię, telefon, id) są w siatce 2-kolumnowej
(`grid-template-columns: 1fr 1fr`), pola z potencjalnie dłuższą treścią
(email, nazwa oddziału, multiselect, pola opcjonalne tekstowe) dostają klasę
`.field--wide` (`grid-column: 1 / -1`), żeby nie marnować poziomej przestrzeni
na krótkich polach ani nie ścieśniać długich. Świadomie NIE użyto `p-stepper`
tutaj — w przeciwieństwie do lifecycle (dokładnie jeden aktywny krok na raz),
to jest formularz rejestracyjny, gdzie użytkownik korzysta z przeglądarkowego
"znajdź" / przewija całość, a wizard tylko wydłużałby proces bez potrzeby.
Walidacja działa per-pole (`touched && invalid`), submit blokowany dopóki cały
`FormGroup` jest invalid (`[disabled]="form.invalid"` na przycisku).

### Zasady

- Każdy `id` musi być unikalny w całym renderowanym DOM (shell + strona
  routowana współdzielą dokument).
- `id` nie może zawierać spacji, kropek ani ukośników — segmenty tras
  (`/loans`) są oczyszczane przed użyciem w `id`, a etykiety z wolnym tekstem
  (np. nazwy uprawnień) zastępowane indeksem pętli.
- Każdy element interaktywny (`button`, `input`, `a`, `select`, `textarea`,
  `[role="button"]`) musi mieć realne id na elemencie, który faktycznie
  renderuje się w DOM jako ten element — nie wystarczy id na opakowującym
  go custom-elemencie Angulara, jeśli host ≠ widoczny element (patrz sekcja
  powyżej).
- Weryfikacja wykonywana jest w żywej aplikacji (`document.querySelectorAll`)
  dla każdej trasy: brak duplikatów, brak niedozwolonych znaków, zero
  elementów interaktywnych bez id.
