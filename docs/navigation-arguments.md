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
7. [Nawigacja z ViewModelu](#7-nawigacja-z-viewmodelu)
8. [Argumenty a zakładki](#8-argumenty-a-zakładki)
9. [Checklista: nowy ekran z argumentami](#9-checklista-nowy-ekran-z-argumentami)
10. [Testowanie](#10-testowanie)
11. [Ograniczenia i TODO](#11-ograniczenia-i-todo)

---

## 1. Zasady w pigułce

1. **Argumenty ekranu to pola jego klucza** (`data class OrderDetailsNavKey(val orderId: OrderId)`). Nie ma osobnego „Bundle z argumentami” ani stringowych nazw.
2. **Klucz leży w module `api` feature'a.** Inne feature'y znają tylko klucz, nigdy ekran.
3. **W kluczu są tylko małe, serializowalne dane**: ID i proste opcje. Nie ma w nim obiektów domenowych, callbacków ani list.
4. **ID są typowane** (`ItemId`, `OrderId`), więc nie da się podać ID zamówienia tam, gdzie oczekiwane jest ID produktu.
5. **ViewModel dostaje klucz w konstruktorze** (Hilt assisted injection). Nie ma `SavedStateHandle["orderId"]`.
6. **Każdy wpis na stosie ma własną tożsamość.** Ten sam ekran z tymi samymi argumentami dwa razy to dwa niezależne ekrany.
7. **Wynik wraca typowanym kluczem** (`ResultKey<T>`) dokładnie do ekranu, który otworzył zwracający ekran. Stan, który ma przetrwać dłużej, trafia do repozytorium.

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
profile:impl  ──navigate(ItemPickerNavKey(...))──▶  details:api   (klucz + ResultKey)
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

- **Używane wszędzie, gdzie ID przekracza granicę modułu**: w kluczach, repozytoriach (`observeOrder(id: OrderId)`), wynikach (`ResultKey<ItemId>`) i trasach linków (`OrderDetailsNavKey(OrderId(it))`).
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

### 6.1 Kiedy wynik, a kiedy repozytorium

| Sytuacja | Mechanizm |
|---|---|
| Wybór, o którym decyduje ekran wywołujący (picker: produkt, adres, data, filtr) | **wynik** (`ResultKey`) |
| Zmiana, która ma przetrwać i być widoczna wszędzie (edycja profilu, ocena zamówienia, zapis adresu) | **repozytorium**: ekran zapisuje, inne obserwują; żadnego wyniku |
| Logowanie, które ma odblokować przerwany link | `resumePendingLink()` (patrz [deeplinks.md](deeplinks.md)) |

### 6.2 API

```kotlin
// details:api – obok klucza ekranu, który ZWRACA wynik
@Serializable
data class ItemPickerNavKey(val selected: ItemId? = null) : NavKey

data object ItemPickerResult : ResultKey<ItemId>("details.itemPicker")
```

```kotlin
// details:impl – picker oddaje wybór
entry<ItemPickerNavKey> { key ->
    ItemPickerRoute(
        selected = key.selected,
        onPick = { itemId -> navigator.goBackWithResult(ItemPickerResult, itemId) },
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
    // Delivered when the picker opened from THIS screen goes back with a choice.
    ResultEffect(ItemPickerResult, viewModel::onFavouritePicked)
    ...
}

// ProfileViewModel – wynik zapisany przez odbiorcę (przeżyje śmierć procesu)
fun onFavouritePicked(itemId: ItemId) {
    savedStateHandle[KEY_FAVOURITE] = itemId.value
}
```

### 6.3 Jak to działa

```
Profile (tab2/0) ──navigate──▶ ItemPicker (tab2/1)
                                   │ goBackWithResult(ItemPickerResult, ItemId("5"))
                                   ▼
NavigationState: pop → nowy wierzch = tab2/0 → NavResultStore[tab2/0, "details.itemPicker"] = 5
                                   ▼
ResultEffect w Profile (tab2/0): widzi wynik dla SIEBIE → take() → onFavouritePicked(5)
```

- **Adresowanie po tożsamości wpisu** (rozdział 5). Wynik trafia do ekranu, który jest na wierzchu po powrocie. Ten sam ekran w innym miejscu stosu ani na innej zakładce go nie dostanie.
- **Dostarczenie jednorazowe**: `take()` usuwa wynik. Rotacja nie wywoła go drugi raz.
- **Wyniki do zdjętych wpisów są usuwane** przy kolejnym `goBackWithResult`. Przy wylogowaniu (`resetAll`) usuwane są wszystkie.
- **Rotacja.** Magazyn wyników jest trzymany w `ViewModel` aktywności, więc nic nie ginie, nawet jeśli rotacja wypadnie między powrotem a odebraniem.
- **Śmierć procesu.** Wyniki nie są zapisywane, bo dostarczenie trwa jedną klatkę. Odbiorca (ViewModel) zapisuje wynik u siebie, np. w `SavedStateHandle`. Gdy proces zginie, kiedy picker jest otwarty, oba ekrany odtworzą się ze stosu, a wynik zostanie dostarczony normalnie po wyborze.

### 6.4 Czego nie robić

| Antywzorzec | Problem |
|---|---|
| Callback w kluczu (`ItemPickerNavKey(onPick = ...)`) | nie da się go zapisać; po śmierci procesu znika |
| Wspólny ViewModel „wyników” wstrzykiwany do obu ekranów | ukryte powiązanie; nie wiadomo, kto i kiedy czyta; wynik może trafić do niewłaściwego ekranu |
| Globalny event bus | brak adresata, wynik ginie, gdy nikt akurat nie nasłuchuje |
| `ResultKey` w module odbiorcy | to zwracający ekran definiuje, co zwraca. Klucz wyniku leży obok jego klucza nawigacji |

---

## 7. Nawigacja z ViewModelu

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

## 8. Argumenty a zakładki

| Sytuacja | Zachowanie |
|---|---|
| `navigate(DetailsNavKey(id))` | push na **bieżącą** zakładkę, z tymi argumentami |
| Ten sam ekran z tymi samymi argumentami na dwóch zakładkach | dwa niezależne ekrany (różny stos w tożsamości) |
| Korzenie zakładek (`HomeNavKey`, `OrdersNavKey`...) | **bez argumentów** (`data object`). Zakładka ma jeden korzeń, a jej stan i filtry trzyma jej ViewModel |
| Ekran z argumentem, który ma się otworzyć na konkretnej zakładce (np. z linku) | `parentOf` w `DeepLinkConfig.kt`, patrz [navigation.md, rozdział 5](navigation.md#5-gdzie-ląduje-nowy-ekran) |
| Wynik z pickera na zakładce Profile | wraca do ekranu na zakładce Profile; inne zakładki są nietknięte |

---

## 9. Checklista: nowy ekran z argumentami

1. **Klucz w `feature:x:api`**: `@Serializable data class XNavKey(val id: XId, val mode: Mode = Mode.Default) : NavKey`.
2. **Tylko ID i proste opcje**; nowe pola z wartościami domyślnymi.
3. **Typowane ID** z `core:model`. Jeśli trzeba nowego typu, dodaj `value class` w `Ids.kt`.
4. **ViewModel z `@AssistedInject` i `@Assisted navKey: XNavKey`**, a w Route `hiltViewModel<VM, Factory> { it.create(navKey) }`.
5. **Entry provider**: `entry<XNavKey> { key -> XRoute(navKey = key, ...) }`.
6. **Klucze list w UI**: `key = { it.id.value }`, nigdy sam `value class`.
7. **Jeśli ekran zwraca wynik**: `data object XResult : ResultKey<T>("x.name")` obok klucza i `navigator.goBackWithResult(XResult, value)`. Odbiorca woła `ResultEffect(XResult) { vm.onX(it) }` i zapisuje wynik u siebie.
8. **Inny feature, który chce otworzyć ekran**: dodaje `implementation(projects.feature.x.api)`. Nigdy `impl`.

---

## 10. Testowanie

| Test | Co sprawdza |
|---|---|
| `EntryIdTest` | równe klucze na jednym stosie to różne wpisy; tożsamość stabilna przy zmianach nad wpisem; inny ekran na tej samej pozycji to nowy wpis |
| `NavigationStateTest` | wynik trafia do ekranu, który otworzył zwracający; ten sam ekran niżej na stosie go nie dostaje; wyniki do zdjętych wpisów są usuwane; `resetAll` czyści wszystko |
| `DeepLinkContractTest` | trasy linków budują klucze z typowanymi ID (`OrderDetailsNavKey(OrderId(1001))`) |
| ViewModel (do dopisania per feature) | `DetailsViewModel(DetailsNavKey(ItemId("7")), FakeItemsRepository())`, bez Androida |

Na urządzeniu:
- **Item 7 → 8 → 7:** etykiety „ViewModel #1/#2/#3”; *wstecz* pokazuje #2, potem #1.
- **Profil → Favourite item → wybór:** Profil pokazuje wybrany produkt; ponowne otwarcie pickera zaznacza go (argument `selected`).
- **Śmierć procesu po wyborze:** wybór zostaje (`SavedStateHandle`).

---

## 11. Ograniczenia i TODO

- **Brak automatycznej kontroli kluczy.** Nic nie sprawdza w buildzie, że każdy `NavKey` jest `@Serializable` i nie zawiera „ciężkich” pól. Dobry kandydat na regułę lint/detekt albo test, który serializuje każdy zarejestrowany klucz.
- **Wynik do ekranu pod flow.** Jeśli flow ma jeden ekran, a ten zwraca wynik, wynik trafia do ekranu zakładki pod flow, bo to on jest na wierzchu po powrocie. To zamierzone, ale warto o tym pamiętać.
- **Wyniki nie przeżywają śmierci procesu** w ułamku sekundy między powrotem a odebraniem (patrz 6.3).
- **Etykieta „ViewModel #N” w szczegółach** to element demonstracyjny. W prawdziwym ekranie należy ją usunąć.
