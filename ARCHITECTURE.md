# Architecture

## Overview

WP Padel Tracker is a padel match scorer for smartwatches, with a phone companion (WP Padel Board, on Android and iPhone) that mirrors the watch. Everything that is not drawing pixels lives once in a Kotlin Multiplatform module (`shared`): the rules, the undo history, the setup flow, what each screen shows, saving, and the watch ↔ phone payloads. Each app keeps only a thin state holder and its native UI: Jetpack Compose on Android, SwiftUI on watchOS and iOS. On each platform the watch and phone apps share their views: `android/common` (Compose) and `apple/Common` (SwiftUI).

```
 Android (Compose)                                    Apple (SwiftUI)
┌────────────────────────┐                          ┌────────────────────────┐
│ Wear OS watch          │                          │ watchOS watch          │
│ WatchViewModel         │                          │ MatchStore             │
└──────┬──────────┬──────┘                          └──────┬──────────┬──────┘
       │ Data     │        ┌──────────────────────┐        │          │ Watch-
       │ Layer    └───────►│ shared (KMP)         │◄───────┘          │ Connectivity
       │                   │ match/   MatchSession│                   │
       │       ┌──────────►│          MatchHistory│◄──────────┐       │
       │       │           │ setup/   SetupState  │           │       │
       │       │           │ display/ MatchDisplay│           │       │
       │       │           │          SummaryStats│           │       │
       │       │           │ sync/    Scoreboard  │           │       │
       │       │           │          PhoneBoard  │           │       │
       │       │           │          NamesBook   │           │       │
       │       │           │ engine/  MatchEngine │           │       │
       │       │           │ model/   Snapshot    │           │       │
       ▼       │           └──────────────────────┘           │       ▼
┌──────────────┴─────────┐                          ┌─────────┴──────────────┐
│ Android phone          │                          │ iPhone                 │
│ BoardViewModel         │                          │ ScoreboardModel        │
└────────────────────────┘                          └────────────────────────┘
 views: android/common                                 views: apple/Common
```

The watches drive a `MatchSession` and send each `Scoreboard` to their phone; the phones hold the newest one in a `PhoneBoard` and send the team names back (`NamesBook` on both sides).

---

## Module Structure

