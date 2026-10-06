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

### Tagi/chipy statusowe — `app-status-tag` jako jedyny sposób renderowania

Każda etykieta statusowa lub kategoryzująca (status sprawy, etap procesu,
priorytet, kanał, processor/sieć karty, uprawnienie e-bankowości) renderuje
się przez `app-status-tag` (`shared/components/status-tag.component.ts`) —
nigdy przez surowy `p-tag` czy `p-chip` wprost w szablonie strony. Dzięki
temu jeden komponent kontroluje wygląd (radius, padding, typografia) i jedno
miejsce odpowiada za mapowanie znaczenia biznesowego na `severity`.

- `labelKey` — klucz i18n (większość przypadków, np. `'cases.status.' + status`).
- `label` — surowy tekst, gdy wartość NIE jest kluczem i18n (nazwa sieci
  karty/processora, nazwa uprawnienia e-bankowości, priorytet). Ma priorytet
  nad `labelKey`, jeśli oba są podane.
- `severity` — zawsze pochodzi z mapy `Record<Status, StatusSeverity>`,
  nigdy z inline ternary w szablonie.

**Gdzie żyją mapy severity.** Każda domena biznesowa trzyma swoją mapę przy
własnym modelu (konwencja ustalona zanim ten dokument powstał, zachowana
dla nowych map): `CASE_SEVERITY` (`cases.model.ts`), `CARD_STATUS_SEVERITY`
(`cards.model.ts`), `LOAN_STAGE_SEVERITY` / `INSTALLMENT_SEVERITY`
(`loans.model.ts`), `IFRS9_SEVERITY` (`risk.model.ts`), `ACCESS_SEVERITY`
(`ebanking.model.ts`), `REPORT_SEVERITY` (`reporting.model.ts`). Strony, które
odnoszą się do statusu innej domeny (np. dashboard pokazuje sprawy i raporty
regulacyjne, contact-center pokazuje dyspozycje powiązane ze sprawami),
importują i reużywają tę samą mapę — nie definiują własnej kopii z inną
semantyką dla tego samego stringa statusu.

`shared/utils/status-severity.ts` jest punktem odniesienia dla wartości
współdzielonych między wiele domen, a nie przypisanych do jednej: re-export
typu `StatusSeverity`, `NEUTRAL_TAG_SEVERITY` (etykiety kategoryzujące, które
nie są statusem — kanał, processor/sieć karty, uprawnienie) i
`PRIORITY_SEVERITY` (priorytet sprawy, skala low/medium/high używana w kilku
modułach).

**Parametryzacja wyglądu.** Kształt wszystkich tagów (radius, padding,
font-size, font-weight) sterowany jest zmiennymi `--tag-*` w `:root`
(`styles.css`), zmapowanymi na tokeny designu PrimeNG Tag (`--p-tag-*`) przez
selektor hosta `app-status-tag` — bez `::ng-deep`, bo custom properties
dziedziczą się przez encapsulation. Zmiana jednej zmiennej w `:root`
przestylowuje globalnie wszystkie tagi. Kolory per `severity` (success/
info/warn/danger/secondary/contrast) pochodzą z domyślnych tokenów
komponentu Tag theme Nora, poza `primary`/`secondary`/`contrast`, które
pochodzą z `semantic.primary` i `semantic.colorScheme` w `bank-preset.ts`;
wszystkie adaptują się do `.app-dark` automatycznie — nie są hardkodowane.

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
- `p-dialog` → przycisk zamykania (X) to wewnętrzny `p-button` (komponent,
  nie directywa `pButton`), więc wzorem `p-splitbutton` wymaga
  zagnieżdżonego `pt.pcCloseButton.root.id` (nie `pt.closeButton` wprost —
  sekcja w `DialogClasses` nazywa się `pcCloseButton`).
- `p-confirmdialog` → przyciski accept/reject to też wewnętrzne `p-button`,
  ten sam wzorzec: `pt.pcAcceptButton.root.id` / `pt.pcRejectButton.root.id`
  (sekcje `ConfirmDialogClasses.pcAcceptButton`/`pcRejectButton`).

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

### Proces z wieloma nawigowalnymi krokami (wizard) — `app-wizard-dialog`

