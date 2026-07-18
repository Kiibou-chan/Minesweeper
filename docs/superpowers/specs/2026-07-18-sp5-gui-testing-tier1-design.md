# SP-5 · GUI Testing Framework, Tier 1 — Design

**Status:** draft for review
**Date:** 2026-07-18
**Base:** post-SP-4 polish round (branch `claude/project-roadmap-brainstorm-d4466h`)

## Goal

Headless interaction tests that drive the real widgets through the real event dispatcher
and hit-testing: find widgets by test tag, click and type on them, swap the network for a
fake, and assert on screens and sent messages. No OpenGL, no sockets, no coordinate
guessing.

Motivation (from this session): three real bugs — the client connect-callback ordering,
hidden screens swallowing clicks, and the stale `hierarchyDepth` that silently ate all
board clicks — passed 48 unit tests and were only caught by fragile xdotool sweeps.
Tier 1 makes exactly that class of bug testable in-JVM: a robot click computes the
widget's *real* position and goes through *production* dispatch, so wrong hit-testing
fails the test instead of the player.

## Non-goals (Tier 2/3, later specs)

- Rendered/pixel assertions, screenshots, golden images.
- In-process windowed runs under xvfb (replacing the journey scripts).
- Multi-client GUI journeys (protocol already covered by `RoomFlowIntegrationTest`).
- Testing the game `Map` screen headless (its `Picture`s and image loading are untested
  territory headless; attempted opportunistically, dropped if images block it).

## Architecture

### graphics-library — the framework

1. **Widget addressing:** `var testTag: String? = null` on `GraphicsElement`
   (Compose-style). Tags are dot-namespaced by screen, e.g. `menu.singleplayer`,
   `lobby.start`, `rooms.joinInput`, `rooms.row.<id>`; screen roots get `screen.<name>`.
2. **Scene query:** read-only roots accessor (`GApplet.elementRoots`, backed by
   `GraphicsManager`), a DFS `walk()` over children, `findByTag`.
3. **Headless text metrics:** the one hard blocker today is that `TextElement` sizing
   calls `FontRegistry`/`createFont` + GL text measurement, which require a live sketch.
   Seam: `interface TextMetrics { width(fontName, sizePx, text); height(fontName,
   sizePx, text) }` with `GApplet.textMetrics: TextMetrics?`:
   - `null` (production default): exactly today's path — `FontRegistry` + GL
     measurement. Zero behavior change in the running app.
   - Tests set `EstimatingTextMetrics`: deterministic estimate
     (`width = 0.6 · size · length`, `height = size`), making text widgets constructible
     and layout computable headless. Real pixel-perfect sizing stays a Tier-2 concern.
4. **`GuiRobot(app)`:**
   - `clickOn(tag | element)` — synthesizes PRESS + RELEASE `processing.event.MouseEvent`s
     at the element's real center and pumps the dispatcher (`pre()`); `rightClickOn`
     likewise (flagging).
   - `type("text")`, `pressEnter()` — synthesizes TYPE key events to the focused element
     (focus acquired by a prior `clickOn`, exercising the real click-to-focus path).
   - `pump()` — drain queued events.
   - **Packaging:** Gradle `java-test-fixtures` on `graphics-library`, so dependent
     modules get the robot via `testImplementation(testFixtures(...))` without shipping
     it in the production jar. Fallback if the fixtures plugin fights Kotlin/kapt: a
     `space.kiibou.test` package in main sources (pragmatic, noted in docs).

### minesweeper — adoption

5. **Network seam:** `interface GameConnection` with the two `send` overloads the UI
   uses; `ClientGameConnection` wraps the socket `Client`; `Minesweeper.client` becomes
   a `GameConnection`. UI code is unchanged (same call shapes).
6. **Headless boot:** `Minesweeper.initHeadless(connection)` (internal) — sets the
   estimating metrics, injects the connection, builds screens + message handlers
   (extracted `initUi()` from `setup()`), and runs element init. No surface, GL, or
   socket touched.
7. **`FakeGameConnection`** (test double): records sent messages; incoming messages are
   injected through the existing `eventDispatcher.messageEvent(...)` and pumped — the
   production routing path.
8. **Tags** on menu/room-list/lobby widgets and screen roots.

### The flow tests this must enable (acceptance)

- Clicking **Singleplayer** sends `CreateRoom` + `SetReady(true)`; receiving
  `YourId`/`RoomState` shows the lobby with the right title; owner controls (presets,
  Start) visible iff I am the owner.
- Clicking **Multiplayer** sends `ListRooms`; receiving a `RoomList` renders row
  buttons; clicking a row sends `JoinRoom(handle)`.
- Clicking the join field, typing `7`, Enter sends `JoinRoom(GameHandle(7))`.
- Typing a name on the menu then entering multiplayer sends `SetName` before
  `ListRooms`.

## Scope addition discovered during design (needs approval)

**Empty `TextInput`s are effectively unclickable in the real app**: their width binds to
their text width, so with empty initial text they have ~zero clickable area. This
affects the *shipped* room-number field and name field, not just tests. Proposed fix in
this stage: a minimum width for `TextInput` (e.g. `max(text width, 60·scale)`), with a
regression test. Small, user-visible, and squarely in this stage's blast radius —
recommended to include.

## Testing strategy

Framework proves itself RED→GREEN in `graphics-library`: robot-click fires a real
`Button.clicked`; click-to-focus + typing edits a `TextInput` end-to-end through the
dispatcher; `findByTag` resolves nested tags; `TextElement` lays out headless under the
estimating metrics; empty `TextInput` is clickable (min-width). Then the minesweeper
flow tests above. All existing suites stay green; `gradle build` green.

## Risks / open points

- `java-test-fixtures` + Kotlin interplay (fallback named above).
- Headless screens all sit at (0,0) and overlap — intentionally exercised: the
  hidden/inactive dispatch filters from the polish round are what make that safe.
- `Button.focusable` is `active`-backed, so clicks focus buttons; harmless for these
  flows but noted for Tier 2 keyboard-navigation work.