```
code/
├── shared/src/commonMain/kotlin/ch/wastedpotential/padel/
│   ├── model/      Enums.kt, Snapshot.kt        immutable, @Serializable state; SetScore.winner
│   ├── engine/     MatchEngine.kt               pure scoring rules
│   ├── match/      MatchHistory.kt              undo stack, screen, epoch and revision, save/restore, Feedback
│   │               MatchSession.kt              a watch app's whole match logic, incl. setup and back
│   │               MatchSetup.kt                setup answers → Config
│   │               Buzz.kt                      Feedback → vibration pattern
│   ├── setup/      SetupState.kt                the setup flow as screens of choices
│   ├── display/    MatchDisplay.kt              what the match screen shows, incl. its Prompt
│   │               SummaryStats.kt              what the summary screen shows
│   │               Choice.kt                    Prompt, Choice, Arrow, Tone
│   │               UiText.kt, UiTags.kt         every on-screen string; UI test ids (choice tags too)
│   │               BoardStyle.kt                BoardColors, BoardAlpha, BoardMetrics
│   │               UiTimings.kt                 timings both apps share
│   ├── platform/   Platform.kt                  clock, TextStore
│   │               MemoryTextStore.kt           in-memory TextStore (tests, UI-test launch modes)
│   └── sync/       Scoreboard.kt                what a paired phone shows
│                   PhoneBoard.kt                the phone's newest board and which screen to draw
│                   NamesBook.kt, TeamNames.kt   names typed on the phone
│                   SyncKeys.kt, SampleBoards.kt payload keys; sample boards for the phones' UI-test modes
│
├── build-logic/                                 convention plugins every Gradle module applies (see Build)
├── config/detekt/detekt.yml                     detekt overrides on top of its default rules
├── gradle/libs.versions.toml                    every version, SDK level and the JDK, in one place
│
├── android/
│   ├── common/src/main/kotlin/ch/wastedpotential/padel/   used by the watch AND phone apps
│   │   ├── board/      Theme.kt                 LocalBoardScale, scaledDp/Sp, BoardWhite, Tone and Team colours, BoardText
│   │   │               MatchBoard.kt            panels, set scores, pill (the match board)
│   │   │               SummaryParts.kt          title, result panels, result pill, stat rows
│   │   │               StrokeIcon.kt            line icons
│   │   ├── storage/    FileTextStore.kt         shared TextStore on an AtomicFile: read once off the main thread, one app-wide disk thread
│   │   └── link/       SyncChannel.kt           DataLayerChannel (Wear Data Layer)
│   │   (res/: launcher icon and backup rules for both apps; src/testFixtures/: MemoryChannel for every Android test)
│   ├── watch/src/main/kotlin/ch/wastedpotential/padel/presentation/
│   │   ├── MainActivity.kt                      stores, Data Layer link, vibrator
│   │   ├── WatchApp.kt                          screen routing, back handling, WatchFrame (scaling)
│   │   ├── WatchViewModel.kt                    Compose state over MatchSession and NamesBook, the auto summary
│   │   ├── Haptics.kt                           Feedback → one vibrator waveform (Buzz)
│   │   ├── prompt/PromptView.kt                 tap prompt and left/right picker for any Prompt
│   │   ├── setup/SetupView.kt                   draws SetupState.screen
│   │   ├── match/                               MatchScreen (tap zones + MatchBoard, TalkBack actions), UndoButton
│   │   └── summary/SummaryScreen.kt
│   └── phone/src/main/kotlin/ch/wastedpotential/padel/phone/
│       ├── MainActivity.kt                      landscape, screen on, no system bars, sample boards
│       ├── BoardViewModel.kt                    BoardState over PhoneBoard + NamesBook, Data Layer link
│       ├── BoardApp.kt                          BoardScreen: match board, landscape summary, waiting screen
│       └── NameEditor.kt
│
└── apple/
    ├── Common/                                  compiled into the watch AND phone apps
    │   ├── MatchBoard.swift                     MatchLayout, panels, set scores, pill row (the match board)
    │   ├── SummaryParts.swift                   title, result panels, result pill, stat rows
    │   ├── Pill.swift, Scaling.swift            pill, hairline; boardScale, BoardMetrics sizes, BoardStyle
    │   ├── BoardColors.swift                    Color(argb:), the shared palette, ToneColors (the one tone mapping)
    │   ├── Launch.swift                         the UI-test launch arguments (Debug builds only)
    │   ├── NamesModel.swift                     SwiftUI wrapper over the shared NamesBook
    │   ├── FileTextStore.swift, Log.swift      FileTextStore (shared TextStore in Application Support); os.Logger
    │   ├── LinkSession.swift                    what both links use of WCSession, so tests can stand in a fake
    │   └── Assets.xcassets                      one icon set for both apps
    ├── WPPadelTracker/                      watch app
    │   ├── WPPadelTrackerApp.swift              entry point + RootView routing
    │   ├── MatchStore.swift                     SwiftUI wrapper over the shared MatchSession; haptics and workout follow it
    │   ├── UITestScenario.swift                 matches the UI tests open on (Debug builds only)
    │   ├── Sync/PhoneLink.swift                 WatchConnectivity → phone (a ScoreboardSink)
    │   ├── Workout/                             WorkoutManager (lifecycle), WorkoutBackend (protocol), HealthKitWorkout
    │   ├── Haptics/HapticManager.swift          plays the shared Buzz (a FeedbackPlayer)
    │   └── Views/                               Setup, Match (+ undo), Summary, Components (ChoiceStack, PromptView)
    ├── WPPadelBoard/                        iPhone companion
    │   ├── WPPadelBoardApp.swift                entry point, landscape lock (OrientationGate)
    │   ├── ScoreboardModel.swift                SwiftUI wrapper over PhoneBoard + name edits
    │   ├── WatchLink.swift                      WatchConnectivity → watch
    │   └── ScoreboardView.swift, NameEditor.swift
    ├── Config/                                  Info.plists, entitlements, xcconfigs (App.xcconfig generated)
    ├── Scripts/compile-kotlin.sh                the "Compile Kotlin Framework" build phase
    └── WPPadel{Tracker,Board}{Tests,UITests}/
```

The two apps use the same file and component names on purpose. A change to one screen usually has an obvious twin on the other platform.

---

## Shared Module

### MatchEngine: the rules

`MatchEngine` is a stateless, module-internal Kotlin `object`. Every function maps a `Snapshot` to a new `Snapshot` with no side effects:

```
initialSnapshot(config, startedAt) → Snapshot
score(snapshot, team)              → Snapshot
pickOpponentFirstServer(snapshot, player)
confirmPositionSwitch(snapshot, team, doSwitch)
acknowledgeCourtSideChange(snapshot)
computePill(snapshot)              → PillState
```

Each returns the *same instance* when the action does not apply, and callers use this to detect a no-op. `score` plays a point only while `Snapshot.acceptsPoints`: the match is neither won nor ended, and no prompt is open (serve pick, side change or position switch). One rule, `winsNextPoint`, decides both who wins a game and the game, set and match points of the pill.

Everything a snapshot can derive is derived, not stored: `isMatchOver` and the sets won come from `MatchState.completedSets`, the serving team from the serving player, the score corners from the rally count.

### MatchHistory: the one source of truth

`MatchHistory` (module-internal) holds the undo stack (`List<Snapshot>`). `MatchSession` wraps it and adds the setup flow, saving and the phone mirror; the platform state holders wrap `MatchSession` and add only observability.

| Operation | Stack effect | Why |
|---|---|---|
| `start(config, now)` | reset to one snapshot | new match |
| `score(team)` → `Feedback` | push | undo removes the point |
| `answerPrompt(choice)` → `Feedback` | the serve pick pushes (undo shows the picker again); the side change and position switch replace the top | undo skips straight past those prompts |
| `end(now)` | replace the top (sets `endedAt`); a no-op once ended | moves to the summary |
| `reopen()` | pop if the match was won, otherwise clear `endedAt` | undo across the match end |
| `undo()` → `Feedback` | pop | `NONE` when there was nothing to undo |
| `clear()` | empty | back to setup |

