# SP-1 · Game-Logic Test Foundation — Design

**Status:** draft for review
**Branch base:** `guilib-reactive-kotlin`
**Date:** 2026-07-17

## Goal

Make the server-side Minesweeper game logic **deterministically unit-testable**,
then fix the known correctness bugs under green tests. This is the on-ramp for
SP-3 (lobby/rooms), which will build on a `GameState` that has real test
coverage.

## Non-goals (explicitly deferred)

- The REKotlin event-system migration (SP-2).
- Real lobby/room creation, join, list (SP-3). SP-1 only fixes the *lifecycle
  bugs* in the current single-room-`0` wiring; it does not add room UI or
  multi-room routing.
- Difficulty selector, persistence/high-scores, chording, game-over overlay.
- Any change to the client rendering layer (`graphics-library`, `game/*` UI).

## Context: why the logic is untestable today

`GameState` (in the `minesweeper` module) has three hard couplings:

1. **Outbound messaging** — every mutation calls
   `gameService.messageService.send(...)`, which serializes and does real socket
   I/O via `server.sendMessage`.
2. **The timer** — `startTimer()` spins a real `fixedRateTimer` background thread
   that ticks every 1000 ms.
3. **Randomness** — bomb placement uses `.shuffled()` with no injectable seed.

Additionally, the `minesweeper` module depends on `:graphics-library` and on
`REKotlin:0.1.1` from `mavenLocal`, so nothing in the module compiles (tests
included) until REKotlin is published locally.

## Setup prerequisite

Publish the sibling `reactive-kotlin` project to Maven local so
`graphics-library`/`minesweeper` resolve:

```
cd <reactive-kotlin checkout> && ./gradlew publishToMavenLocal
```

Verify the published coordinate/version matches what `minesweeper` and
`graphics-library` request (`space.kiibou.reactive-kotlin:REKotlin:0.1.1`). If
the versions differ, reconcile before writing tests (this is a plan-time check).

## Architecture — the test seam (Approach A: output port + injected deps)

`GameState` stops talking to transport, the timer, and the RNG directly. It
depends on three injected collaborators:

### 1. `GameEvents` — the output port

An interface with one method per event the game emits. `GameState` calls these
instead of `messageService.send(...)`. It becomes the **single source of game
events**, so the two emissions currently done from `GameService`
(`RevealTiles`, `Restart`) move into `GameState` behind this interface.

```
interface GameEvents {
    fun revealTiles(tiles: List<TileInfo>)
    fun setFlag(x: Int, y: Int, flagged: Boolean)
    fun setBombsLeft(count: Int)
    fun setTime(seconds: Int)
    fun restart()
    fun win()
    fun lose()
}
```

- **Production impl** — `BroadcastGameEvents(handles, messageService)` maps each
  call to `handles.forEach { messageService.send(it, <MessageType>, <payload>) }`,
  preserving today's broadcast-to-all-handles behavior.
- **Test impl** — `RecordingGameEvents` appends each call to a list, so tests
  assert semantically (`events.wins == 1`,
  `events.reveals.flatten().count { it.type == BOMB }`, etc.).

### 2. `Ticker` — the timer abstraction

```
interface Ticker {
    fun start(onTick: () -> Unit)   // onTick fires once per game-second
    fun stop()
}
```

- **Production impl** — `FixedRateTicker`, wrapping the existing
  `fixedRateTimer(period = 1000ms)`.
- **Test impl** — `ManualTicker` exposing `advance(seconds: Int)` which invokes
  the registered `onTick` that many times.

`GameState`'s tick handler does `time++; events.setTime(time)`. In tests,
`ticker.advance(3)` deterministically produces `time == 3` and three `setTime`
events — no threads, no wall-clock.

### 3. `Random` — injected RNG

`GameState` takes `random: Random = Random.Default`. Bomb placement uses
`pool.shuffled(random).take(bombs)`. Tests pass `Random(seed)` for a fully
known board.

### Resulting constructor (shape)

```
class GameState(
    val handles: MutableList<ConnectionHandle>,
    width: Int, height: Int, bombs: Int,
    private val events: GameEvents,
    private val ticker: Ticker,
    private val random: Random,
)
```

`GameService` constructs the production collaborators when it creates a
`GameState`; nothing else in its control flow changes except the two emissions
that move into `GameState` and the lifecycle fixes below.

## Behavior changes & bug fixes (each lands under a failing-first test)

### B1. Non-square board bug (correctness)

