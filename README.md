# WP Padel Tracker

A padel match scoring app for smartwatches (**Wear OS** and **watchOS**), plus **WP Padel Board**, a phone companion (Android and iPhone) that mirrors the watch as a big courtside scoreboard. The scoring rules, display logic and sync payloads live once in a Kotlin Multiplatform module that every app uses.

---

## Overview

Track a live padel match from your wrist. Tap the top half to score for the opponent and the bottom half to score for you. Tap the round button to undo the last point. Hold it for 1.5 s to turn it into a red X, then tap to end the match early.

The running match is saved on the watch after every point, so it survives the app being closed or killed. From the summary screen you can undo the winning point (or resume a match you stopped early) and keep playing.

On the phone (Android or iPhone), **WP Padel Board** shows the live score in landscape and keeps the screen on, which makes it a courtside display. It is also where you type the team names; they then appear on the watch too.

**Supported formats:**
- Singles and Doubles
- Best of 3 or Best of 5 sets
- Standard rules (deuce/advantage)
- Golden Point (one deciding point at deuce)
- Star Point (advantage twice, then one deciding point)
- Tie-break at 6:6 with automatic serve rotation
- "Change sides" prompt after every set but the last; in doubles each team may then swap court positions

---

## Platforms

| App | Platform | Language | UI Toolkit | Min OS | Target |
|---|---|---|---|---|---|
| WP Padel Tracker | Wear OS | Kotlin | Jetpack Compose | API 26 | API 37 |
| WP Padel Board (companion) | Android | Kotlin | Jetpack Compose | API 26 | API 37 |
| WP Padel Tracker | watchOS | Swift 6 | SwiftUI | watchOS 10.6 | current SDK |
| WP Padel Board (companion) | iOS | Swift 6 | SwiftUI | iOS 17 | current SDK |

The Android watch and phone apps talk over the Wear Data Layer, so they share one `applicationId` and one signing key (`tool/app_config.py` checks the id).

---

## Repository layout

```
wp-tracker/
├── app.json   # names, ids, team and version of every app — edit only here
├── tool/       # app_config.py (applies / checks app.json), simulator.py (CI simulator picker), xccov_floor.py (Swift coverage floor), their tests, and testing_icons.swift (redraws the "Testing" icons that Debug builds show)
├── .github/    # CI workflow and Dependabot
├── .editorconfig, .swiftlint.yml  # code style (ktlint reads .editorconfig) and SwiftLint rules
├── code/
│   ├── build-logic/  # Gradle convention plugins shared by every module
│   ├── config/detekt/  # detekt rules
│   ├── gradle/libs.versions.toml  # every version, SDK level and the JDK
│   ├── shared/     # Kotlin Multiplatform: rules, match history, display logic, sync payloads
│   ├── android/
│   │   ├── common/   # Compose board UI, file storage and Data Layer link shared by both Android apps
│   │   ├── watch/    # Wear OS app
│   │   └── phone/    # Android phone companion (WP Padel Board)
│   └── apple/      # WPPadel.xcodeproj
│       ├── Common/                 # Swift + assets shared by the watch and phone apps
│       ├── WPPadelTracker/         # watchOS app
│       ├── WPPadelBoard/           # iPhone companion
│       ├── WPPadel*Tests/, *UITests/  # unit and UI tests per app
│       └── Config/                 # Info.plists, entitlements, generated App.xcconfig
├── docs/           # Privacy policies (published as a static site)
├── WP_Logo/        # Logo and icon sources; testing/ holds the two approved "Testing" icon masters
└── initial_spark/  # Original concept (German)
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for how the pieces fit together.

---

## Building

### Prerequisites

- **JDK:** Gradle provisions its own JDKs (25 runs Gradle, `gradle/gradle-daemon-jvm.properties`; 21 compiles the code, `jvm` in `gradle/libs.versions.toml`), but a JDK must exist to start the wrapper. Android Studio's bundled runtime works:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- **Android:** Android Studio (latest stable), Android SDK 37
- **watchOS:** Xcode 26 or newer, on macOS with Apple Silicon

### Android (Wear OS watch and phone)

```bash
cd code
./gradlew :android:watch:assembleDebug :android:phone:assembleDebug           # debug APKs
./gradlew clean :android:watch:bundleRelease :android:phone:bundleRelease     # release AABs for the Play Console
```

Output: `code/android/<watch|phone>/build/outputs/bundle/release/<watch|phone>-release.aab`. Both go into the same Play Console app (same `applicationId`): the watch bundle on the Wear OS form factor, the phone bundle on phones. Play needs a different `versionCode` for every bundle of one app, so each is `version.build × 10 + android.<app>.versionCodeOffset` (phone 1, watch 2).

Shared build configuration lives in convention plugins in `code/build-logic` (see [ARCHITECTURE.md](ARCHITECTURE.md#build)): `wppadel.android.application` gives both apps their name, id and version from `app.json` and the same release key. The SDK levels and the JDK are defined once, in `code/gradle/libs.versions.toml` (`android-compileSdk`, `android-minSdk`, `android-targetSdk`, `jvm`). The configuration cache and the build cache are on (`code/gradle.properties`).

**Release signing** reads `code/android/keystore.properties` (git-ignored). A relative keystore path in it is relative to that file:

```properties
prod.keystore=keystore/wp_padel_tracker.keystore
prod.storePassword=…
prod.alias=…
prod.password=…
```

Without that file (in CI, for example), the key comes from the environment: `WPPADEL_KEYSTORE_FILE`, `WPPADEL_KEYSTORE_PASSWORD`, `WPPADEL_KEY_ALIAS` and `WPPADEL_KEY_PASSWORD`. Keep the keystore itself out of git too. With no key at all, a release build stops with "No release key"; the passwords may stay empty until a release, which then stops with "Release key incomplete" naming the empty or missing properties (or variables); with a key whose keystore is missing, AGP stops at `validateSigningRelease`. Debug builds need no key and never fail on an incomplete one.

To show a sample scoreboard on a debug build of the phone app without a watch: `adb shell am start -n ch.wastedpotential.padel/ch.wastedpotential.padel.phone.MainActivity --es sample_board match` (or `summary`). Release builds ignore the extra.

**Watch to phone for real** has no automated test (it needs a paired pair). By hand: pair the watch with the phone (Wear OS or Galaxy Wearable app), install both debug apps, type names on the phone (they appear on the watch), play a point (the phone mirrors it), kill and relaunch the phone app (it shows the current board again), then reinstall the watch app and play a point (the phone follows the new match without a relaunch).

### watchOS and iPhone

Open `code/apple/WPPadel.xcodeproj`. There are two schemes:

- **WPPadelTracker** runs the watch app.
- **WPPadelBoard** runs the iPhone app, which contains the watch app. Archive this one for the App Store.

Each app target has a **Compile Kotlin Framework** build phase (`apple/Scripts/compile-kotlin.sh`) that runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`. The shared module is therefore always built for the current configuration (Debug or Release) and device or simulator, with no manual framework step. Without `JAVA_HOME` the script uses an installed JDK 17+ (`/usr/libexec/java_home`), else the runtime bundled with Android Studio.

