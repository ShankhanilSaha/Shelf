# Decisions

Each entry records a choice that the task description left open, or a place where the build
deviates from it, and why. Newest decisions are appended at the end of each section.

## Toolchain and versions

All versions were looked up on 2026-11-24 from Google Maven, Maven Central, the Gradle Plugin
Portal and services.gradle.org (latest stable, no alpha/beta/rc). They live in
`gradle/libs.versions.toml`.

| Component | Version | Notes |
|---|---|---|
| Gradle | 9.9.2 | Wrapper pins the distribution SHA-256. |
| Android Gradle Plugin | 9.5.2 | Built-in Kotlin and the new DSL are on (AGP 9 defaults). |
| Kotlin | 2.4.21 | Compose compiler and serialization plugins use the same version. |
| KSP | 2.3.14 | KSP2; versioned independently of Kotlin since 2.3.0. |
| Compose BOM | 2026.11.01 | Compose 1.13.0, Material 3 1.5.0, adaptive 1.3.0. |
| Navigation 3 | 1.2.2 | Includes the `deeplink` and `result` packages. |
| Hilt / AndroidX Hilt | 2.60.1 / 1.4.0 | |
| Room | 2.8.5 | See "Room 2.8 instead of Room 3" below. |
| Paging / WorkManager / DataStore | 3.5.3 / 2.12.0 / 1.3.0 | |
| CameraX / ML Kit barcode | 1.7.0 / 17.3.0 | Bundled ML Kit model (works offline). |
| Retrofit / OkHttp | 3.0.0 / 5.5.1 | Retrofit ships its own kotlinx.serialization converter. |
| kotlinx.serialization / coroutines | 1.13.0 / 1.11.0 | |
| Coil | 3.6.3 | |
| Firebase BoM | 35.2.0 | Plugins: google-services 4.6.0, Crashlytics 3.0.8. |
| Credential Manager / googleid | 1.6.0 / 1.2.1 | |
| Turbine / MockK / Robolectric | 1.2.1 / 1.14.12 / 4.18.1 | Robolectric 4.18 ships an SDK 37 runtime. |
| detekt / Spotless / ktlint | 1.23.8 / 8.12.0 / 1.8.0 | detekt 2.0 is still alpha. |

**JDK.** Gradle runs on JDK 21 (locally the JetBrains Runtime 21 in `~/.jdks`, Temurin 21 on CI).
All modules compile to Java 17 bytecode (`jvmTarget = 17`), which every supported Android
version handles through D8.

## SDK levels

- **compileSdk 37.2.** The task text says Compose BOM 2026.11.01 needs compileSdk 37.1. Checking
  the AAR metadata confirmed it: `androidx.compose.ui:ui-android:1.13.0` declares
  `minCompileSdk=37`, `minCompileMinorSdk=1` and `minAndroidGradlePluginVersion=9.1.0`. Both
  platform 37.1 and 37.2 are on the SDK repository's stable channel, so the build uses the newest,
  37.2 (`compileSdk = 37`, `compileSdkMinor = 2`). The machine only had `android-37.0`, so the
  command-line tools were installed and `platforms;android-37.2` plus `build-tools;37.0.0` were
  added with `sdkmanager`. CI installs the same two packages.
- **targetSdk 37** (target SDK has no minor level). **minSdk 26** as specified. Because minSdk is
  26, `java.time` is available natively and no core library desugaring is needed.

## Build configuration

- **Convention plugins** live in `build-logic/convention` (`shelf.android.application`,
  `shelf.android.application.compose`, `shelf.android.library`, `shelf.android.library.compose`,
  `shelf.android.feature`, `shelf.android.room`, `shelf.android.screenshot`, `shelf.hilt`,
  `shelf.jvm.library`, `shelf.root`).
  The layout follows Google's Now in Android sample. Module build files only add a namespace and
  dependencies.
- **No `org.jetbrains.kotlin.android` plugin.** AGP 9 compiles Kotlin itself. The `kotlin`
  extension that built-in Kotlin registers is still used for compiler options.
