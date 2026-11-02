# Sandbox – jeden ficzer bez całej aplikacji

Każdy ficzer może mieć mini-aplikację `:feature:x:sandbox`, która uruchamia **tylko jego ekrany** –
bez `:app`, bez innych `impl`, bez logowania, blokady i deep linków całej aplikacji.

## Szybki start

W Android Studio wybierz konfigurację uruchomieniową modułu `feature.orders.sandbox` albo:

```bash
./gradlew :feature:orders:sandbox:installDebug
```

Sandbox instaluje się obok prawdziwej aplikacji (`dev.cyrzan.skeleton.sandbox.orders`, „Orders sandbox”).

Gradle kompiluje tylko to, od czego sandbox zależy (ten ficzer, `:core`, potrzebne `:api`) – nie `:app`
ani innych `impl`, choć są w `settings.gradle.kts`. Konfigurację projektu przy kolejnych uruchomieniach
pomija configuration cache.

## Dlaczego to działa

Architektura już to umożliwia – sandbox niczego w niej nie zmienia:

- `impl` wpina ekrany przez `Set<EntryProviderInstaller>` (Hilt). Sandbox zbiera ten sam set co `:app`,
  ale na classpathie jest tylko jeden `impl` – więc tylko jego ekrany są prawdziwe.
- Ficzery znają się wyłącznie przez klucze z `:api`. Przejście do ekranu innego ficzera
  (np. `DetailsNavKey` z zamówienia) otwiera zaślepkę „Not in this sandbox” z kluczem i argumentami –
  widać, że nawigacja zadziałała i z czym.
- `impl` nigdy nie zależy od innego `impl`, więc sandbox nie ciągnie za sobą reszty aplikacji.

## Co jest w środku

| Element | Rola |
|---|---|
| `:sandbox` | Wspólna część: `SandboxActivity`, launcher scenariuszy, `SandboxNavigator`, zaślepka, minimalne bindingi, które normalnie daje `:app` |
| `skeleton.android.sandbox` | Plugin konwencji: aplikacja + Compose + Hilt, `applicationId`, etykieta, zależności na `api`/`impl` ficzera (z jego ścieżki), bez wariantu release |

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
4. W `settings.gradle.kts`: `include(":feature:x:sandbox")`.

## Rozszerzanie – przykład `:feature:details:sandbox`

Moduł sandboxa to zwykła aplikacja: można do niej dołożyć wszystko, czego ficzer potrzebuje,
bez zmian w `:sandbox`.

**Prawdziwy sąsiad zamiast zaślepki** – z detali przechodzi się do wyników wyszukiwania:

```kotlin
// feature/details/sandbox/build.gradle.kts
implementation(projects.feature.search.impl)
```

**Własny ekran tylko w sandboxie** – `ItemPicker` zwraca wynik, ale Details sam go nie otwiera.
`PickerHarness.kt` dodaje ekran, który go otwiera i pokazuje, co wróciło. Wpina się dokładnie
tak jak ekrany ficzerów: własny `EntryProviderInstaller` `@IntoSet` + klucz + scenariusz
`Scenario("Item picker (with result)", PickerHarnessNavKey)`.

Tą samą drogą: ekran debugowy, podgląd stanów komponentów, własne bindingi Hilta (mocki, Chucker).

## Ograniczenia i dalsze kroki

- Dane to fake'owe repozytoria z `:core:data` – te same co w aplikacji. Gdy dojdą prawdziwe,
  sandbox powinien móc podmienić je na fake'i/mocki (np. osobny moduł bindingów zamiast `DataModule`).
- Android Studio synchronizuje cały projekt. Jeśli w dużym monolicie sync zacznie boleć, można
  dołożyć ładowanie tylko wybranego ficzera (np. plugin Focus od Dropboxa) – sandboxy działają bez zmian.
- CI buduje sandboxy razem z resztą (`./gradlew assembleDebug`), więc nie gniją.