`answerPrompt` accepts only an index of the open prompt's choices (`0` for the one-choice side change); anything else is `NONE`. The **screen is derived from the stack**: empty → `SETUP`, `endedAt` set → `SUMMARY`, otherwise `MATCH`. Restoring a saved stack therefore restores the screen too.

`revision` counts the changes: every operation that changes the stack raises it by one (undo too), and a no-op does not. It is saved with the stack and carries on through `clear()`, so it only ever grows on a watch. `MatchSession` saves and publishes exactly when it moved, and the phone orders scoreboards by it. `epoch` names that count: a fresh history (first launch, a reinstall, cleared data) draws a random one, and it is saved and kept through every change, `clear()` and a relaunch. The phone orders by revision only within one epoch, so a reinstalled watch, whose revisions start over, is followed from its first board.

`isAwaitingSummary` is true when the match was won but `end()` has not been called yet. Both apps wait `UiTimings.AUTO_SUMMARY_DELAY_MS` (400 ms) on the final score and then call `end()`. An undo in that window cancels it.

`Feedback` tells the app what the watch should feel. `Buzz.of(feedback)` is a rhythm of `Pulse`s (TAP 150 ms, BUZZ 350 ms, LONG_BUZZ 750 ms, 450 ms apart, lengths in `UiTimings`): `CONFIRM` (an answer) 1 tap, `POINT_YOU` 1 buzz, `POINT_OPP` 2 buzzes, `UNDO` 3 taps, `GAME_WON` 1 long, `SIDE_CHANGE` 2 long. Every pattern is distinct, its pulses start at least 600 ms apart and it ends within 2 s; `BuzzTest` checks all three.

### MatchSession: what a watch app wraps

`MatchSession(store, phone, clock)` owns the `MatchHistory` and the setup flow, saves to its `TextStore` after every change (and only then: `end()` on the summary or `reopen()` during a match saves nothing) and sends a `Scoreboard` to its `ScoreboardSink`. Its `state` (`SessionState`) holds the screen, the `MatchDisplay` or `SummaryStats`, the current setup screen, `canUndo`, `isAwaitingSummary` and `handlesBack`. `chooseSetup(index)` answers a setup screen and starts the match on the last answer; an index outside the screen's choices is `NONE`. `back()` steps back through setup, is swallowed during a match, and returns false on the first setup step and on the summary. Each watch app only observes `onChange` and plays the returned `Feedback`.

### Persistence

`MatchHistory.encode()` / `MatchHistory.decode(data)` turn the whole stack, its epoch and its revision into versioned JSON (kotlinx.serialization, format 3; a missing field is unreadable). The platform storage writes it to one file after every change, off the main thread:

- Android: `filesDir/match.json` (and `names.json`) via `FileTextStore` (an `AtomicFile`), read once on the disk thread at launch and then answered from memory
- watchOS: `Application Support/match.json` and `names.json` via `FileTextStore`, written atomically; unlike Android it reads the file when the state holder asks for it at launch (a synchronous read on its queue, after any queued write, of two tiny files)
- iPhone: `Application Support/names.json` via the same `FileTextStore`

On launch the state holder decodes the file. Unreadable or other-version data starts fresh (a new epoch). "New match" saves the empty history, which keeps the epoch and the revision for the next match.

### Watch ↔ phone sync

A `Scoreboard` (sent by `MatchSession` from its `SessionState`) is the whole phone screen: the screen, plus either the `MatchDisplay` or the `SummaryStats`, plus the history's `epoch` and `revision`. It is JSON, so the phone never runs the engine and draws exactly what the watch draws. `PhoneBoard` keeps the board with the highest revision of the current epoch, which drops messages that arrive out of order whatever the watch clock does (a board with the same revision, a resend, replaces it). A board from another epoch is always taken: the watch was reinstalled or its data cleared, and its revisions start over near 0, so without the epoch the phone would ignore it until relaunched. Epochs are not ordered, so two watches on one phone would take turns; the design is one watch per phone. The payload carries `version` (4); decoding drops other versions and boards without an epoch, and ignores fields a newer watch adds. `TeamNames` travels the other way; `NamesBook.edit` trims the input, keeps the current name for a blank field, and caps the length.

On watchOS, `PhoneLink` sends every scoreboard as the WatchConnectivity application context (the latest state, kept for the phone's next launch), and as a live message when the phone app is open. On the phone, `WatchLink` sends the names as application context, as a queued user-info transfer (delivered even while the watch app sleeps; it replaces any transfer still queued, since only the latest names matter), and as a live message; when the link comes up it sends only names typed on that phone (`NamesModel.isSaved`). A live message that fails is logged on both sides, like a failed context update. Both links talk to `WCSession` through `LinkSession`, the few members they use, so their unit tests run them over a fake session. On Android both directions are Wear Data Layer data items (`DataLayerChannel`): the watch puts every scoreboard at `SyncKeys.SCOREBOARD_PATH` (urgent), the phone puts the names at `SyncKeys.NAMES_PATH` (at launch only names saved on that phone, so a reinstalled phone never overwrites the watch's names with its defaults). Both listen on `wear://*/<path>` (the item from any node). A data item keeps the latest value, so each app also reads the current item when it starts (dropped if a live change already came in, since it is then older); the phone then shows what the watch last showed, which is still true (a match is saved and resumes). The Data Layer only connects apps with the same `applicationId` and signing key. The payload keys and paths are in the shared `SyncKeys`.

