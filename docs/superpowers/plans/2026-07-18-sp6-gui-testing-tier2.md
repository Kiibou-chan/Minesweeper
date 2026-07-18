# SP-6 · GUI Testing, Tier 2 (Windowed E2E) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In-process windowed journeys (xvfb) driven by the Tier-1 robot: singleplayer
menu→lobby→board→reveal, and the Expert-resize regression. Opt-in via `-Dgui.e2e=true`.

**Architecture:** per the approved spec
`docs/superpowers/specs/2026-07-18-sp6-gui-testing-tier2-design.md`. Synthetic input
only; assertions structural + sparse pixel probes; screenshots on failure.

**Tech Stack:** Kotlin 2.2.21, JUnit4 (`Assume`, `TestWatcher`), Processing
`runSketch`, xvfb in this environment.

## Global Constraints

- No AI co-author trailer in commits.
- Default `gradle build` must not run Tier-2 tests and must stay green without a display.
- Tier-2 run command (this environment):
  `xvfb-run -a gradle :minesweeper:test -Dgui.e2e=true --tests "space.kiibou.e2e.*" -Porg.gradle.java.installations.paths=/opt/jdk24`
- All waits are condition-polls with timeouts (helper `awaitUntil`), never bare sleeps.

---

### Task 1: Enablers — exit safety, dispatcher idle, robot pump strategy, tags

**Files:**
- Modify: `graphics-library/src/main/kotlin/space/kiibou/GApplet.kt` — exit-safety hook:
  ```kotlin
  /** Set by test harnesses: exit() disposes the sketch instead of killing the JVM. */
  var suppressSystemExit: Boolean = false

  override fun exitActual() {
      if (!suppressSystemExit) super.exitActual()
  }
  ```
- Modify: `event/EventDispatcher.kt` — idle signal for frame-synced pumping:
  ```kotlin
  /** True when no queued events are waiting to be dispatched. */
  fun isIdle(): Boolean =
      synchronized(mouseQueue) { mouseQueue.isEmpty() } &&
          synchronized(keyQueue) { keyQueue.isEmpty() } &&
          synchronized(messageQueue) { messageQueue.isEmpty() }
  ```
- Modify: `testFixtures/.../GuiRobot.kt` — constructor gains
  `private val pumpStrategy: () -> Unit = { app.eventDispatcher.pre() }`; `pump()` calls
  it. Tier 1 callers are unchanged (default).
- Modify: `minesweeper/.../game/Tile.kt` — `init { testTag = "tile.$tileX.$tileY" }`.
- Modify: `minesweeper/.../game/Map.kt` — `init { testTag = "screen.map" }`.
- Modify: `minesweeper/.../lobby/RoomLobbyScreen.kt` — tag the preset buttons
  `lobby.preset.beginner|intermediate|expert`.
- Test: extend `graphics-library` `ScreenTest`/`GuiFrameworkTest` minimally:
  `isIdle` false after enqueue, true after `pre()`; robot honors a custom pump strategy
  (counter lambda).

- [ ] **1. Failing tests** for `isIdle` + pump strategy (compile RED).
- [ ] **2. Run** `:graphics-library:test` — RED.
- [ ] **3. Implement** all files above.
- [ ] **4. Run** `:graphics-library:test` and `:minesweeper:test` — GREEN (tags are
  inert additions).
- [ ] **5. Commit** `feat: add windowed-test enablers (exit safety, idle signal, tags)`.

### Task 2: WindowedAppHarness

**Files:**
- Create: `minesweeper/src/test/kotlin/space/kiibou/e2e/WindowedAppHarness.kt`
- Modify: `minesweeper/build.gradle.kts` — pass the flag into the test JVM:
  ```kotlin
  tasks.withType<Test>().configureEach {
      systemProperty("gui.e2e", System.getProperty("gui.e2e", "false"))
  }
  ```

**Harness contract:**