Do procesów tworzenia nowego rekordu wymagających kilku logicznych kroków
(np. nowy wniosek kredytowy, nowe zajęcie komornicze, nowa sprawa klienta)
używany jest współdzielony komponent `shared/components/wizard-dialog.component.ts`
(`app-wizard-dialog`) — `p-dialog` + `p-stepper` z krokami **nawigowalnymi**
(Next/Back), w przeciwieństwie do read-only steppera lifecycle opisanego
wyżej (tam krok jest dokładnie jeden, bez nawigacji, `[disabled]="true"` na
każdym `p-step`). Komponent odpowiada tylko za layout, nawigację i
blokowanie Next/Submit przez `[stepValid]` — logika formularza i danych
zostaje całkowicie w stronie wywołującej.

**Kontrakt komponentu** (inputs/outputs): `dialogId` (prefiks id dla
wszystkich elementów wewnętrznych — step-list, przyciski, close button),
`titleKey` (klucz i18n nagłówka dialogu), `steps: WizardStep[]` (każdy z
`labelKey`), `visible`, `stepValid` (bool — wynik walidacji aktualnego
kroku, liczony przez stronę), `(visibleChange)`, `(stepChange)` (numer
nowego aktywnego kroku), `(cancel)`, `(submit)`.

**Wzorzec strony korzystającej z wizarda** (patrz `enforcement.page.ts`,
`cases.page.ts`, `loans.page.ts`):
- Reactive Forms (`FormBuilder`/`Validators` z `@angular/forms`) — NIE
  signal forms. Formularz dzielony na zagnieżdżone `FormGroup` per krok
  (np. `details`, `amounts`), żeby łatwo sprawdzić walidność jednego kroku
  (`this.form.controls.details.valid`) bez walidowania całości. Projected
  content owinięty w `<div [formGroup]="form">` (NIE `<form>`, żeby natywny
  event `submit`/Enter nie wyemitował fałszywie output `(submit)` wizarda
  przez bubbling), a grupa danego kroku podłączona przez
  `formGroupName="details"` na elemencie wewnątrz — **nigdy**
  `[formGroup]="form.controls.details"` zagnieżdżone w drugim elemencie z
  `[formGroup]`: Angular dopuszcza taki zapis składniowo, ale zagnieżdżona
  druga directywa `formGroup` tworzy osobny kontekst, przez co pola nadal
  działają wizualnie, lecz walidacja/wartości nie zawsze propagują się
  przewidywalnie do rodzica — źródło trudnych do zdiagnozowania błędów typu
  "pola wypełnione, ale Next nieaktywny".
- Strona trzyma własny sygnał `wizardStep` (numer aktywnego kroku),
  synchronizowany z wizardem przez `(stepChange)="wizardStep.set($event)"`
  — **nie** czyta kroku bezpośrednio z wewnętrznego stanu komponentu przy
  każdym renderze, bo treść kroków jest projektowana przez jedyny
  `<ng-content />` wizarda i strona musi sama decydować, co pokazać
  (`@switch (wizardStep())`), pokazując TYLKO treść aktywnego kroku. Ten
  sam sygnał (nie wywołanie metody na komponencie wizarda) jest jedynym
  źródłem prawdy o aktywnym kroku na stronie.
- `stepValid` liczone jako `computed()` na podstawie `wizardStep()` i
  walidności odpowiedniej grupy (`switch` po numerze kroku); na ostatnim
  kroku (podsumowanie) zwraca `form.valid`. **Pułapka:** trigger
  re-ewaluacji musi być `toSignal(this.form.valueChanges, ...)`, NIE
  `toSignal(this.form.statusChanges, ...)` — status formularza
  (`VALID`/`INVALID`) nie zmienia się przy każdej zmianie wartości pola
  (np. wypełnienie kroku 1 gdy krok 2 jest jeszcze pusty: root form
  zostaje `INVALID` przed i po), więc `toSignal` z domyślną dedupikacją
  `Object.is` nigdy nie wykryje zmiany i `computed()` się nie przeliczy —
  Next/Submit zostaje trwale wyszarzony mimo poprawnie wypełnionych pól
  aktywnego kroku, bez żadnego komunikatu błędu. `valueChanges` emituje
  nowy obiekt wartości przy każdej zmianie, więc problem nie występuje.
  Ten sam sygnał napędza `summary` (podsumowanie musi odświeżać się po
  Back → edycja → Next).