### Display logic

- `MatchDisplay.of(snapshot)`: score texts (0/15/30/40/AD, raw tie-break points), which corner each score sits in, the centred decider layout, the serve stripe side, the pill label, set scores, and the `prompt` covering the match (a `Prompt` of kind `TAP` or `LEFT_RIGHT`, answered with `MatchSession.answerPrompt(index)`; 0 is the left or only answer)
- `SummaryStats.of(snapshot)` (an ended match): winner (null when stopped with level sets), result label, sets (including a partly played set), points, games, breaks, average points per game, tie-breaks (7:6 sets only), deuces, Golden/Star deciders won, and the duration from start to end as HH:MM
- `Choice`: one answer of a `Prompt` or setup screen. `label` is words only, what TalkBack and VoiceOver say; `text` is what the apps draw, the label with the `Arrow` glyph of a left/right answer pointing to its half
- `SetScore.winner`: who won a set, the one rule both apps draw with; null for a set still being played or cut short by "End match". A completed set is never level (the constructor refuses it)
- A **deuce** is any point that leaves a regular game level at 40:40 or beyond (`GamePhase.isDeuce`): every deuce in Standard and Star, the Star Point included, and the 40:40 of a Golden game
- `MatchSetup.config(...)`: the serve order for each setup answer

### State model

```
Snapshot
├── config: Config            bestOf (3|5), ruleMode, playMode, serveOrder
├── match: MatchState         completedSets, startedAt, endedAt?   (setsWon, isMatchOver derived)
├── set: SetScore             youGames, oppGames of the live set (never completed)
├── game: GameState           mode (REGULAR|TIEBREAK), points, phase, starAdvCount
├── serve: ServeState         serverPlayer, serveOrderIndex, opponentServerConfirmed
├── stats: StatsState         points, breaks, deuces, decidersPlayed / decidersWonYou (Golden and Star)
├── awaitingServePick         doubles game 2: which opponent serves?
├── awaitingCourtSideChange   after every set but the last
└── awaitingYou/OppPositionSwitch   doubles, after the court side change
```

### Game phases

```
NORMAL ──(40:40)──► DEUCE (Standard, Star) or GOLDEN (Golden)

Standard:  DEUCE ⇄ ADV_YOU / ADV_OPP ──► game
Golden:    GOLDEN ──(next point)──► game
Star:      DEUCE → STAR_ADV_x ──(lost)──► DEUCE → STAR_ADV_x ──(lost)──► STAR_POINT ──► game
```

### Status pill priority

`computePill()` returns the first match of:

```
MATCH_POINT → SET_POINT → GOLDEN_POINT → STAR_POINT → BREAK_POINT → TIEBREAK → HIDDEN
```

A deciding point outranks a break point because it says more about that point.

### Serve rotation

