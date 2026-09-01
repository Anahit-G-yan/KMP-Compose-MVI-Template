# Rick & Morty KMP — Production-Ready Clean MVI Client

> A Kotlin Multiplatform client for [The Rick and Morty API](https://rickandmortyapi.com), built to show what a Senior-level Android/iOS codebase looks like when Clean Architecture, MVI, and Compose Multiplatform are taken seriously — not just name-dropped.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-3.2.3-087CFA?style=flat&logo=ktor&logoColor=white)
![Koin](https://img.shields.io/badge/Koin-4.1.0-F7A600?style=flat)
![Coil](https://img.shields.io/badge/Coil3-3.2.0-FF6F00?style=flat)
![Coroutines](https://img.shields.io/badge/Coroutines-1.10.2-000000?style=flat&logo=kotlin&logoColor=white)
![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS-3DDC84?style=flat)
![Architecture](https://img.shields.io/badge/architecture-Clean%20%2B%20MVI-blueviolet?style=flat)

---

## Overview

This is a cross-platform Rick & Morty character browser sharing **~95% of its code** — network layer, domain logic, ViewModels, *and the UI itself* — between Android and iOS through Compose Multiplatform. Only two things are genuinely platform-specific: the Ktor HTTP engine (OkHttp / Darwin) and the handful of lines each platform needs to bootstrap Koin.

The goal of this project isn't to be a from-scratch trivia demo — it's a reference for how a real feature (paginated list, image loading, error/retry, pull-to-refresh) gets built when the priority is a codebase that survives code review, scales past one screen, and doesn't fall over when a public API rate-limits you.

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.4.10 |
| UI | Compose Multiplatform | 1.11.1 |
| Networking | Ktor Client (OkHttp / Darwin engines) | 3.2.3 |
| Serialization | kotlinx.serialization | 1.8.1 |
| Dependency Injection | Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) | 4.1.0 |
| Async | Kotlin Coroutines & Flow | 1.10.2 |
| Image Loading | Coil3 (`coil-compose`, `coil-network-ktor3`) | 3.2.0 |
| Build | Android Gradle Plugin (`com.android.kotlin.multiplatform.library`) | 9.0.1 |

## Architecture & Data Flow

The project is organized as three strictly one-directional layers, all living in `shared/src/commonMain`:

```
Presentation (Compose + MVI)
        │  Intent
        ▼
   ViewModel (StateFlow<State> / SharedFlow<Effect>)
        │  calls
        ▼
Domain  ── UseCase → Repository (interface) ── pure Kotlin, zero framework deps
        │  implemented by
        ▼
Data    ── RepositoryImpl → RemoteDataSource → Ktor HttpClient → REST API
        │  DTO → domain mapping happens right here, and only here
        ▼
   NetworkResult<T>  (Success | Error)
```

**The Domain layer is completely isolated.** `domain/model`, `domain/repository`, and `domain/usecase` import nothing from Ktor, kotlinx.serialization, Android, or Compose — only Kotlin stdlib and coroutines. `CharacterDto` never crosses the Data → Domain boundary; mapping to the pure `Character` model happens exclusively inside `RickAndMortyRepositoryImpl`. This means the domain layer could be lifted into a completely different app, tested without a single mock of an HTTP client, or have its data source swapped (REST → GraphQL → local cache) without either the ViewModel or the UI ever noticing.

```
presentation/characters/   → CharactersContract (State/Intent/Effect), CharactersViewModel, CharactersScreen
domain/model/               → Character, CharacterStatus, PageInfo, CharacterPage
domain/repository/          → RickAndMortyRepository (interface)
domain/usecase/             → GetCharactersUseCase
data/remote/                → NetworkResult, DTOs, RickAndMortyRemoteDataSource(Impl)
data/repository/            → RickAndMortyRepositoryImpl (DTO → domain mapping)
di/                         → KoinModules (appModule + expect platformModule)
```

## Advanced Engineering Features

The parts of this codebase actually worth reading on a code review.

### 1. Serial Intent Queue via Kotlin `Channel`

`CharactersViewModel` doesn't launch a new coroutine per incoming `Intent`. Every `Intent` is pushed into an **unlimited `Channel`**, drained by a single coroutine started in `init`:

```kotlin
private val intents = Channel<CharactersContract.Intent>(Channel.UNLIMITED)

init {
    viewModelScope.launch {
        for (intent in intents) { handleIntent(intent) }
    }
}

fun setIntent(intent: CharactersContract.Intent) = intents.trySend(intent)
```

Because `handleIntent` is `suspend` and called directly inside that `for` loop, two intents can **never execute concurrently** — no matter how many `LoadNextPage` events a fast scroll fires, they're processed strictly one at a time. `currentPage` and `hasNextPage` are mutated by exactly one coroutine, ever. No `Mutex`, no manual job cancellation, no race conditions — for free, by construction.

### 2. Efficient Pagination with `derivedStateOf`

Endless scrolling doesn't poll scroll position on every frame. `derivedStateOf` collapses `LazyListState.layoutInfo` reads into a single boolean that only changes value when the list crosses its "near the end" threshold:

```kotlin
val shouldLoadMore by remember {
    derivedStateOf {
        val info = listState.layoutInfo
        info.totalItemsCount > 0 &&
            info.visibleItemsInfo.lastOrNull()?.index ?: -1 >= info.totalItemsCount - PAGINATION_TRIGGER_OFFSET
    }
}

LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) viewModel.setIntent(CharactersContract.Intent.LoadNextPage)
}
```

`LaunchedEffect(shouldLoadMore)` only restarts on a `false → true` transition, so a fast fling doesn't spam `setIntent` on every recomposition — just once per threshold crossing.

### 3. Client-Side Rate Limiting (HTTP 429 Protection)

A fast fling can cross the pagination threshold several times within milliseconds. The intent queue above guarantees those requests never run *concurrently*, but without throttling they'd still fire **back-to-back with ~0ms between them** — enough to trip the public API's rate limiter. `CharactersViewModel` enforces a minimum spacing between requests using a monotonic clock:

```kotlin
private var lastFetchMark: TimeMark? = null

private suspend fun fetchPage(nextPage: Int) {
    lastFetchMark?.elapsedNow()?.let { elapsed ->
        if (elapsed < MIN_REQUEST_INTERVAL) delay(MIN_REQUEST_INTERVAL - elapsed)
    }
    lastFetchMark = TimeSource.Monotonic.markNow()
    // ...network call
}
```

Deliberately implemented as a **delay**, not a dropped/ignored request: because the scroll trigger above is edge-triggered, silently discarding a throttled `LoadNextPage` risks the list getting permanently stuck near the bottom if the user stops scrolling right after firing it. Delaying guarantees every accepted intent is eventually honored — just spaced out enough to keep the API happy.

### 4. Shared Ktor Engine for Coil3

Coil3 doesn't auto-detect a network stack the way Coil2 did — it has to be told explicitly how to fetch images. Instead of letting Coil spin up its own client, the app's Compose root resolves the **same `HttpClient` instance** the API layer uses from Koin and hands it to Coil's Ktor fetcher:

```kotlin
val httpClient = koinInject<HttpClient>()
setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient)) }
        .build()
}
```

One connection pool, one `Logging` plugin, one place that configures timeouts — for both JSON API calls and image downloads.

## Dependency Injection Setup

Koin 4.x modules are split along exactly the one axis that's genuinely platform-specific — the Ktor engine — with everything else living in `commonMain`:

```kotlin
// commonMain — identical on every platform
val appModule = module {
    single<RickAndMortyRemoteDataSource> { RickAndMortyRemoteDataSourceImpl(get()) }
    single<RickAndMortyRepository> { RickAndMortyRepositoryImpl(get()) }
    factory { GetCharactersUseCase(get()) }
    viewModelOf(::CharactersViewModel)
}

expect val platformModule: Module   // supplies HttpClientEngine only

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, networkModule, appModule)
    }
}
```

```kotlin
// androidMain
actual val platformModule: Module = module { single<HttpClientEngine> { OkHttp.create() } }

// iosMain
actual val platformModule: Module = module { single<HttpClientEngine> { Darwin.create() } }
```

Every dependency is bound against its **interface** (`RickAndMortyRemoteDataSource`, `RickAndMortyRepository`), never a concrete `*Impl` — nothing above the data layer can accidentally resolve an implementation type from the container.

## How to Run

**Android:**
```bash
./gradlew :androidApp:installDebug
```

**iOS:**
```bash
open iosApp/iosApp.xcodeproj
```
then run from Xcode (`shared` is linked in automatically as an XCFramework via the KMP Gradle plugin).

**Build everything without deploying:**
```bash
./gradlew build
```

---

<p align="center">Built as a demonstration of production-grade Kotlin Multiplatform architecture.</p>