- Pola formularza w krokach grupowane w `<div class="wizard-field-group">`
  (per krok) z każdym polem w `<div class="field">` — te klasy są
  zdefiniowane GLOBALNIE w `styles.css` (nie per-strona, nie w
  `wizard-dialog.component.ts`, bo style komponentu nie dotykają contentu
  projektowanego z `<ng-content>` pod Emulated encapsulation), żeby odstępy
  między polami były identyczne na wszystkich stronach z wizardem. Etykieta
  pola wymaganego ma klasę `label.required`, która dokleja gwiazdkę przez
  `::after { content: ' *' }` **bez ustawionego koloru** (gwiazdka
  dziedziczy kolor tekstu etykiety — to standardowe oznaczenie
  wymagalności, nie błąd, więc nie czerwona). Błąd walidacji pola to
  `<small class="error">` pod inputem, widoczny gdy `control.invalid &&
  control.touched` (ten sam wzorzec co `register.page.html`), z treścią z
  kluczy `wizard.validation.required` / `.min` / `.max` / `.minLength`
  (wspólne klucze, parametryzowane przez pipe: `'wizard.validation.min' |
  translate: { min: 0 }`).
- `[fluid]="true"` na każdym polu formularza (input/p-select/p-inputnumber/
  p-datepicker) w kroku wizarda, żeby pole wypełniało szerokość kolumny
  niezależnie od długości treści — bez tego pola o różnej długości
  domyślnej szerokości component'u wyglądają nierówno w jednej kolumnie.
- Otwieranie wizarda (`openWizard()`) resetuje formularz do wartości
  domyślnych, `wizardStep.set(0)`, oraz `viewChild(WizardDialogComponent)`
  → `wizard().reset()` (resetuje wewnętrzny `activeIndex` komponentu) —
  inaczej po wcześniejszym submit/cancel wizard otworzyłby się z
  niezsynchronizowanym krokiem. To samo resetowanie powtórzone w
  `cancelWizard()`/`submitWizard()`, żeby kolejne otwarcie nigdy nie
  pokazało danych z poprzedniej sesji wizarda.
- Ostatni krok to zawsze read-only podsumowanie wprowadzonych danych
  (`dl/dt/dd` lub `.field-grid`) przed zatwierdzeniem; statusy/priorytety w
  podsumowaniu renderowane przez `app-status-tag`, nigdy surowym tekstem z
  kolorem inline.
- Dane nowego rekordu: jeśli lista jest czytana w więcej niż jednym miejscu
  (np. `loans.page.ts` i `loan-detail.page.ts` oba czytają wnioski
  kredytowe), źródło danych żyje w dedykowanym store
  (`@Injectable({ providedIn: 'root' })`, signal + `add()`/`findById()`,
  patrz `loans.store.ts`), nie w lokalnym sygnale jednej strony. Jeśli
  lista ma tylko jednego konsumenta (enforcement, cases), wystarczy lokalny
  `signal<T[]>` w komponencie stronicowym, aktualizowany przez
  `update(list => [newItem, ...list])`.
- `id` nowego rekordu generowany lokalnie (`<PREFIX>-<losowe cyfry>`,
  sprawdzana unikalność względem istniejących `id`), bo nie ma prawdziwego
  backendu.

**Potwierdzenie przerwania procesu.** Każde zamknięcie wizarda bez
zatwierdzenia — przycisk Cancel, X w headerze dialogu, klawisz Escape —
przechodzi przez jedno potwierdzenie (`p-confirmdialog`, `key` unikalny per
wizard: `dialogId() + '-cancel-confirm'`), zaimplementowane WEWNĄTRZ
`wizard-dialog.component.ts` (nie per-strona), żeby zachowanie było
identyczne we wszystkich wizardach. Mechanizm: `[visible]` na `p-dialog` w
komponencie jest bindowane JEDNOSTRONNIE z sygnału strony (`visible()`), a
nie `[(visible)]` dwustronnie — dzięki temu, gdy PrimeNG próbuje zamknąć
dialog samo (X, Escape) i emituje `(visibleChange)` z `false`, handler
komponentu (`onVisibleChange`) może po prostu NIE przepuścić tej wartości
do strony i wywołać `requestCancel()` zamiast — w kolejnym cyklu detekcji
zmian Angular ponownie wymusza `[visible]="true"` z sygnału strony, więc
dialog wraca widoczny, a realne zamknięcie następuje tylko po
`accept()` potwierdzenia (który emituje `(cancel)` do strony). Dzięki temu
jest jedna ścieżka zamykania, nie trzy.

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
