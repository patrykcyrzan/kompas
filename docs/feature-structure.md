# Struktura feature'a (MVI) – na przykładzie `feature/invoices`

Lista faktur z API i pobieranie PDF-a do cache'u (`:core:file`), potem otwarcie go w innej aplikacji. Wejście: Profil → „Invoices”.

## 1. Moduły

```
core/file/                    # generyczne pliki – nic nie wie o fakturach
  FileStorage                 # saveToCache(name, mime, bytes|stream): CachedFile (suspend), clearCache()
  CachedFile                  # uri (content://), name, mimeType + viewIntent()/shareIntent()
  FileName                    # sanitize – nazwy z backendu nie wyjdą poza katalog
  AndroidFileStorage, CoreFileProvider, di/FileModule

feature/invoices/
  api/InvoicesNavKey.kt       # publiczny kontrakt: tylko klucz nawigacji
  impl/.../invoices/impl/
    list/                     # jeden folder = jeden ekran
      InvoicesContract.kt     # State + Intent + Event
      InvoicesViewModel.kt
      InvoicesScreen.kt       # InvoicesRoute (VM, eventy, Android) + InvoicesScreen (bezstanowy, @Preview)
      components/InvoiceRow.kt
    domain/                   # nie importuje niczego z data/ ani z Retrofita
      Invoice.kt              # modele feature'a
      InvoicesRepository.kt   # interfejs – czego domena potrzebuje od danych
      InvoicesError.kt        # Network / NotFound / Unknown
      Outcome.kt              # Success | Failure – błąd w sygnaturze
      DownloadInvoiceUseCase.kt
    data/                     # zależy od domain/, nie odwrotnie
      DefaultInvoicesRepository.kt  # DTO → domena, wyjątki → InvoicesError
      remote/                 # InvoicesService (Retrofit) + DTO z mapperami
    di/InvoicesModule.kt      # @Binds repozytorium, @Provides serwisu z Retrofit z :core:network
    navigation/               # EntryProvider (jak w innych feature'ach)
```

Grupowanie **po ekranach**, nie po warstwach – drugi ekran to drugi folder obok `list/` (`details/` ze swoim Contract/ViewModel/Screen).

## 2. Zasady

| Element | Gdzie i dlaczego |
|---|---|
| **State** | `XContract.kt` obok ViewModelu. Jedna `data class`, wszystko co ekran rysuje. ViewModel trzyma `MutableStateFlow`, na zewnątrz `StateFlow`. |
| **Intent** | `sealed interface` w Contract. Jedyne wejście do ViewModelu: `fun onIntent(intent)`. |
| **Event** | `sealed interface` w Contract, przez `Channel(BUFFERED).receiveAsFlow()`. Tylko jednorazowe efekty (otwórz plik, snackbar, nawigacja). Co ma przetrwać obrót → State. |
| **Route vs Screen** | `Route` łączy VM i Androida (`startActivity`, launchery), zbiera eventy w `repeatOnLifecycle(STARTED)`. `Screen` dostaje `state` + `onIntent` – podgląd i testy UI bez Hilta. |
| **Repository** | Interfejs w `domain/`, implementacja w `data/` (odwrócenie zależności). Zwraca `Outcome<T, InvoicesError>` – wyjątki (`IOException`, `HttpException`, zły JSON) zamienia na błąd domenowy w jednym miejscu (`call {}`), `CancellationException` przepuszcza. Wyżej nikt nie zna Retrofita ani OkHttp. |
| **Błędy** | `InvoicesError` w `domain/` – w kategoriach, na które ekran może zareagować. Tekst dla użytkownika robi UI (`InvoicesError.message` w `InvoicesScreen.kt`). Błąd listy → `State.loadError`, błąd pobrania → `Event.DownloadFailed(error)`. |
| **Use case** | Tylko gdy łączy kilka źródeł albo jest współdzielony (`DownloadInvoiceUseCase` = repozytorium + `FileStorage`). Lista idzie z VM prosto do repozytorium. |
| **DTO** | `data/remote`, `@SerialName` na nazwy z backendu. Nie wychodzą poza `data/`. |
| **Gdzie API** | Używa go jeden feature → `impl/data`. Drugi feature też → `:core:data` (zob. [network.md](network.md)). |

## 3. Mocki (debug)

Skeleton Debug → „Mock API”:

- `GET /v1/invoices` → dwie faktury,
- `GET /v1/invoices/2026101/document` → mały PDF (Base64),
- `GET /v1/invoices/2026091/document` → 500, żeby zobaczyć snackbar z błędem.

## 4. Testy

- `InvoicesViewModelTest` – intenty na wejściu, state i eventy na wyjściu, fake repozytorium, bez Androida.
- `DefaultInvoicesRepositoryTest` – mapowanie DTO i wyjątków na `InvoicesError` (fake `InvoicesService`, bez sieci).

Udane pobranie wymaga `android.net.Uri` (`CachedFile`) – to temat na test instrumentalny albo Robolectric.
