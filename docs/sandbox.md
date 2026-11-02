# Sandbox – jeden ficzer bez całej aplikacji

Każdy ficzer może mieć mini-aplikację `:feature:x:sandbox`, która uruchamia **tylko jego ekrany** –
bez `:app`, bez innych `impl`, bez logowania, blokady i deep linków całej aplikacji. Do tego osobny,
**lekki build** w `sandboxes/x/`: Android Studio synchronizuje i indeksuje wtedy tylko moduły tego ficzera.

## Szybki start

W Android Studio: **File → Open → `sandboxes/orders`** i uruchom konfigurację `feature.orders.sandbox`.
Z terminala, z katalogu głównego repo:

```bash
./gradlew -p sandboxes/orders :feature:orders:sandbox:installDebug
```

Sandbox instaluje się obok prawdziwej aplikacji (`dev.cyrzan.skeleton.sandbox.orders`, „Orders sandbox”).
Pierwsze otwarcie w Studio tworzy `sandboxes/orders/local.properties` (ścieżka do SDK). Z samego
terminala skopiuj go z katalogu głównego albo ustaw `ANDROID_HOME`.

## Dwa buildy, te same moduły

- **Pełny build** (katalog główny): wszystkie moduły, także wszystkie sandboxy, więc CI je buduje
  i nie gniją. W `settings.gradle.kts` to zwykłe `include(...)`, bez żadnej logiki.
- **Lekki build** (`sandboxes/x/`): własny `settings.gradle.kts` z jawną listą modułów potrzebnych
  temu sandboxowi. Ścieżki (`:feature:orders:impl`) i katalogi są te same co w pełnym buildzie,
  więc kod, `build.gradle.kts` i plugin konwencji działają bez zmian.

Lista w lekkim buildzie jest utrzymywana ręcznie. Gdy ficzer zacznie zależeć od nowego modułu,
Gradle zgłosi „project ... not found” i trzeba go tam dopisać.

## Dlaczego to działa

Architektura już to umożliwia – sandbox niczego w niej nie zmienia:

- `impl` wpina ekrany przez `Set<EntryProviderInstaller>` (Hilt). Sandbox zbiera ten sam set co `:app`,
  ale na classpathie jest tylko jeden `impl` – więc tylko jego ekrany są prawdziwe.
- Ficzery znają się wyłącznie przez klucze z `:api`. Przejście do ekranu innego ficzera
  (np. `DetailsNavKey` z zamówienia) otwiera zaślepkę „Not in this sandbox” z kluczem i argumentami –
  widać, że nawigacja zadziałała i z czym.
- `impl` nigdy nie zależy od innego `impl`, więc lekki build może bezpiecznie pominąć `:app` i pozostałe `impl`.

## Co jest w środku

| Element | Rola |
|---|---|
| `:sandbox` | Wspólna część: `SandboxActivity`, launcher scenariuszy, `SandboxNavigator`, zaślepka, minimalne bindingi, które normalnie daje `:app` |
| `skeleton.android.sandbox` | Plugin konwencji: aplikacja + Compose + Hilt, `applicationId`, etykieta, zależności na `api`/`impl` ficzera (z jego ścieżki), bez wariantu release |
| `sandboxes/x/` | Lekki build: `settings.gradle.kts` z listą modułów + kopie konfiguracji Gradle z katalogu głównego |

Nawigacja to ta sama `NavigationState` / `NavigationDisplay` co w aplikacji, z jedną zakładką
(launcherem). Ekrany `fullScreen()` otwierają się jako flow, wyniki (`goBackWithResult`) działają,
`openLink` rozpoznaje linki tego ficzera. Guardów nie ma – sesję przełącza się na launcherze.

## Scenariusze

Scenariusz to gotowy punkt startowy: stos ekranów + przygotowanie stanu. Wszystko w jednym module Hilta:

```kotlin
@Provides
fun provideSandbox(session: SessionRepository) = Sandbox(
    name = "Orders",
    scenarios = listOf(
        Scenario("Orders list", OrdersNavKey),
        Scenario("Delivered order", OrdersNavKey, OrderDetailsNavKey(OrderId(1001))) { session.signIn() },
    ),
)
```

„Back” z pierwszego ekranu scenariusza wraca do launchera.

## Sandbox dla nowego ficzera

1. `feature/x/sandbox/build.gradle.kts`:
   ```kotlin
   plugins { alias(libs.plugins.skeleton.android.sandbox) }
   ```
2. `src/main/AndroidManifest.xml` z `<application android:name=".XSandboxApp" />`
   i `@HiltAndroidApp class XSandboxApp : Application()` (korzeń Hilta musi być w module aplikacji).
3. Moduł Hilta z `Sandbox(...)` i scenariuszami.
4. W głównym `settings.gradle.kts`: `include(":feature:x:sandbox")`.
5. Lekki build: skopiuj `sandboxes/orders` do `sandboxes/x`, zmień nazwę i listę modułów
   w `settings.gradle.kts` (`core`, `:sandbox`, `api`/`impl`/`sandbox` ficzera i `api` ficzerów,
   do których nawiguje).

## Rozszerzanie – przykład `:feature:details:sandbox`

Moduł sandboxa to zwykła aplikacja: można do niej dołożyć wszystko, czego ficzer potrzebuje,
bez zmian w `:sandbox`.

**Prawdziwy sąsiad zamiast zaślepki** – z detali przechodzi się do wyników wyszukiwania:

```kotlin
// feature/details/sandbox/build.gradle.kts
implementation(projects.feature.search.impl)
```

plus `:feature:search:impl` na liście w `sandboxes/details/settings.gradle.kts`.

**Własny ekran tylko w sandboxie** – `ItemPicker` zwraca wynik, ale Details sam go nie otwiera.
`PickerHarness.kt` dodaje ekran, który go otwiera i pokazuje, co wróciło. Wpina się dokładnie
tak jak ekrany ficzerów: własny `EntryProviderInstaller` `@IntoSet` + klucz + scenariusz
`Scenario("Item picker (with result)", PickerHarnessNavKey)`.

Tą samą drogą: ekran debugowy, podgląd stanów komponentów, własne bindingi Hilta (mocki, Chucker).

## Ograniczenia i dalsze kroki

- Dane to fake'owe repozytoria z `:core:data` – te same co w aplikacji. Gdy dojdą prawdziwe,
  sandbox powinien móc podmienić je na fake'i/mocki (np. osobny moduł bindingów zamiast `DataModule`).
- Lekki build ma kopie `gradle.properties`, `build.gradle.kts` (deklaracje pluginów) i
  `gradle/wrapper`, `gradle/gradle-daemon-jvm.properties` z katalogu głównego. Przy podbiciu
  Gradle lub zmianie tych plików trzeba zaktualizować też kopie.
- Oba buildy piszą do tych samych katalogów `build/`: nie uruchamiaj ich jednocześnie. Dzięki
  lokalnemu build cache przełączanie się między nimi jest szybkie.
- CI buduje sandboxy w pełnym buildzie (`./gradlew assembleDebug`). Żeby wyłapać nieaktualne listy
  modułów, warto dodać `./gradlew -p sandboxes/x :feature:x:sandbox:assembleDebug`.
