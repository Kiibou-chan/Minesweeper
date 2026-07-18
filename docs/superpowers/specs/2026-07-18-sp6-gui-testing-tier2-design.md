# SP-6 · GUI Testing, Tier 2 (Windowed E2E) — Design

**Status:** draft for review
**Date:** 2026-07-18
**Base:** SP-5 Tier 1 complete (headless robot, tags, `GameConnection` seam).

## Goal

Run the *real* app — real OpenGL window (under xvfb in this environment), real fonts and
images, real sketch lifecycle, real sockets — inside the test JVM, and drive it with the
same `GuiRobot` and tags as Tier 1. This replaces the blind xdotool sweep scripts with
deterministic in-process journeys, and finally covers the game `Map` screen (images/GL)
that Tier 1 cannot construct.

Decisions from review: **synthetic input** (events injected at the dispatcher; the
native NEWT translation layer is explicitly out of scope) and **opt-in execution**
(skipped unless `-Dgui.e2e=true`; `gradle build` stays fast and display-independent).

## Non-goals

- Native cursor input (`java.awt.Robot`) — revisit only if the translation layer ever
  produces a real bug.
- Golden-image comparisons (rot across GL drivers). Pixel *probes* only, plus
  screenshots as failure artifacts.
- Two-windowed-clients GUI journeys (protocol covered by `RoomFlowIntegrationTest`,
  per-client UI by Tier 1). The Tier-2 journey plays solo.
- CI pipeline setup (no CI exists in this repo yet).

## Architecture

### Harness (`minesweeper` test sources; promotable to fixtures later)

`WindowedAppHarness` — JUnit helper that owns one app instance per test class:

1. **Server first:** boot the in-process server on 8454 (`main(arrayOf("--port=8454"))`)
   so the sketch's connect finds it and no subprocess is spawned. Skip if already
   listening (port reuse across classes).
2. **Sketch boot:** start the real `Minesweeper` via `PApplet.runSketch` on its own
   thread; await readiness by polling for "setup finished" state (screens initialized
   and first frame drawn — exposed as a countdown/flag, e.g. `frameCount > 0`).
3. **Exit safety:** `PApplet.exit()` calls `System.exit` — lethal to the test JVM (and
   the app calls `exit()` on server disconnect). The harness must neutralize it: a
   test-mode flag consulted by an overridden `exitActual()` (small production-code hook
   in `Minesweeper` or `GApplet`), so teardown disposes the surface instead of killing
   the JVM.
4. **Teardown:** `@AfterClass` — dispose sketch, stop awaiting threads. The in-process
   server thread is non-daemon; rely on Gradle's test-worker exit (as
   `RoomFlowIntegrationTest` already does).
5. **Frame-synced robot:** Tier 1's `pump()` calls `pre()` directly; in a live sketch
   the animator thread owns the frame loop, so the harness instead *waits* for the
   dispatcher's queues to drain (poll with timeout) after enqueuing events —
   `awaitFrame()` — to avoid racing the render thread.
6. **Gating:** every Tier-2 test class starts with
   `Assume.assumeTrue(System.getProperty("gui.e2e") == "true")`.

### Assertions & artifacts

- **Pixel probe:** `probe(x, y): Int` reading from the sketch's pixel buffer
  (`loadPixels`/`get`) — used sparingly (e.g. "board region is not background color
  after reveal").
- **Screenshot on failure:** a JUnit rule saving `saveFrame`-style PNGs to
  `build/reports/gui-e2e/` for any failed test.
- Primary assertions remain *structural* via the scene graph (same as Tier 1): screen
  shown, tile revealed state, network log not required.

### The journeys (acceptance)

1. **Singleplayer journey** (the xdotool `journey5` reborn, deterministic): boot →
   menu visible → `clickOn("menu.singleplayer")` → lobby → `clickOn("lobby.start")` →
   map screen shown → click a real `Tile` → its `revealed` flips true (server round
   trip through real sockets) → pixel probe confirms the board rendered.
2. **Resize journey:** pick the Expert preset in the lobby before Start; the window
   resizes for the 30x16 board and the board still responds to clicks — this is the
   exact scenario that broke the xdotool run (stale window handle) and it becomes a
   real regression test in-process.

### Script retirement

The scratchpad xdotool scripts are superseded; the boot-only smoke (app starts, no
exceptions) is kept as part of journey 1's setup phase. Nothing xdotool remains in the
verification story.

## Risks / open points

- `runSketch` re-entry: Processing statics may make a *second* sketch in the same JVM
  fragile — mitigated by one-sketch-per-class and, if needed, one Gradle-forked JVM per
  Tier-2 class (`forkEvery`).
- Readiness/teardown timing under xvfb is the flakiest part; all waits are
  condition-polls with generous timeouts, never sleeps.
- The exit-safety hook touches production code (small, flag-guarded); called out here
  so it is a reviewed decision, not a surprise.
