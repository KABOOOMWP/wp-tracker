# Architecture

## Overview

WP Tracker is a cross-platform padel match scorer for smartwatches. The scoring engine is written once in Kotlin and shared across both platforms via Kotlin Multiplatform (KMP). Each platform uses its native UI toolkit: Jetpack Compose on Wear OS, SwiftUI on watchOS.

```
┌─────────────────────────────────────────────────────────────┐
│                        WP Tracker                           │
│                                                             │
│  ┌──────────────────┐          ┌──────────────────────┐    │
│  │   Wear OS App    │          │    watchOS App        │    │
│  │  Kotlin/Compose  │          │    Swift/SwiftUI      │    │
│  │                  │          │                       │    │
│  │  MatchViewModel  │          │    MatchStore         │    │
│  └────────┬─────────┘          └──────────┬────────────┘    │
│           │                               │                 │
│           └──────────────┬────────────────┘                 │
│                          │                                  │
│              ┌───────────▼──────────────┐                   │
│              │     shared (KMP)         │                   │
│              │                          │                   │
│              │  MatchEngine  (object)   │                   │
│              │  Snapshot     (model)    │                   │
│              │  Config       (model)    │                   │
│              │  Enums        (types)    │                   │
│              └──────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
code/
├── shared/                          # KMP module — zero platform deps
│   └── src/
│       ├── commonMain/kotlin/com/wptracker/
│       │   ├── engine/
│       │   │   └── MatchEngine.kt   # Pure scoring engine
│       │   └── model/
│       │       ├── Enums.kt         # All domain enums
│       │       └── Snapshot.kt      # All immutable state classes
│       └── commonTest/kotlin/       # 16 test files
│
├── wearos/                          # Wear OS application module
│   └── src/main/kotlin/com/wptracker/
│       ├── haptic/
│       │   └── HapticManager.kt
│       └── presentation/
│           ├── MainActivity.kt
│           ├── match/
│           │   ├── MatchScreen.kt
│           │   └── MatchViewModel.kt
│           ├── setup/
│           │   └── SetupScreen.kt
│           ├── summary/
│           │   └── SummaryScreen.kt
│           └── theme/
│               └── Theme.kt
│
└── watchos/WPTrackerWatch/          # watchOS application
    ├── AppModel.swift               # MatchStore + routing enum
    ├── WPTrackerWatchApp.swift      # App entry point
    ├── Haptics/
    │   └── HapticManager.swift
    └── Views/
        ├── Match/MatchView.swift
        ├── Setup/SetupView.swift
        └── Summary/SummaryView.swift
```

---

## Shared Module

### MatchEngine

`MatchEngine` is a Kotlin `object` — effectively a stateless singleton with no mutable fields. Its only public contract is:

```
score(snapshot: Snapshot, team: Team) → Snapshot
computePill(snapshot: Snapshot) → PillState
computeScoreLayout(snapshot: Snapshot) → ScoreLayout
setDeciderSide(snapshot: Snapshot, side: ServeSide) → Snapshot
startMatch(config: Config) → Snapshot
```

Every function is a pure transformation: given the same inputs it always returns the same output, with no side effects. This makes the entire scoring logic trivially testable without any mocking or platform setup.

### State model

All state is captured in a single `Snapshot` data class. Every scored point produces a new `Snapshot`; the previous one is never mutated.

```
Snapshot
├── config: Config           ← set once, never changes
│   ├── bestOf: Int          (3 or 5)
│   ├── ruleMode: RuleMode   (STANDARD | GOLDEN | STAR)
│   ├── playMode: PlayMode   (SINGLES | DOUBLES)
│   └── serveOrder: List<Player>
│
├── match: MatchState
│   ├── setsWonYou / setsWonOpp
│   ├── setScores: List<SetScore>
│   ├── startedAt: Long
│   └── endedAt: Long?
│
├── set: SetState
│   ├── currentSetIndex
│   ├── youGames / oppGames
│
├── game: GameState
│   ├── mode: GameMode       (REGULAR | TIEBREAK)
│   ├── youPoints / oppPoints
│   ├── phase: GamePhase
│   ├── starAdvCount: Int    (Star mode deuce counter, 0..2)
│   └── deciderReceiveSideOverride: ServeSide?
│
├── serve: ServeState
│   ├── serverTeam / serverPlayer
│   ├── serveSide: ServeSide
│   ├── serveOrderIndex: Int
│   └── opponentServerConfirmed: Boolean
│
├── stats: StatsState
│   ├── totalPlayedPoints / pointsWonYou
│   ├── breaksYou / breaksOpp
│   ├── deuceCount
│   └── goldenDecidersPlayed / goldenDecidersWonYou
│
├── isMatchOver: Boolean
└── awaitingServePick: Boolean
```

