# WP Tracker

A padel match scoring app for smartwatches. Built with Kotlin Multiplatform, targeting **Wear OS** and **watchOS**.

---

## Overview

WP Tracker lets you track a live padel match from your wrist. Tap to score points, undo mistakes with a long-press, and follow the current serve side, score layout, and match status at a glance.

**Supported formats:**
- Singles and Doubles
- Best-of-3 or Best-of-5 sets
- Standard rules (deuce/advantage)
- Golden Point (one deciding point at deuce)
- Star Point (advantage ×2, then one deciding point)
- Tie-break scoring with automatic serve rotation

---

## Platforms

| Platform | Language | UI Toolkit | Min OS |
|---|---|---|---|
| Wear OS | Kotlin | Jetpack Compose | API 26 (Wear OS 2) |
| watchOS | Swift | SwiftUI | watchOS (current) |

---

## Architecture

```
wp-tracker/
├── code/
│   ├── shared/          # Kotlin Multiplatform — pure business logic
│   │   └── src/
│   │       ├── commonMain/   # MatchEngine, models, enums
│   │       └── commonTest/   # 16 test files, ~3,750 LOC
│   ├── wearos/          # Wear OS app (Kotlin + Compose)
│   └── watchos/         # watchOS app (Swift + SwiftUI)
```

### Shared module

All padel scoring logic lives in the `shared` module as pure Kotlin with no platform dependencies.

- **`MatchEngine`** — stateless object; `score(snapshot, team)` returns a new `Snapshot`
- **`Snapshot`** — immutable data class representing the complete match state at any point in time
- **`Config`** — match settings set once at start (rule mode, play mode, serve order, best-of)

The caller (each platform's ViewModel/Store) maintains a history stack of `Snapshot` objects. Undo is simply popping the last entry.

### Data flow

```
User tap → ViewModel/Store → MatchEngine.score() → new Snapshot → UI re-renders
                                                  ↑
                                        History stack (for undo)
```

### State model

| Class | Contents |
|---|---|
| `Config` | `bestOf`, `ruleMode`, `playMode`, `serveOrder` |
| `MatchState` | Sets won, set scores, match timestamps |
| `SetState` | Current set index and game scores |
| `GameState` | Points, game phase (Normal/Deuce/Advantage/Golden/Star), rally index |
| `ServeState` | Current server, serve side, order index |
| `StatsState` | Total points, breaks, deuces, golden deciders |

### Status pill priority

The UI shows a contextual pill label derived by `MatchEngine.computePill()`:

`MATCH POINT > SET POINT > BREAK POINT > GOLDEN POINT > STAR POINT > TIEBREAK > (hidden)`

---

## Building

### Prerequisites

- **Android / Wear OS:** Android Studio (latest stable), Android SDK 36
- **watchOS:** Xcode (latest stable), macOS

### Wear OS

1. Open `code/` in Android Studio.
2. Select the `wearos` run configuration.
3. Deploy to a Wear OS emulator or physical device.

To run shared module tests:

```bash
cd code
./gradlew :shared:commonTest
```

### watchOS

The watchOS app requires the compiled `Shared.xcframework`. Build it first from the `code/` directory:

```bash
cd code
./gradlew :shared:assembleSharedXCFramework
```

Then open `code/watchos/WPTrackerWatch.xcodeproj` in Xcode and run on a watchOS simulator or device.

---

## Key Dependencies

| Dependency | Version |
|---|---|
| Kotlin | 2.3.10 |
| Android Gradle Plugin | 9.1.0 |
| Compose BOM | 2026.02.01 |
| Wear Compose | 1.5.6 |
| Kotlinx Coroutines | 1.10.2 |

All version pinning is managed via `code/gradle/libs.versions.toml`.

---

## Testing

Tests live in `code/shared/src/commonTest/` and cover:

- Game phase transitions (Normal → Deuce → Advantage → Win)
- Serve rotation (singles, doubles, tie-break)
- Undo chain integrity
- Status pill detection and priority
- Match and set completion conditions
- Golden Point and Star Point deciding points
- Score position (diagonal alternation)
- Invariants (no impossible states)

Run all shared tests:

```bash
cd code
./gradlew :shared:commonTest
```

---

## App Version

Current version: **1.1.1** (`versionCode` = 20260313)