- **Kotlin warnings are errors** (`allWarningsAsErrors`) so the code base stays warning-free.
  `-PwarningsAsErrors=false` turns this off locally. Opt-ins use `@OptIn` at the call site rather
  than global compiler flags, because a global opt-in for a library that is not on a module's
  classpath is itself a warning.
- **Hilt + KSP** adds `kotlin-metadata-jvm` (same version as Kotlin) to the KSP classpath so that
  Hilt can read Kotlin 2.4 metadata. Now in Android does the same.
- **Unit tests run on the debug variant only.** Release unit-test variants are disabled with the
  AGP 9 `hostTests` variant API, so `./gradlew test` does not run every test twice.
- **`failOnNoDiscoveredTests = false`** on test tasks. Gradle 9 fails a test task when it finds
  compiled test classes but no tests; KSP/Hilt generate classes into the unit-test output of
  modules that have no tests of their own.
- **Coverage uses AGP's built-in JaCoCo** (`enableUnitTestCoverage` on the debug build type) plus
  the Gradle `jacoco` plugin for the two pure-Kotlin modules. `./gradlew coverageReport` (root
  convention plugin) prints line coverage per module and for the aggregate of all `*ViewModel`
  classes, and writes `build/reports/coverage-summary.md`. Kover was not used because AGP's own
  integration is guaranteed to understand AGP 9 variants. The summary leaves out generated code
  (Hilt and Dagger factories and modules, Room `_Impl` classes, serializers,
  `ComposableSingletons`, `BuildConfig`) and keeps everything written by hand, including the
  Firebase implementations that unit tests cannot reach. `-Pcoverage.minimum=0.7` turns it into a
  gate for `:core:data` and the ViewModels; CI uses it.
- **Configuration cache is on** (`problems=warn` so a third-party plugin cannot break a build).
- **Gradle daemon metaspace is 2 GB.** With the screenshot plugin the daemon ran out at 1 GB.

## Static analysis

- **Spotless + ktlint 1.8.0** with the `android_studio` code style and a 120 character line
  limit (`.editorconfig`). Composable functions are exempt from the function-naming rule.
- **detekt 1.23.8 runs once from the root project** over every module's sources, using
  `config/detekt/detekt.yml` on top of the default rule set. detekt 1.23 predates AGP 9's new
  DSL, so its per-module Android integration was not used. Its embedded Kotlin compiler is pinned
  to the version it was built with (`getSupportedKotlinVersion()`), as detekt documents.
- The only Gradle deprecation warnings in the build (`ReportingExtension.file`,
  `Configuration.setVisible`) come from the detekt 1.23.8 plugin itself (verified with
  `-Dorg.gradle.deprecation.trace=true`). They are scheduled for removal in Gradle 10/11 and are
  fixed in detekt 2.0, which is not stable yet.
- **Android Lint** runs per module (`lint`), with version-freshness checks disabled because they
  depend on the day the build runs.

## Room 2.8 instead of Room 3

Room 3.0 (`androidx.room3`) became stable in July 2026 and Room 2.8.5 was released on the same
day as Room 3.0.3 (2026-09-09), so both lines are maintained. Room 3 is a new artifact group with
a driver-based, coroutine-first API and renamed packages. Room 2.8 was chosen because it is the
widely documented line, it integrates with Paging and `MigrationTestHelper` exactly as the
specification describes, and the Room Gradle plugin and KSP processor for it are proven with AGP 9
(Now in Android uses them). Moving to Room 3 is listed as a follow-up.

## Architecture and UI state

- **One sealed `UiState` per screen**, exposed as a `StateFlow` and collected with
  `collectAsStateWithLifecycle`. One-off events are state, as the architecture guide recommends:
  snackbars are a `UserMessage` in the state that the UI acknowledges with `onMessageShown()`;
  leaving a screen is a state too (`LibraryBookUiState.Closed`, `LogReadingUiState.Saved`,
  `AuthUiState.SignedIn`); a deep link waits in `MainActivityViewModel` until the UI has applied it.