```kotlin
object WindowedAppHarness {
    lateinit var app: Minesweeper; lateinit var robot: GuiRobot

    fun assumeEnabled() = Assume.assumeTrue(System.getProperty("gui.e2e") == "true")

    fun boot() {                       // @BeforeClass in each journey
        assumeEnabled()
        if (!NetUtils.checkServerListening("localhost", 8454, 100)) main(arrayOf("--port=8454"))
        app = Minesweeper().also { it.suppressSystemExit = true }
        PApplet.runSketch(arrayOf("space.kiibou.Minesweeper"), app)
        awaitUntil("first frame drawn", 30_000) { app.frameCount > 0 }
        robot = GuiRobot(app) { awaitUntil("events drained", 5_000) { app.eventDispatcher.isIdle() } }
    }

    fun shutdown() { if (::app.isInitialized) app.exit() }   // @AfterClass; exit is suppressed → dispose

    fun awaitUntil(what: String, timeoutMs: Long, cond: () -> Boolean)  // poll every 20 ms, fail on timeout

    // draw-thread capture: registerMethod("draw", hook); volatile request → g.get()/save on sketch thread
    fun probe(x: Int, y: Int): Int
    fun screenshot(path: String)
}
```

Failure screenshots: a `TestWatcher` rule in the journey classes calling
`screenshot("build/reports/gui-e2e/<test>.png")` on `failed`.

- [ ] **1.** Write the harness + a trivial `BootJourneyTest` (boot → menu screen shown:
  `robot.findByTag("screen.menu").effectivelyHidden == false`).
- [ ] **2.** Run WITHOUT the flag → test skipped (assumption); run WITH
  `xvfb-run … -Dgui.e2e=true` → GREEN.
- [ ] **3. Commit** `feat: add in-process windowed app harness for GUI e2e tests`.

### Task 3: Journey 1 — singleplayer to reveal

**File:** `minesweeper/src/test/kotlin/space/kiibou/e2e/SingleplayerJourneyTest.kt`

Steps inside one `@Test` (class boots harness via `@BeforeClass`):
1. `clickOn("menu.singleplayer")` → awaitUntil lobby shown (`screen.lobby` visible).
2. `clickOn("lobby.start")` → awaitUntil map screen shown (`screen.map` visible; real
   socket round trip).
3. `clickOn("tile.4.4")` → awaitUntil `(findByTag("tile.4.4") as Tile).revealed`.
4. Pixel probe at the clicked tile's center differs from the flat background color —
   proves the board actually rendered.

- [ ] Write; run under xvfb with the flag; GREEN.
- [ ] **Commit** `test: add windowed singleplayer journey`.

### Task 4: Journey 2 — Expert resize regression

**File:** `minesweeper/src/test/kotlin/space/kiibou/e2e/ResizeJourneyTest.kt`

1. `clickOn("menu.singleplayer")` → lobby.
2. `clickOn("lobby.preset.expert")` → awaitUntil settings text/window state reflects
   30x16 (structural: lobby state; the resize happens after start).
3. `clickOn("lobby.start")` → map shown; awaitUntil `app.width` grew to fit the board.
4. `clickOn("tile.29.15")` (far corner, only correct if layout/resize are consistent)
   → awaitUntil revealed. This is the exact scenario that broke the xdotool run.

- [ ] Write; run under xvfb with the flag; GREEN.
- [ ] **Commit** `test: add expert-resize journey guarding board clicks after resize`.

### Task 5: Verification + docs

- [ ] Default `gradle build` (no flag): Tier-2 tests skipped, everything green.
- [ ] Flagged xvfb run: both journeys + boot test green, twice in a row (flake check).
- [ ] Delete the scratchpad xdotool scripts mention from WORKFLOW (superseded);
  document the run command; mark SP-6 complete.
- [ ] **Commit** `docs: mark SP-6 GUI-testing tier 2 complete`.

## Self-Review

- **Spec coverage:** harness pieces 1–6 → Tasks 1–2; journeys → Tasks 3–4; gating →
  Tasks 1–2 (`Assume` + gradle property); probes/screenshots → Task 2; script
  retirement → Task 5. Exit-safety hook is the reviewed production touch (Task 1).
- **Placeholder scan:** none; concrete signals (`frameCount > 0`, `isIdle`), exact tags,
  exact commands.
- **Type consistency:** `suppressSystemExit`, `isIdle`, pump-strategy parameter, and
  tag names match across tasks.
- **Risk noted:** if a second sketch per JVM proves fragile, add
  `forkEvery = 1` for the e2e package (Task 5 fallback).

## Execution Handoff

Inline (recommended; checkpoints after Task 2 and Task 5) or subagent-driven.
