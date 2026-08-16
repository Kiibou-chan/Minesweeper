# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commit convention

Commits in this repo must **not** include the Claude/AI co-author trailer or generation banner
(`includeCoAuthoredBy` is set to `false` in `.claude/settings.json`). Write commit messages as if
authored solely by the human developer.

## Project

A multiplayer Minesweeper built on a bespoke Kotlin GUI framework rendered via Processing/JOGL
(OpenGL), talking to a bespoke TCP game server over a custom message-routing protocol. There is no
README; this file is the source of truth for orientation.

Gradle multi-module build, Kotlin 2.2.21, JVM toolchain 24 (root `build.gradle.kts`), though
individual modules pin `sourceCompatibility`/`targetCompatibility` to Java 17 — be aware of this
mismatch if touching build config. Gradle wrapper is 8.14.3, never below: 8.10.2 cannot read Java 24
bytecode, so its detector calls every test-source class a test (23 phantom `InvalidTestClassError`s).

Modules (`settings.gradle.kts`), in dependency order:

- **`annotations`** — marker annotations only: `@AutoLoad` (source-retention, marks a `Service` for
  auto-registration) and `@Inject` (runtime-retention field injection), plus the `ServiceLoadInfo`
  serializable DTO.
- **`annotation-processor`** — a `kapt`/`AutoService` `javax.annotation.processing.Processor`
  (`AutoLoadProcessor`) that scans for `@AutoLoad`-annotated classes and writes/merges their
  qualified names into a `META-INF/server/services/Services.json` resource at compile time. It reads
  any pre-existing file first and merges, so this works additively across modules.
- **`graphics-library`** — the custom GUI/graphics layer. `GApplet` extends Processing's `PApplet`;
  `GraphicsElement` is the scene-graph node base class (own event dispatch, hierarchy, JavaFX
  `SimpleIntegerProperty`/`SimpleListProperty` used for reactive-ish state such as scale cascading to
  children). `EventDispatcher` queues Processing mouse/key/touch callbacks and a message queue, then
  drains them once per frame via `pre()`. Depends on `server` (marked `TODO: remove` in the build
  file — a known layering issue). Also depends on an external `space.kiibou.reactive-kotlin:REKotlin`
  artifact resolved from `mavenLocal()` (a sibling project published locally, not in this repo) — the
  current branch (`guilib-reactive-kotlin`) is mid-migration of the event system
  (`event/Event.kt`, `EventListener.kt`, `EventModifier.kt`, `KeyEvent.kt`) toward it.
- **`server`** — generic (game-agnostic) networking: `Server` accepts raw sockets
  (`SocketConnection`), `ServiceLoader` reads the merged `Services.json` off the classpath at
  startup, reflectively instantiates each `Service(server)`, then does field injection for
  `@Inject`-annotated fields by matching the field's declared type against other loaded services
  (see `ServiceLoader.injectServices`) — this is the whole DI mechanism, there is no external DI
  framework. `MessageService` and `RoutingService` are themselves `@AutoLoad` services providing
  send/broadcast and per-`MessageType` callback routing respectively. `Serial` is a process-wide
  mutable `kotlinx.serialization` `Json` (polymorphic, `classDiscriminator = "#class"`) that modules
  extend at class-init time via `Serial.addModule(...)` — this must happen before any (de)serialization
  touches those types.
- **`minesweeper`** — the actual game. Client (`Minesweeper.kt`, `@file:JvmName("MinesweeperMain")`)
  auto-spawns the server as a **subprocess** on startup if nothing is listening on `localhost:8454`
  (`NetUtils.checkServerListening` + `Server.kt`'s `startServer()`, which relaunches the current JVM
  classpath via `ProcessBuilder`). Server-side game logic lives in `server/GameService.kt`
  (`@AutoLoad`, injects `RoutingService`/`MessageService`, keeps per-`GameHandle` `GameState`).
  Client-side rendering (`game/Map.kt`, `Tile.kt`, `ControlBar.kt`, `SmileyStatus.kt`, etc.) is built
  from `graphics-library` `GraphicsElement`s.

## Build / run / test

```
./gradlew build              # build everything
./gradlew :minesweeper:test  # tests for one module
./gradlew test --tests "space.kiibou.GridTest"   # single test class
```

- `./gradlew :minesweeper:run` / `:server:run --args="--port=8454"` work (`application.mainClass` is
  set to `space.kiibou.MinesweeperMain` / `space.kiibou.net.server.ServerKt` respectively), as does
  the installed script from `:minesweeper:installDist` / `:server:installDist`.
- `graphics-library` and `minesweeper` pull JOGL/native windowing deps from
  `https://jogamp.org/deployment/maven` and need `mavenLocal()` for the `REKotlin` dependency —
  ensure that repo has been `publishToMavenLocal`'d if graphics-library fails to resolve.
- `graphics-library`'s test task adds `--add-exports` JVM args for `jogl.all` module internals
  (see its `build.gradle.kts`); this is required for tests that touch JOGL/OpenGL internals to run
  at all under the module system.
- Tests use `kotlin.test` + JUnit4 (`kotlin("test-junit")`), not JUnit5/Kotest.

## Reactive boundaries (settled — do not re-litigate)

`graphics-library` deliberately uses **two** reactive systems with a fixed boundary:

- **REKotlin** (`space.kiibou.reactive`) drives **events and derived state**: the
  mouse/key event system, `Evt`/`Var`/`Signal` state such as `TextInput.value`, and
  anything discrete or low-frequency.
- **JavaFX properties/bindings** drive **layout**: `xProp`/`widthProp`/`scaleProperty`
  and all container binding math (`BorderBox`, `Grid`, `VerticalList`, `TextElement`
  sizing). This was migrated to REKotlin once and reverted: REKotlin propagates eagerly
  (every `Var.set` re-propagates its whole downstream cone immediately, with no
  batch/transaction API), which is far too slow for per-frame layout graphs. JavaFX
  bindings are lazy — invalidate now, recompute on read — which fits layout exactly.
- The sanctioned bridge is **`Signal.toFX`** (`data/Utils.kt`): REKotlin values feed
  JavaFX-bound consumers, never the other way around.

Moving layout onto REKotlin would first require a batching/transaction API in the
REKotlin project itself (one propagation per frame, not per set). Until that exists,
keep new reactive layout code on JavaFX and new event/state code on REKotlin.
