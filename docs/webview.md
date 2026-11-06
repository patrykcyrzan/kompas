# WebView – jeden mechanizm, reguły w ficzerach

Jak w aplikacji wielomodułowej pokazywać strony WWW tak, żeby każdy moduł nie pisał własnego `WebViewClient`
i nie powtarzał tych samych błędów. Opis rozwiązania z Kompasu (`:core:webview`, `:feature:webpage`)
i tego, jak to wygląda w dużych, produkcyjnych aplikacjach.

## Spis treści

1. [Problem](#1-problem)
2. [Podział: mechanizm w core, reguły w ficzerze](#2-podział-mechanizm-w-core-reguły-w-ficzerze)
3. [Stan strony: Loading / Loaded / Error](#3-stan-strony-loading--loaded--error)
4. [Linki: łańcuch reguł zamiast `shouldOverrideUrlLoading` w każdym module](#4-linki-łańcuch-reguł)
5. [Bezpieczeństwo](#5-bezpieczeństwo)
6. [Compose, nawigacja i cykl życia](#6-compose-nawigacja-i-cykl-życia)
7. [Sandbox i testy](#7-sandbox-i-testy)
8. [Przenoszenie istniejącego modułu z własnym clientem](#8-przenoszenie-istniejącego-modułu)
9. [Co jeszcze robią duże aplikacje](#9-co-jeszcze-robią-duże-aplikacje)

---

## 1. Problem

Typowy moduł z WebView ma własny client, mniej więcej taki:

```kotlin
class XWebViewClient(private val openBrowserAction: (String) -> Unit) : WebViewClient() {
    var hasError = false
    var dataLoaded = false
    fun reset() { hasError = false; dataLoaded = false }

    override fun onPageStarted(...)   { /* status = Loading */ }
    override fun onReceivedError(...) { hasError = true /* status = Error */ }
    override fun onPageFinished(...)  { if (!hasError) { dataLoaded = true /* status = Loaded */ } }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.getQueryParameter("target") == "newTab") { openBrowserAction(request.url.toString()); return true }
        return false
    }
}
```

Działa, dopóki moduł jest jeden. Gdy z aplikacji robi się wiele modułów, pojawiają się takie problemy:

| Problem | Skutek |
|---|---|
| Każdy moduł ma swój client | Te same błędy powtarzają się w N miejscach, a poprawka trafia tylko do jednego. |
| Dwie flagi i trzy statusy | `hasError = true` razem z `dataLoaded = true` to stan niemożliwy, a da się go zapisać. |
| Ręczne `reset()` | Zapomniany przy reloadzie, przycisku „Spróbuj ponownie” albo nowym URL-u, a wtedy stary błąd zostaje. |
| `onReceivedError` bez `isForMainFrame` | Pada obrazek albo skrypt analityki, a cały ekran pokazuje błąd. |
| Ustawienia `WebSettings` kopiowane per moduł | Jeden moduł ma `allowFileAccess = true`, drugi nie ma; bezpieczeństwo zależy od tego, kto pisał. |

## 2. Podział: mechanizm w core, reguły w ficzerze

Tak robią to duże aplikacje: **jeden client na całą aplikację**, a ficzer dokłada tylko swoje reguły.

```
:core:webview                  mechanizm – taki sam dla każdego WebView
├── KompasWebView              composable: AndroidView + bezpieczne ustawienia + Back + lifecycle
├── KompasWebViewClient        jedyny WebViewClient: callbacki → zdarzenia, linki → reguły
├── WebViewState / PageLoad    stan strony (Loading / Loaded / Error), tytuł, historia
├── UrlHandler                 interfejs jednej reguły dla linków
└── HostAllowlist, openExternally   klocki do reguł

:feature:webpage               polityka – co TEN ekran robi z linkami
├── api: WebPageNavKey(url, title)
├── impl: WebPageScreen        toolbar, pasek postępu, ekran błędu (UI w stylu aplikacji)
│         WebPageLinks.kt      reguły: newTab → przeglądarka, linki aplikacji → ekrany, obce hosty → przeglądarka
└── sandbox                    scenariusze: linki, 404, brak sieci, zły certyfikat, crash renderera
```

Zasada jest taka: **ficzer nigdy nie dziedziczy po `WebViewClient`**. Gdy potrzebuje czegoś nowego, robi jedną z trzech rzeczy:
- dodaje regułę `UrlHandler`;
- korzysta z hooka (`onDownload`, `configure` do JS bridge);
- jeśli to przyda się wszystkim, rozszerza `:core:webview`.

Inne ficzery też mogą z tego korzystać na dwa sposoby:
- **otworzyć stronę**: `navigator.navigate(WebPageNavKey(url))`, przykładem jest Help w Settings;
- **wstawić WebView do własnego ekranu**: `KompasWebView(...)` z własnymi regułami, przykładem jest harness w sandboxie pdfviewera z JS bridge.

## 3. Stan strony: Loading / Loaded / Error

```kotlin
sealed interface WebPageState {
    data class Loading(val progress: Int = 0) : WebPageState
    data object Loaded : WebPageState
    data class Error(val error: WebPageError) : WebPageState   // Network / Http / Ssl / RendererGone
}
```

Jedna wartość zamiast dwóch flag. Cała logika siedzi w `PageLoad.reduce(event)`, czyli w czystym Kotlinie
bez Androida, dzięki czemu jest testowana zwykłym JUnitem (`WebPageStateTest`). Client tylko tłumaczy
callbacki WebView na zdarzenia `Started` / `Progress` / `Failed` / `Finished`.

Pułapki, które ta maszyna stanów obsługuje (każda zdarza się w prawdziwych aplikacjach):

1. **`onPageFinished` przychodzi także po błędzie.** WebView pokazuje wtedy własną stronę błędu i ją
   „kończy”. Bez ochrony `Error` zostaje nadpisany przez `Loaded`, i właśnie dlatego istnieje flaga `hasError`.
2. **`onReceivedError` / `onReceivedHttpError` przychodzą też dla subresource'ów**, czyli obrazków, skryptów i iframe'ów.
   Liczy się tylko `request.isForMainFrame`.
3. **Błąd HTTP przychodzi PRZED `onPageStarted` swojej strony.** Sprawdzone na urządzeniu, kolejność dla 404:
   ```
   Failed(Http(404))  →  Started  →  Finished
   ```
   Błąd przychodzi z nagłówkami odpowiedzi, a „start strony” dopiero z pierwszymi bajtami treści.
   Naiwny reset w `onPageStarted` (a ręczne `reset()` przy nowym ładowaniu działa tak samo) **gubi każdy błąd HTTP**,
   więc strona 404 kończy jako `Loaded`. Dla błędów sieci (brak hosta) kolejność jest inna: `Started → Failed → Finished`.
   Dlatego `PageLoad` pamięta błąd, który przyszedł między ładowaniami, i pokazuje go przy najbliższym `Started`.
4. **Jeden problem, dwa błędy.** Zły certyfikat daje najpierw błąd SSL, potem ogólny błąd sieci. Zostaje pierwszy, bo mówi, co się stało.
5. **Crash renderera** (`onRenderProcessGone`) bez `return true` zabija **całą aplikację**. Po nim ten WebView
   jest martwy: `KompasWebView` usuwa go z kompozycji, a „Spróbuj ponownie” tworzy nowy. Renderer pada też wtedy,
   gdy system zabija go, żeby odzyskać pamięć w tle, więc to nie jest teoretyczny przypadek.
6. **Reset nie jest potrzebny.** Każde ładowanie zaczyna się od `Started`, który czyści błąd poprzedniej strony.
   `reset()` znika z API, więc nie ma czego zapomnieć.

Tytuł strony (`state.title`) bierze się z `onReceivedTitle`. Przy błędzie jest czyszczony, bo strona błędu WebView ma własny
tytuł („Web page not available”), który inaczej trafiłby do toolbara.

## 4. Linki: łańcuch reguł

```kotlin
fun interface UrlHandler {
    fun handle(url: Uri): Boolean   // true = reguła bierze link, WebView go nie ładuje
}
```

Reguły ficzera `:feature:webpage` (kolejność ma znaczenie):

```kotlin
listOf(
    // 1. ?target=newTab – strona sama prosi o wyjście z aplikacji
    UrlHandler { url -> (url.isHierarchical && url.getQueryParameter("target") == "newTab").also { if (it) openInBrowser(url) } },
    // 2. linki aplikacji → ekrany, przez te same guardy co linki z zewnątrz; Blocked NIE idzie do przeglądarki
    UrlHandler { url -> openLink(url.toString()) != OpenLinkResult.Unsupported },
    // 3. obcy host → przeglądarka; nasza strona nigdy nie dostaje „ramki” aplikacji
    UrlHandler { url -> (url.scheme in WEB && !TRUSTED_HOSTS.allows(url.host)).also { if (it) openInBrowser(url) } },
)
```

Gdy żadna reguła nie weźmie linku, decyduje core:
- `http(s)` ładuje się w WebView;
- `tel:` / `mailto:` / `sms:` / `geo:` otwierają się w swoich aplikacjach;
- reszta (`intent:`, `file:`, `javascript:`, `chrome:`) jest odrzucana.

Na co uważać:

- **Tylko main frame.** `shouldOverrideUrlLoading` przychodzi też dla iframe'ów, np. mapy, wideo albo widgetu płatności.
  Gdyby trafiały do reguł, iframe z obcego hosta wyrzucałby użytkownika do przeglądarki.
- **`getQueryParameter` rzuca wyjątek dla URI nieprzezroczystych** (`mailto:x`, `tel:1`), więc trzeba najpierw sprawdzić `isHierarchical`.
- **Host porównujemy po hoście, nie `startsWith` na URL-u.** `https://example.com.evil.net` i `https://example.com@evil.net`
  oba zaczynają się od `https://example.com`. Testy pokrywają to w `HostAllowlistTest`.
- **Przekierowania też przechodzą przez reguły.** Zaufana strona, która przekierowuje do logowania u zewnętrznego dostawcy (SSO),
  wypadnie do przeglądarki, chyba że host logowania jest na allowliście.
- **POST nie przechodzi przez `shouldOverrideUrlLoading`.** Formularz wysłany POST-em ładuje się bez pytania reguł.
- **Otwieranie na zewnątrz** (`openExternally`) przepuszcza tylko bezpieczne schematy, dodaje `CATEGORY_BROWSABLE` i **nie dodaje
  `FLAG_ACTIVITY_NEW_TASK`, jeśli jest Activity**. Z tą flagą przeglądarka dostaje osobny task i Back z niej
  wraca na ekran główny zamiast do aplikacji (sprawdzone na urządzeniu).
- **Linki do pobrania** (PDF, ZIP) nie przechodzą przez reguły, tylko przez `DownloadListener`. W Kompasie PDF otwiera się
  w przeglądarce PDF z `:feature:pdfviewer`, a reszta trafia do przeglądarki.

## 5. Bezpieczeństwo

Domyślne ustawienia w `KompasWebView`, w jednym miejscu dla całej aplikacji:

| Ustawienie | Dlaczego |
|---|---|
| `allowFileAccess = false`, `allowContentAccess = false` | Strona nie czyta plików ani content providerów aplikacji. |
| `mixedContentMode = NEVER_ALLOW` | Strona HTTPS nie ładuje skryptów po HTTP. |
| `safeBrowsingEnabled = true` | Ostrzeżenie Google przed znanymi stronami phishingowymi. |
| `setSupportMultipleWindows(false)` | `target="_blank"` / `window.open()` otwierają się w tym samym WebView, więc przechodzą przez reguły. |
| `domStorageEnabled = true` | Bez `localStorage` większość nowoczesnych stron nie działa. |

Do tego:

- **SSL: zawsze `handler.cancel()`, nigdy `proceed()`.** Google Play odrzuca aplikacje z `proceed()`.
  Na ekranie błędu SSL nie ma też „Otwórz w przeglądarce”.
- **`WebPageNavKey` nie jest celem deeplinka.** Link z zewnątrz nie może otworzyć dowolnej strony w ramce aplikacji,
  bo to gotowy phishing. Tę samą decyzję podjęliśmy dla `PdfViewerNavKey`.
- **JS bridge (`addJavascriptInterface`) widzi każda strona i każdy iframe** w tym WebView. Metoda bridge'a
  musi sprawdzić origin (`webView.url`, czytane na wątku Main, bo bridge działa na wątku JavaBridge). Przykład jest w
  `WebViewHarness.kt` w sandboxie pdfviewera. Jeszcze lepiej użyć `WebViewCompat.addWebMessageListener` z `androidx.webkit`
  z listą dozwolonych originów, bo wtedy weryfikację robi WebView (patrz [9](#9-co-jeszcze-robią-duże-aplikacje)).
- **Nagłówki z `loadUrl(url, headers)` idą tylko z pierwszym requestem**, a nie z linkami klikniętymi później.
  Sesja webowa (np. token) powinna być w cookies (`CookieManager`), a nie w nagłówku.

## 6. Compose, nawigacja i cykl życia

- **Nieaktualne lambdy.** Client i listenery powstają raz, w `factory` `AndroidView`. Lambda złapana tam
  (np. `openBrowserAction`) z czasem staje się nieaktualna. W `KompasWebView` reguły są podmieniane w `update`
  przy każdej rekompozycji, a `onDownload` idzie przez `rememberUpdatedState`.
- **Nav3 usuwa ekran z kompozycji**, gdy na wierzch wchodzi inny ekran (np. PDF z linku). AndroidView jest wtedy niszczony.
  `WebViewState` zapisuje historię strony (`WebView.saveState`) i po powrocie nowy WebView wraca na tę samą stronę.
  To samo dzieje się po śmierci procesu, bo stan idzie przez `rememberSaveable`. Ograniczenie: strona przeładowuje się,
  więc stan DOM (wypełniony formularz, scroll) przepada.
- **Back** najpierw cofa się w historii strony (`BackHandler(enabled = canGoBack)`), a dopiero potem zamyka ekran.
  Przycisk ✕ w toolbarze zamyka ekran od razu.
- **`onPause` / `onResume`** idą za cyklem życia ekranu, żeby strona w tle nie odtwarzała animacji ani mediów.
  Wywołanie `pauseTimers()` jest **globalne** dla wszystkich WebView w procesie, dlatego go tu nie ma.

## 7. Sandbox i testy

```bash
./gradlew :feature:webpage:sandbox:installDebug
./gradlew :core:webview:testDebugUnitTest
```

| Scenariusz | Co sprawdza |
|---|---|
| Links | Każdy rodzaj linku z sekcji 4: zaufany, `_blank`, `newTab`, obcy host, podobny host, `tel:`/`mailto:`, link aplikacji, PDF, `intent:`, zepsuty obrazek (strona ma zostać `Loaded`). |
| Loads | Zwykłe ładowanie i tytuł z `<title>`. |
| 404 | Błąd HTTP przed `onPageStarted` (pułapka 3). |
| Unknown host | Błąd sieci, ten sam przypadek co brak internetu. |
| Bad certificate | SSL: `cancel()` i ekran błędu. |
| Renderer crash | `chrome://crash` zabija renderer. Aplikacja żyje, a „Try again” tworzy nowy WebView. |

Testy jednostkowe obejmują maszynę stanów (`WebPageStateTest`) i allowlistę hostów (`HostAllowlistTest`).
Reguły na `android.net.Uri` wymagałyby Robolectrica albo testów na urządzeniu (Espresso-Web). Tego tu jeszcze nie ma.

## 8. Przenoszenie istniejącego modułu

Jak przełożyć client z sekcji 1 na ten podział:

| Było w module | Jest |
|---|---|
| Statusy Loading / Loaded / Error | `WebViewState.pageState` (`WebPageState`) z core |
| `hasError`, `dataLoaded` | Znikają. To szczegół `PageLoad`, pokryty testami. |
| `reset()` | Znika. Każde ładowanie zaczyna się od `Started`. |
| `openBrowserAction` + `newTab` w `shouldOverrideUrlLoading` | `UrlHandler` w module + `openExternally` z core |
| `onReceivedError` / `onPageFinished` w module | Core, z `isForMainFrame`, błędami HTTP, SSL i crashem renderera |
| `WebSettings` ustawiane w module | Domyślne w core; specyficzne rzeczy (JS bridge) idą przez `configure` |

Kolejność kroków:
1. Napisz `:core:webview` i jego testy, zanim ruszysz moduł.
2. W module podmień własny client na `KompasWebView` z regułami modułu. UI (progress, błąd) zostaje w module.
3. Dodaj sandbox modułu ze scenariuszami błędów. Przy okazji wyjdą błędy, które stary client miał od zawsze,
   np. pułapka 3: sprawdź, czy Wasz `reset()` nie gubi 404.
4. Następne moduły z WebView od razu korzystają z core.

## 9. Co jeszcze robią duże aplikacje

Rzeczy, których tu (jeszcze) nie ma, a które w dużych aplikacjach się pojawiają:

- **Custom Tabs zamiast zewnętrznej przeglądarki** (`androidx.browser`). Obca strona otwiera się „nad” aplikacją,
  w kolorach aplikacji, ze współdzielonymi cookies przeglądarki. WebView zostaje dla **własnych** stron, które
  potrzebują integracji (bridge, sesja, reguły linków).
- **`androidx.webkit`**:
  - `WebViewFeature.isFeatureSupported(...)` przed użyciem nowszych API (WebView aktualizuje się osobno, więc wersje są różne);
  - `addWebMessageListener` z `allowedOriginRules` zamiast `addJavascriptInterface`;
  - `WebViewAssetLoader` do ładowania lokalnych plików pod adresem `https://appassets.androidplatform.net` zamiast `file://`;
  - `setAlgorithmicDarkeningAllowed` dla trybu ciemnego.
- **Wspólna sesja natywna i webowa.** Po zalogowaniu w aplikacji cookies sesji trafiają do `CookieManager`, a po wylogowaniu
  `removeAllCookies` + `WebStorage.deleteAllData()`.
- **Upload plików i uprawnienia.** `WebChromeClient.onShowFileChooser` (formularze z załącznikami) i `onPermissionRequest`
  (kamera, mikrofon) też należą do core, nie do modułów.
- **Wydajność.** Pierwsze utworzenie WebView kosztuje kilkaset ms (ładowanie Chromium), więc duże aplikacje
  rozgrzewają go wcześniej albo trzymają pulę gotowych instancji. Ekrany, które nie mogą tracić stanu DOM, trzymają WebView poza
  kompozycją (np. w obiekcie związanym z wpisem na back stacku, na `MutableContextWrapper`, żeby nie wyciekała Activity).
- **Monitoring.** Każdy `Error` i crash renderera trafia do logów / Crashlytics razem z hostem (bez pełnego URL-a, bo może zawierać tokeny).
  Bez tego błędy WebView są niewidoczne, bo użytkownik widzi tylko „coś nie działa”.
