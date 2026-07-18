# SP-3 (server half) · Room Lifecycle — Design

**Status:** draft for review
**Branch base:** `claude/project-roadmap-brainstorm-d4466h` (includes SP-1)
**Date:** 2026-07-18
**Sequencing:** first of three stages (SP-3 server → SP-2 → SP-3 client). This spec is
server-only: no GUI work, no new client screens. It must be fully testable with the SP-1
seam (`RecordingGameEvents`, `ManualTicker`, seeded `Random`) and ship with the existing
client still able to play (see Compatibility).

## Goal

Replace the hardcoded single room (`GameHandle(0)`, auto-created on join, game always
implicitly running) with a real room lifecycle: players gather in a room lobby, the owner
picks settings everyone sees, members mark Ready, the owner starts the game when all are
ready, and the room returns to the lobby after the game ends.

## Non-goals

- Any client/UI change beyond the minimal compatibility shim (below). Lobby screens are
  the SP-3 client spec, after SP-2.
- Typed room codes/names (SP-3b). Rooms are identified by their numeric `GameHandle`.
- Mid-game join or board-state snapshots — joining is only possible in the LOBBY phase,
  which is what makes snapshots unnecessary.
- Player display names, colors, per-player attribution, scoring (competitive modes).
- Spectating, chat, persistence.

## Room model

A `Room` (new class, `minesweeper` module, `space.kiibou.server`) owns what today is
spread between `GameRegistry` and an always-on `GameState`:

- `handle: GameHandle` — room identity.
- `phase: RoomPhase` — `LOBBY` or `PLAYING`.
- `members: LinkedHashMap<ConnectionHandle, Boolean>` — join-ordered; value = ready flag.
- `owner: ConnectionHandle` — always the first entry initially; on owner leave, ownership
  passes to the longest-present remaining member (first entry of the map).
- `settings: MapInfo` — width/height/bombs; defaults 9×9/10.
- `gameState: GameState?` — null in LOBBY, created on start.

### Rules

- **Join** — allowed only in `LOBBY`. Joining a `PLAYING` room is rejected (see
  `JoinRefused`). A new joiner is not ready.
- **Ready** — any member toggles their own flag. Changing settings resets nobody's
  readiness (owner tweaks are visible live; members re-decide only if they want to).
- **Settings** — owner only; non-owner attempts are ignored with a logged warning.
  Settings changes are only accepted in `LOBBY`.
- **Start** — owner only; requires `phase == LOBBY` and every member ready (including the
  owner). Creates the `GameState` from `settings` with the room's member handles, flips
  to `PLAYING`, and emits the game's initial events (`Restart`, `SetBombsLeft`,
  `SetTime`) via the existing `BroadcastGameEvents` path.
- **Game over** — on win or lose the room flips back to `LOBBY` and all ready flags
  reset. Clients keep rendering the final board until the player acts; no server work is
  needed for that lingering (the phase is already LOBBY, so the join window is open).
  The `GameState` is dropped (a new one is built on next start).
- **Leave / disconnect** — remove the member; transfer ownership if the owner left; if
  the room empties, dissolve it. Leaving mid-`PLAYING` uses the existing
  `GameState.removePlayer` path (shared-fate co-op continues for the rest).
- **Room listing** — only `LOBBY`-phase rooms are listed (a PLAYING room is invisible to
  joiners, which is the "lock on start" behaviour).

## Protocol additions (`MinesweeperMessageType`)

Client → server:

| Message | Payload | Notes |
|---|---|---|
| `CreateRoom` | – | Server allocates the next `GameHandle`, creator joins as owner. |
| `ListRooms` | – | Reply is a `RoomList`. |
| `JoinRoom` | `GameHandle` | Replaces `JoinGame`. Refused unless LOBBY. |
| `LeaveRoom` | – | Also implied by disconnect. |
| `SetReady` | `ReadyInfo(ready: Boolean)` | Own flag only. |
| `SetSettings` | `MapInfo` | Owner only, LOBBY only. Replaces `InitMap`. |
| `StartGame` | – | Owner only, all ready. |

Server → client:

| Message | Payload | Notes |
|---|---|---|
| `RoomList` | `RoomListInfo(rooms: List<RoomSummary>)` | `RoomSummary(handle, memberCount, settings)`; LOBBY rooms only. |
| `RoomState` | `RoomStateInfo(handle, owner: MemberId, members: List<MemberState>, settings, phase)` | Broadcast to room members on every change (join/leave/ready/settings/ownership/phase). `MemberState(id: MemberId, ready: Boolean)`. |
| `JoinRefused` | `GameHandle` | Room started or vanished; client should re-`ListRooms`. |
| `GameStarted` | – | Signals the switch to the game view; followed by the usual game events. |

`MemberId` is an opaque `Long` derived from the connection handle — enough for the client
to distinguish "me"/"owner"/others without leaking connection internals or adding a
naming system (names are out of scope).

Existing in-game messages (`RevealTile`, `ToggleFlag`, `RevealTiles`, `SetFlag`,
`SetBombsLeft`, `SetTime`, `Win`, `Loose`, `Restart`) are unchanged. `Restart` as a
client→server request is no longer routed (the round loop goes through the lobby);
server→client `Restart` remains the "fresh board" signal at game start.

## Structure

- **`Room`** — the lifecycle state machine above. Pure logic; talks to the world via the
  injected `GameState` factory and a new `RoomEvents` output port (same pattern as SP-1's
  `GameEvents`): `roomState(RoomStateInfo)`, `joinRefused(GameHandle)`, `gameStarted()`.
  Test impl records; production impl broadcasts via `MessageService`.
- **`RoomRegistry`** — evolves `GameRegistry`: allocates handles (`CreateRoom`), maps
  connection → room, lists LOBBY rooms, dissolves empty rooms. Same testing style as
  `GameRegistryTest`.
- **`GameService`** — routes the new message types to `RoomRegistry`/`Room`; keeps the
  `withGame`-style null-guarding for connections that aren't in a room.
- `GameState` itself is untouched except: it is now created per-start instead of
  per-room-creation.

## Compatibility (keeping the app runnable during this stage)

The current client (`Map.initImpl`) sends `JoinGame(0)` + `InitMap(...)` and expects an
immediately playable board; the lobby UI does not exist until the SP-3 client stage. To
keep `:minesweeper:run` working end-to-end throughout:

- `JoinGame` and `InitMap` remain routed as a **solo-compat path**: `JoinGame(h)` creates
  or joins room `h`, marks the joiner ready, and `InitMap` sets settings and immediately
  starts the game if the sender is the only member. Multi-member rooms require the new
  flow.
- This shim is explicitly temporary and is deleted in the SP-3 client stage, when the
  client speaks the new protocol.

## Testing strategy

Same discipline as SP-1 (RED→GREEN per behaviour, no transport, no threads):

- `RoomTest` — phase transitions: start requires owner + all ready; join refused while
  PLAYING; game-over returns to LOBBY and clears ready flags; settings guarded to owner
  and LOBBY; ownership transfer on owner leave; dissolve-on-empty; leave mid-game keeps
  the game running for the rest.
- `RoomRegistryTest` — handle allocation, listing filters out PLAYING rooms, connection
  mapping, dissolution.
- `RecordingRoomEvents` test double mirrors `RecordingGameEvents`.
- End-to-end smoke: boot the real app (compat path) under xvfb and verify the solo flow
  still produces the SP-1-verified message sequence.

## Success criteria

- All new tests green; all SP-1 tests still green.
- The real app still boots and plays solo via the compat shim (verified end-to-end).
- Two simulated connections can: create a room, join it, exchange ready/settings state,
  start only when both ready, play a shared board, and land back in LOBBY after win/lose
  — demonstrated in tests via recorded events.

## Risks / open points

- **Protocol growth**: seven new client→server and four new server→client message types;
  all must be registered in `MinesweeperMessageType.serializersModule` (forgetting one is
  a runtime serialization error — a test should assert every new type round-trips).
- **`Restart` semantics change** (no longer client-initiated): the current client's
  smiley-restart stops working against the new server except via the compat shim. The
  shim covers it until the client stage lands.
- **`GameRegistry` → `RoomRegistry`**: `GameRegistryTest` from SP-1 is superseded —
  its cases must be ported, not deleted.