### Game phases

```
NORMAL ──(reach 40:40)──► DEUCE
                              │
              STANDARD mode ◄─┤─► GOLDEN mode
              │                         │
              ▼                         ▼
         ADV_YOU/OPP               GOLDEN (deciding point)
              │
              ▼
           STAR mode
              │
       ┌──────┴──────┐
       ▼             ▼
  STAR_ADV_YOU  STAR_ADV_OPP
       │             │
       └──(2nd deuce)┘
              │
              ▼
          STAR_POINT (deciding point)
```

### Status pill priority

`computePill()` evaluates conditions in this fixed order, returning the first match:

```
MATCH_POINT  →  SET_POINT  →  BREAK_POINT  →  GOLDEN_POINT  →  STAR_POINT  →  TIEBREAK  →  HIDDEN
```

### Serve rotation

- **Singles:** alternates between A1 and B1 every game.
- **Doubles:** cycles through `serveOrder` [A1 → B1 → A2 → B2] every game. In game 2 the opponent server is unknown until the user picks — `awaitingServePick` gates scoring until confirmed.
- **Tie-break:** server changes after the first point, then every 2 points. The starting server is derived from `serveOrderIndex` at tie-break entry.

### Score layout (diagonal alternation)

Point scores alternate corners each rally to help players glance at the correct number without confusion. The rule is deterministic:

```
rallyIndex = youPoints + oppPoints   (resets to 0 on new game)

even rallyIndex → You: bottom-right,  Opp: top-left
odd  rallyIndex → You: bottom-left,   Opp: top-right
```

Applies to regular games, deuce/advantage states, and tie-break.

---

## Cross-platform Bridge

The `shared` module compiles to an `XCFramework` for watchOS consumption.

```
./gradlew :shared:assembleSharedXCFramework
```

This produces `code/shared/build/XCFrameworks/release/Shared.xcframework` targeting:

- `watchos-arm64_arm64_32` (physical device)
- `watchos-arm64_x86_64-simulator` (simulator)

The Swift code imports it as `import Shared` and calls `MatchEngine.shared.score(snapshot:team:)` etc. directly, with no bridging layer needed.

---

## Wear OS Module

### Navigation

`MainActivity` is the single activity. Navigation is handled as a simple sealed state variable — no navigation library is used.

```
Screen.Setup  ──(onStart)──►  Screen.Match  ──(onMatchEnd)──►  Screen.Summary
     ▲                                                               │
     └──────────────────────(onNewMatch)────────────────────────────┘
```

### MatchViewModel

`MatchViewModel` is the only ViewModel. It holds a `mutableStateListOf<Snapshot>` as the history stack. All engine calls are synchronous and cheap, so no coroutine dispatch is needed for scoring — coroutines are only used for the long-press ring animation timer.

```
history: [Snapshot₀, Snapshot₁, Snapshot₂, …]
                                          ▲
                                       current
```

- `score(team)` → appends `MatchEngine.score(current, team)`
- `undo()` → removes last element
- Everything else delegates to `MatchEngine` helpers and replaces `history.last()`

### Composition locals

Two `CompositionLocal` values are provided at the root in `MainActivity` and consumed throughout the tree:

| Local | Type | Purpose |
|---|---|---|
| `LocalWatchScale` | `Float` | Scale factor relative to 192 dp reference width |
| `LocalIsRoundScreen` | `Boolean` | Increases horizontal inset on round displays |

### Screen scaling

All layout constants in `ML` (MatchScreen) and inline values in other screens are multiplied by `scale`. The reference size is 192 dp (common Wear OS square). Round screens add extra horizontal inset to avoid content clipping at the curved edges.

### Haptic feedback

`HapticManager` wraps Android's `VibrationEffect` API. It is created once in `MainActivity` and passed down as a parameter — no dependency injection framework.

| Event | Pattern |
|---|---|
| Point (you) | 1 × 80 ms |
| Point (opp) | 2 × 80 ms (100 ms gap) |
| Game won | 1 × 500 ms |
| Undo | 3 × 80 ms |