- **`UiText`** (string resource, plural or raw, with nested arguments) lets ViewModels describe
  text without a `Context`, so all copy stays in `strings.xml`.
- **Text field content is Compose snapshot state in the ViewModel** (`var query by
  mutableStateOf`), following Google's guidance on TextField state; a parallel flow feeds the
  debounced search.
- **Errors are classified once** in `:core:network` (`ShelfException(ShelfError)`), travel
  through Paging's `LoadState.Error` and repository results, and become copy in `:core:ui`.
- **The splash screen** stays until preferences are loaded, so the theme never flashes.
  Onboarding is a first-launch screen gated by a DataStore flag.

## Navigation

- **Navigation 3 with one back stack per top-level tab** and "exit through home" back behaviour.
  Back stacks are serializable (`NavKey` classes are `@Serializable`) and survive process death.
- **Adaptive navigation**: a navigation bar on compact windows and a navigation rail from medium
  width (`WindowSizeClass`); the scanner is full screen.
- **Deep links** `shelf://book/{workKey}` and `shelf://log` are matched with the Navigation 3
  deep link helpers. `shelf://log` opens the Stats tab with the "Log reading" sheet (a bottom-sheet
  scene strategy).
- Opening a scanned book replaces the scanner on the back stack, so Back returns to where the
  scan started.

## Search

- 400 ms debounce, at least two characters after trimming, and `flatMapLatest` cancels the
  previous request. Pressing the search key searches at once; clearing returns to the idle state
  at once.
- The idle placeholder reports a loading refresh, so switching to results never flashes
  "no results".
- Open Library pages by page number, so the `PagingSource` uses a fixed page size of 20, also for
  the first load.
- Paging fakes use a real `Pager` over `asPagingSourceFactory()`: `PagingData.from(list)` passed
  through `cachedIn` never reports load states and made `asSnapshot` hang.
- Tapping a result seeds the book cache with the search summary (insert if absent), so details
  open instantly and fill in when the fetch returns.

## Book details

- Details combine the work, its first edition (page count) and up to three author names. A merge
  never replaces a cached value with a missing one, so a partial fetch cannot make a book worse.
- **First publication year**: the earliest of the work record's date, the year that came with the
  search result, and, for books that did not come from search (scanned ones, deep links), the year
  the search index computes for the work. An edition's own date is never used. Found on the
  emulator: the work record of *The Fellowship of the Ring* says 1972 and was overwriting 1954.
- **Descriptions are shown as plain text**: Open Library descriptions are often Markdown
  (emphasis, links, footnotes, rules), and the markup is removed. **Catalogue tags** such as
  `genre:fantasy` or `nyt:...=2020-10-04` are not shown as subjects. Both were found with real data
  while testing on API 26.

## Library, stats and goals

- Shelf entries reference cached books (foreign key). The library opens on the Reading tab.
  Swipe to remove also has an accessibility custom action, and Undo restores the entry as it was.
- The book page shows the shelf as filter chips (they wrap at large font sizes) with radio
  semantics; notes save 500 ms after typing stops and are flushed when the screen closes.
- Reaching the last page or moving a book to Finished sets `finishedAt` and asks for a rating.
- **Goal**: DataStore holds the current goal (it is a setting) and a Room `goals` table keeps its
  history, so raising the goal does not change whether past days met it.
- **Streaks** (`CalculateStreakUseCase`, written test first): days are local calendar days in the
  clock's time zone; today's unmet goal does not break the streak until the day is over; logs
  dated in the future are ignored until their day.
- **Log reading** is a button in the Today card rather than a floating button, which covered the
  delete button of the last session. Locale-dependent formatting reads `LocalLocale`.

## Reminders and notifications

- The reminder is one unique one-time `ReminderWorker` whose initial delay is the time until the
  next occurrence of the chosen wall-clock time, so it follows daylight-saving changes. Changing
  the time re-enqueues with `REPLACE`; the worker schedules the next run with `APPEND_OR_REPLACE`
  (`REPLACE` from inside the worker would cancel the worker itself). The delay is recomputed at
  app start, which covers time-zone changes.