Build settings are in `apple/Config/*.xcconfig` (one base file, one per platform and one per target), not in the Xcode project.

The project uses folder-synced groups: a new Swift file in a target folder is part of that target automatically.

| ID (from `app.json`) | App |
|---|---|
| `ch.wastedpotential.padel` | iPhone app (WP Padel Board) |
| `ch.wastedpotential.padel.watchkitapp` | watch app; it also runs without the phone |
| `ch.wastedpotential.padel` | Wear OS and Android phone apps (`applicationId`, must be equal) |

Test bundles add `.tests` / `.uitests` to their app's ID.

To build the standalone XCFramework anyway:

```bash
cd code
./gradlew :shared:assembleSharedXCFramework
```

---

## App config and version

**`app.json` is the single source** of every app's name, bundle ID / applicationId, Apple team and version:

```bash
# edit app.json, then
python3 tool/app_config.py apply    # regenerates code/apple/Config/App.xcconfig
```

- **Gradle** reads `app.json` directly (`applicationId`, `versionName`, `versionCode` from `version.build` and `versionCodeOffset`, app label).
- **Xcode** includes the generated `Config/App.xcconfig` from `Config/Base.xcconfig`, the project's base configuration. Every target refers to `$(PHONE_BUNDLE_ID)`, `$(WATCH_BUNDLE_ID)`, `$(MARKETING_VERSION)` and so on, so the phone and the embedded watch app always share one version. Do not set Version, Build, Team or Bundle Identifier in Xcode's General tab: that writes the value into the project and overrides `app.json`.
- **`python3 tool/app_config.py check`** fails if `app.json` is invalid (a key is missing, the Apple team is not a 10-character team ID, the watch ID does not start with the phone ID, the two Android apps have different ids or the same `versionCodeOffset`), if `App.xcconfig` is stale, or if the Xcode project hard-codes any of these values. It exits 1 on a problem and 2 on an unknown command. It runs before every Android build (`./gradlew verifyAppConfig`) and as the first step of every Xcode app build. `tool/test_app_config.py` tests it.
- `version.build` must grow with every store upload. On Android each app's `versionCode` is `build × 10 + versionCodeOffset`, so the two bundles of the one Play app never collide.

