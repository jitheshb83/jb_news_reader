# Architecture — JB News Feed

Reference doc for the actual built app (package `com.jithesh.newsreader`, internal module/class names still say "NewsReader" — only the user-visible app name was changed to "JB News Feed"). Loaded on demand per `CLAUDE.md`; not preloaded into every session.

## Stack versions (as built)
- Kotlin 2.3.10, AGP 9.3.1 (using the classic `org.jetbrains.kotlin.android` + `kotlin.plugin.compose` plugins — AGP 9's new built-in-Kotlin/new-DSL features are opted out via `android.builtInKotlin=false` / `android.newDsl=false` in `gradle.properties`, since those were too new/unstable to target confidently)
- compileSdk / targetSdk 37, minSdk 30
- Compose BOM 2026.06.01, Room 2.8.4, Hilt 2.60.1, Retrofit 3.0.0, OkHttp 5.4.0, Coil 2.7.0, DataStore 1.2.1, ML Kit Translate 17.0.3
- Every dependency (including Compose BOM-managed artifacts like `ui`, `material3`, `ui-test-junit4`) is declared in `gradle/libs.versions.toml` and referenced via `libs.*` in `app/build.gradle.kts` — no raw `"group:artifact:version"` string literals in the dependency block. Keep it that way: add new deps to the catalog first, then reference them, so there's one place to see what's actually pulled in.

## Package layout (single `app` module, layer-based)
```
com.jithesh.newsreader/
├── NewsReaderApp.kt          @HiltAndroidApp Application
├── MainActivity.kt           single Activity, bottom-nav Scaffold + NavHost, reads theme_mode
├── di/                       Hilt modules — Database, Network (OkHttp/Retrofit), DataStore
├── data/
│   ├── db/                   Room: FeedEntity, ArticleEntity, ArticleWithFeed (join projection),
│   │                         FeedDao, ArticleDao, AppDatabase
│   ├── network/               FeedFetchService (Retrofit, @Streaming @GET @Url),
│   │                         FeedParser (hand-rolled RSS 2.0 + Atom 1.0 via SAX)
│   ├── repository/           FeedRepository — the one place fetch/parse/throttle/prune/store meet
│   ├── settings/              UserSettings, ThemeMode, SettingsRepository (DataStore-backed)
│   ├── suggested/            DefaultFeeds — curated feed list + category constants
│   └── translation/          NorwegianTranslator (ML Kit wrapper)
├── ui/
│   ├── navigation/           Destinations (routes), NavGraph
│   ├── home/                 HomeScreen + HomeViewModel (category-grouped preview)
│   ├── feedlist/              FeedListScreen, AddFeedDialog, FeedListViewModel
│   ├── articlelist/          ArticleListScreen + ArticleListViewModel (one feed's full list)
│   ├── articledetail/         ArticleDetailScreen + ArticleDetailViewModel
│   ├── settings/              SettingsScreen + SettingsViewModel
│   ├── common/                 ArticleRow (shared list row), EmptyState
│   └── theme/                 Color, Type, Theme (System/Light/Dark via MaterialTheme)
└── util/                      DateUtils (feed date parsing), HtmlUtils (stripHtml for display)
```

## Data flow

**Add feed** (typed URL, or tap a suggested chip in `AddFeedDialog`) → `FeedRepository.addFeed(url, category)` → inserts a `FeedEntity` → immediately calls `refreshFeed(force = true)`.

**Refresh** (`FeedRepository.refreshFeed`): checks the 15-minute throttle (`MIN_REFRESH_INTERVAL_MS`) unless `force = true` → `FeedFetchService.fetchFeed(url)` (OkHttp, 15s timeouts, 5MB disk cache for conditional-GET 304s) → `FeedParser.parse(stream)` (SAX-based, hand-rolled, caps at 500 items/feed) → upserts `FeedEntity` (title/siteLink/lastFetchedAt/lastFetchError) → `ArticleDao.insertAll` (dedup on `(feedId, guid)` unique index, `OnConflictStrategy.IGNORE` so re-fetch never clobbers `isRead`) → `ArticleDao.pruneOldest(feedId, 200)` caps per-feed storage.

Failures (HTTP error, malformed XML, network error) are caught broadly (not just `IOException` — untrusted external content, including `SAXException` from bad XML, must degrade to `Result.failure` + `lastFetchError` rather than crash) and never propagate past the repository boundary. `refreshAllFeeds` uses `supervisorScope` so one bad feed can't cancel the others' in-flight fetches.

**Home**: `HomeViewModel` combines `SettingsRepository.settingsFlow` (topics/count/showImages) with `FeedRepository.observeCategories()` to resolve effective topics (explicit selection → else all known categories → else `DefaultFeeds.ALL_CATEGORIES` as last resort), queries `ArticleDao.observeArticlesForCategories(topics, limit)` (joined with `feeds` for title/category, newest-first, capped), then groups the result by `feedCategory` into `CategorySection`s for the grouped/sectioned UI (grouping order falls out of `groupBy` on an already-sorted list, so sections land "most recently active category first" for free).

**Settings**: Jetpack Preferences DataStore (`data/settings`), a single `Flow<UserSettings>` read everywhere it's needed (Home, Settings screen, MainActivity's theme). No explicit save step — every write goes straight to DataStore and every reader is already collecting the same Flow.

