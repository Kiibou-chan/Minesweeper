# SP-4 · Main Menu, Player Names, Typed Room Join — Design + Plan

**Date:** 2026-07-18 · **Base:** SP-3 client complete.
Combined spec+plan (small stage; decisions confirmed in conversation).

## Goal

- **Main menu** at boot: title, name field, **Singleplayer** and **Multiplayer** buttons —
  structured as a button list so future entries (Leaderboard, Settings) are additive.
- **Player names**: settable from the menu (`TextInput`), shown in lobbies; server
  defaults to "Player <id>".
- **SP-3b typed join**: join a room by typing its number in the room list screen.

## Design decisions

- **Singleplayer** = create a room + auto-ready → the ready-lobby doubles as the
  difficulty selector; Start launches. (One flow for both modes; no separate game path.)
- **Multiplayer** = the existing room list (now with a Back-to-menu button and a
  "join by number" `TextInput`).
- **Names** live server-side in `GameService` (connection → name, cleared on
  disconnect); `Room` resolves display names via an injected `nameOf` function, so room
  logic stays transport-free. `MemberState` gains `name`. `SetName` while in a room
  triggers a `RoomState` rebroadcast (`Room.refreshState`). Names trimmed to 24 chars.
- Client sends any typed-but-unsubmitted name when clicking Single-/Multiplayer, so the
  Enter key is not required for the name to stick.
- After game over the room returns to LOBBY with ready flags cleared (unchanged) — in
  singleplayer that means Ready → Start for a rematch; acceptable for now, noted as
  future polish (auto-ready solo rooms).

## Tasks

1. **Protocol + server names** — `NameInfo`, `SetName`; `MemberState(id, name, ready)`;
   `Room(nameOf)` + `refreshState()`; `GameService` name map + routing + disconnect
   cleanup. RED→GREEN: round-trip entry; `RoomTest` default names and name in state;
   integration test sets a name before creating and asserts it appears in `RoomState`.
   Commit `feat: add player names to rooms`.
2. **Client screens** — `MainMenuScreen` (title, name `TextInput`, Singleplayer,
   Multiplayer); `RoomListScreen` gains Back + join-by-number `TextInput`;
   `RoomLobbyScreen` renders `member.name`; `Minesweeper` boots into the menu,
   `startSingleplayer()` = send name + `CreateRoom` + `SetReady(true)`; `RoomState`
   switches to the lobby from menu or room list. Build green.
   Commit `feat: add main menu with names and typed room join`.
3. **E2E + docs** — xvfb: sweep-click Singleplayer → lobby → Start → reveal (full mouse
   journey if positions cooperate; minimum: CreateRoom+SetReady observed, no
   exceptions). Update WORKFLOW.md (stage complete; future menu entries noted).
   Commit `docs: mark SP-4 stage complete`.

## Success criteria

All suites green; app boots to menu; singleplayer reachable in ≤3 clicks; names visible
in lobby member list; joining by typed number works against a second client.