- `POST_NOTIFICATIONS` is requested when the reminder is switched on (API 33+); a refusal shows a
  snackbar that opens the notification settings.
- Export uses the Storage Access Framework (`CreateDocument`) and writes a versioned JSON format
  (`shelf-library`, version 1).

## Scanner

- CameraX preview (`CameraXViewfinder`) and `ImageAnalysis` with an `MlKitAnalyzer` restricted to
  EAN-13 and EAN-8. Only Bookland EAN-13 codes (978/979) are ISBNs; other codes show a hint
  instead of a failed lookup.
- Repeated detections of the same code are dropped while it stays in view (2 s window). Analysis
  is detached as soon as a code is accepted, and the camera is unbound in `onDispose`.
- Android reports "should show rationale" as false both before the first request and after a
  permanent denial, so a saved "asked before" flag tells them apart; the permanently denied state
  links to system settings. Manual ISBN entry (ISBN-10 or ISBN-13, checksum validated) is always
  available.

## Database and migrations

- Schema version 2 added the sync bookkeeping (`dirty` and `deleted` columns, `updated_at` on
  reading logs, a `sync_state` table). The hand-written migration marks existing rows dirty so the
  first sign-in uploads the library that was built before sync existed.
- The Room Gradle plugin exposes schemas only to instrumented tests, so `MigrationTest`
  (Robolectric) builds the version 1 database from `schemas/.../1.json` itself and lets Room
  validate the migrated database against the current entities when it opens. The migration was
  also checked on the emulator by upgrading an installed version 1 build that held data.

## Sync and sign-in

- **Where the Firebase Auth wrapper lives.** The specification puts it in `:feature:auth`, but
  `SyncWorker` in `:core:data` needs the signed-in user, and core modules must not depend on
  features. So `AuthRepository` and its Firebase and no-op implementations are in `:core:data`
  next to `SyncGateway`; `:feature:auth` holds the sign-in screen and the Credential Manager client
  that obtains the Google ID token.
- **Choosing implementations**: `SyncGateway` and `AuthRepository` are provided with `@Provides`
  because the choice (`FirebaseApp.getApps(context).isNotEmpty()`) is made at run time; every other
  repository is bound with `@Binds`. Firebase objects are only created when Firebase is configured.
- **Conflict policy** (`ConflictPolicy`, unit tested): last write wins per document, by the edit
  time `updatedAt`. On a tie the server copy wins over unsaved local changes so that devices
  converge; a clean local row with the same time is the same version and is left alone. A deletion
  is a write (a tombstone), so an edit made after a deletion brings a book back.
- **Pulling**: each document also carries the server time `syncedAt`, and the cursor in
  `sync_state` is that server time, so a device with a wrong clock cannot hide its changes from
  the others. Pulls ask for documents at or after the cursor because documents written together
  share a timestamp; applying a document twice changes nothing.
- **Pushing** happens in Firestore transactions that re-check the policy against the server copy.
  A row is marked clean only if it did not change while the upload ran.
- **Tombstones** carry `deletedAt` (only tombstones have it, so a single-field index is enough)
  and are dropped after 30 days on both sides; locally only once they were uploaded.
- **The first sync of an account on a device** marks every local row dirty: the local library
  merges into whichever account signs in.
- **Scheduling**: after every local write, a unique one-time `SyncWorker` (`REPLACE` with a 5 s
  delay, so a burst of edits becomes one run; no delay after sign-in), plus a unique periodic sync
  every 6 hours while signed in. Both need a connection and back off exponentially; network
  failures are retried up to 5 times.
- Firestore's own cache is kept in memory only, because Room is the single source of truth.
  Documents are mapped by hand, so R8 needs no keep rules for them.
- **Google sign-in** uses Credential Manager's "Sign in with Google" option. It is called from the
  composable because it needs the activity; the ViewModel receives the result. The web client id
  is read from `google-services.json` at build time (`BuildConfig.GOOGLE_WEB_CLIENT_ID`).
