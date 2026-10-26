# Argumenty i wyniki w nawigacji

Ten dokument opisuje, **jak przekazywać argumenty między ekranami i jak zwracać wyniki** w aplikacji wielomodułowej z Navigation 3 i dolnymi zakładkami. Zakres to zwykła nawigacja w aplikacji (klik → ekran), bez linków.

Model stanu (zakładki, flow, `AppNavDisplay`) opisuje [navigation.md](navigation.md), a linki opisuje [deeplinks.md](deeplinks.md).

---

## Spis treści

1. [Zasady w pigułce](#1-zasady-w-pigułce)
2. [Klucz to kontrakt ekranu](#2-klucz-to-kontrakt-ekranu)
3. [Typowane ID](#3-typowane-id)
4. [Od klucza do ViewModelu](#4-od-klucza-do-viewmodelu)
5. [Tożsamość wpisu – ten sam ekran dwa razy](#5-tożsamość-wpisu--ten-sam-ekran-dwa-razy)
6. [Wyniki – „wybierz i oddaj”](#6-wyniki--wybierz-i-oddaj)
7. [Złożone argumenty – obiekt wartości](#7-złożone-argumenty--obiekt-wartości)
8. [Formularz na kilka ekranów – wspólny ViewModel](#8-formularz-na-kilka-ekranów--wspólny-viewmodel)
9. [Nawigacja z ViewModelu](#9-nawigacja-z-viewmodelu)
10. [Argumenty a zakładki](#10-argumenty-a-zakładki)
11. [Checklista: nowy ekran z argumentami](#11-checklista-nowy-ekran-z-argumentami)
12. [Testowanie](#12-testowanie)
13. [Ograniczenia i TODO](#13-ograniczenia-i-todo)

---

## 1. Zasady w pigułce

1. **Argumenty ekranu to pola jego klucza** (`data class OrderDetailsNavKey(val orderId: OrderId)`). Nie ma osobnego „Bundle z argumentami” ani stringowych nazw.
2. **Klucz leży w module `api` feature'a.** Inne feature'y znają tylko klucz, nigdy ekran.
3. **W kluczu są tylko małe, serializowalne dane**: ID i proste opcje. Nie ma w nim obiektów domenowych, callbacków ani list.
4. **ID są typowane** (`ItemId`, `OrderId`), więc nie da się podać ID zamówienia tam, gdzie oczekiwane jest ID produktu.
5. **ViewModel dostaje klucz w konstruktorze** (Hilt assisted injection). Nie ma `SavedStateHandle["orderId"]`.
6. **Każdy wpis na stosie ma własną tożsamość.** Ten sam ekran z tymi samymi argumentami dwa razy to dwa niezależne ekrany.
7. **Wynik wraca przez `ResultEventBus` z Nav3**, z jawnym kluczem z `api` zwracającego ekranu. Stan, który ma przetrwać dłużej, trafia do repozytorium.

---

## 2. Klucz to kontrakt ekranu

```kotlin
// feature/orders/api – publiczny kontrakt feature'a
@Serializable
data class OrderDetailsNavKey(val orderId: OrderId) : NavKey, Destination, RequiresAuthentication

// feature/details/api – argument opcjonalny z wartością domyślną
@Serializable
data class ItemPickerNavKey(val selected: ItemId? = null) : NavKey
```

Kto z czego korzysta:

```
orders:impl   ──navigate(DetailsNavKey(itemId))──▶  details:api   (klucz)
profile:impl  ──navigate(ItemPickerNavKey(...))──▶  details:api   (klucz + ItemPickerResult.KEY)
details:impl  ── entry<DetailsNavKey> { ... }       (ekran, ViewModel – nikt spoza go nie zna)
```

### Co wolno, a czego nie wolno włożyć do klucza

| ✅ Tak | ❌ Nie | Dlaczego |
|---|---|---|
| `ItemId`, `OrderId` | `Item`, `Order` (cały obiekt) | Obiekt w kluczu to nieaktualna kopia: ekran pokaże stare dane, gdy w repozytorium już są nowe. Ekran sam pobiera dane po ID. |
| `enum`, `Boolean`, krótki `String` (np. tryb, źródło wejścia) | lambdy, callbacki | Klucz jest zapisywany przy śmierci procesu. Lambdy nie da się zapisać, więc znika (albo aplikacja się wysypie). |
| `ItemId?` z wartością domyślną | długie listy, obrazy, JSON-y | Zapisany stan całej aplikacji ma limit (`TransactionTooLargeException`). Każdy klucz na każdym stosie się do niego wlicza. |
| `@Serializable` | klasy bez `@Serializable` | Nav3 zapisuje stos przez `kotlinx.serialization`. Bez tego stan nie przeżyje śmierci procesu. |

**Dane „na start”, żeby nie migał loader.** Przekaż samo ID, a dane niech dostarczy repozytorium z pamięci podręcznej (lista i szczegóły czytają ten sam `Flow`). Jeśli koniecznie trzeba pokazać coś od razu (np. tytuł), dodaj do klucza małe pole „podglądowe”, ale traktuj je jako tymczasowe i nadpisuj danymi z repozytorium.

**Ewolucja klucza.** Nowe pole dodawaj z wartością domyślną. Wtedy wszystkie istniejące wywołania w innych modułach dalej się kompilują. Zmiana nazwy albo typu pola to zmiana kontraktu, więc kompilator pokaże każde miejsce, które trzeba poprawić.

---

## 3. Typowane ID

```kotlin
// core/model/Ids.kt
@Serializable
@JvmInline
value class ItemId(val value: String) {
    override fun toString(): String = value
}

@Serializable
@JvmInline
value class OrderId(val value: Long) {
    override fun toString(): String = value.toString()
}
```

- **Używane wszędzie, gdzie ID przekracza granicę modułu**: w kluczach, repozytoriach (`observeOrder(id: OrderId)`), wynikach (`ResultEffect<ItemId>`) i trasach linków (`OrderDetailsNavKey(OrderId(it))`).
- `value class` nie alokuje obiektu w czasie działania i serializuje się jako sama wartość.
- Moduł `api` feature'a udostępnia `core:model` (`api(projects.core.model)`), bo ID są częścią jego kontraktu.

> ⚠️ **Pułapka: klucze w `LazyColumn` i `rememberSaveable`.** Muszą dać się zapisać w Bundle, a opakowany `value class` się do tego nie nadaje. Aplikacja wysypie się wyjątkiem `Type of the key ... is not supported`. Używaj surowej wartości:
>
> ```kotlin
> items(orders, key = { it.id.value }) { order -> ... }
> ```

---

## 4. Od klucza do ViewModelu

Klucz trafia do ViewModelu w konstruktorze, przez Hilt assisted injection:

```kotlin
// details:impl
@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)
internal class DetailsViewModel @AssistedInject constructor(
    @Assisted navKey: DetailsNavKey,
    itemsRepository: ItemsRepository,
) : ViewModel() {

    val uiState = itemsRepository.observeItem(navKey.itemId) ...

    @AssistedFactory
    interface Factory {
        fun create(navKey: DetailsNavKey): DetailsViewModel
    }
}

// Route – jedyne miejsce, gdzie klucz „wchodzi” do ViewModelu
@Composable
internal fun DetailsRoute(
    navKey: DetailsNavKey,
    onRelatedClick: (ItemId) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel<DetailsViewModel, DetailsViewModel.Factory>(
        creationCallback = { factory -> factory.create(navKey) },
    ),
)

// Entry provider – klucz z back stacku → Route
entry<DetailsNavKey> { key ->
    DetailsRoute(
        navKey = key,
        onRelatedClick = { itemId -> navigator.navigate(DetailsNavKey(itemId)) },
        onBackClick = { navigator.goBack() },
    )
}
```

Dlaczego assisted injection, a nie `SavedStateHandle`:

| | Klucz w konstruktorze | `SavedStateHandle["itemId"]` |
|---|---|---|
| Typy | sprawdzane przez kompilator | `String` → rzutowanie w czasie działania |
| Nazwy argumentów | pola klasy | stringi, które muszą się zgadzać w dwóch miejscach |
| Test ViewModelu | `DetailsViewModel(DetailsNavKey(ItemId("7")), fakeRepo)` | trzeba zbudować `SavedStateHandle` z odpowiednimi kluczami |
| Śmierć procesu | klucz jest odtwarzany ze stosu i trafia ponownie do konstruktora | zależy od tego, kto i jak zapełni handle |

`SavedStateHandle` dalej się przydaje, ale do **stanu tworzonego na ekranie** (np. wynik z pickera w Profilu, szkic formularza), a nie do argumentów.

**Ekran bez ViewModelu albo argument czysto wizualny** (np. `ItemPickerNavKey.selected`, który tylko zaznacza pozycję) może trafić prosto do Route jako parametr.

---

## 5. Tożsamość wpisu – ten sam ekran dwa razy

**Problem.** Nav3 przypisuje zapisany stan i ViewModel wpisowi po jego `contentKey`, a domyślnie jest nim sam klucz. Ścieżka *Item 7 → Item 8 → Item 7* kładzie na stos dwa równe klucze `DetailsNavKey(7)`, więc oba wpisy dzieliły **jeden ViewModel i jeden zapisany stan**. Na telefonie było to widać jako „ViewModel #1” na obu ekranach Item 7. W czasie animacji przejścia dwa wpisy z tym samym kluczem mogą też kolidować.

**Rozwiązanie** (`EntryId.kt`, używane przez `AppNavDisplay`):

```
contentKey = <stos>/<pozycja>/<klucz>        np. tab0/3/DetailsNavKey(itemId=7)
```

- **Pozycja jest stała przez całe życie wpisu**: stos zmienia się tylko na górze, a `resetTo` zachowuje wspólny prefiks.
- **Klucz w identyfikatorze** sprawia, że inny ekran na tej samej, zwolnionej pozycji to nowy wpis, a nie przejęcie cudzego stanu.
- **Stos w identyfikatorze** sprawia, że ten sam ekran na dwóch zakładkach to dwa wpisy.

Efekt na urządzeniu: *Item 7 (#1) → Item 8 (#2) → Item 7 (#3)*, a *wstecz* wraca do #2, potem do #1.

Feature'y nie muszą nic z tym robić. Nie trzeba też blokować duplikatów ani wymyślać unikalnych pól w kluczach.

---

## 6. Wyniki – „wybierz i oddaj”

Wyniki idą przez **wbudowany w Nav3 1.2 `ResultEventBus`** (`androidx.navigation3.runtime.result`). Nie ma własnego mechanizmu: `AppNavDisplay` tylko udostępnia jedną szynę dla całej aplikacji.

### 6.1 Kiedy wynik, a kiedy repozytorium

| Sytuacja | Mechanizm |
|---|---|
| Wybór, o którym decyduje ekran wywołujący (picker: produkt, adres, data, filtr) | **wynik** (`ResultEventBus`) |
| Zmiana, która ma przetrwać i być widoczna wszędzie (edycja profilu, ocena zamówienia, zapis adresu) | **repozytorium**: ekran zapisuje, inne obserwują; żadnego wyniku |
| Logowanie, które ma odblokować przerwany link | `resumePendingLink()` (patrz [deeplinks.md](deeplinks.md)) |

### 6.2 API

```kotlin
// details:api – obok klucza ekranu, który ZWRACA wynik
@Serializable
data class ItemPickerNavKey(val selected: ItemId? = null) : NavKey

object ItemPickerResult {
    const val KEY: String = "dev.cyrzan.skeleton.feature.details.itemPicker"
}
```

```kotlin
// details:impl – picker wysyła wybór i wraca
entry<ItemPickerNavKey> { key ->
    val resultBus = LocalResultEventBus.current
    ItemPickerRoute(
        selected = key.selected,
        onPick = { itemId ->
            resultBus.sendResult(ItemPickerResult.KEY, itemId)
            navigator.goBack()
        },
        onBackClick = { navigator.goBack() },   // anulowanie = brak wyniku
    )
}
```

```kotlin
// profile:impl – otwiera picker i odbiera wynik
entry<ProfileNavKey> {
    ProfileRoute(onFavouriteClick = { current -> navigator.navigate(ItemPickerNavKey(selected = current)) }, ...)
}

@Composable
internal fun ProfileRoute(..., viewModel: ProfileViewModel = hiltViewModel()) {
    ResultEffect<ItemId>(ItemPickerResult.KEY) { itemId -> viewModel.onFavouritePicked(itemId) }
    ...
}

// ProfileViewModel – wynik zapisany przez odbiorcę (przeżyje śmierć procesu)
fun onFavouritePicked(itemId: ItemId) {
    savedStateHandle[KEY_FAVOURITE] = itemId.value
}
```

**Jawny klucz zamiast domyślnego.** `ResultEffect<ItemId>()` bez klucza używa nazwy typu, więc każdy ekran zwracający `ItemId` wpadałby do tego samego kanału. Klucz należy do kontraktu zwracającego ekranu i leży w jego `api`, obok klucza nawigacji.

### 6.3 Jak to działa i czego się spodziewać

```
Profile ──navigate──▶ ItemPicker
                         │ sendResult("…itemPicker", ItemId("5")); goBack()
                         ▼
ResultEventBus: kanał "…itemPicker" (buforowany) ── ItemId("5")
                         ▼
Profile znów w kompozycji → ResultEffect zbiera z kanału → onFavouritePicked(5)
```

- **Kanał per klucz, nie per ekran.** Wynik dostaje **ten, kto akurat zbiera** dany klucz, i tylko raz. W praktyce to ekran, który otworzył picker, bo tylko on ma `ResultEffect` na ten klucz i to on jest pod pickerem.
- **Buforowanie.** Jeśli nikt akurat nie zbiera, wynik czeka w kanale i trafi do **następnego** ekranu, który zacznie zbierać ten klucz. Dlatego `ResultEffect` stawiaj tylko w ekranie, który otwiera picker, a klucz ma być unikalny dla kontraktu. W razie potrzeby `resultBus.removeResult(KEY)` czyści kanał.
- **Rotacja.** Szyna jest trzymana w `ViewModel` aktywności (`ResultEventBusHolder`), więc wynik wysłany tuż przed rotacją nie ginie. Domyślne `rememberResultEventBus()` by go zgubiło.
- **Śmierć procesu.** Szyna nie jest zapisywana. Wynik żyje ułamek sekundy, a odbiorca (ViewModel) zapisuje go u siebie, np. w `SavedStateHandle`. Gdy proces zginie, kiedy picker jest otwarty, oba ekrany odtworzą się ze stosu, a wybór zadziała normalnie.

### 6.4 Czego nie robić

| Antywzorzec | Problem |
|---|---|
| Callback w kluczu (`ItemPickerNavKey(onPick = ...)`) | nie da się go zapisać; po śmierci procesu znika |
| `ResultEffect<ItemId>()` bez klucza | wszystkie wyniki typu `ItemId` w jednym kanale; wynik jednego pickera może trafić do ekranu, który czeka na inny |
| `ResultEffect` na ekranie, który nie otwiera pickera | ten ekran może „przechwycić” wynik przeznaczony dla innego |
| Wspólny ViewModel „wyników” wstrzykiwany do obu ekranów | ukryte powiązanie; nie wiadomo, kto i kiedy czyta |
| Klucz wyniku w module odbiorcy | to zwracający ekran definiuje, co zwraca. Klucz leży obok jego klucza nawigacji |

---

## 7. Złożone argumenty – obiekt wartości

Gdy samo ID nie wystarcza, argumentem może być **mały obiekt wartości**. Przykład w kodzie: wyszukiwanie.

```kotlin
// feature/search/api – typ należy do kontraktu wyszukiwania, nie do domeny
@Serializable
data class SearchCriteria(
    val query: String = "",
    val category: ItemCategory? = null,
    val maxPrice: Int? = null,
    val sort: SearchSort = SearchSort.Relevance,
) {
    init {
        require(query.length <= MAX_QUERY_LENGTH) { "Query too long" }
        require(maxPrice == null || maxPrice > 0) { "Max price must be positive" }
    }
}

@Serializable data class SearchNavKey(val initial: SearchCriteria = SearchCriteria()) : NavKey, Destination
@Serializable data class SearchResultsNavKey(val criteria: SearchCriteria) : NavKey, Destination
```

Dlaczego to jest w porządku, a cały `Item` w kluczu nie:

| `SearchCriteria` | `Item` / `Order` |
|---|---|
| nie ma tożsamości ani „nowszej wersji” gdzie indziej: **to jest** zapytanie | ma tożsamość; w repozytorium jest zawsze nowsza wersja niż kopia w kluczu |
| ograniczony rozmiar (walidacja w `init`) | rośnie razem z modelem (listy, opisy, zdjęcia) |
| mieści się w URL-u (`search?q=&category=&maxPrice=&sort=`) | nie da się go zbudować z linku |
| należy do `search:api` | należy do domeny i przecieka do `api` celu |

Kto buduje kryteria:

| Skąd | Wywołanie |
|---|---|
| Formularz wyszukiwania (Home → 🔍) | `navigator.navigate(SearchResultsNavKey(criteria))`; stan pól formularza to stan UI (`rememberSaveable`), a dalej idzie dopiero gotowy obiekt |
| Szczegóły produktu, „More in Books” (inny moduł) | `navigator.navigate(SearchResultsNavKey(SearchCriteria(category = item.category)))` |
| Link `myapp://search?q=item&category=books&maxPrice=100&sort=price_asc` | trasa w `SearchDeepLinks` waliduje każdy parametr (nieznana wartość → link odrzucony); `parentOf` daje stos `[Home, SearchNavKey(criteria), SearchResultsNavKey(criteria)]`, więc *wstecz* z wyników pokazuje wypełniony formularz |

**Wyniki nigdy nie są argumentem.** Ekran wyników dostaje kryteria i sam ładuje dla nich dane (`SearchResultsViewModel` z kluczem w konstruktorze).

Ściąga dla innych złożonych przypadków:

| Co | Jak |
|---|---|
| Obiekt z ID | ID + repozytorium (cache, żeby nie migało) |
| Filtry, kryteria, warianty (`sealed`) | mały `@Serializable` typ w `api` celu |
| Szkic na kilka ekranów | wspólny ViewModel (rozdział 8) albo repozytorium szkicu z `DraftId` |
| Jednorazowe dane bez ID | token + magazyn tymczasowy; odbiorca obsługuje brak |
| Plik / obraz | `Uri` jako `String` |

---

## 8. Formularz na kilka ekranów – wspólny ViewModel

Przykład w kodzie: **Contact support** (Profil albo szczegóły zamówienia → *Report a problem*). Formularz ma trzy kroki (temat → opis i e-mail → podsumowanie i *Send*) i działa jako flow pełnoekranowy.

### 8.1 Ocena: czy wspólny ViewModel to dobry pomysł?

**Tak, dla krótkiego formularza wysyłanego na końcu to dobry domyślny wybór.** Jeden stan, jedna walidacja, jedno `submit()`. Kroki nie przekazują sobie danych, więc nie ma kopii, które się rozjeżdżają. Krok 2 i 3 nie mają argumentów, bo dane są w ViewModelu.

Trzeba jednak pamiętać o kosztach:

| Ryzyko | Co się dzieje | Jak to rozwiązano tutaj |
|---|---|---|
| **Nav3 nie ma ViewModeli współdzielonych przez kilka wpisów** (brak „grafów” jak w Navigation 2) | trzeba samemu zbudować zakres | `sharedViewModelScope(...)` + `rememberSharedViewModelScope()` w `core:navigation` |
| **Śmierć procesu na kroku 3** | Naiwne rozwiązanie (krok 2 i 3 „pożyczają” ViewModel kroku 1) gubi wszystko, co wpisano po opuszczeniu kroku 1, bo zapisany stan kroku 1 zamraża się, gdy ten ekran znika z kompozycji. | Zakres ma **własny zapis stanu**, utrzymywany przez `AppNavDisplay`, dopóki zakres żyje. Pola formularza są w `SavedStateHandle`. |
| **Który krok tworzy ViewModel?** | Po śmierci procesu tworzy go widoczny krok (np. 3), a nie krok 1 | ViewModel jest tworzony zawsze z **kluczem pierwszego kroku** (`scope.firstKey`), niezależnie od tego, który krok go tworzy |
| **Kiedy go czyścić?** | Za wcześnie: utrata danych. Za późno: następny formularz startuje z cudzymi danymi | Zakres żyje dokładnie tyle, ile wpis kroku 1. *Wstecz* z kroku 1, ✕ i *Done* (`closeFlow()`) go czyszczą |
| **„God ViewModel”** | ViewModel na 3 kroki łatwo rośnie w nieskończoność | Stan formularza to osobna, czysta klasa (`SupportFormState`) z walidacją per krok, testowana bez Androida |
| **Wejście prosto na krok 3** (link, powiadomienie) | niemożliwe, bo krok 3 nie ma sensu bez 1 i 2 | świadomie: kroki 2 i 3 są `internal`, publiczny jest tylko klucz kroku 1 |
| **Wysyłka w trakcie śmierci procesu** | żądanie mogło dojść albo nie | numer zgłoszenia jest w `SavedStateHandle`, więc po wysłaniu wynik przetrwa. Ponowna wysyłka po przerwaniu wymaga klucza idempotentności po stronie API (TODO dla prawdziwego backendu) |

**Kiedy wybrać coś innego:**
- **Repozytorium szkicu (`DraftId`)**: gdy szkic ma przetrwać zamknięcie aplikacji („masz niedokończone zgłoszenie”), gdy do kroków da się wejść z kilku miejsc albo gdy formularz jest długi i cenny (np. wniosek kredytowy).
- **Jeden ekran z krokami w środku** (jeden wpis nawigacji, kroki jako stan): gdy kroki są małe, a przejścia między nimi nie muszą być prawdziwą nawigacją. Wspólny stan jest wtedy za darmo, ale *wstecz* między krokami, predictive back i animacje trzeba obsłużyć samemu.

### 8.2 Jak tego użyć

```kotlin
// support:impl – kroki 2 i 3 są wewnętrzne i nie mają argumentów
@Serializable internal data object SupportDetailsNavKey : NavKey
@Serializable internal data object SupportSummaryNavKey : NavKey

// Entry provider: każdy krok (także pierwszy) deklaruje wspólny zakres
val form = sharedViewModelScope(SupportRequestNavKey::class)
entry<SupportRequestNavKey>(metadata = form) { SupportTopicRoute(onNext = { navigator.navigate(SupportDetailsNavKey) }, onClose = { navigator.closeFlow() }) }
entry<SupportDetailsNavKey>(metadata = form) { SupportDetailsRoute(onNext = { navigator.navigate(SupportSummaryNavKey) }, onBack = { navigator.goBack() }) }
entry<SupportSummaryNavKey>(metadata = form) { SupportSummaryRoute(onDone = { navigator.closeFlow() }, onBack = { navigator.goBack() }) }

// Jeden helper dla wszystkich kroków
@Composable
internal fun supportViewModel(): SupportViewModel {
    val scope = rememberSharedViewModelScope<SupportRequestNavKey>()
    return hiltViewModel<SupportViewModel, SupportViewModel.Factory>(scope.owner) { factory -> factory.create(scope.firstKey) }
}

// ViewModel: argumenty z pierwszego kroku, pola w SavedStateHandle
@HiltViewModel(assistedFactory = SupportViewModel.Factory::class)
internal class SupportViewModel @AssistedInject constructor(
    @Assisted first: SupportRequestNavKey,          // orderId, jeśli otwarty z zamówienia
    private val savedStateHandle: SavedStateHandle, // zapisywany przez zakres – przeżyje śmierć procesu na każdym kroku
    private val supportRepository: SupportRepository,
) : ViewModel()
```

### 8.3 Jak to działa pod spodem

```
flow: [SupportRequest(orderId=1001), SupportDetails, SupportSummary]
         └──────────────── zakres "flow/0/SupportRequestNavKey(orderId=1001)" ─────────────┘

AppNavDisplay
 ├─ SaveableStateProvider("flow/0/…")   ← zapis stanu zakresu, składany, DOPÓKI zakres żyje
 ├─ ViewModelStoreProvider["flow/0/…"]  ← jeden ViewModelStore na zakres
 └─ NavDisplay
      └─ krok N: rememberSharedViewModelScope() → ViewModelStoreOwner(store zakresu, zapis stanu zakresu)
```

- **Identyfikator zakresu** to tożsamość wpisu kroku 1 (rozdział 5). Dwa formularze otwarte naraz (np. na dwóch zakładkach albo jeden po drugim na tym samym stosie) są niezależne.
- **Krok bez kroku 1 pod sobą** to błąd programisty, zgłaszany od razu z czytelnym komunikatem.
- **Koniec zakresu**: gdy wpis kroku 1 znika ze wszystkich stosów, ViewModel jest czyszczony, ale dopiero gdy ostatni krok opuści kompozycję (po animacji wyjścia). Zapisany stan jest wtedy usuwany.
- **Testy**: `SharedViewModelScopeTest` (który wpis należy do którego zakresu) i `SupportFormStateTest` (walidacja kroków, brak podwójnej wysyłki).

---

## 9. Nawigacja z ViewModelu

Typowy przypadek: po zapisie trzeba przejść do szczegółów nowego obiektu (`OrderId` znany dopiero po odpowiedzi serwera).

ViewModel **nie dostaje `Navigator`a**, bo stan nawigacji żyje w kompozycji i jest zapisywany razem z nią. ViewModel wystawia stan, a Route reaguje:

```kotlin
// ViewModel
sealed interface CheckoutUiState { ...; data class Placed(val orderId: OrderId) : CheckoutUiState }

// Route
LaunchedEffect(uiState) {
    if (uiState is CheckoutUiState.Placed) onOrderPlaced(uiState.orderId)   // → navigator.navigate(OrderDetailsNavKey(id))
}
```

Stan (a nie jednorazowe „zdarzenie” w `Channel`) sprawia, że nic nie ginie przy rotacji ani wtedy, gdy ekran akurat nie jest widoczny.

---

## 10. Argumenty a zakładki

| Sytuacja | Zachowanie |
|---|---|
| `navigate(DetailsNavKey(id))` | push na **bieżącą** zakładkę, z tymi argumentami |
| Ten sam ekran z tymi samymi argumentami na dwóch zakładkach | dwa niezależne ekrany (różny stos w tożsamości) |
| Korzenie zakładek (`HomeNavKey`, `OrdersNavKey`...) | **bez argumentów** (`data object`). Zakładka ma jeden korzeń, a jej stan i filtry trzyma jej ViewModel |
| Ekran z argumentem, który ma się otworzyć na konkretnej zakładce (np. z linku) | `parentOf` w `DeepLinkConfig.kt`, patrz [navigation.md, rozdział 5](navigation.md#5-gdzie-ląduje-nowy-ekran) |
| Wynik z pickera na zakładce Profile | odbiera go Profil (jedyny ekran z `ResultEffect` na ten klucz); ekrany na ukrytych zakładkach nie są w kompozycji, więc nie zbierają |

---

## 11. Checklista: nowy ekran z argumentami

1. **Klucz w `feature:x:api`**: `@Serializable data class XNavKey(val id: XId, val mode: Mode = Mode.Default) : NavKey`.
2. **Tylko ID i proste opcje**; nowe pola z wartościami domyślnymi.
3. **Typowane ID** z `core:model`. Jeśli trzeba nowego typu, dodaj `value class` w `Ids.kt`.
4. **ViewModel z `@AssistedInject` i `@Assisted navKey: XNavKey`**, a w Route `hiltViewModel<VM, Factory> { it.create(navKey) }`.
5. **Entry provider**: `entry<XNavKey> { key -> XRoute(navKey = key, ...) }`.
6. **Klucze list w UI**: `key = { it.id.value }`, nigdy sam `value class`.
7. **Jeśli ekran zwraca wynik**: `object XResult { const val KEY = "…" }` obok klucza, a w ekranie `LocalResultEventBus.current.sendResult(XResult.KEY, value)` i `navigator.goBack()`. Odbiorca woła `ResultEffect<T>(XResult.KEY) { vm.onX(it) }` i zapisuje wynik u siebie.
8. **Inny feature, który chce otworzyć ekran**: dodaje `implementation(projects.feature.x.api)`. Nigdy `impl`.

---

## 12. Testowanie

| Test | Co sprawdza |
|---|---|
| `EntryIdTest` | równe klucze na jednym stosie to różne wpisy; tożsamość stabilna przy zmianach nad wpisem; inny ekran na tej samej pozycji to nowy wpis |
| `NavigationStateTest` | flow nad zakładkami, podział syntetycznego stosu, `resetAll` |
| `DeepLinkContractTest` | trasy linków budują klucze z typowanymi ID (`OrderDetailsNavKey(OrderId(1001))`) |
| `SearchMatchingTest` | filtrowanie i sortowanie wyników dla `SearchCriteria` |
| `SharedViewModelScopeTest` | kroki należą do zakresu najbliższego kroku 1 pod nimi; drugi formularz ma własny zakres; krok bez kroku 1 to błąd |
| `SupportFormStateTest` | walidacja kroku 2, brak podwójnej wysyłki |
| ViewModel (do dopisania per feature) | `DetailsViewModel(DetailsNavKey(ItemId("7")), FakeItemsRepository())`, bez Androida |

Na urządzeniu:
- **Item 7 → 8 → 7:** etykiety „ViewModel #1/#2/#3”; *wstecz* pokazuje #2, potem #1.
- **Profil → Favourite item → wybór:** Profil pokazuje wybrany produkt; ponowne otwarcie pickera zaznacza go (argument `selected`).
- **Śmierć procesu po wyborze:** wybór zostaje (`SavedStateHandle`).
- **Wyszukiwanie:** Home → 🔍 → kryteria → wyniki; *wstecz* wraca do wypełnionego formularza; „More in …” w szczegółach otwiera wyniki od razu; link `adb shell am start -a android.intent.action.VIEW -d "myapp://search?category=books&sort=price_asc"`.
- **Formularz:** wypełnij kroki 1 i 2, na kroku 3 wyjdź do ekranu głównego i zabij proces (`adb shell am kill <applicationId>`), wróć. Krok 3 powinien pokazać wszystkie dane; *Send* → numer zgłoszenia; ponowne otwarcie formularza startuje pusty.

---

## 13. Ograniczenia i TODO

- **Brak automatycznej kontroli kluczy.** Nic nie sprawdza w buildzie, że każdy `NavKey` jest `@Serializable` i nie zawiera „ciężkich” pól. Dobry kandydat na regułę lint/detekt albo test, który serializuje każdy zarejestrowany klucz.
- **Wyniki nie są adresowane do konkretnego ekranu**, tylko do klucza (patrz 6.3). Wystarcza, dopóki `ResultEffect` stoi tylko w ekranie, który otwiera picker.
- **Wyniki nie przeżywają śmierci procesu** w ułamku sekundy między wysłaniem a odebraniem (patrz 6.3).
- **Ponowna wysyłka formularza po przerwaniu** (śmierć procesu w trakcie żądania) wymaga klucza idempotentności w API.
- **Etykieta „ViewModel #N” w szczegółach** to element demonstracyjny. W prawdziwym ekranie należy ją usunąć.
