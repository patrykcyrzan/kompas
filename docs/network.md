# Sieć – `:core:network`, warianty debug/release, Chucker i mocki

Ten dokument opisuje, **jak zbudowana jest warstwa HTTP**: co jest w `:core:network`, skąd biorą się timeouty i base URL, co dokłada build debug (Chucker, mocki z assetów) i jak dodać nowe API.

---

## 1. Zasady w pigułce

1. **Konfiguracja sieci siedzi w `:core:network`, nie w `BuildConfig`.** Moduł ma własne source sety `debug`/`release` z `VariantNetworkConfig.kt`; wariant `debug` aplikacji bierze wariant `debug` tego modułu. Featurów moduł nie zna.
2. **`:app` dokłada narzędzia debug**: dostarcza `ExtraInterceptors` (osobna wersja w `src/debug` i `src/release`).
3. **Featury nie widzą sieci.** Graf: `feature:*:impl → core:data → core:network`. Serwisy Retrofit i DTO są w `:core:data`, na zewnątrz wychodzą modele z `:core:model`.
4. **Narzędzia debug nie trafiają do release'owego APK** – ani Chucker, ani mocki, ani ekran debug.

## 2. Moduły i klasy

| Gdzie | Co |
|---|---|
| `core/network/src/main/.../NetworkConfig.kt` | `baseUrl`, `connectTimeout`, `readTimeout`; stała `API_BASE_URL` |
| `core/network/src/{debug,release}/.../VariantNetworkConfig.kt` | `networkConfig` z timeoutami danego wariantu |
| `core/network/src/{debug,release}/.../VariantInterceptors.kt` | `HttpLoggingInterceptor` w debug, pusta lista w release |
| `core/network/.../ExtraInterceptors.kt` | lista interceptorów od aplikacji; kolejność = od zewnętrznego |
| `core/network/.../di/NetworkModule.kt` | `Json`, `OkHttpClient`, `Retrofit` (`@Singleton`) |
| `app/src/debug/.../network/VariantNetworkModule.kt` | Chucker + `MockInterceptor` |
| `app/src/release/.../network/VariantNetworkModule.kt` | pusta lista |
| `app/src/debug/.../network/DebugMenuActivity.kt` | przełącznik mocków, wejście do Chuckera |

Oba `VariantNetworkModule` mają tę samą nazwę i pakiet – w danym wariancie kompiluje się dokładnie jeden. Brak jednego z nich wywali build tego wariantu na grafie Hilta (brak `ExtraInterceptors`).

## 3. Timeouty i base URL

W `:core:network`, osobno dla każdego build type'u: `src/debug/.../VariantNetworkConfig.kt` i `src/release/.../VariantNetworkConfig.kt`:

| | debug | release |
|---|---|---|
| connect | 60 s | 10 s |
| read | 60 s | 15 s |

Debug ma zapas na breakpointy i wolne środowiska dev, release ma szybko się poddać i pokazać retry. `API_BASE_URL` jest wspólny w `NetworkConfig.kt` – przy kilku środowiskach zrób flavory (`dev`/`staging`/`prod`) i trzymaj adres w `src/<flavor>` tego modułu.

Nowy build type w `:app` (np. `benchmark`, `staging`) musi wskazać, który wariant `:core:network` brać: `matchingFallbacks += "release"`. Bez tego Gradle nie dopasuje wariantu.

Dłuższy timeout dla pojedynczego endpointu (upload) – nie podbijaj globalnego, użyj `chain.withReadTimeout(...)` w interceptorze albo `client.newBuilder()` z kwalifikatorem Hilta.

## 4. Interceptory debug – kolejność i gdzie je dodawać

Łańcuch w debug (od zewnętrznego): `HttpLoggingInterceptor` → Chucker → `MockInterceptor` → sieć. Logowanie i Chucker widzą więc też odpowiedzi z mocków.

- **Czysto sieciowe** (logowanie, sztuczne opóźnienie) → `core/network/src/debug/.../VariantInterceptors.kt`. Działa w każdej aplikacji używającej modułu.
- **Wymagające aplikacji** (`Context`, assety, ekran debug) → `ExtraInterceptors` w `app/src/debug/.../VariantNetworkModule.kt`.
- Zależność biblioteki tylko przez `debugImplementation` i odpowiednik w `src/release` z pustą listą.

Logcat: tag `okhttp.OkHttpClient`, poziom `BODY`, nagłówki `Authorization` i `Cookie` zamazane.

## 5. Chucker

Tylko `debugImplementation` – release nie ma nawet `library-no-op`, bo żaden kod z `src/main` go nie dotyka. Nagłówki `Authorization` i `Cookie` są zamazywane. Otwierasz go z notyfikacji, z ekranu „Skeleton Debug” albo ze skrótu na ikonie aplikacji.

## 6. Mocki

`MockInterceptor` odpowiada z `app/src/debug/assets/mocks/`, kluczem jest metoda i ścieżka:

```
GET /v1/orders       -> mocks/get/v1/orders.json
POST /v1/orders      -> mocks/post/v1/orders.json
GET /v1/orders (500) -> mocks/get/v1/orders.500.json   // wygrywa z orders.json
```

- **Brak pliku → prawdziwy backend.** Można mockować pojedyncze endpointy.
- Kod błędu w nazwie pliku (400, 401, 403, 404, 409, 422, 500, 503) pozwala przeklikać stany błędów. Żeby wrócić do 200, usuń/zmień nazwę pliku z kodem.
- Odpowiedź z mocka ma nagłówek `X-Mock: true` i jest widoczna w Chuckerze (Chucker jest zewnętrzny).
- Włączanie: ikona **Skeleton Debug** w launcherze (osobna aktywność w osobnym tasku) → „Mock API”. Stan przeżywa restart (SharedPreferences).

Mocki służą do ręcznego klikania. W testach jednostkowych/UI używaj `MockWebServer`.

## 7. Jak dodać API

1. W `:core:data` dodaj DTO (`@Serializable`) i interfejs Retrofit, np. `OrdersApi`.
2. Utwórz go w module Hilt `:core:data`: `retrofit.create(OrdersApi::class.java)`.
3. Napisz repozytorium mapujące DTO → model z `:core:model` i podmień binding `Fake*Repository` w `DataModule`.
4. Dorzuć plik do `assets/mocks`, jeśli chcesz pracować bez backendu.

## 8. Testowanie

`app/src/testDebug/.../MockInterceptorTest.kt` – dopasowanie pliku, priorytet kodu błędu, fallback do backendu, wyłączone mocki. Sieci nie ma: „backend” to interceptor na końcu łańcucha.

## 9. Ograniczenia i TODO

- Brak `AuthInterceptor` – token z sesji do dopięcia, gdy `SessionRepository` dostanie prawdziwy backend.
- Repozytoria w `:core:data` są nadal `Fake*`; `mocks/get/v1/orders.json` to przykład formatu, nikt go jeszcze nie woła.
- Base URL `https://api.kompas.example.com/` to placeholder.
- `DebugMenuActivity` żyje w osobnym tasku i nie jest objęta blokadą PIN (tylko debug).
