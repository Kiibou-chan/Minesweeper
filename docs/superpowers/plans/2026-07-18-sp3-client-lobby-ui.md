# SP-3 (client) · Screen System + Lobby UI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The client boots into a room list, players gather in a ready-lobby, the owner
starts the game, and the board screen appears — the compat shim dies. Multiplayer is
proven by an in-process two-client integration test.

**Architecture:** `graphics-library` gains a minimal `ScreenManager` (screens are
registered `GraphicsElement`s toggled via `hide/show` + `activate/deactivate`; the
dispatcher learns to ignore inactive elements). The `minesweeper` client gets
`RoomListScreen` and `RoomLobbyScreen`; all message routing moves from `Map.initImpl`
into `Minesweeper` (registered once, forwarding to the current screen/map). A new
`YourId` message tells each client its member id so the lobby can render "(you)"/owner
controls.

**Tech Stack:** unchanged (Kotlin 2.2.21, REKotlin, JavaFX bindings, kotlin.test/JUnit4).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-18-sp3-server-room-lifecycle-design.md`
  (client half) + brainstorm decisions in `WORKFLOW.md`.
- No AI co-author trailer. Test commands as before with `/opt/jdk24`.
- The app must build at every commit; the compat shim is removed only in the same commit
  that makes the client speak the room protocol.
- Click-only UI (no typing) — `TextInput`/room codes are SP-3b.

---

### Task 1: graphics-library screen enablers

**Files:** Modify `event/EventDispatcher.kt`, `util/GraphicsManager.kt`;
Create `gui/ScreenManager.kt`; Tests: `ScreenTest.kt`, extend `FocusTest.kt`.

- Dispatcher: `topElement` candidates and focus candidates must skip elements with
  `active == false` (hidden screens must not swallow clicks or take focus).
- `GraphicsManager`: remember when the one-shot `pre()` init has run; `registerGraphicsElement`
  after that point calls `element.init()` immediately (late-registered screens/maps get
  their `initImpl`).
- `ScreenManager(app)`: `fun add(screen)` (registers + starts hidden/inactive),
  `fun show(screen)` (hide+deactivate current, show+activate target), `val current`.
- [ ] Failing tests: inactive element under the cursor is skipped in favor of an active
  one below it / receives no focus; `ScreenManager.show` flips `hidden`/`active` flags of
  both screens; late registration triggers `initImpl` (counter in a test element).
- [ ] Implement → GREEN → commit `feat: add screen manager and active-aware event dispatch`.

### Task 2: YourId protocol + server emission

**Files:** Modify `common/Data.kt`, `server/RoomEvents.kt`, `server/Room.kt`,
`server/GameService.kt` (`BroadcastRoomEvents`); extend `MessageRoundTripTest`, `RoomTest`,
`RecordingRoomEvents`.

- `@Serializable data class YourIdInfo(val id: Long)`; `YourId : MessageType<YourIdInfo>`
  registered in the module.
- `RoomEvents.memberId(to: ConnectionHandle, id: Long)`; `Room.join` emits it on success
  (id = `handle.handle`); production impl sends `YourId`.
- [ ] Failing tests: round-trip entry; `RoomTest` asserts a successful join emits the
  joiner's id and a refused join does not.
- [ ] Implement → GREEN → commit `feat: tell joining clients their member id`.

### Task 3: client lobby screens + protocol switch (shim removal)

**Files:** Create `minesweeper/.../lobby/RoomListScreen.kt`, `lobby/RoomLobbyScreen.kt`;
Modify `Minesweeper.kt`, `game/Map.kt`, `game/ControlBar.kt`, `server/GameService.kt`
(delete shim).

- `Map` loses all `initImpl` networking; constructor takes `MapInfo`; message handlers
  move to `Minesweeper`, registered once, forwarding to the current `Map?`.
- `RoomListScreen`: title, Create Room button (`CreateRoom`), Refresh button
  (`ListRooms`), one row-button per `RoomSummary` ("Room N · M players · WxH/B") sending
  `JoinRoom(handle)`; `update(rooms)` rebuilds rows.
- `RoomLobbyScreen`: room title; member rows "Player <id> [ready] (owner) (you)";
  settings line; owner-only preset buttons (Beginner 9x9/10, Intermediate 16x16/40,
  Expert 30x16/99 → `SetSettings`) and Start button (`StartGame`); Ready toggle
  (`SetReady`); Leave button (`LeaveRoom` + back to room list). `update(state, myId)`
  re-renders and shows/hides owner controls.
- `Minesweeper` flow: connect → `ListRooms`; `RoomList` → update list screen; `YourId` →
  store; `RoomState` → update lobby (and switch to it from the list screen);
  `GameStarted` → build `Map` from last settings, show it; `JoinRefused` → refresh list;
  smiley click after game over (room phase back to LOBBY) → show lobby instead of
  sending `Restart`; draw() centers whatever screen is current.
- Delete the `JoinGame`/`InitMap`/`Restart` compat routes in `GameService` (message
  types stay for wire stability).
- [ ] Build green (`gradle build`); manual reasoning check only — behavior is verified in
  Tasks 4–5. Commit `feat: boot into room list and drive games through the ready-lobby`.

### Task 4: two-client integration test (the multiplayer proof)

**Files:** `minesweeper/src/test/kotlin/space/kiibou/server/RoomFlowIntegrationTest.kt`.

In-process `Server` on an ephemeral port (services auto-load from the test classpath) +
two real `Client`s. Script: A `CreateRoom` → A gets `YourId`+`RoomState`; B `ListRooms` →
sees the room; B `JoinRoom` → both see 2 members; A `SetSettings`; both `SetReady`; A
`StartGame` → both get `GameStarted` and `Restart`; A `RevealTile(0,0)` → **both**
receive the same `RevealTiles`; loss/win eventually flips both back to a LOBBY
`RoomState` with ready flags cleared. Await with polling + timeout; no sleeps in
assertions.

- [ ] Failing first (before Task 3's server-side pieces are in place it cannot pass);
  then GREEN → commit `test: prove the two-player room flow end to end`.

### Task 5: e2e boot + docs

- [ ] xvfb boot: app reaches the room list (log shows `ListRooms`→`RoomList` exchange),
  no exceptions; click regression replaced by the integration test.
- [ ] `WORKFLOW.md`: mark SP-3 client complete; note SP-3b (typed room codes via
  `TextInput`) as the remaining stage.
- [ ] Commit `docs: mark SP-3 client stage complete`.

## Self-Review

Brainstorm decisions → tasks: click-list lobby (T3), ready-lobby UI (T3), owner controls
(T2+T3), back-to-lobby via smiley (T3), no mid-game join UI (server refuses; T3 refreshes
on `JoinRefused`). Shim deletion tied to protocol switch (T3). Multiplayer proof (T4).
No placeholders; names consistent (`ScreenManager.show`, `RoomEvents.memberId`,
`YourIdInfo`).

## Execution Handoff

Inline execution, checkpoints after Task 3 (client switched) and Task 5 (stage done).