- **Singles:** alternates A1 and B1 every game.
- **Doubles:** cycles through `serveOrder` [first server, opponent, partner, opponent's partner]. Before game 2, `awaitingServePick` asks which opponent serves. A1/B1 play the right side, A2/B2 the left.
- **Tie-break:** the next player in the rotation serves one point, then each server serves two.

### Score layout (diagonal alternation)

```
rallyIndex = youPoints + oppPoints   (resets every game)
even → You bottom-right, Opp top-left
odd  → You bottom-left,  Opp top-right
```

`MatchDisplay.youScoreOnLeft` is that one rule; at a Golden or Star deciding point both scores are centred.

---

## Build

`code/build-logic` is an included build with four convention plugins, so each module's `build.gradle.kts` keeps only its namespace, dependencies and (for `shared`) its targets:

| Plugin | Applied by | Adds |
|---|---|---|
| `wppadel.root` | root project | `verifyAppConfig`; ktlint and detekt over the build scripts and build-logic |
| `wppadel.kotlin.multiplatform` | `shared` | Kotlin Multiplatform and the Android KMP library, its SDK levels |
| `wppadel.android.library` | `android/common` | the Android setup below |
| `wppadel.android.application` | `android/watch`, `android/phone` | the Android setup, plus name, id and version from `app.json`, the release key and R8 |

The Android setup is the SDK levels, Compose, the instrumentation runner and lint with warnings as errors. Every module gets one JDK for all JVM and Android targets (`jvm` in `libs.versions.toml`, provisioned by Gradle toolchains), test logging, ktlint (through Spotless, style from the repository's `.editorconfig`), detekt (`config/detekt/detekt.yml`) and Kover coverage of its unit tests, with a line and branch floor per module (`koverVerify`, the floor in the module's `build.gradle.kts`). `Coverage.kt` excludes what only a device runs, so the floors measure the view models, storage and link helpers: composables and the lambdas inside them, the Compose files' top-level values (`<File>Kt`), the activities, `DataLayerChannel` and the vibrator factory; the instrumented tests cover those. `check` runs all three. The root build loads every third-party plugin once with `apply false`, and build-logic only compiles against them.

The parsing in build-logic is free of Gradle types so it has plain unit tests (`build-logic/src/test`, `./gradlew :build-logic:test`): `AppConfig.parse` reads one app's entry and the version code out of the text of `app.json`, and `ReleaseKey.from` turns keystore.properties or the environment into a sealed result (`Found`, `Incomplete` with the reason, `Missing`). The `Project` extensions only feed them the file contents and the environment.

`verifyAppConfig` runs `tool/app_config.py check` before every Android build. It declares the script, `app.json`, `App.xcconfig` and the Xcode project as inputs and writes a report, so it is up to date or comes from the build cache until one of them changes. `gradle.properties` turns on the configuration cache, the build cache and parallel builds.

Both apps sign releases with one key, or the Data Layer would not link them. It comes from `code/android/keystore.properties` (git-ignored) or, without that file, from the `WPPADEL_KEYSTORE_FILE`, `WPPADEL_KEYSTORE_PASSWORD`, `WPPADEL_KEY_ALIAS` and `WPPADEL_KEY_PASSWORD` environment variables. Without either, a release build stops at packaging ("No release key"); with a key that lacks a part or leaves one empty it stops there too ("Release key incomplete", naming the properties or variables). Only `packageRelease` and `signReleaseBundle` check, so a debug build never fails on the key.

---

## Cross-platform bridge

Each Apple app target has a **Compile Kotlin Framework** build phase (before *Sources*) that runs `Scripts/compile-kotlin.sh`: it checks `app.json` (`tool/app_config.py check`), finds a JDK (`JAVA_HOME`, else `/usr/libexec/java_home -v 17+`, else the runtime bundled with Android Studio, wherever it is installed) and runs:

```
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

This builds `Shared.framework` for the exact configuration, SDK and architectures Xcode is building, and embeds and signs it. Swift calls `import Shared`, then for example `MatchSession(store:phone:)`, `session.chooseSetup(index:)` or `UiText.shared.NEW_MATCH`. Kotlin objects are reached through `.shared`, Kotlin interfaces (`TextStore`, `ScoreboardSink`) are Swift protocols that `NSObject` subclasses implement, and Kotlin enums appear as classes with lower-camel-case cases (`.pointYou`, `.gameWon`). Kotlin default arguments do not cross over, so Swift goes through `MatchSession` for the setup flow.

`shared` builds in Kotlin's explicit API mode: only what the apps use is public. The rules and their state (`MatchEngine`, `MatchHistory`, `Snapshot`, `SetupState`) are `internal`, so the apps reach them only through `MatchSession`, `PhoneBoard` and the display types, and the Swift header stays small.

Targets: `watchosArm64` (arm64_32), `watchosDeviceArm64` (arm64), `watchosSimulatorArm64`, `iosArm64` and `iosSimulatorArm64`. Intel simulators are not built (Kotlin dropped them), so the Xcode targets exclude `x86_64` for simulator SDKs.

The phone app embeds the watch app ("Embed Watch Content"), so both always ship together with the same version from `app.json` (via the generated `Config/App.xcconfig`).

Build settings live in `Config/*.xcconfig`, not in the project: `Base.xcconfig` (project level, includes `App.xcconfig`), `Watch.xcconfig` / `Phone.xcconfig` (SDK, device family, deployment target; the iPhone app is iPhone only), `LinksShared.xcconfig` (for targets that link `Shared`), `AppTarget.xcconfig` (both apps), and one file per target. Only the Debug/Release differences stay in the project.

---

## Wear OS app

- **WatchViewModel:** opens both `FileTextStore`s off the main thread, then holds the shared `MatchSession` (setup included; mirrored to the phone through the `SyncChannel`) and the `NamesBook`, and exposes `state` and `names` as Compose state. `state` is null until the stores are open, so the app shows its black background rather than a setup screen that the saved match would replace. Every action is a pass-through that returns the session's `Feedback` (`NONE` before opening). It opens the summary `AUTO_SUMMARY_DELAY_MS` after a win (a job that an undo cancels), like `MatchStore` on watchOS. Its coroutine scope is a constructor parameter (tests pass a virtual clock), and the link is registered with `addCloseable`, so clearing the view model closes it.
- **Routing and back:** `WatchApp` switches on `state.screen` and wires each screen's callbacks to the view model and `Haptics`. Its `BackHandler` (enabled by `state.handlesBack`) calls the shared `back()`: a step back through the setup, swallowed during a match, and the system's own back (leaving the app) on the first setup step and the summary. On Wear OS 5 and 6 (checked on a watch and on the emulator) the swipe from the left edge is that same back event, so a stray swipe never leaves or ends a running match. Older versions may dismiss the window instead; the saved match then resumes on the next launch.
- **Views:** the screens are stateless: `SetupView` draws `state.setup` (a stack of choices, or a `PromptView` for the picker); `MatchScreen` takes the `MatchDisplay`, the names and one callback per action and draws the shared `MatchBoard` and any `prompt` through the same `PromptView`. Each has a `@Preview` inside `WatchFrame`, which scales the board as the app does (the match and summary previews come from `SampleBoards`). Text from `UiText`, colours from `BoardColors` and `BoardAlpha`, sizes and their minimums from `BoardMetrics` (every size scaled), test tags from `UiTags`. The UI tests use the same `UiText` and `UiTags` constants.
- **TalkBack:** each tap zone is a button labelled `UiText.pointFor(name)`, valued with that team's score and hinted with `SCORE_HINT` (as on watchOS), and carries custom actions for a point to either team, undo and end match, so ending needs no hold. The board over the zones reads as one element (`MatchDisplay.spokenScore`, tagged `UiTags.MATCH_BOARD`), as on the phone and on watchOS. A left/right picker half draws `Choice.text` but reads `Choice.label`, so the arrow is never spoken. The undo button is one labelled button (undo or end match, as its icon shows). The summary reads each result panel as one item.
- **Buttons:** every clickable passes `indication = null`; the apps set no Material theme, so Compose's debug indication would otherwise draw a grey press rectangle.
- **Scaling:** `LocalBoardScale` is the screen width / `BoardMetrics.WEAR_REFERENCE_WIDTH` (192 dp); `LocalIsRoundScreen` widens the side insets. Board text (`BoardText`) divides out the system font scale, so it stays fixed like the Apple boards' points and the board never overflows.
- **Screen on, no ambient mode:** the match screen keeps the screen on, so every tap during a match scores (in ambient mode the first tap only wakes the watch). The app does not declare ambient support: outside a match the watch dims to its watch face as usual, and a saved match resumes when the app is opened again.
- **Haptics:** `Haptics` hands the shared `Buzz.of(feedback)` to the vibrator as one waveform (`Buzz.timings`, `Buzz.amplitudes`); a new one replaces one still playing. The vibrator is a constructor parameter, so the waveform is unit-tested.

## watchOS app

- **MatchStore:** an `@MainActor ObservableObject` over the shared `MatchSession`, created by `MatchStore.live()`. Every action goes through it: it calls the session, plays the returned `Feedback` on its `FeedbackPlayer` (views never trigger haptics), republishes `session.state` only when it changed, saves through a `FileTextStore`, mirrors to the phone through `PhoneLink` (a `ScoreboardSink`), keeps the workout in step with the screen (below), and opens the summary `AUTO_SUMMARY_DELAY_MS` after a win (through an injected sleep, so tests control the pause).
- **Views:** `SetupView` draws `state.setup` (a `ChoiceStack`, whose choices VoiceOver reads as buttons, or a `PromptView` for the left/right picker) and answers with `chooseSetup(index)`; a swipe right (`SwipeBack`, a high-priority gesture, so it also wins over the picker's buttons) or the VoiceOver escape gesture calls the shared `back()`. Choices draw `Choice.text` and are labelled `Choice.label`, so VoiceOver says the words without the arrow. `MatchView` draws `MatchDisplay` and any `prompt` through the same `PromptView`, answering with `answerPrompt(index)`; while a prompt is open nothing under it takes taps or VoiceOver focus. All text comes from `UiText`, colours from `BoardColors` (one tone mapping, `ToneColors`), sizes from `BoardMetrics`, the remaining fixed details from `BoardStyle`.
- **VoiceOver:** each tap zone is a button labelled `UiText.pointFor(name)` ("Point for <team name>"), valued with its score and hinted with `SCORE_HINT`; activating it scores, and its actions offer undo and end match (no long press needed). The board reads as one element (the shared `MatchDisplay.spokenScore`: both scores, the pill, the games of each set), tagged `UiTags.MATCH_BOARD`. The undo button offers end match as an action too. A tap prompt is one button (its header the label, its hint the value); a left/right prompt reads its question, then its two answers.
- **Workout:** one HealthKit tennis workout (location type unknown) per match. `WorkoutManager` runs the lifecycle over a `WorkoutBackend` (`HealthKitWorkout` in the app, a fake in tests): starting the match starts an `HKWorkoutSession` with its `HKLiveWorkoutBuilder`; "End match" (or the auto summary) pauses it; reopening resumes the same session; leaving the summary for setup ends it and saves exactly one workout. At launch it asks for access, then `recoverActiveWorkoutSession` adopts a session left running or paused (`recover()` returns its `Phase`) and reconciles it with the saved match: kept or resumed when the match is live, ended and saved otherwise; a live match without one (or one whose recovery failed) starts a new one. Steps run one after another, so a quick start → end cannot race. A start whose data collection fails ends its session, so it cannot block the next start. Session failures and changes made outside the app arrive through `HKWorkoutSessionDelegate` (an outside end saves the workout); every error goes to `os.Logger`. It is write-only: it saves the workout and reads nothing. The running session also keeps the app in front when the display dims.
- **Scaling:** the `boardScale` environment value is the screen width / `BoardMetrics.APPLE_REFERENCE_WIDTH` (184 pt, 44 mm).
- **Haptics:** `HapticManager` plays one system haptic per `Pulse` at its `startsMs` (`.click` tap, `.directionUp` buzz, `.success` long; watchOS has no custom lengths). A new feedback drops what is left of the previous one. The device and the scheduling are injected, so tests check the pattern of every `Feedback`.
- **Concurrency:** Swift 6 language mode with no `nonisolated(unsafe)`, `assumeIsolated` or `@unchecked Sendable`. `PhoneLink` is not actor-bound (WatchConnectivity is thread-safe; the last scoreboard sits behind a lock): every delegate callback goes through `receive(payload)`, which hands the names to the main actor and answers with the latest scoreboard.

---

## Android phone companion (WP Padel Board)

- **BoardViewModel** opens the names store off the main thread, then holds the shared `PhoneBoard` (newest scoreboard) and `NamesBook` and publishes one immutable `BoardState` (`match`, `summary`, `showsNameEditor`, `names`; null until the names are read). It then listens to the watch over the `SyncChannel` (which delivers the stored board first) and sends the names on launch only when this phone has saved some, and after every `editNames()`. The link is registered with `addCloseable`.
- **BoardApp** holds the editor's open state and draws `BoardScreen`, which is stateless: the shared `MatchBoard` and summary parts from `android/common` with `LocalBoardScale` set from the screen height, or a waiting screen with the names before a match. Nothing on the match board is tappable, so TalkBack reads it as one element (`MatchDisplay.spokenScore`), like the iPhone. The pencil opens the `NameEditor`, which focuses its first field inside the dialog; each field carries its label as content description, so TalkBack names it once the placeholder is gone. Every button is at least 48 dp. `@Preview`s show the match, summary and waiting boards from `SampleBoards`.
- **MainActivity** is landscape only, keeps the screen on and hides the system bars. In debug builds the `sample_board` intent extra (`match` or `summary`) shows a `SampleBoards` board without the watch.

## iPhone companion (WP Padel Board)

- **ScoreboardModel** publishes the shared `PhoneBoard` (the scoreboard with the highest revision of the watch's current epoch; older, unreadable or other-version ones are ignored) and holds the `NamesModel` (shared `NamesBook`); `editNames()` saves an edit and sends it to the watch, and `sendSavedNames()` (when the link activates) sends only names typed on this phone, so a reinstalled phone never resets the watch to the defaults.
- **ScoreboardView** draws `PhoneBoard.match`, `.summary`, or the waiting screen with the names, and the pencil while `showsNameEditor`. It uses the shared `MatchBoard` and summary parts with `boardScale` from the screen height (`BoardMetrics.PHONE_*`), so the board is the watch screen scaled up and VoiceOver reads it as one element. The waiting screen (text styles) and the pencil (`@ScaledMetric`) follow Dynamic Type; the waiting screen scrolls when the text outgrows it. `NameEditor` cuts each name at `TeamNames.MAX_LENGTH` while typing, as the Android editor does, so Done never shortens what the field showed.
- **Landscape** is locked except while the name editor is open (`OrientationGate`, on the main actor). The idle timer is off, so the screen stays on courtside. The app is iPhone only.
- **UI tests:** `-UITesting` disables the watch link; `-UITestBoard match|summary` loads a sample from the shared `SampleBoards`. Both exist in Debug builds only.

## Screen flow (watches)

### Setup

```
Play mode → Format → Rule mode → Who serves → Which player (doubles only)
```

Each step is a full-screen stack of choices with a pill on each divider (`SetupView` on Wear OS, `ChoiceStack` on watchOS). The last step is a left/right picker (`PromptView` on both platforms). Back returns to the previous step: the system back on Wear OS, a swipe right on watchOS.

### Match

```
┌───────────────────────────┐
│  Opponent panel (tap)     │
├── set scores ── pill ─────┤  ← undo button on the right edge
│  You panel (tap)          │
└───────────────────────────┘
```

Prompts: the serve pick (doubles game 2), "CHANGE SIDES" (after every set but the last), and the position switch for each team (doubles).

The undo button: a tap undoes the last point; holding for 1.5 s (a ring fills) toggles it to a red X; tapping the X ends the match. On Wear OS one gesture coroutine decides: the hold times out → toggle, released before → undo, slid off → nothing; the ring is only animation. On watchOS a drag gesture does the same: a release outside the button's bounds only cancels the hold, so a press that slides off neither undoes nor ends.

### Summary

The result for both teams with per-set games, the winner pill ("MATCH ENDED" when stopped with level sets), Points, Games and Breaks, then Duration, Avg pts/game, Tiebreaks, Deuces, and Golden or Star won. Buttons: **UNDO LAST POINT** (or **RESUME MATCH** when stopped early) and **NEW MATCH**.

---

## Testing

| Suite | Location | What it covers |
|---|---|---|
| Engine | `shared/src/commonTest/.../engine/` (13 classes) | phases, tie-breaks, serve rotation, serve pick, position switch, pill priority, stats, refused points; `InvariantTest` plays seeded random matches in every format and checks the rules, statistics, pill and undo after every action |
| History and session | `shared/src/commonTest/.../match/` | stack semantics, screens, end/reopen, revision and epoch (fresh histories differ, kept through every change), undo across every boundary, feedback and its buzz patterns, save/restore round trip (a missing epoch is rejected, a file from before the live set carried `isCompleted` still reads), serve orders; MatchSession incl. setup, back, saving only on change, phone ordering across clock changes, and a reinstalled watch followed from its first board |
| Setup | `shared/src/commonTest/.../setup/` | every setup screen and the Config it builds |
| Display | `shared/src/commonTest/.../display/` | score texts, the diagonal layout through deuce, tie-breaks and new games, prompts (arrows drawn, words spoken), every summary statistic |
| Android common | `android/common/src/test/` | FileTextStore (save, replace, loads and saves that never wait for the disk, failed write, a new store reading queued writes), MemoryChannel behaving like the Data Layer (replay, pair, close), Subscriptions (no listener after close), SnapshotGate (a stored item after a live change is dropped) |
| Data Layer | `android/common/src/androidTest/` | DataLayerChannel on a real device: the stored item reaches a later listener, nothing after close, nothing to a listener added after close |
| Wear view model | `android/watch/src/test/` | WatchViewModel over MatchSession (nothing shown or played before the stores open, score, undo, prompts, restore, end/reopen/new match), the auto summary and its undo on a virtual clock, setup flow, back handling, phone mirroring with a growing revision (the epoch kept across a relaunch, new after a reinstall), names from the phone (also sent before launch), link closed on clear; Haptics (each feedback's waveform reaches the vibrator) |
| Wear UI | `android/watch/src/androidTest/` | Compose tests of setup, match (the board as one spoken element), prompts (also one without choices; the picker drawn with arrows and spoken as words), names, summary panels, the end-match hold on the test clock with an injected hold time (a press that slides off does nothing), the auto summary on a virtual clock, haptics, TalkBack (labels, score and hint on the zones, scoring, undo and ending through accessibility actions), back (swallowed in a match, leaving from the first step and the summary) |
| Android phone view model | `android/phone/src/test/` | BoardViewModel: nothing shown before the names open, scoreboards by revision, a reinstalled watch, garbage, samples, a board sent before launch, what to draw, name edits saved and sent, no names sent by a fresh install, link closed on clear |
| Android phone UI | `android/phone/src/androidTest/` | waiting screen, match board (also as one spoken element) and summary board, name editor (save, blank field, cancel, focus, length cap, labelled fields), 48 dp buttons, MainActivity with sample boards (landscape, screen on) |
| Model | `shared/src/commonTest/.../model/` | `SetScore.winner` for won, tie-break, unfinished and cut-short sets; a completed level set is refused |
| Platform | `shared/src/commonTest/.../platform/` | `currentTimeMs` is wall-clock milliseconds on every target |
| Sync | `shared/src/commonTest/.../sync/` | scoreboard per screen, JSON round trips (the live set always carries `isCompleted`), payload version (and the epoch required), ordering by revision within an epoch and a reinstalled watch taken, forward compatibility, name edits, the phone screen choice |
| watchOS glue | `apple/WPPadelTrackerTests/` | PromptView drawn over a tap prompt with and without choices; MatchStore over MatchSession (setup, back, score, undo, refused actions, prompts, restore, end/reopen/new match, phone mirroring, haptics played, the auto summary on an injected sleep, and the workout following the screen); WorkoutManager over a fake HealthKit (start, end → pause, reopen → resume, one save, relaunch recovery, a quick start → end, failed access, recovery, start and save, events from outside); HapticManager (pulse mapping and timing of every feedback); FileTextStore; NamesModel (incl. `isSaved`); PhoneLink over a fake session (activation, the names the phone stored applied on activation, each board as context and as a live message, no message out of reach until the phone returns, nothing before activation, a failed message reported, a phone payload answered with the latest board and its names applied on the main actor; the latest scoreboard kept offline); SwipeBack |
| watchOS UI | `apple/WPPadelTrackerUITests/` | XCUITests by accessibility id and value: setup and swipe-back (also over the picker's buttons), scoring, the board as one element, undo (a press that slides off the button does nothing), prompts (arrows drawn, words spoken; the zones out of reach under them), end-and-resume, the summary; long matches open on a `-UITestMatch` scenario instead of tapping there |
| iPhone glue | `apple/WPPadelBoardTests/` | ScoreboardModel over PhoneBoard (revision order, a reinstalled watch, payload version, screen choice), name edits sent to the watch, only saved names sent when the link comes up; WatchLink over a fake session (names as context, as a transfer replacing the queued ones and as a live message when reachable, nothing before activation, a failed message reported, a scoreboard by message or context, the board the watch stored shown on activation, the saved names on activation, offline); the Common `Color(argb:)` and tone colours |
| iPhone UI | `apple/WPPadelBoardUITests/` | waiting screen, name editor (save, cancel, the length cap while typing), match board as one element, summary, an accessibility audit (every element described), the waiting board growing with the text size |
| Build logic | `build-logic/src/test/` | `ReleaseKey.from` (a complete file or environment, the file winning, relative and absolute keystore paths, missing, a part left out or empty, each with the message naming the source); `AppConfig.parse` (the module's entry, the version code arithmetic, a missing key or module, the repository's own `app.json`) |
| Tools | `tool/test_*.py` | `app_config.py` (every validation rule, the generated xcconfig, stray values in the Xcode project, bare or quoted, the command line), `simulator.py` (the newest runtime, kinds and availability, an unpaired watch first), `xccov_floor.py` (the target's share, the floor, a missing target, the command line) |

Static checks: ktlint and detekt for all Kotlin (also the build scripts and build-logic), Android lint with warnings as errors, and SwiftLint (`.swiftlint.yml`) for all Swift, the `tool` script included. CI also runs R8 over both apps' release variant, which needs no signing key.

Commands are listed in the [README](README.md#testing).