### Ambient mode

`AmbientLifecycleObserver` is registered in `MainActivity` to keep the activity alive when the watch display dims. This prevents the match state being lost mid-game.

---

## watchOS Module

### State management

`MatchStore` is a `@MainActor ObservableObject` that mirrors `MatchViewModel`. It holds the same history-stack pattern and delegates all logic to the compiled KMP engine.

`WKExtendedRuntimeSession` is started when a match begins and invalidated when it ends or the app is dismissed. This prevents watchOS from suspending the app mid-match.

### Navigation

`RootView` reads `MatchStore.screen: AppScreen` and switches between views:

```
AppScreen.setup  ──►  AppScreen.match  ──►  AppScreen.summary
     ▲                                              │
     └─────────────────(newMatch())────────────────┘
```

### Screen scaling

`WatchScaleKey` is an `EnvironmentKey` providing a `Float` scale factor derived from the device screen width relative to a 184 pt reference (44 mm watch). All layout constants are multiplied by this value at instantiation, equivalent to the Wear OS `LocalWatchScale` approach.

### Haptic feedback

`HapticManager` (Swift) uses `WKHapticType` via `WKInterfaceDevice.current().play(_:)`.

| Event | Pattern |
|---|---|
| Point (you) | 1 × `.directionUp` |
| Point (opp) | 2 × `.directionUp` (180 ms apart) |
| Game won | 1 × `.success` |
| Undo | 3 × `.directionUp` |

---

## Screen Flow (both platforms)

### Setup — 5 steps

```
Step 1: Play Mode      →  SINGLES / DOUBLES
Step 2: Match Format   →  Best of 3 / Best of 5
Step 3: Rule Mode      →  Standard / Golden Point / Star Point
Step 4: Who Serves     →  YOU / OPPONENT
Step 5: Which Player   →  LEFT / RIGHT  (doubles only)
```

Each step uses a full-screen split with left/right (or top/bottom) tap zones. Progress is shown in a status pill. Tapping back cycles through previous steps.

### Match

```
┌───────────────────────────┐
│  [Opponent score — tap]   │  ← top half scores for OPP
├──── set scores ── pill ───┤  ← mid row
│  [Your score — tap]       │  ← bottom half scores for YOU
│                      [⟲]  │  ← undo button (right edge)
└───────────────────────────┘
```

The undo button has two states:
- **Arrow:** tap = undo last point; long-press (1.5 s, shown via ring progress) = switch to X
- **X:** tap = end match immediately

Overlays appear on top of the match UI when needed:
- **Serve-pick overlay** — doubles game 2, asks which opponent player serves
- **Decider-side picker** — Golden/Star Point 40:40, asks receiving team which side

### Summary

Scrollable screen showing:
- Opponent panel and You panel with per-set scores and total sets won
- YOU WIN / OPPONENT WINS pill
- Split stats: Points, Games, Breaks
- Aggregate stats: Duration, Avg pts/game, Tiebreaks, Deuces, Golden/Star deciders
- NEW MATCH button → returns to Setup

---

## Testing

All tests target the `shared` module exclusively via `kotlin.test`. Platform UI layers are not tested.

| File | What it covers |
|---|---|
| `MatchCompletionTest` | Match end conditions (2-0, 2-1, 3-2) |
| `TieBreakTest` | Tie-break scoring and serve rotation |
| `ServeRotationTest` | Serve order through full matches |
| `ServePickTest` | Doubles game 2 server selection |
| `DeciderSideTest` | Golden/Star Point receiver side |
| `GamePhaseTransitionTest` | Normal → Deuce → Advantage → Win |
| `PillDetectionTest` | Pill priority order |
| `PillDetectionEdgeCaseTest` | Pill edge cases |
| `InvariantTest` | No impossible states in any snapshot |
| `UndoTest` | Full undo history chain |
| `UndoInvariantsTest` | Undo combined with edge cases |
| `StatsTest` | Statistics accuracy |
| `ServeRotationStressTest` | Serve sequences under stress |
| `DiagonalPositionTest` | Score position alternation |
| `TieBreakStartTest` | Tie-break initiation conditions |
| `ScoreTransitionTest` | Phase transition correctness |

Run:

```bash
cd code && ./gradlew :shared:commonTest
```