The current version is `version.name` / `version.build` in [`app.json`](app.json).

## Testing

| Suite | Command | Runs on |
|---|---|---|
| Shared engine, logic and sync (28 classes) | `./gradlew :shared:allTests` | JVM, Android host, watchOS and iOS simulators |
| Android shared code (file store, test link, Data Layer ordering) | `./gradlew :android:common:testDebugUnitTest` | JVM |
| Wear OS view model (incl. the auto summary) | `./gradlew :android:watch:testDebugUnitTest` | JVM |
| Android phone view model | `./gradlew :android:phone:testDebugUnitTest` | JVM |
| Wear OS UI (Compose) | `ANDROID_SERIAL=<watch> ./gradlew :android:watch:connectedDebugAndroidTest` | Wear OS emulator or watch |
| Android phone UI (Compose) | `ANDROID_SERIAL=<phone> ./gradlew :android:phone:connectedDebugAndroidTest` | phone or phone emulator |
| Build logic (release key, app.json parsing) | `./gradlew :build-logic:test` | JVM |
| Kotlin coverage floors (line and branch, Kover) | `./gradlew koverVerify` | JVM |
| Android lint (warnings are errors) | `./gradlew :android:common:lintDebug :android:watch:lintDebug :android:phone:lintDebug` | JVM |
| Kotlin style and static analysis (ktlint, detekt) | `./gradlew spotlessCheck detekt` (`./gradlew spotlessApply` formats) | JVM |
| Swift style | `swiftlint lint --strict` (from the repository root; `swiftlint --fix` formats) | macOS |
| watchOS unit + UI | `xcodebuild test -project apple/WPPadel.xcodeproj -scheme WPPadelTracker -destination 'platform=watchOS Simulator,name=Apple Watch Series 12 (46mm)'` | watchOS simulator or watch |
| iPhone unit + UI | `xcodebuild test -project apple/WPPadel.xcodeproj -scheme WPPadelBoard -destination 'platform=iOS Simulator,name=iPhone 18 Pro'` | iOS simulator or iPhone |
| Swift coverage floor of an app target | `python3 tool/xccov_floor.py <bundle>.xcresult <Target>.app <min-line-%>` after an `xcodebuild test … -enableCodeCoverage YES -resultBundlePath <bundle>.xcresult` | macOS |
| Release shrinking (R8, no key needed) | `./gradlew :android:watch:minifyReleaseWithR8 :android:phone:minifyReleaseWithR8` | JVM |
| App config, simulator and coverage tools | `python3 -m unittest discover -s tool` (from the repository root) | any Python 3.9+ |

`koverVerify` runs the JVM and Android host unit tests of `shared` and `android/*` and fails when a module's line or branch coverage drops below the floor in its `build.gradle.kts`. `./gradlew koverHtmlReport` writes the per-module report to `<module>/build/reports/kover/html/index.html` (for example `shared/build/reports/kover/html/index.html`). Left out of the measurement is what only a device can run: composables and the lambdas inside them, the Compose files' top-level icons and colours, the activities, the Data Layer link and the vibrator factory (the Compose UI tests and `DataLayerChannelTest` cover them on a device). The floors therefore measure the view models, the storage and the link helpers (`code/build-logic/src/main/kotlin/wppadel/Coverage.kt` lists the exclusions).

