# SP-3 (server) · Room Lifecycle — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ready-lobby rooms with the lifecycle LOBBY → PLAYING → LOBBY, replacing the
hardcoded always-running room 0, fully unit-tested without transport.

**Architecture:** A `Room` state machine (pure logic, injected `RoomEvents` output port +
`GameState` factory) on top of a `RoomRegistry` (handle allocation, connection mapping,
listing, dissolution). `GameService` routes the new messages; a temporary solo-compat
shim keeps the current client playable until the SP-3 client stage.

**Tech Stack:** Kotlin 2.2.21, kotlinx.serialization, `kotlin.test` + JUnit4.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-18-sp3-server-room-lifecycle-design.md`.
- No AI co-author trailer in commits. Human-authored voice.
- Test command: `gradle :minesweeper:test` (with `-Porg.gradle.java.installations.paths=/opt/jdk24`
  in this environment).
- All SP-1 tests must stay green throughout.
- Every new `MessageType` must be registered in `MinesweeperMessageType.serializersModule`
  and covered by the round-trip test (Task 1).

---

### Task 1: Protocol additions + round-trip serialization test

**Files:**
- Modify: `minesweeper/src/main/kotlin/space/kiibou/common/Data.kt`
- Test: `minesweeper/src/test/kotlin/space/kiibou/common/MessageRoundTripTest.kt`

**Produces (exact types later tasks use):**

```kotlin
@Serializable data class ReadyInfo(val ready: Boolean)
@Serializable enum class RoomPhase { LOBBY, PLAYING }
@Serializable data class MemberState(val id: Long, val ready: Boolean)
@Serializable data class RoomSummary(val handle: GameHandle, val memberCount: Int, val settings: MapInfo)
@Serializable data class RoomListInfo(val rooms: List<RoomSummary>)
@Serializable data class RoomStateInfo(
    val handle: GameHandle,
    val owner: Long,
    val members: List<MemberState>,
    val settings: MapInfo,
    val phase: RoomPhase,
)
```

New `MinesweeperMessageType` objects (each `@Serializable object … : MessageType<…>(…)`,
each added to `serializersModule`): `CreateRoom: Unit`, `ListRooms: Unit`,
`JoinRoom: GameHandle`, `LeaveRoom: Unit`, `SetReady: ReadyInfo`, `SetSettings: MapInfo`,
`StartGame: Unit`, `RoomList: RoomListInfo`, `RoomState: RoomStateInfo`,
`JoinRefused: GameHandle`, `GameStarted: Unit`.

- [ ] **Step 1: failing test** — `MessageRoundTripTest` registers the serializers modules
  (same three `Serial.addModule` calls as `GameService`), then for every
  `MinesweeperMessageType` (old + new) encodes a `Message(type, samplePayload)` with
  `Serial.json` + `InternalMessageSerializer` and decodes it back, asserting equality.
  Fails to compile (types missing).
- [ ] **Step 2: run, expect compile failure.**
- [ ] **Step 3: add the payload types + message objects + module registrations.**
- [ ] **Step 4: run, expect PASS.**
- [ ] **Step 5: commit** — `feat: add room lifecycle message types`.

### Task 2: Room state machine — lobby phase

**Files:**
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/RoomEvents.kt`
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/Room.kt`
- Create: `minesweeper/src/test/kotlin/space/kiibou/server/RecordingRoomEvents.kt`
- Test: `minesweeper/src/test/kotlin/space/kiibou/server/RoomTest.kt`

**Interfaces:**

```kotlin
interface RoomEvents {
    fun roomState(state: RoomStateInfo)   // broadcast to members on every change
    fun joinRefused(to: ConnectionHandle, room: GameHandle)
    fun gameStarted()                     // broadcast to members
}

