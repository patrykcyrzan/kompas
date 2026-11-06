# PDF viewer – podgląd PDF w aplikacji

Ekran podglądu PDF z przyciskami **Udostępnij** i **Zapisz** na dole. Otwiera PDF-y z WebView – zarówno
linki, jak i dokumenty, które strona przekazuje jako Base64 przez `@JavascriptInterface`.

| Moduł | Zawartość |
|---|---|
| `api` | `PdfViewerNavKey`, `PdfSource`, `PdfImporter`, `PdfFileName` – tylko to widzą inne feature'y |
| `impl` | pobieranie, render (`PdfRenderer`), ekran, `FileProvider` |
| `sandbox` | scenariusze + ekran z prawdziwym WebView: `./gradlew :feature:pdfviewer:sandbox:installDebug` |

---

## Jak podpiąć w WebView

### 1. Link do PDF-a

WebView nie wyświetla PDF-ów – zgłasza je jako pobranie. Wystarczy `DownloadListener`:

```kotlin
webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
    PdfViewerNavKey.fromDownload(url, contentDisposition, mimeType)?.let(navigator::navigate)
}
```

`fromDownload` rozpoznaje PDF po typie MIME, nazwie z `Content-Disposition` albo końcówce `.pdf` w URL-u
(serwery często wysyłają PDF jako `application/octet-stream`). Dla innych plików zwraca `null`.

Viewer pobiera plik sam, **z cookies WebView** dla tego URL-a – PDF-y za logowaniem też działają.

### 2. PDF jako Base64 z `@JavascriptInterface`

Gdy strona generuje PDF albo ma go pod `blob:` URL-em, nie ma czego pobrać – strona przekazuje bajty.
Wstrzyknij `PdfImporter`:

```kotlin
@JavascriptInterface
fun onPdf(base64: String?, filename: String?) {
    base64 ?: return
    scope.launch {                                     // JavaBridge → Main
        // Mostek widzą wszystkie strony i ramki w tym WebView – tylko nasza może podsunąć dokument.
        if (!webView.url.orEmpty().startsWith(TRUSTED_ORIGIN)) return@launch
        val key = try {
            pdfImporter.fromBase64(base64, filename)
        } catch (e: IllegalArgumentException) {
            return@launch                              // to nie był Base64
        }
        navigator.navigate(key)
    }
}
```

- Przyjmuje zwykły Base64 i `data:application/pdf;base64,…` (wynik `FileReader.readAsDataURL`), pomija białe znaki.
- Dekoduje strumieniowo prosto do pliku w cache.
- W kluczu ląduje **tylko ścieżka** (`PdfSource.Local`), nigdy Base64 – back stack trafia do zapisanego stanu (limit ~1 MB).

### 3. Zwykły URL

```kotlin
navigator.navigate(PdfViewerNavKey("https://…/statement.pdf"))
navigator.navigate(PdfViewerNavKey("https://…/doc?id=42", fileName = "Wyciąg październik.pdf"))
```

---

## Jak to działa

```
WebView ──link──> fromDownload ──> PdfViewerNavKey(Remote(url))  ──┐
        ──JS────> PdfImporter  ──> PdfViewerNavKey(Local(path))  ──┤
                                                                   ▼
                                  PdfFiles.fetch → cache/pdf/<id>/<nazwa>.pdf
                                                                   ▼
                                  PdfDocument (PdfRenderer) → lista stron
                                                                   ▼
                                  [ Udostępnij ]  [ Zapisz ]
```

| Element | Szczegóły |
|---|---|
| **Pliki** | Tylko prywatny `cacheDir/pdf/`, nigdy pamięć współdzielona. Każdy plik we własnym katalogu (te same nazwy się nie gryzą). Usuwany po zamknięciu podglądu; pozostałości po ubitym procesie – przy następnym pobraniu/imporcie (starsze niż 1 dzień). |
| **Nazwa pliku** | `fileName` z klucza → `Content-Disposition` (`filename*` wygrywa, więc polskie znaki przechodzą) → ostatni segment URL-a → `document.pdf`. Zawsze oczyszczona (bez `/`, `..`, znaków niedozwolonych) i z `.pdf`. |
| **Render** | Systemowy `PdfRenderer` – zero bibliotek. Strony renderowane tylko gdy widoczne, w szerokości ekranu. `PdfRenderer` nie jest thread-safe – wszystko pod jedną blokadą, także `close()`. |
| **Zoom** | Szczypanie i podwójne tapnięcie (do 4×). Po przybliżeniu jeden palec przesuwa w bok, lista dalej przewija w pionie. |
| **Udostępnij** | `ACTION_SEND` przez własny `PdfFileProvider` (authority `${applicationId}.pdfviewer.fileprovider`) – odbiorca dostaje dostęp tylko do tego jednego pliku. |
| **Zapisz** | Systemowe „Zapisz jako” (`CreateDocument`) z podpowiedzianą nazwą – użytkownik wybiera folder, bez żadnych uprawnień. Snackbar po zapisie. |
| **Błędy** | Nie udało się pobrać · chroniony hasłem · to nie PDF (np. strona logowania zamiast dokumentu). „Spróbuj ponownie” tylko dla pobierania z URL-a. |

### Dlaczego tak

- **Własny ekran, nie zewnętrzna aplikacja** – użytkownik nie wychodzi z aplikacji, nie potrzebuje czytnika PDF. Zewnętrzna aplikacja jest o jedno kliknięcie dalej przez Udostępnij.
- **`PdfViewerNavKey` nie jest `Destination`** – link z zewnątrz nie może kazać aplikacji pobrać dowolnego URL-a.
- **`PdfImporter` w `api`, implementacja w `impl`** – tylko `impl` zna katalog cache i `FileProvider`; inne feature'y zależą wyłącznie od `api`.

---

## Ograniczenia i TODO

- Strony renderowane w rozdzielczości ekranu – przy dużym zoomie lekko rozmyte (rozwiązanie: ponowny render po zakończeniu gestu).
- PDF-y z hasłem nie są obsługiwane (`PdfRenderer` umie je otworzyć dopiero od API 35).
- Brak wyszukiwania i zaznaczania tekstu – do tego `androidx.pdf` (nowsze urządzenia) albo komercyjne SDK.
- PDF generowany POST-em: `DownloadListener` dostaje tylko URL – potrzebny GET po stronie backendu albo ścieżka przez Base64.
