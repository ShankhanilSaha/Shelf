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
  `shelf.android.feature`, `shelf.android.room`, `shelf.hilt`, `shelf.jvm.library`, `shelf.root`).
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
  integration is guaranteed to understand AGP 9 variants.
- **Configuration cache is on** (`problems=warn` so a third-party plugin cannot break a build).

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
