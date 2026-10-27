# Nawigacja – zakładki, stosy, przepływy i blokada aplikacji

Ten dokument opisuje, **jak zbudowana jest nawigacja w aplikacji z dolnymi zakładkami**: jaki jest model stanu, jak zachowuje się przy każdej akcji użytkownika, jak jest opakowany Navigation 3, gdzie lądują ekrany otwierane z linków, czym są przepływy pełnoekranowe i jak działa blokada PIN po powrocie z tła.

Deeplinki (parser, guardy, polityka back stacku) mają osobny dokument: [deeplinks.md](deeplinks.md).
Przekazywanie argumentów, typowane ID i zwracanie wyników między ekranami: [navigation-arguments.md](navigation-arguments.md).

---

## Spis treści

1. [Zasady w pigułce](#1-zasady-w-pigułce)
2. [Model stanu](#2-model-stanu)
3. [Warstwy i klasy](#3-warstwy-i-klasy)
4. [Zachowanie – akcja po akcji](#4-zachowanie--akcja-po-akcji)
5. [Gdzie ląduje nowy ekran](#5-gdzie-ląduje-nowy-ekran)
6. [Przepływy pełnoekranowe (flow)](#6-przepływy-pełnoekranowe-flow)
7. [Opakowanie Navigation 3 – `AppNavDisplay`](#7-opakowanie-navigation-3--appnavdisplay)
8. [Zapis stanu](#8-zapis-stanu)
9. [Sesja i wylogowanie](#9-sesja-i-wylogowanie)
10. [Blokada aplikacji (PIN / biometria)](#10-blokada-aplikacji-pin--biometria)
11. [Jak dodać…](#11-jak-dodać)
12. [Testowanie](#12-testowanie)
13. [Ograniczenia i TODO](#13-ograniczenia-i-todo)

---

## 1. Zasady w pigułce

1. **Każda zakładka ma własny stos** i własną historię. Przełączenie zakładki niczego nie niszczy.
2. **Ekrany pełnoekranowe (logowanie, onboarding, checkout) są nad zakładkami**, w osobnym stosie – pasek zakładek wtedy znika.
3. **Blokada aplikacji nie jest nawigacją.** To nakładka nad wszystkim; stosy pod nią zostają nietknięte.
4. **Feature nie wie, że są zakładki.** Woła `navigator.navigate(key)` – o tym, *gdzie* ekran trafi, decyduje aplikacja.
5. **Cały stan jest jedną, czystą klasą** (`NavigationState`), testowaną bez emulatora. Compose i Nav3 tylko go rysują.

---

## 2. Model stanu

```
┌──────────────────────────────────────────────────────────┐
│ AppLock (nakładka)       – poza nawigacją, nad wszystkim │
├──────────────────────────────────────────────────────────┤
│ flow:  [Login, ...]      – pełny ekran, bez paska         │
├──────────────────────────────────────────────────────────┤
│ tabs:  Home    = [Home, Details(7)]                       │
│        Orders  = [Orders, OrderDetails(1001)]  ◀ wybrana  │
│        Profile = [Profile, About]                         │
└──────────────────────────────────────────────────────────┘
```

| Poziom | Co tam trafia | Pasek zakładek | Klasa |
|---|---|---|---|
| **Zakładki** | zwykła nawigacja „w głąb”: lista → szczegóły → podszczegóły | widoczny | `TabbedBackStack` |
| **Flow** | ekrany, które przejmują całą aplikację na czas zadania | ukryty | `NavigationState` (stos `flow`) |
| **Blokada** | „czy to wciąż ty?” po powrocie z tła | zasłonięty | `AppLock` + `LockScreen` |

- Zakładka jest identyfikowana przez **swój korzeń** (np. `HomeNavKey`), który zawsze jest pierwszym elementem jej stosu.
- **Pierwsza zakładka to zakładka startowa** – do niej prowadzi *wstecz* z korzenia każdej innej.
- `flow` może być pusty (zwykle jest). Gdy nie jest pusty – użytkownik jest „w zadaniu” i wszystko, co otworzy, trafia na flow.

---

## 3. Warstwy i klasy

```
feature (ekran)
   │  navigator.navigate(OrderDetailsNavKey(1001))
   ▼
Navigator  (interfejs, core:navigation)          ← jedyne API dla feature'ów
   ▼
AppNavigator  (app)                              ← + linki (InApp/External) przez pipeline deeplinków
   ▼
NavigationState  (core:navigation, czysty stan)  ← zakładki + flow, reguły „gdzie ląduje ekran”
   ├── TabbedBackStack                           ← stos per zakładka + wybrana zakładka
   │      └── BackStackNavigator × N             ← operacje na jednym NavBackStack
   └── flow: NavBackStack
   ▼
AppNavDisplay  (core:navigation, Compose)        ← rysuje stan przez Nav3 NavDisplay + pasek
```

| Klasa / plik | Moduł | Odpowiedzialność |
|---|---|---|
| `Navigator` | `core:navigation` | API dla feature'ów: `navigate`, `goBack`, `closeFlow`, `openLink`, `resumePendingLink`. |
| `BackStackNavigator` | `core:navigation` | Jeden zapisywalny `NavBackStack`: push, pop (nigdy nie zdejmuje korzenia), `resetTo` zachowujący wspólny prefiks. |
| `TabbedBackStack` | `core:navigation` | Stos per zakładka + indeks wybranej. `selectTab`, `navigate`, `goBack`, `resetTo`, `resetAll`. |
| `NavigationState` | `core:navigation` | Zakładki + flow. Reguły z [rozdziału 5](#5-gdzie-ląduje-nowy-ekran). Czysty stan, bez UI. |
| `rememberNavigationState` | `core:navigation` | Tworzy i zapisuje stan (rotacja, śmierć procesu). |
| `AppNavDisplay` | `core:navigation` | Nav3: dekoratory per stos, jeden `NavDisplay`, pasek ukrywany przy flow, insety. |
| `AppNavigator` | `app` | Implementacja `Navigator`; linki InApp/External → `NavigationState`. |
| `TopLevelTab` | `app` | Lista zakładek (korzeń, etykieta, ikona) w kolejności wyświetlania. |
| `fullScreen()` | `core:navigation`, używane w `impl` feature'a | Metadane wpisu: ekran otwiera flow nad zakładkami. |
| `parentOf(destination)` | `app` (`DeepLinkConfig.kt`) | Hierarchia ekranów – do której zakładki należy ekran otwarty z linku z zewnątrz. |
| `rememberAppNavigation` | `app` | Tworzy nawigator, wpisy i stan w jedynej poprawnej kolejności. |
| `SkeletonNavHost` | `app` | Skleja wszystko: nawigacja, linki, reset po wylogowaniu, pasek zakładek. |
| `AppLock`, `LockScreen` | `app` (`lock/`) | Blokada po powrocie z tła. |

Dlaczego tak:

- **`core:navigation` nie zna żadnego feature'a ani zakładek konkretnej aplikacji** – dostaje listę korzeni i funkcję `isFullScreen` (w aplikacji: odczyt metadanych wpisu). Można go przenieść do innego projektu bez zmian.
- **`app` tylko opisuje** strukturę (zakładki, hierarchię dla linków). To, że ekran jest pełnoekranowy, deklaruje feature, który go posiada. Nie dotyka dekoratorów, `NavDisplay` ani list wpisów.
- **Feature'y znają tylko `Navigator` i klucze z modułów `api`.**

---

## 4. Zachowanie – akcja po akcji

| Akcja użytkownika | Co się dzieje | Gdzie w kodzie |
|---|---|---|
| Kliknięcie innej zakładki | przełączenie, **historia zakładki zachowana** (stos, scroll, ViewModele) | `TabbedBackStack.selectTab` |
| Kliknięcie bieżącej zakładki | powrót do jej korzenia | `TabbedBackStack.selectTab` |
| Wejście w szczegóły | push na **bieżącą** zakładkę | `NavigationState.navigate` |
| Akcja „idź do sekcji” (np. *Moje zamówienia* w Profilu) | `navigate(OrdersNavKey)` → **przełączenie zakładki**, nie kopia korzenia na obcym stosie | `TabbedBackStack.navigate` |
| Otwarcie ekranu pełnoekranowego | push na flow, pasek znika | `NavigationState.navigate` |
| Nawigacja wewnątrz flow | push na flow (użytkownik najpierw kończy zadanie) | `NavigationState.navigate` |
| Zakończenie flow | `navigator.closeFlow()` – cały flow naraz | `NavigationState.closeFlow` |
| *Wstecz* we flow | pop flow | `NavigationState.goBack` |
| *Wstecz* w głębi zakładki | pop zakładki | `TabbedBackStack.goBack` |
| *Wstecz* na korzeniu nie-startowej zakładki | przejście do zakładki startowej | `TabbedBackStack.goBack` |
| *Wstecz* na korzeniu zakładki startowej | wyjście z aplikacji (system) | `NavDisplay` nie przechwytuje – jest tylko jeden wpis |
| Wylogowanie | **każdy** stos do korzenia, flow pusty, zakładka startowa | `NavigationState.resetAll` |
| Powrót z tła po czasie karencji | nakładka PIN; stosy bez zmian | `AppLock` |

Wybory, które warto znać:

- **Wstecz z korzenia zakładki → zakładka startowa**, a nie „historia odwiedzonych zakładek”. Tak zaleca Material; jest przewidywalne i dobrze współgra z predictive back (podgląd pokazuje dokładnie zakładkę startową).
- **Ponowne kliknięcie zakładki czyści jej stos.** Kliknięcie *innej* zakładki – nie.

---

## 5. Gdzie ląduje nowy ekran

`NavigationState.navigate(key)`:

```
key jest korzeniem zakładki?        → zamknij flow, wybierz tę zakładkę, pokaż jej korzeń
flow otwarty LUB isFullScreen(key)? → push na flow
w przeciwnym razie                  → push na bieżącą zakładkę
```

To samo dotyczy linków z wnętrza aplikacji. Linki z zewnątrz działają inaczej:

| | Link **InApp** (`navigator.openLink`) | Link **External** (Intent z systemu) |
|---|---|---|
| Kto | ekran aplikacji (np. baner z backendu) | e-mail, push, przeglądarka, inna aplikacja |
| Zasada | **liczy się, gdzie jest użytkownik** | **liczy się, gdzie ekran mieszka** |
| Działanie | jak `navigate(key)` (jeśli to nie ten sam ekran) | `NavigationState.resetTo(syntetyczny stos)` |
| `BackStackPolicy` / `parentOf` | nieużywane | wyznacza zakładkę i stos pod spodem |
| Inne zakładki | bez zmian | bez zmian |

Syntetyczny stos z linku External ma postać:

```
[korzeń zakładki, ...ekrany w zakładce, ...ekrany pełnoekranowe]
```

`resetTo` dzieli go na dwie części (pierwszy klucz pełnoekranowy rozpoczyna flow):

| Link | Stos z `parentOf` | Wynik |
|---|---|---|
| `myapp://orders/1001` | `[Orders, OrderDetails(1001)]` | zakładka Orders = ten stos, flow pusty |
| `myapp://settings` | `[Profile, Settings]` | zakładka Profile = ten stos |
| `myapp://orders/1001` **bez sesji** | redirect → `[Login]` | flow = `[Login]` **nad bieżącą zakładką** (nic jej nie resetuje) |

Hierarchię opisuje `parentOf` w `DeepLinkConfig.kt`:

```kotlin
internal fun parentOf(destination: Destination): Destination? = when (destination) {
    HomeNavKey, OrdersNavKey, ProfileNavKey -> null // tab roots
    LoginNavKey -> null // full screen: opens above whatever tab is selected
    is DetailsNavKey -> HomeNavKey
    is OrderDetailsNavKey -> OrdersNavKey
    SettingsNavKey, AboutNavKey -> ProfileNavKey
    else -> HomeNavKey
}
```

Test kontraktu pilnuje, żeby każda hierarchia kończyła się na korzeniu zakładki.

> **Ten sam klucz na dwóch zakładkach to dwa niezależne ekrany.** Item #7 otwarty z banera (Home) i z zamówienia (Orders) ma osobny ViewModel i osobny zapisany stan – patrz [rozdział 7](#7-opakowanie-navigation-3--appnavdisplay).

---

## 6. Przepływy pełnoekranowe (flow)

**Kiedy coś powinno być flow, a nie ekranem w zakładce?** Gdy jest *zadaniem*, którego użytkownik nie powinien porzucić w połowie przez kliknięcie innej zakładki: logowanie, onboarding, checkout, aparat, kreator, edytor.

Deklaruje to feature, który jest właścicielem ekranu, metadanymi wpisu w swoim module `impl`:

```kotlin
// feature/auth/impl
entry<LoginNavKey>(metadata = fullScreen()) { ... }
```

Każdy inny feature po prostu woła `navigator.navigate(LoginNavKey)`. Nie wie, jak ten ekran zostanie pokazany.

Dlaczego metadane, a nie lista kluczy w `app` ani interfejs na kluczu:

- **Tak to wygląda w Nav3.** Metadane wpisu opisują, *jak* go wyświetlić (przejścia, dialog, list-detail). „Otwórz nad zakładkami” to ta sama kategoria.
- **Dodanie feature'a nie wymaga zmian w `app`.** Przy liście w `app` łatwo zapomnieć dopisać klucz. Nic się wtedy nie wysypie, ekran po prostu otworzy się pod zakładkami, z paskiem.
- **Klucz w `api` zostaje czystym kontraktem** (argumenty, link, `RequiresAuthentication`). Sposób prezentacji zna tylko `impl`.
- **`app` dalej decyduje o powłoce.** Metadane wyrażają intencję, a co z nią zrobić (np. inaczej na tablecie), rozstrzyga `NavigationState` lub `AppNavDisplay`.

Oznacza się tylko ekran, który *otwiera* flow. Kolejne kroki i tak trafiają na otwarty flow. Wyjątek: link z zewnątrz, który od razu prowadzi do dalszego kroku. Wtedy ten krok też potrzebuje `fullScreen()`, bo stos syntetyczny dzieli się na pierwszym kluczu pełnoekranowym.

Reguły flow:

- Pasek zakładek jest ukryty, dopóki flow nie jest pusty.
- **Wszystko otwarte z flow trafia na flow** (np. regulamin z ekranu logowania) – dzięki temu *wstecz* wraca w obrębie zadania, a zakładki pod spodem się nie zmieniają.
- Wyjątek: korzeń zakładki zamyka flow i przełącza zakładkę.
- `navigator.closeFlow()` zamyka **cały** flow (wieloetapowe zadanie kończy się jednym wywołaniem).

### Przykład: logowanie i wznowienie linku

```kotlin
entry<LoginNavKey> {
    LoginRoute(
        onSignedIn = {
            // Leave the sign-in flow first, then open the link that needed a session (if any).
            navigator.closeFlow()
            navigator.resumePendingLink()
        },
        onCloseClick = { navigator.goBack() },
    )
}
```

Kolejność ma znaczenie: najpierw `closeFlow()`, potem `resumePendingLink()`. Inaczej wznowiony link InApp trafiłby na flow (bo flow byłby jeszcze otwarty).

Scenariusz z linkiem z zewnątrz bez sesji:

```
myapp://orders/1001 (wylogowany)
  → AuthenticationGuard: Redirect(Login), link odłożony
  → flow = [Login] nad bieżącą zakładką
  → „Sign in” → closeFlow() → resumePendingLink()
  → link przechodzi guardy jeszcze raz → zakładka Orders = [Orders, OrderDetails(1001)]
```

---

## 7. Opakowanie Navigation 3 – `AppNavDisplay`

`SkeletonNavHost` w `app` sprowadza się do opisu:

```kotlin
val navigation = rememberAppNavigation(entryProviderInstallers, deepLinkDispatcher, startBackStack)
val state = navigation.state
val navigator = navigation.navigator

AppNavDisplay(
    state = state,
    entryProvider = navigation.entryProvider,
    bottomBar = {
        NavigationBar {
            TopLevelTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = state.tabs.currentTab == tab.root,
                    onClick = { state.selectTab(tab.root) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label) },
                )
            }
        }
    },
    resultEventBus = navigation.resultEventBus,
    onBack = { navigator.goBack() },
)
```

`rememberAppNavigation` (`app/navigation/AppNavigation.kt`) tworzy wszystko w jedynej kolejności, która działa:

1. **nawigator**: feature'y zapamiętują go we wpisach, ale wywołują dopiero w callbackach;
2. **wpisy**, zbudowane z tym nawigatorem;
3. **stan**, który pyta wpisy, czy klucz ma `fullScreen()` (także przy podziale stosu startowego);
4. **podpięcie stanu do nawigatora** (`attach`), zanim jakikolwiek ekran może go wywołać.

Cykl jest tylko między referencjami do obiektów: nawigator musi *istnieć* przed wpisami, ale *działać* zaczyna dopiero po kompozycji. Kolejność jest zamknięta w jednej funkcji, więc nie da się jej przestawić. Stan jest prywatny w `AppNavigator`, a użycie nawigatora przed podpięciem kończy się czytelnym błędem (sprawdza to `AppNavigatorTest`).

Co `AppNavDisplay` robi pod spodem:

1. **Dekoruje każdy stos osobno i cały czas** (`rememberDecoratedNavEntries` + `SaveableStateHolder` + `ViewModelStore` per stos). Ukryta zakładka nie traci stanu, a usunięty ze stosu wpis czyści swój ViewModel.
2. **Nadaje każdemu wpisowi własną tożsamość** (`contentKey` = stos + pozycja + klucz, np. `tab0/2/DetailsNavKey(itemId=7)`). Ten sam klucz na dwóch stosach albo dwa razy na jednym (Item 7 → 8 → 7) to osobne wpisy – z osobnym ViewModelem i zapisanym stanem. Szczegóły: [navigation-arguments.md, rozdział 5](navigation-arguments.md#5-tożsamość-wpisu--ten-sam-ekran-dwa-razy).
3. **Pokazuje jeden `NavDisplay`** z listą `zakładka startowa + wybrana zakładka + flow`. Dzięki temu predictive back zawsze podgląda dokładnie to, co pokaże `goBack()`:
   - we flow → poprzedni ekran flow albo zakładkę pod nim,
   - na korzeniu innej zakładki → zakładkę startową,
   - na korzeniu zakładki startowej → lista ma jeden element, więc *wstecz* obsługuje system (wyjście z aplikacji).
4. **Pasek pod `NavDisplay`, ukrywany animacją, gdy flow jest otwarty.** Gdy pasek jest widoczny, dolny inset systemowy jest „zużyty” (`consumeWindowInsets`) – ekrany z własnym `Scaffold` nie dodają drugiego odstępu. Ekrany pełnoekranowe dostają całe okno i same obsługują insety.

Dlaczego jeden `NavDisplay`, a nie zagnieżdżone (osobny na zakładki i na flow)? Zagnieżdżone `NavDisplay` rywalizują o obsługę *wstecz*, a predictive back nie potrafi pokazać przejścia „z flow do zakładki”. Jedna lista wpisów to jedna, spójna animacja.

---

## 8. Zapis stanu

| Sytuacja | Co przeżywa | Jak |
|---|---|---|
| Obrót ekranu | wszystko | `rememberNavBackStack` (stosy), `rememberSaveable` (wybrana zakładka) |
| Śmierć procesu | stosy, wybrana zakładka, flow, stan UI ekranów | to samo + `SaveableStateHolder` per wpis |
| Zimny start z linku | link staje się **początkowym** stanem | `rememberNavigationState(startBackStack)` dzieli go na zakładkę i flow |
| Odtworzenie po śmierci procesu z linkiem w Intencie | link **nie** jest przetwarzany drugi raz | `MainActivity` czyta Intent tylko gdy `savedInstanceState == null` |

Klucze nawigacji muszą być `@Serializable` – inaczej stan nie zapisze się po śmierci procesu.

---

## 9. Sesja i wylogowanie

- `SessionRepository` (`core:data`) wystawia `isSignedIn: StateFlow<Boolean>`.
- **Wylogowanie resetuje całą nawigację** (`NavigationState.resetAll`): każda zakładka wraca do korzenia, flow znika, wybrana jest zakładka startowa. Bez tego `OrderDetails` poprzedniego użytkownika zostałby np. w zakładce, do której nikt nie zajrzał.
- Reset robi `SkeletonNavHost`, obserwując sesję – feature (Profil) woła tylko `session.signOut()` i nie musi znać nawigacji:

```kotlin
// Sign-out: nothing of the previous user may stay on any stack (also on tabs nobody looks at).
LaunchedEffect(state, isSignedIn) {
    isSignedIn.drop(1).filter { signedIn -> !signedIn }.collect { state.resetAll() }
}
```

---

## 10. Blokada aplikacji (PIN / biometria)

### 10.1 Logowanie a blokada

| | Logowanie | Blokada |
|---|---|---|
| Pytanie | *kim jesteś?* | *czy to wciąż ty?* |
| Kiedy | brak sesji; link wymaga sesji | zalogowany użytkownik: zimny start, powrót z tła po czasie karencji |
| Gdzie | **flow** (nawigacja) | **nakładka** poza nawigacją |
| Stosy | po sukcesie: zamknięcie flow + wznowienie linku | **nietknięte** – po odblokowaniu użytkownik widzi dokładnie to, co zostawił |
| *Wstecz* | zamyka logowanie | **chowa aplikację** (`moveTaskToBack`), nie odblokowuje |

Gdyby blokada była wpisem w stosie, trzeba by ją wstawiać na wierzch właściwego stosu (której zakładki? flow?), pilnować, żeby *wstecz* jej nie zdjęło, i żeby zapis stanu jej nie przywracał ani nie gubił. Nakładka nie ma żadnego z tych problemów.

### 10.2 Jak to działa

```
ProcessLifecycleOwner ──ON_STOP──▶ AppLock.onAppBackgrounded()   zapamiętuje czas (elapsedRealtime)
                      ──ON_START─▶ AppLock.onAppForegrounded()   minęła karencja i jest sesja → locked = true

MainActivity:
  Box {
      SkeletonNavHost(...)          // cały czas skomponowany; przy blokadzie ukryty przed czytnikiem ekranu
      if (isLocked) LockScreen(...) // nieprzezroczysty, przechwytuje dotyk i wstecz
  }
```

- **`ProcessLifecycleOwner`, nie Activity.** Przejście między Activity aplikacji to nie „wyjście do tła”, a obrót ekranu jest odfiltrowany (zdarzenia `ON_STOP` mają opóźnienie).
- **Zegar monotoniczny** (`SystemClock.elapsedRealtime`) – zmiana godziny w telefonie nie pozwala ominąć blokady.
- **Zimny start z sesją = od razu zablokowane** (`AppLock` startuje z `locked = isSignedIn`). Dotyczy to też powrotu po śmierci procesu.
- **Karencja** (`LOCK_AFTER` w `AppLockModule`, w demo 10 s) to decyzja produktowa. `0` = blokada od razu przy wyjściu. Karencja pokrywa też krótkie wyjścia będące częścią zadania (aparat, wybór pliku, płatność, share sheet).
- **Limit prób**: po 5 błędnych PIN-ach sesja jest kończona → nawigacja się resetuje → użytkownik loguje się od nowa. „Not you? Sign out” robi to samo od razu.

### 10.3 Szczegóły, o których łatwo zapomnieć

| Problem | Rozwiązanie w kodzie |
|---|---|
| Miniatura w „Ostatnich aplikacjach” pokazuje dane | `setRecentsScreenshotEnabled(false)` (API 33+) w `MainActivity` |
| Mignięcie treści przed blokadą po powrocie | stan zbierany przez `collectAsState()` także w tle; przy karencji `0` blokada ustawia się już przy wyjściu, więc pierwsza klatka po powrocie to blokada |
| TalkBack czyta ekran pod blokadą | `Modifier.clearAndSetSemantics {}` na `SkeletonNavHost`, gdy zablokowane |
| *Wstecz* odsłania aplikację | `BackHandler { moveTaskToBack(true) }` w `LockScreen` |
| Utrata stanu ekranów | treść **nie jest** usuwana z kompozycji – nakładka jest tylko nad nią |
| Link przychodzi, gdy aplikacja jest zablokowana | jest normalnie stosowany **pod** nakładką; po odblokowaniu użytkownik od razu widzi cel |
| Link wymagający sesji, a sesji brak | najpierw logowanie (flow); świeżo zalogowanego użytkownika nie pytamy o PIN |

### 10.4 Biometria

Demo ma tylko PIN (`1234`). Biometria to drugi sposób wywołania `unlock`:

- `BiometricPrompt` z `androidx.biometric` (wymaga `FragmentActivity` zamiast `ComponentActivity`),
- najlepiej z `CryptoObject` powiązanym z kluczem w Keystore (`setUserAuthenticationRequired(true)`), którym odszyfrowuje się token sesji. Sam prompt bez kryptografii to tylko bramka w UI.

PIN w prawdziwej aplikacji: nigdy jawnie – hash z solą (klucz w Keystore) albo weryfikacja na serwerze; licznik prób zapisany trwale.

---

## 11. Jak dodać…

### 11.1 Ekran w zakładce

1. Klucz w module `api` feature'a (`@Serializable`, `NavKey`, `Destination` jeśli ma mieć link).
2. `entry<TwójKlucz> { ... }` w `EntryProviderInstaller` feature'a.
3. Nawigacja: `navigator.navigate(TwójKlucz(...))` – trafi na bieżącą zakładkę.
4. Jeśli ma link z zewnątrz: dopisz go w `parentOf` (do której zakładki należy) i w teście kontraktu.

### 11.2 Ekran pełnoekranowy (flow)

Jak wyżej, tylko pierwszy ekran zadania dostaje metadane:

```kotlin
entry<CheckoutNavKey>(metadata = fullScreen()) { ... }
```

W `app` nic się nie zmienia.

Na końcu zadania feature woła `navigator.closeFlow()`. Dla linku z zewnątrz: `parentOf(Checkout) = null` otworzy go nad bieżącą zakładką, a `parentOf(Checkout) = CartNavKey` – nad zakładką koszyka.

### 11.3 Nową zakładkę

1. Feature z kluczem-korzeniem (np. `SearchNavKey`).
2. Pozycja w `TopLevelTab` (kolejność = kolejność na pasku; pierwsza = startowa).
3. `parentOf`: korzeń → `null`, ekrany tej zakładki → `SearchNavKey`.

Nic w `core:navigation` ani w `AppNavDisplay` się nie zmienia.

---

## 12. Testowanie

### 12.1 Testy jednostkowe (bez emulatora)

| Test | Co sprawdza |
|---|---|
| `TabbedBackStackTest` | push na wybraną zakładkę, zachowanie historii, reselect, *wstecz* do startowej, `resetTo` |
| `NavigationStateTest` | flow nad zakładkami, *wstecz* najpierw z flow, korzeń zamyka flow, podział syntetycznego stosu, `resetAll` |
| `AppLockTest` | blokada przy zimnym starcie, karencja, `0` = od razu, brak blokady bez sesji, limit prób → wylogowanie |
| `DeepLinkContractTest` | publiczne URL-e → zakładka i stos; link do zamówienia bez sesji → logowanie nad zakładkami |

```bash
./gradlew :core:navigation:testDebugUnitTest :app:testDebugUnitTest
```

### 12.2 Na urządzeniu

```bash
# link wymagający sesji (wylogowany) → logowanie, po zalogowaniu zamówienie na zakładce Orders
adb shell am start -a android.intent.action.VIEW -d "myapp://orders/1001"

# blokada: zaloguj się, wyjdź do ekranu głównego na > 10 s, wróć → PIN 1234
# wylogowanie po 5 złych PIN-ach → wszystkie zakładki od korzenia
```

---

## 13. Ograniczenia i TODO

- **Duplikaty w głębi stosu** – pętla A → B → A → B rośnie bez końca. Można cofać się do istniejącego wpisu zamiast pushować.
- **Ponowne kliknięcie zakładki na jej korzeniu** powinno przewinąć listę do góry – wymaga sygnału od zakładki do ekranu.
- **Duże ekrany** – `NavigationSuiteScaffold` (rail/drawer zamiast paska) i `SceneStrategy` list-detail. Model stanu się nie zmienia, zmienia się tylko `bottomBar` i strategia sceny.
- **Dialogi i bottom sheety jako wpisy stosu** (sceny Nav3) – tylko jeśli muszą przeżyć śmierć procesu albo mieć link; w pozostałych przypadkach zwykły stan UI ekranu.
- **Biometria** i trwały licznik prób – patrz [10.4](#104-biometria).
- **Zakładki zależne od uprawnień** – zmiana listy zakładek wymaga zbudowania stanu od nowa.