- **Sign-out** cancels sync work and keeps local data unless the user ticks "Also remove my
  library and reading log from this device", which clears the database (preferences stay).
- **Crashlytics**: the SDK is always on the classpath and its Gradle plugin is applied only with
  `google-services.json`; collection is off in debug builds.

## Accessibility

- Content descriptions on covers and actionable icons, headings, semantic roles on custom
  controls (switch rows, chips, rating, chart), live regions for scan results and errors, and a
  text alternative for the bar chart.
- **Material 3 1.5's `ExtendedFloatingActionButton` drops the semantics of its text** (found with a
  UI Automator dump), so `LabeledExtendedFab` sets the label as the content description.
- **Contrast** of the fallback palette was computed for light and dark: every text pair reaches
  4.5:1 and graphical elements 3:1. The "goal not met" chart bars were 1.9:1 and now use the
  outline color (3.5:1 light, 3.9:1 dark).
- **Automated checks**: Compose's Accessibility Test Framework integration does nothing on
  Robolectric and needs API 34+, so it runs as instrumented tests (`connectedDebugAndroidTest`)
  over Library, Book detail, Stats and Settings in light and dark. They pass on the API 37
  emulator; the only warning concerns a row cut off at the bottom edge of the screen.
- **Font scale 1.3** is covered by the screenshot tests and was checked by eye: text wraps, long
  titles end with an ellipsis, and nothing is clipped.

## Testing

- Every ViewModel has tests, and every ViewModel test class has at least one Turbine test.
  `awaitUntil` waits for a matching state because `stateIn(WhileSubscribed)` may or may not expose
  `Loading`, depending on dispatcher timing.
- **Screenshot tests** use Compose Preview Screenshot Testing 0.0.1-alpha16, which works with AGP
  9.5. Library, Book detail, Stats and Settings are rendered in light, dark and at font scale 1.3.
  Preview names avoid dots because the plugin cuts file names at the first dot. Validation is not
  part of `check` because font rendering differs between operating systems.
- **Instrumented tests** are the accessibility checks only. Everything else, including Room and
  Compose UI tests, runs on Robolectric, so `./gradlew test` needs no device.

## Release and CI

- R8 and resource shrinking need no keep rules of the app's own: every library ships consumer
  rules, including the rule for the reflectively loaded Credential Manager provider. The release
  build was run on the emulator through search, details, adding to a shelf, the log deep link,
  Stats, a camera scan and a sync run.
- **Signing** comes from `SHELF_KEYSTORE_FILE`, `SHELF_KEYSTORE_PASSWORD`, `SHELF_KEY_ALIAS` and
  `SHELF_KEY_PASSWORD`. Without them a release build is signed with the debug key so that it can be
  tested locally. `release.yml` decodes the keystore from a secret into the runner's temporary
  directory and deletes it at the end; the signing path was tested with a throwaway keystore and
  `jarsigner`.
- CI installs SDK platform 37.2 and build-tools 37.0.0, which the runner image does not have.

## Deviations from the specification

| Specification | What was built | Why |
|---|---|---|
| Firebase Auth wrapper in `:feature:auth` | `AuthRepository` and its implementations in `:core:data`; sign-in UI and Credential Manager client in `:feature:auth` | `SyncWorker` needs the user, and core modules must not depend on features |
| Repositories bound with `@Binds` | `SyncGateway` and `AuthRepository` use `@Provides` | Firebase or no-op is chosen at run time |
| Screenshot tests for at least four screens | Done for four screens, but not run by `check` or CI | Font rendering differs between operating systems |
| Accessibility | Automated ATF checks run as instrumented tests, not in CI | They need an API 34+ device and CI has no emulator job |
| Cloud sync works once a Firebase config is added | Implemented and tested with fakes; never run against a live project | No Firebase credentials were available while building |
| Room (no version given) | Room 2.8 | See "Room 2.8 instead of Room 3" |
