# Superpowers Workflow (working method for this project)

This project follows the **Superpowers** methodology (`obra/superpowers`) for all
non-trivial work: design before code, decompose ruthlessly, and drive
implementation through test-first, bite-sized tasks. This file is the durable
reference so any session can pick the method back up.

Source skills (public): `https://raw.githubusercontent.com/obra/superpowers/main/skills/<name>/SKILL.md`
— the ones in use here are `brainstorming` and `writing-plans`.

## The core discipline

- **Hard gate:** do NOT write code, scaffold, or invoke an implementation step
  until a design has been **presented and approved by the user**. Applies to
  every change regardless of how simple it looks.
- **One question at a time** when brainstorming. Prefer multiple-choice.
- **Propose 2–3 approaches** with trade-offs and a recommendation before settling.
- **Present the design in sections**, scaled to complexity; get approval per section.
- **Decompose** anything spanning multiple independent subsystems into
  sub-projects. Each sub-project gets its own spec → plan → implementation cycle
  and must produce working, testable software on its own.
- **YAGNI / DRY** — cut unnecessary scope from every design.

## Phase 1 — brainstorming (idea → approved design)

1. Explore project context (files, docs, recent commits).
2. Ask clarifying questions one at a time (purpose, constraints, success criteria).
3. Propose 2–3 approaches with trade-offs + a recommendation.
4. Present the design in sections; get approval after each.
5. Write the spec to `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md` and commit.
6. Spec self-review: scan for placeholders/TODOs, internal contradictions,
   scope creep, and ambiguous requirements; fix inline.
7. Ask the user to review the committed spec before proceeding.
8. Terminal state: invoke **writing-plans**. Do not jump to any other skill.

## Phase 2 — writing-plans (design → task list)

- Write for an engineer with zero context and questionable taste: exact file
  paths, complete code in every step, exact commands with expected output.
- Map the **file structure** first (one clear responsibility per file; files that
  change together live together).
- **Task right-sizing:** a task is the smallest unit that carries its own test
  cycle and is worth a fresh reviewer's gate. Each task ends with an
  independently testable deliverable.
- **Bite-sized steps (2–5 min each)**, TDD RED→GREEN→REFACTOR:
  1. Write the failing test → 2. Run it, watch it fail → 3. Minimal code →
  4. Run it, watch it pass → 5. Commit.
- **No placeholders** ("TBD", "add error handling", "similar to Task N", or
  references to undefined types) — these are plan failures.
- Save the plan to `docs/superpowers/plans/YYYY-MM-DD-<feature>.md`.
- Self-review the plan against the spec: coverage, placeholder scan, type/name
  consistency across tasks.

## Phase 3 — execution

- Options: **subagent-driven-development** (fresh subagent per task, two-stage
  review — spec compliance then code quality) or **executing-plans** (batched
  with human checkpoints).
- **requesting-code-review** between tasks; critical issues block progress.
- **using-git-worktrees** for isolation on non-trivial work.

## Project conventions that interact with this workflow

- Commits **must not** carry an AI co-author trailer
  (`.claude/settings.json` sets `includeCoAuthoredBy: false`). Write messages as
  if authored solely by the human developer.
- Artifacts live under `docs/superpowers/{specs,plans}/`.

---

## Current roadmap state (as of this doc)

