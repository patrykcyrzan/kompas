# Nawigacja z zakładkami – specyfikacja implementacji

Zwięzła specyfikacja do odtworzenia nawigacji (zakładki + flow pełnoekranowy na Navigation 3) w innym projekcie albo przez inną osobę lub agenta. Opisuje **co** ma powstać i **jakie decyzje są już podjęte**. Uzasadnienia i scenariusze krok po kroku są w [navigation.md](navigation.md), a argumenty i wyniki w [navigation-arguments.md](navigation-arguments.md).

Wszystkie punkty poniżej są rozstrzygnięte. Nie trzeba o nie dopytywać.

---

## 1. Model

```
flow:  [Login, Terms]                   ← osobny stos NAD zakładkami; niepusty = zasłania zakładki, pasek ukryty
tabs:  Home    = [Home, Details(7)]
       Orders  = [Orders]               ← wybrana
       Profile = [Profile, About]
```

- **Każda zakładka ma własny stos z korzeniem** (`BackStackNavigator`). Stos zakładki nigdy nie jest pusty.
- **Flow to zwykła lista** (`NavBackStack<NavKey>` z `rememberNavBackStack`), **nie** `BackStackNavigator`. Zwykle jest pusta, a pop ostatniego elementu zamyka flow. `BackStackNavigator` jest dla stosu z korzeniem: nie zdejmuje korzenia i nie przyjmuje pustej listy.
- **Blokada aplikacji (PIN) nie jest częścią nawigacji.** To nakładka nad całością.

## 2. Moduły i pliki

| Plik | Moduł | Rola |
|---|---|---|
| `BackStackNavigator.kt` | `core:navigation` | stos z korzeniem nad `NavBackStack`; `replaceWith` zachowujący wspólny prefiks |
| `TabbedBackStack.kt` | `core:navigation` | stosy zakładek + indeks wybranej |
| `NavigationState.kt` | `core:navigation` | zakładki + flow; wszystkie reguły; czysty stan bez UI |
| `FullScreen.kt` | `core:navigation` | metadane wpisu `fullScreen()` / `isFullScreen()` |
| `EntryId.kt` | `core:navigation` | tożsamość wpisu (`contentKey`) |
| `ResultKey.kt` | `core:navigation` | typowany klucz wyniku, `ResultEffect(key)`, `rememberAppResultEventBus()` |
| `Navigator.kt` | `core:navigation` | interfejs, którego używają feature'y |
| `NavigationDisplay.kt` | `core:navigation` | renderowanie: dekoratory, tożsamość wpisów, jeden `NavDisplay`; bez layoutu |
| `AppShell.kt` | `app` | układ aplikacji: `NavigationDisplay` + pasek zakładek, insety |
| `AppNavigation.kt` | `app` | `rememberAppNavigation` – tworzy wszystko w poprawnej kolejności |
| `AppNavigator.kt` | `app` | implementacja `Navigator` (+ linki) |
| `TopLevelTab.kt` | `app` | lista zakładek (korzeń, etykieta, ikona) |

`core:navigation` nie zna żadnego feature'a ani zakładek konkretnej aplikacji.

## 3. `BackStackNavigator` i `TabbedBackStack`

```kotlin
class BackStackNavigator(val backStack: NavBackStack<NavKey>) {
    val current: NavKey?
    fun navigate(key: NavKey)          // push
    fun goBack(): Boolean              // nie zdejmuje korzenia: size <= 1 → false
    fun resetTo(keys: List<NavKey>)    // require(keys.isNotEmpty()); zachowuje wspólny prefiks
}

internal fun MutableList<NavKey>.replaceWith(keys: List<NavKey>)  // wspólny prefiks zostaje nietknięty

class TabbedBackStack(val tabs: List<NavKey>, stacks: Map<NavKey, BackStackNavigator>, selectedIndex: MutableIntState) {
    val startTab: NavKey               // tabs.first()
    val currentTab: NavKey
    val current: NavKey?
    fun stackOf(tab: NavKey): BackStackNavigator
    fun isTab(key: NavKey): Boolean
    fun selectTab(tab: NavKey)         // klik w pasek; ta sama zakładka → powrót do jej korzenia
    fun navigate(key: NavKey)          // korzeń zakładki → przełącz i pokaż korzeń; inny klucz → push na wybraną
    fun goBack(): Boolean              // pop wybranej; z korzenia innej zakładki → zakładka startowa; z korzenia startowej → false
    fun resetTo(keys: List<NavKey>)    // keys[0] musi być korzeniem zakładki; wybiera ją i podmienia jej stos
    fun resetAll()                     // każda zakładka do korzenia, wybrana startowa
}
```