class Room(
    val handle: GameHandle,
    private val events: RoomEvents,
    private val createGame: (MapInfo, MutableList<ConnectionHandle>) -> GameState,
) {
    var phase: RoomPhase; private set
    var settings: MapInfo; private set
    val members: List<ConnectionHandle>      // join order
    val owner: ConnectionHandle?             // first joiner, transfers on leave
    var gameState: GameState?; private set

    fun join(handle: ConnectionHandle): Boolean   // false + joinRefused when PLAYING
    fun leave(handle: ConnectionHandle)           // ownership transfer; removePlayer when PLAYING
    fun setReady(handle: ConnectionHandle, ready: Boolean)
    fun setSettings(handle: ConnectionHandle, settings: MapInfo)  // owner+LOBBY only, else ignore
    fun startGame(handle: ConnectionHandle): Boolean              // owner+LOBBY+all ready
    fun onGameOver()                              // → LOBBY, ready flags reset, gameState dropped
    val isEmpty: Boolean
    fun stateInfo(): RoomStateInfo
}
```

Lobby-phase behaviours under test (each RED→GREEN): first joiner owns · join emits
`roomState` · new joiner not ready · ready toggling own flag only · settings owner-only,
LOBBY-only · ownership transfer on owner leave (join order) · `isEmpty` after last leave ·
join to full lifecycle emits a `roomState` per change.

- [ ] Steps 1–5 as usual (test → RED → implement → GREEN → commit
  `feat: add room lobby state machine`).

### Task 3: Start, PLAYING, and back-to-lobby

**Files:** Modify `Room.kt`; Test `RoomTest.kt`.

Behaviours: `startGame` refused unless owner + all ready + LOBBY · start builds
`GameState` from settings with the room's members list, flips to PLAYING, emits
`gameStarted` then `roomState` · join while PLAYING → `joinRefused`, membership
unchanged · `onGameOver` returns to LOBBY, resets ready flags, drops `gameState`,
emits `roomState` · leave while PLAYING calls `GameState.removePlayer`.

Wiring game-over: `GameService` constructs the room's `GameState` with a `GameEvents`
decorator that forwards `win()`/`lose()` to `room.onGameOver()` after broadcasting
(decorator over `BroadcastGameEvents`; test with `RecordingGameEvents` + manual call).

- [ ] Steps 1–5; commit `feat: add game start and return-to-lobby room phases`.

### Task 4: RoomRegistry (evolves GameRegistry)

**Files:**
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/RoomRegistry.kt`
- Delete: `minesweeper/src/main/kotlin/space/kiibou/server/GameRegistry.kt`
- Test: `minesweeper/src/test/kotlin/space/kiibou/server/RoomRegistryTest.kt`
  (ports the three `GameRegistryTest` cases, which is then deleted)

```kotlin
class RoomRegistry(private val createRoom: (GameHandle) -> Room) {
    fun createAndJoin(handle: ConnectionHandle): Room      // allocates next GameHandle
    fun join(handle: ConnectionHandle, room: GameHandle): Boolean
    fun roomFor(handle: ConnectionHandle): Room?
    fun leave(handle: ConnectionHandle)                    // dissolves empty rooms
    fun listLobbyRooms(): List<RoomSummary>                // LOBBY phase only
    fun activeRoomCount(): Int
}
```

Behaviours: sequential handle allocation · listing hides PLAYING rooms · unknown-handle
lookups/leaves safe (ported) · dissolve-on-empty (ported) · survive-while-occupied (ported).

- [ ] Steps 1–5; commit `feat: replace GameRegistry with room-aware RoomRegistry`.

### Task 5: GameService routing + compat shim + e2e smoke

**Files:** Modify `GameService.kt`; Test: extend `RoomRegistryTest`/`RoomTest` only where
routing logic is extracted; e2e via xvfb run.

- `BroadcastRoomEvents(members-provider, messageService)` production impl.
- Route: `CreateRoom`→`createAndJoin` · `ListRooms`→reply `RoomList` ·
  `JoinRoom`→`join` (refusal already emitted by Room) · `LeaveRoom`/disconnect→`leave` ·
  `SetReady`/`SetSettings`/`StartGame`→room calls · in-game messages via
  `roomFor(handle)?.gameState?.…` with the existing null-guard logging.
- Compat shim (temporary, delete in SP-3 client stage): `JoinGame(h)` → join-or-create
  room `h` + auto-ready; `InitMap` → `setSettings` + `startGame` when sender is the only
  member.
- [ ] e2e: `xvfb-run` boot, assert the solo flow still emits
  `SetBombsLeft`/`SetTime`/`Restart` and a first click reveals (log-grep as in SP-1).
- [ ] Full suite green; commit `feat: route room lifecycle messages with solo compat shim`.

## Self-Review

Spec coverage: rules table → Tasks 2–3; protocol → Task 1; registry/listing → Task 4;
routing/compat/e2e → Task 5. No placeholders; names consistent (`RoomEvents.roomState`,
`Room.startGame`, `RoomRegistry.listLobbyRooms` used identically throughout).

## Execution Handoff

Inline execution with checkpoints after Task 3 (state machine complete) and Task 5
(everything routed + e2e), matching the SP-1 execution mode.
