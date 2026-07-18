# SP-5 · GUI Testing Framework, Tier 1 — Design + Plan

**Date:** 2026-07-18 · **Base:** post-SP-4 polish round.
Combined spec+plan; design agreed in conversation (Tier 1 of the three-tier proposal).

## Goal

Headless interaction tests that drive the real widgets through the real event
dispatcher and hit-testing: find widgets by test tag, click/type on them, swap the
network for a fake, and assert on screens and sent messages. No GL, no sockets, no
coordinate guessing.

## Components

### graphics-library (the framework)

- **`testTag: String?`** on `GraphicsElement` — stable widget addressing (Compose-style).
- **Scene query** — read-only `GApplet.elementRoots` (backed by `GraphicsManager`), DFS
  walker, `findByTag`, `allElements`.
- **Headless text metrics** — `TextMetrics` interface (`width/height(fontName, sizePx,
  text)`); `GApplet.textMetrics: TextMetrics?` defaults to null = production path
  (FontRegistry + GL measurement). Tests set `EstimatingTextMetrics` (deterministic
  `0.6·size·length` width, `size` height), which makes `TextElement` fully constructible
  and layout computable without a sketch.
- **`GuiRobot`** — `clickOn(element|tag)` (PRESS+RELEASE synthesized at the element's
  real center, through `EventDispatcher`), `rightClickOn`, `type(text)`, `pressEnter`,
  `pump()` (drain the dispatcher). Shipped via Gradle `java-test-fixtures` so dependent
  modules reuse it (fallback if the plugin fights Kotlin/kapt: a `space.kiibou.test`
  package in main).

### minesweeper (adoption)

- **`GameConnection` seam** — tiny interface with the two `send` overloads; production
  adapter wraps the socket `Client`; `Minesweeper.client` becomes a `GameConnection`.
- **`Minesweeper.initHeadless(connection)`** (internal) — builds screens + message
  handlers + runs element init with the estimating metrics, skipping surface/GL/network.
- **`FakeGameConnection`** (test fixture) — records sent messages; `receive(type,
  payload)` injects an incoming message through the dispatcher.
- **Tags** on menu/lobby widgets (`menu.singleplayer`, `lobby.start`, `rooms.joinInput`, …).
- **GUI flow tests**: singleplayer click → CreateRoom+SetReady sent → RoomState received
  → lobby shown with owner controls; non-owner state hides Start; multiplayer → room
  list → clicking a listed room row sends JoinRoom; typed room number joins; name typed
  on the menu is sent before entering multiplayer.

## Constraints / notes

- Image-backed widgets (`Picture`, the game `Map`) may not construct headless
  (`loadImage`); Tier-1 tests cover menu/lobby/room flows. The map screen remains
  Tier-2 (xvfb) territory. Attempt `GameStarted` coverage; drop if images block it.
- All existing suites stay green; the xdotool journey scripts remain for Tier 2 until an
  in-process harness replaces them.

## Tasks

1. **Framework in graphics-library** (RED→GREEN): tags+query, metrics, robot; proved by
   framework tests: robot clicks a real `Button` (REKotlin `clicked` fires), robot
   focuses a `TextInput` by click and types into it end-to-end, `findByTag` resolves
   nested tags, `TextElement` lays out headless with estimating metrics.
   Commit `feat: add headless GUI testing framework (tags, scene query, robot)`.
2. **Seam + tags + flow tests in minesweeper** (RED→GREEN): `GameConnection`,
   `initHeadless`, widget tags, `FakeGameConnection`, the five flow tests above.
   Commit `feat: adopt GUI test framework for menu and lobby flows`.
3. **Docs**: WORKFLOW.md marks SP-5 Tier 1 complete; note Tier 2 as future work.
   Commit `docs: mark SP-5 GUI-testing tier 1 complete`.

## Success criteria

`gradle build` green; new GUI tests fail meaningfully when a widget stops responding
(they go through production dispatch, so regressions like the hierarchy-depth bug are
caught at this level); no GL or network in any new test.