Newest work lives on branch **`guilib-reactive-kotlin`** (Kotlin 2.2.21, JVM
toolchain 24, Processing from Maven, vendored binaries removed). It carries two
in-flight efforts: a partial migration of the graphics-library event system onto
the sibling **REKotlin** reactive engine (`space.kiibou.reactive-kotlin`,
resolved from `mavenLocal` — the repo must be `publishToMavenLocal`'d to build),
and the groundwork for **shared-board multiplayer** (`GameState` now holds a
`handles` list and broadcasts; `GameHandle`/`JoinGame` added; every client joins
a hardcoded room `0`).

Decomposed into three sub-projects, each its own spec → plan → build:

- **SP-1 · Game-logic test foundation** *(COMPLETE — see plan doc; 8 tests green)* — loosen the
  `GameState`↔messaging seam, add `kotlin.test` coverage, fix known bugs under
  green tests. Decisions so far:
  - Base: branch off `guilib-reactive-kotlin`; publish cloned reactive-kotlin to
    `mavenLocal` as one-time setup so `minesweeper`/`graphics-library` build.
  - Seam: **Approach A** — `GameState` depends on a `GameEvents` output
    interface (one method per emitted event; prod impl broadcasts to `handles`,
    test impl records) plus injected `Random` and a `Ticker` abstraction
    replacing the live `fixedRateTimer`.
  - Bugs to fix under test: (1) non-square board — `possibleTilePositions` uses
    `it / height`, should be `it / width`; (2) lose-reveal `&&` skips a whole
    row+column of wrongly-flagged tiles; (3) no first-click safety; plus the
    multiplayer lifecycle bugs — `gameStates`/`users` never cleaned on
    disconnect (leak), and `getGameState` NPEs if a message arrives before
    `JoinGame`.
- **SP-2 · Finish the REKotlin event migration** — complete event/key/focus
  move; formally freeze the layout system on JavaFX (document the `Signal.toFX`
  boundary); delete dead JavaFX event code. Note: REKotlin is eager/push-based
  with no batch/transaction API, which is why it was too slow as a wholesale
  layout backend — keep it off the per-frame layout hot path.
- **SP-3 · Real lobby/rooms** — replace hardcoded `GameHandle(0)` with
  create/join/list rooms. Depends on SP-1's tested `GameState`.

SP-1 is implemented on `claude/project-roadmap-brainstorm-d4466h` (seam + all five
fixes, RED→GREEN per the plan).

SP-2/SP-3 brainstorm outcome (specs in `specs/2026-07-18-*`): staged lobby —
click-list rooms first, typed room codes later (SP-3b, needs SP-2's TextInput).
Rooms are ready-lobbies: owner sets settings all members see, members ready-up,
owner starts when all ready; lifecycle LOBBY → PLAYING → back to LOBBY on game
over (ready flags reset, join window reopens; no mid-game join, so no board
snapshot subsystem). Ownership passes to the longest-present member; empty rooms
dissolve. Sequencing: **SP-3 server (room lifecycle) → SP-2 (events/TextInput/
layout freeze) → SP-3 client (screen system + lobby UI) → SP-3b (typed codes)** —
screens build on the finished event system.

**SP-3 server is COMPLETE** (plan `plans/2026-07-18-sp3-server-room-lifecycle.md`,
all 5 tasks, 23 tests green, e2e-verified under xvfb): protocol types +
round-trip test, `Room` state machine, `RoomRegistry` (replaces `GameRegistry`),
`GameService` routing with `BroadcastRoomEvents`/`GameOverNotifying`, and a
temporary solo-compat shim (`JoinGame`/`InitMap`/`Restart` handlers marked for
deletion in the SP-3 client stage).

**SP-2 is COMPLETE** (plan `plans/2026-07-18-sp2-event-migration-completion.md`):
key codes mapped via public JavaFX API (no more `com.sun` internals), focus moves
on click instead of hover (`EventDispatcher.focused` exposed), `TextInput` widget
added (REKotlin `Var<String>` value, TYPE-based editing, Enter submit; visual
child deferred to `initImpl` so logic tests run headless), TestMain demo, and the
layout freeze documented in CLAUDE.md ("Reactive boundaries"). Keyboard editing is
verified at unit level (13 graphics-library tests); typing in a live window is a
manual check via TestMain.

**SP-3 client is COMPLETE** (plan `plans/2026-07-18-sp3-client-lobby-ui.md`): the
app boots into `RoomListScreen`, joins/creates rooms into `RoomLobbyScreen`
(member ready markers, owner-only presets + Start, Leave), and `GameStarted`
swaps in a per-game `Map` via the new `graphics-library` `ScreenManager`
(active-aware event dispatch, late-registration init). `YourId` tells clients
their member id. Message routing moved from `Map.initImpl` to `Minesweeper`
(registered once). The solo-compat shim is deleted; `JoinGame`/`InitMap` message
types remain defined but unrouted. Two connect-ordering bugs found by e2e and
fixed (client field assignment; `Client.connect` fired `onConnect` before the
connection was usable). Verified: 24 minesweeper + 16 graphics-library tests
green, incl. a two-client socket integration test of the full room flow, plus a
live xvfb run clicking Create Room into a lobby.

**SP-4 is COMPLETE** (plan `plans/2026-07-18-sp4-menu-names-typed-join.md`) —
this finished SP-3b and closed the original brainstormed roadmap:
- **Main menu** at boot: name `TextInput`, Singleplayer (create room +
  auto-ready → the lobby doubles as difficulty select), Multiplayer (room list,
  which gained Back-to-menu). Future entries (Leaderboard, Settings) are
  additive rows in the menu's button list.
- **Player names**: `SetName`/`NameInfo`; `MemberState` carries `name`; server
  keeps connection→name (cleared on disconnect, default "Player <id>", 24-char
  cap); `Room` resolves names via injected `nameOf`; renaming mid-room
  rebroadcasts state.
- **Typed room join (SP-3b)**: join-by-number `TextInput` on the room list.
- Verified: all suites green (integration test now asserts the chosen name
  appears in RoomState), plus a full mouse-driven xvfb journey: menu →
  Singleplayer → lobby → Start → board → reveal.

Future polish noted: auto-ready solo rooms after game over; leaderboard and
settings menu entries; win/lose overlay; chording; best-times persistence.

Build note: with full network access the project builds natively on the real
`jvmToolchain(24)` — REKotlin must be `publishToMavenLocal`'d first, and jogamp.org
resolves normally. JDK 24 can be fetched from `corretto.aws` (GitHub-release
downloads, including Adoptium and the Gradle wrapper dist, may be blocked; use
system Gradle at `/opt/gradle`). Point Gradle at the JDK with
`-Porg.gradle.java.installations.paths=/path/to/jdk24`. The full build, all SP-1
tests, graphics-library tests, and a real app boot (under `xvfb-run`) were
verified green this way.

If a restricted environment lacks JDK 24 or blocks jogamp.org, the fallback is to
lower the 6 module `jvmToolchain(24)` pins to an available JDK and exclude the
`org.jogamp.*` groups from `minesweeper`'s test runtime — kept local via
`git update-index --skip-worktree`, never committed. See Task 0 in the SP-1 plan.