## Room schema
- **FeedEntity**: id, url (unique), title, category (free string, not an enum — see DefaultFeeds), siteLink?, lastFetchedAt?, lastFetchError?, dateAdded
- **ArticleEntity**: id, feedId (FK, `CASCADE` delete), guid, title, link, description?, contentHtml?, author?, publishedAt?, thumbnailUrl?, isRead, dateFetched — unique index `(feedId, guid)`
- No Settings table — DataStore handles that outside Room entirely.

## Categories
Plain strings, not an enum — `DefaultFeeds.CATEGORY_*` constants + `ALL_CATEGORIES` list drive the AddFeedDialog picker and Settings' topic checklist (which only ever shows categories that actually exist among the user's feeds). Currently: Norway, World, India, AI, Software Development, Cloud, Technology News, Cyber Security, Innovation, Trending, General. `DefaultFeeds.FEEDS` is the curated starter list (fetch-verified 2026-07-27; feed URLs drift, re-verify if one starts failing).

## Translation (Norwegian → English)
`NorwegianTranslator` wraps ML Kit's on-device Translate API (no API key, no server round-trip; ~30MB model downloads once on first use and is cached by ML Kit thereafter). Gated everywhere on `feed.category == CATEGORY_NORWAY`:
- **Article detail**: a translate icon in the top bar; tap translates title+body (stripped of HTML first via `util/HtmlUtils.stripHtml`) and caches the result in `ArticleDetailViewModel`; a "Translated from Norwegian · Show original" row toggles between cached original/translated without re-translating.
- **Home / per-feed article list**: `ArticleRow` takes optional `showTranslateAction`/`isTranslating` params; `HomeViewModel`/`ArticleListViewModel` each hold their own `translatedTitles: Map<Long, String>` + `translatingIds: Set<Long>` cache, keyed by article id. Tapping a row's translate icon translates just that title in place and the icon disappears once cached (no in-list toggle back — full original/translated toggle only lives in article detail).

## Caching / resource limits (deliberate, tuned during review passes)
- OkHttp disk cache: 5MB, enables conditional GET (304s) via ETag/Last-Modified
- Refresh throttle: 15 min per feed (`FeedRepository.MIN_REFRESH_INTERVAL_MS`), bypassed by pull-to-refresh (`force = true`)
- Per-feed storage cap: 200 newest articles (`FeedRepository.MAX_ARTICLES_PER_FEED`), pruned after every successful refresh
- Parser item cap: 500 items/feed (`FeedParser`'s `MAX_ITEMS`), defends against a pathological feed
- OkHttp timeouts: 15s connect/read/write, explicit (not relying on library defaults) since `refreshAllFeeds` fires concurrent fetches
- Article-detail hero image: `heightIn(max = 240.dp)` so Coil doesn't decode near full source resolution into memory
- `ArticleDetailScreen`'s "open in browser" only launches `http`/`https` schemes — a feed's `<link>` is untrusted content and an unvalidated scheme could be used for Android intent-scheme injection

## Navigation
Bottom nav: Home / Feeds / Settings (`ui/navigation/Destinations.kt`). Pushed on top: `articleList/{feedId}` (from Feeds), `articleDetail/{articleId}` (from Home or an article list). `MainActivity` hosts a single Scaffold with the bottom `NavigationBar`; `FeedListScreen` nests its own Scaffold for the add-feed FAB.

## Testing
- Unit tests (`app/src/test`): `FeedParserTest` (RSS/Atom fixtures, malformed-XML/missing-guid edge cases), `FeedRepositoryTest` (throttle skip/force, malformed-feed-doesn't-crash, one-bad-feed-doesn't-block-batch, pruning cap — with hand-written fakes for the DAOs/fetch service, no mocking library), `DefaultFeedsTest`, `SettingsRepositoryTest` (real DataStore against a temp file), `DateUtilsTest`.
- Instrumented tests (`app/src/androidTest`, run only when explicitly asked per `CLAUDE.md`): `ArticleDaoTest` (in-memory Room, category+limit query, cascade delete, dedup), `FeedListScreenTest` / `SettingsScreenTest` (Compose UI smoke tests, ViewModels constructed directly with fakes — no Hilt test setup needed since `hiltViewModel()` params are just regular defaultable parameters).
- Run `./gradlew compileDebugAndroidTestKotlin` at minimum after touching anything under `androidTest` — these tests are never executed as part of the normal unit-test loop, so a broken import there won't surface any other way. (Caught one this way: `SemanticsNodeInteraction.assertExists()` moved from a top-level `androidx.compose.ui.test` extension function to a member function in Compose UI Test 1.11.4 — the old `import ...assertExists` line silently referenced a symbol that no longer existed, and nothing caught it until this was checked directly.)

## Known simplifications (deliberate, not oversights)
- Categories are free strings, not a closed enum — flexible for custom feeds, no migration needed to add new suggested categories.
- No WorkManager/background refresh — fetch only happens on app-open (throttled) and pull-to-refresh (forced). Revisit only if "get notified of new articles" becomes a real ask.
- No REST news API integration, despite `CLAUDE.md`'s stack table listing it as "optional" — v1 scope is RSS/Atom only, by explicit choice.
- `docs/FEEDS.md` and `docs/CONVENTIONS.md` (also referenced by `CLAUDE.md`) don't exist yet — create if/when there's enough feed-parsing-contract or naming-convention nuance worth writing down; not needed yet at this size.