Both Xcode schemes record code coverage for their app target (`WPPadelTracker`, `WPPadelBoard`), from the unit and the UI tests together, so every `xcodebuild test` result bundle carries it; `xcrun xccov view --report --only-targets <bundle>.xcresult` prints the line coverage per target, and `tool/xccov_floor.py` fails below a floor (CI runs it with the floors in `ci.yml`, see [CI/CD](#cicd)).

Run the Gradle commands from `code/`. For the Wear OS UI tests, use a **Wear OS** emulator image (for example `system-images;android-37.0;android-wear-signed;arm64-v8a`); a phone image refuses to install the watch app. With both a watch and a phone connected, pick one with `ANDROID_SERIAL`.

The Android UI tests build the view model themselves, with in-memory stores and an in-memory link instead of the Data Layer: `MemoryChannel`, a test fixture of `android/common` that keeps the last value per path like the Data Layer (`MemoryChannel.pair()` links a watch and a phone). Timers run on a virtual clock, so no test waits in real time. The phone tests show sample scoreboards from `SampleBoards`. `:android:common:connectedDebugAndroidTest` runs `DataLayerChannelTest` against the real Data Layer of one device; it is skipped where the Data Layer is missing (a phone without a Wear OS companion app), so run it on a watch.

A watch with wireless debugging can appear twice in `adb devices` (its IP and an `adb-…._adb-tls-connect._tcp` name). Use the `adb-…` name in `ANDROID_SERIAL`: with the IP the connected test task fails even though every test passes.

The UI tests launch the apps with `-UITesting` (these launch arguments exist in Debug builds only):
- **watchOS:** the match and team names stay in memory, there is no phone link, and no HealthKit prompt covers the screen. `-UITestMatch <scenario>` opens on a match played through the real session (`UITestScenario`: a won set in singles or doubles, a won or lost match, a match won under Golden Point rules, match point, a tie-break), so long tests do not tap their way there.
- **iPhone:** there is no watch link, and `-UITestBoard match|summary` shows a sample scoreboard built with the real shared engine.

Run the watch UI tests on a watch simulator that is **not paired** with an iPhone simulator (`xcrun simctl list pairs`; `tool/simulator.py watchOS` prefers one). On a paired watch whose iPhone has no WP Padel Board, the watch's companion sync uninstalls the watch app about 15 s into the run (`appconduitd`: "Uninstalling … because its counterpart is not installed on phone"): the test running at that moment fails with "Failed to launch … FBSApplicationLibrary returned nil", xcodebuild restarts the runner, reinstalls the app and the remaining tests pass. `XCUIApplication.watch()` retries a refused `launch()` once; that does not cover the uninstall.

Run the two Apple schemes one after the other, not in parallel: WPPadelBoard embeds the watch app, so both builds write the same watchOS `Shared.framework` under `code/shared/build/xcode-frameworks`, and one link fails with "found architecture 'arm64', required architecture 'arm64_32'".

The first device run of a new bundle ID (for example a UI-test runner) needs Xcode with your account signed in, so it can create the provisioning profile. After that, `xcodebuild` works from the terminal too.

---

## Key Dependencies

Kotlin, the Android Gradle Plugin, Compose, Coroutines, Serialization, Play services Wearable, ktlint, detekt and Kover: every version is pinned in [`code/gradle/libs.versions.toml`](code/gradle/libs.versions.toml), Gradle's in [`code/gradle/wrapper/gradle-wrapper.properties`](code/gradle/wrapper/gradle-wrapper.properties), and the JDK toolchain resolver plugin's in [`code/settings.gradle.kts`](code/settings.gradle.kts) (a settings plugin cannot use the catalog). detekt is a 2.0 pre-release on purpose: the `dev.detekt` plugin id exists only from 2.0, which has no stable release yet; move to the first stable 2.0 when it ships.

---

## CI/CD

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push to `main` and every pull request:

- **ubuntu:** `app_config.py check` and the `tool` tests, ktlint and detekt, the build-logic tests, `:shared:jvmTest` and `:shared:testAndroidHostTest`, the three Android unit test suites, the Kover coverage floors (`koverVerify`), Android lint, and R8 over both apps' release variant (`minifyReleaseWithR8`, which needs no signing key), so a broken ProGuard rule or a stripped class shows up before a store build.
- **macOS:** SwiftLint, `:shared:allTests` (including the watchOS and iOS simulator targets), then `xcodebuild test` for **WPPadelTracker** and **WPPadelBoard** on the newest installed simulators (`tool/simulator.py` picks them, an unpaired watch when there is one), unsigned (`CODE_SIGNING_ALLOWED=NO`) and with code coverage, which `tool/xccov_floor.py` checks against the line floor of each app target set in `ci.yml` (just below the measured value, like the Kover floors).

There are no release or upload jobs: store builds are made and signed locally. Dependabot ([`.github/dependabot.yml`](.github/dependabot.yml)) proposes Gradle and GitHub Actions updates weekly, one grouped pull request each; every action is pinned to a commit with its version in a comment, and Dependabot moves both.

## Contributing

- [`.editorconfig`](.editorconfig) sets the style: 4 spaces for Kotlin and Swift, 2 for JSON, YAML and HTML, tabs only in Xcode's own files. `./gradlew spotlessApply` and `swiftlint --fix` apply it; CI fails on any ktlint, detekt, SwiftLint or Android lint finding.
- Change names, ids, team or version only in `app.json`, then run `python3 tool/app_config.py apply`.
- Run the suites in [Testing](#testing) that cover your change; CI runs all of them except the Android `connectedDebugAndroidTest` suites, which need a device or emulator.