`possibleTilePositions` computes the row as `it / height`; it must be
`it / width`. Today it is masked because every board is square.
**Test:** a 5×3 board — assert generated positions are unique, all in-bounds, and
that exactly `bombs` bombs are placed.

### B2. Lose-reveal `&&` bug (correctness)

On detonation, wrongly-flagged tiles should be revealed as `NO_BOMB`. The current
guard `x != flagX && y != flagY && isFlagged && !isBomb` wrongly skips every tile
sharing the clicked row *or* column. The clicked tile is itself a bomb, so
`!isBomb` already excludes it; the position guard is removed entirely:

```
if (isFlagged(fx, fy) && !isBomb(fx, fy)) revealed += TileInfo(fx, fy, NO_BOMB)
```

**Test:** flag a safe tile in the same row/column as the detonated bomb; assert
it comes back as `NO_BOMB`.

### B3. First-click safety (behavior change)

Bomb placement moves from `setupVariables` to the **first reveal** (guarded by a
`bombsPlaced` flag). On that first reveal at `(x, y)`:

1. Build the ideal exclusion set: the clicked tile + its in-bounds neighbors
   (9 interior / 6 edge / 4 corner).
2. Candidate pool = all tiles − exclusion set.
3. **Fallback decision (pure counting, no RNG):** if
   `bombs > width*height − excludedCount`, the full 3×3 does not fit — fall back
   to excluding **only the clicked tile** (`excludedCount = 1`, which fits any
   legal board down to `bombs = totalTiles − 1`).
4. Place bombs into the chosen pool via `pool.shuffled(random).take(bombs)`,
   then compute number tiles.

`setupVariables` still resets `revealed`/`flagged`/`tiles` and `bombsLeft`, but
leaves the board bomb-free until first click.
**Tests (seeded RNG):** (a) interior first click never a bomb and opens a pocket
(3×3 all non-bomb); (b) 3×3 board with 8 bombs → fallback path, clicked tile
safe, other 8 tiles all bombs; (c) corner first click on a dense board →
fallback, clicked tile safe.

### B4. `GameService` lifecycle (correctness) — leak + NPE

- **Leak:** `gameStates` (and `users`) are never cleaned. On disconnect, after
  `removePlayer`, if the game has no remaining handles, remove it from
  `gameStates`; also remove the handle from `users`.
- **NPE:** `getGameState` does `users[handle]!!`, which throws if any
  message (or a disconnect) arrives before `JoinGame`. Guard it: resolve the
  game via `users[handle]?.let { gameStates[it] }` and no-op (with a logged
  warning) when absent, in both the disconnect handler and message callbacks.

To make this testable without transport, extract the registry logic
(the `users`/`gameStates` maps + `join` / `leave` / `cleanupIfEmpty` / lookup)
into a small `GameRegistry` class that has no dependency on `Server`/sockets.
`GameService` delegates to it. **Tests:** join then disconnect removes the game;
two players in one game, one disconnects, game survives; message/disconnect for a
never-joined handle is a safe no-op (no throw).

## Testing strategy

- Framework: `kotlin.test` + JUnit4 (already declared in the module), matching
  the existing `GridTest`/`ListTest`.
- New tests: `GameStateTest` (board generation, flood reveal, flag/bombs-left,
  win/lose emission, timer via `ManualTicker`, B1–B3) and `GameRegistryTest`
  (B4). Test doubles: `RecordingGameEvents`, `ManualTicker`.
- Every bug fix follows RED→GREEN: write the failing test first, watch it fail,
  fix, watch it pass, commit.

## Success criteria

- `./gradlew :minesweeper:test` runs green with no real threads/sockets in the
  new tests.
- B1–B4 each have a test that fails before the fix and passes after.
- `GameState` no longer references `messageService`, `fixedRateTimer`, or an
  un-seeded random directly.
- Production behavior over the wire is unchanged (same messages, same broadcast
  semantics) — verified by the `BroadcastGameEvents` mapping being a
  method-for-method translation of today's `send` calls.

## Risks / open points

- **REKotlin version drift:** if the locally published REKotlin version isn't
  `0.1.1`, the module won't resolve; reconcile at plan time.
- **JVM toolchain 24:** the module pins `jvmToolchain(24)`; the build environment
  must have (or provision) a JDK 24 toolchain. Confirm before writing tests.
- **Emission move:** relocating `RevealTiles`/`Restart` emission from
  `GameService` into `GameState` slightly widens the diff; it is justified
  because it makes `GameState` the single, fully-testable event source and
  simplifies SP-3.