## 4. `NavigationState` – reguły

```kotlin
class NavigationState(
    val tabs: TabbedBackStack,
    private val flow: MutableList<NavKey>,
    private val isFullScreen: (NavKey) -> Boolean,
) {
    val flowBackStack: List<NavKey>
    val isFlowOpen: Boolean            // flow.isNotEmpty()
    val current: NavKey?               // flow.lastOrNull() ?: tabs.current
    fun navigate(key: NavKey)
    fun selectTab(tab: NavKey)
    fun goBack(): Boolean
    fun closeFlow()
    fun resetTo(keys: List<NavKey>)
    fun resetAll()
}
```

| Operacja | Reguła |
|---|---|
| `navigate(key)` | 1) korzeń zakładki → `flow.clear()` + `tabs.navigate(key)`; 2) `isFlowOpen \|\| isFullScreen(key)` → `flow.add(key)`; 3) inaczej → push na wybraną zakładkę |
| `goBack()` | najpierw flow (`removeAt(lastIndex)`), potem `tabs.goBack()` |
| `closeFlow()` | `flow.clear()` – cały flow naraz |
| `resetTo(keys)` (linki z zewnątrz) | `splitBackStack`: część przed pierwszym kluczem pełnoekranowym → `tabs.resetTo(...)` (jeśli niepusta), reszta → `flow.replaceWith(...)` (także pusta, wtedy flow się zamyka) |
| `resetAll()` (wylogowanie) | `flow.clear()` + `tabs.resetAll()` |

```kotlin
internal fun splitBackStack(keys: List<NavKey>, isFullScreen: (NavKey) -> Boolean): Pair<List<NavKey>, List<NavKey>>
// [A, B, Login, C] → [A, B] i [Login, C]
```

`rememberNavigationState(tabs, startBackStack, isFullScreen)` dzieli `startBackStack` tak samo jak `resetTo` (tylko przy pierwszym utworzeniu) i zapisuje wszystko na wypadek rotacji i śmierci procesu. `isFullScreen` jest trzymane przez `rememberUpdatedState`.

**Navigator niczego nie decyduje.** Każda operacja nawigacji deleguje do `NavigationState`.

## 5. Ekrany pełnoekranowe – metadane wpisu

```kotlin
fun fullScreen(): Map<String, Any>
fun Map<String, Any>.isFullScreen(): Boolean

// feature:impl – deklaruje feature, który jest właścicielem ekranu
entry<LoginNavKey>(metadata = fullScreen()) { ... }
```

- **Jedna prawda: metadane wpisu.** W `app`: `isFullScreen = { key -> entries(key).metadata.isFullScreen() }`. Wywołanie `entryProvider(key)` jest tanie (tworzy `NavEntry`, niczego nie komponuje).
- **Bez listy kluczy w `app` i bez interfejsu-znacznika na kluczu.** Dodanie feature'a z flow nie wymaga zmian w `app`, a klucz w `api` zostaje czystym kontraktem.
- **Oznacza się tylko ekran, który otwiera flow.** Kolejne kroki trafiają na otwarty flow same.

## 6. Tworzenie nawigacji w `app` – bez `lateinit`

Jest tu cykl referencji: wpisy potrzebują nawigatora (`install(navigator)`), stan potrzebuje wpisów (`isFullScreen`), a nawigator potrzebuje stanu. Rozwiązuje go jedna funkcja, która tworzy wszystko w jedynej poprawnej kolejności:

```kotlin
@Composable
internal fun rememberAppNavigation(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    deepLinkDispatcher: DeepLinkDispatcher,
    startBackStack: List<NavKey>,
): AppNavigation {
    val resultEventBus = rememberAppResultEventBus()
    val navigator = remember(deepLinkDispatcher, resultEventBus) { AppNavigator(deepLinkDispatcher, resultEventBus) }
    val entries = remember(entryProviderInstallers, navigator) {
        entryProvider { entryProviderInstallers.forEach { with(it) { install(navigator) } } }
    }
    val state = rememberNavigationState(TopLevelTab.roots, startBackStack) { key -> entries(key).metadata.isFullScreen() }
    return remember(navigator, entries, state) {
        navigator.attach(state)
        AppNavigation(state, navigator, entries, resultEventBus)
    }
}
```

```kotlin
internal class AppNavigator(private val deepLinks: DeepLinkDispatcher, private val results: ResultEventBus) : Navigator {
    private var attached: NavigationState? = null
    private val state: NavigationState
        get() = checkNotNull(attached) { "Navigator used before the navigation state was attached (see rememberAppNavigation)" }
    fun attach(state: NavigationState) { attached = state }
    ...
}
```

- Stan w nawigatorze jest **prywatny**. Nie ma `lateinit var`.
- `SkeletonNavHost` (host nawigacji) woła tylko `rememberAppNavigation(...)` i nie zna kolejności.
- Nawigator jest wywoływany dopiero z callbacków ekranów i z `LaunchedEffect`, czyli po kompozycji. Dlatego cykl istnieje tylko między referencjami, a nie w zachowaniu.

## 7. `Navigator` (API dla feature'ów)

```kotlin
interface Navigator {
    fun navigate(key: NavKey)
    fun goBack(): Boolean
    fun <T : Any> goBackWithResult(key: ResultKey<T>, result: T)   // sendResult(key.name, result) + goBack()
    fun closeFlow()
    fun openLink(uri: String): OpenLinkResult     // link w aplikacji: push na bieżący ekran
    fun resumePendingLink(): Boolean              // po logowaniu: najpierw closeFlow(), potem to
}
```

Linki: **External** (z systemu) → `state.resetTo(syntetyczny stos)`, **InApp** → `if (state.current != key) state.navigate(key)`.

## 8. Renderowanie – `NavigationDisplay` (core) i `AppShell` (app)

```kotlin
// core:navigation – tylko mechanika Nav3, bez paska i bez insetów
@Composable
fun NavigationDisplay(
    state: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
    resultEventBus: ResultEventBus = rememberAppResultEventBus(),
    onBack: () -> Unit = { state.goBack() },
)

// app – układ aplikacji
@Composable
internal fun AppShell(navigation: AppNavigation, modifier: Modifier = Modifier)
```

Punkty 1–3 i 6 to `NavigationDisplay`, punkty 4–5 to `AppShell`. Granica: wszystko, co zależy od wyglądu konkretnej aplikacji (pasek czy rail, animacja, insety), jest w `app`.

1. **Własne dekoratory dla każdego stosu** (każda zakładka i flow), składane **cały czas**, także gdy stos jest ukryty. Kolejność: `rememberSaveableStateHolderNavEntryDecorator()` **przed** `rememberViewModelStoreNavEntryDecorator()`. ViewModel wpisu bierze z pierwszego dekoratora zapis stanu dla `SavedStateHandle`. Bez dekoratora ViewModeli wszystkie ekrany dzieliłyby ViewModele aktywności.
2. **Tożsamość wpisu**: `NavEntry(key, contentKey = "$scope/$index/$key", metadata = entry.metadata) { entry.Content() }`, gdzie `scope` to `"tab$i"` albo `"flow"`. Domyślny `contentKey` (sam klucz) sprawia, że Item 7 → 8 → 7 dzieli jeden ViewModel.
3. **Jeden `NavDisplay`** z listą: wpisy zakładki startowej + wpisy bieżącej zakładki (jeśli inna niż startowa) + wpisy flow. Dzięki temu predictive back pokazuje dokładnie to, co pokaże `goBack()`.
4. **Pasek zakładek** jest widoczny, gdy flow jest **pusty**: `AnimatedVisibility(visible = !state.isFlowOpen, enter = expandVertically(), exit = shrinkVertically())`. Warunkiem nie jest „wpis na wierzchu ma `fullScreen()`”, bo dalsze kroki flow nie mają tej metadanej.
5. **Insety**: `NavDisplay` w `Column` z `Modifier.weight(1f)`, a gdy pasek jest widoczny, także `consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))`. Pasek rysuje się pod systemowym paskiem nawigacji, więc ekrany nad nim nie mogą dodawać tego odstępu drugi raz.
6. **Wyniki**: `CompositionLocalProvider(LocalResultEventBus provides resultEventBus)` wokół `NavDisplay`. Szyna jest jedna dla aplikacji i trzymana w ViewModelu (`rememberAppResultEventBus()`), więc wynik wysłany tuż przed rotacją nie ginie.

## 9. Wyniki

```kotlin
class ResultKey<T : Any>(val name: String)

@Composable
fun <T : Any> ResultEffect(key: ResultKey<T>, onResult: suspend (T) -> Unit)   // nad ResultEffect z Nav3

// feature:api ekranu, który ZWRACA wynik
val ItemPickerResult = ResultKey<ItemId>("<pakiet feature'a>.itemPicker")

navigator.goBackWithResult(ItemPickerResult, itemId)                       // nadawca
ResultEffect(ItemPickerResult) { itemId -> viewModel.onPicked(itemId) }    // odbiorca
```

Pod spodem jest `ResultEventBus` z Nav3 1.2: wynik jest buforowany per klucz i trafia do tego, kto pierwszy go odbierze. Odbiorca zapisuje wynik u siebie (np. w `SavedStateHandle`).

## 10. Czego świadomie NIE ma

| Nie | Dlaczego |
|---|---|
| Flow jako `BackStackNavigator` | inne niezmienniki (flow bywa pusty, pop ostatniego zamyka flow) |
| Lista kluczy pełnoekranowych w `app` | druga lista do pamiętania; zapomniany klucz otwiera się po cichu pod zakładkami |
| `lateinit var state` w nawigatorze | kruchość przy zmianie kolejności w hoście |
| ViewModel współdzielony przez kilka wpisów | formularz w kilku krokach to jeden wpis z krokami jako stan (`BackHandler` cofa krok) |
| Nawigacja zakładek przez klik w pasek, gdy flow jest otwarty | pasek jest ukryty, a zadania nie porzuca się przypadkiem |

## 11. Testy (czyste JVM)

| Test | Sprawdza |
|---|---|
| `TabbedBackStackTest` | push na wybraną zakładkę, przełączanie z zachowaniem historii, ponowny wybór → korzeń, korzeń zakładki przełącza zamiast dokładać kopię, wstecz do zakładki startowej, `resetTo` |
| `NavigationStateTest` | flow nad zakładkami i przechwytywanie kolejnych pushów, korzeń zakładki zamyka flow, `splitBackStack`, `resetTo`, `resetAll` |
| `EntryIdTest` | równe klucze na jednym stosie to różne wpisy; tożsamość stabilna |
| `FullScreenTest` | `fullScreen()` / `isFullScreen()`, łączenie z innymi metadanymi |
| `AppNavigatorTest` | użycie przed `attach` → czytelny błąd; po `attach` steruje stanem |

## 12. Kryteria odbioru (na urządzeniu)

- Logowanie i formularz otwierają się bez paska zakładek; kolejne kroki zostają we flow; wstecz z pierwszego kroku wraca na zakładkę, na której był użytkownik.
- Item 7 → 8 → 7 to trzy niezależne ekrany (osobne ViewModele); wstecz wraca przez 8 do 7.
- Przełączanie zakładek zachowuje historię i przewinięcie każdej z nich; ponowny klik w bieżącą zakładkę wraca do jej korzenia.
- Link z zewnątrz do ekranu wymagającego logowania (`myapp://orders/1001`): logowanie nad zakładkami, a po nim szczegóły na zakładce zamówień.
- Rotacja i śmierć procesu (`adb shell am kill <applicationId>`) odtwarzają zakładki, flow i dane ekranów.
- Wylogowanie czyści wszystkie stosy, także ukrytych zakładek.
