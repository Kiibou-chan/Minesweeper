# SP-1 · Game-Logic Test Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make server-side `GameState` deterministically unit-testable and fix five known
bugs under green tests.

**Architecture:** `GameState` depends on an injected `GameEvents` output port, a `Ticker`,
and a `Random` instead of talking to `MessageService`, `fixedRateTimer`, and `Random.Default`
directly. A `GameRegistry` holds the connection→game mapping so its lifecycle is testable
without transport. Production wiring lives in `GameService`.

**Tech Stack:** Kotlin 2.2.21, `kotlin.test` + JUnit4 (`kotlin("test-junit")`), Gradle.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-17-sp1-game-logic-test-foundation-design.md`.
- Commits MUST NOT carry an AI co-author trailer (`.claude/settings.json`:
  `includeCoAuthoredBy: false`). Plain messages, human-authored voice.
- Do NOT stage the 6 build files modified as session accommodations
  (`build.gradle.kts`, `annotation-processor/…`, `annotations/…`, `graphics-library/…`,
  `server/…`, and the JOGL exclusion + toolchain in `minesweeper/build.gradle.kts`).
  Stage only the source/test files each task names.
- Test command throughout: `gradle :minesweeper:test` (single class:
  `gradle :minesweeper:test --tests "space.kiibou.server.GameStateTest"`).
- New production code: `minesweeper/src/main/kotlin/space/kiibou/server/`.
  New test code + doubles: `minesweeper/src/test/kotlin/space/kiibou/server/`.

---

## Task 0: Environment setup (already performed this session — recorded for reproducibility)

**Steps** (idempotent; re-run in a fresh environment):
- Publish the sibling `reactive-kotlin` to mavenLocal (JDK 17+ / lowered to 21 here):
  `gradle :REKotlin:publishKotlinMultiplatformPublicationToMavenLocal :REKotlin:publishJvmPublicationToMavenLocal`
- If JDK 24 is unavailable, lower `jvmToolchain(24)`→`(21)` in all 6 module build files.
- If jogamp.org is blocked, exclude `org.jogamp.jogl` / `org.jogamp.gluegen` from
  `configurations.testRuntimeClasspath` in `minesweeper/build.gradle.kts`.
- Verify: `gradle :minesweeper:test` → BUILD SUCCESSFUL (NO-SOURCE is fine).

---

## Task 1: Introduce the test seam (enabling refactor, behavior-preserving)

**Files:**
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/GameEvents.kt`
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/Ticker.kt`
- Create: `minesweeper/src/test/kotlin/space/kiibou/server/RecordingGameEvents.kt`
- Create: `minesweeper/src/test/kotlin/space/kiibou/server/ManualTicker.kt`
- Modify: `minesweeper/src/main/kotlin/space/kiibou/server/GameState.kt`
- Modify: `minesweeper/src/main/kotlin/space/kiibou/server/GameService.kt`
- Test: `minesweeper/src/test/kotlin/space/kiibou/server/GameStateTest.kt`

**Interfaces produced (later tasks rely on these exact signatures):**

```kotlin
// GameEvents.kt
package space.kiibou.server

import space.kiibou.common.TileInfo

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

```kotlin
// Ticker.kt
package space.kiibou.server

import kotlin.concurrent.fixedRateTimer

interface Ticker {
    fun start(onTick: () -> Unit)
    fun stop()
}

class FixedRateTicker(private val name: String) : Ticker {
    private var timer: java.util.Timer? = null
    override fun start(onTick: () -> Unit) {
        stop()
        timer = fixedRateTimer(name, daemon = true, initialDelay = 0L, period = 1000L) { onTick() }
    }
    override fun stop() { timer?.cancel(); timer = null }
}
```

**Test doubles:**

```kotlin
// RecordingGameEvents.kt (test source set)
package space.kiibou.server

import space.kiibou.common.TileInfo

class RecordingGameEvents : GameEvents {
    val reveals = mutableListOf<List<TileInfo>>()
    val flags = mutableListOf<Triple<Int, Int, Boolean>>()
    val bombsLeft = mutableListOf<Int>()
    val times = mutableListOf<Int>()
    var restarts = 0; var wins = 0; var loses = 0

    override fun revealTiles(tiles: List<TileInfo>) { reveals += tiles }
    override fun setFlag(x: Int, y: Int, flagged: Boolean) { flags += Triple(x, y, flagged) }
    override fun setBombsLeft(count: Int) { bombsLeft += count }
    override fun setTime(seconds: Int) { times += seconds }
    override fun restart() { restarts++ }
    override fun win() { wins++ }
    override fun lose() { loses++ }

    fun lastReveal(): List<TileInfo> = reveals.last()
}
```

```kotlin
// ManualTicker.kt (test source set)
package space.kiibou.server

class ManualTicker : Ticker {
    private var onTick: (() -> Unit)? = null
    var running = false; private set
    override fun start(onTick: () -> Unit) { this.onTick = onTick; running = true }
    override fun stop() { running = false }
    fun advance(seconds: Int) { repeat(seconds) { if (running) onTick?.invoke() } }
}
```

**GameState refactor (behavior-preserving):**
- Change the constructor to:
  ```kotlin
  class GameState(
      val handles: MutableList<ConnectionHandle>,
      private var width: Int,
      private var height: Int,
      private var bombs: Int,
      private val events: GameEvents,
      private val ticker: Ticker,
      private val random: kotlin.random.Random = kotlin.random.Random.Default,
  )
  ```
- Replace every `gameService.messageService.send(handle, MinesweeperMessageType.X, payload)`
  broadcast loop with the matching `events` call:
  - `SetFlag` → `events.setFlag(x, y, flagged[x][y])`
  - `SetBombsLeft` → `events.setBombsLeft(left)`
  - `SetTime` → `events.setTime(time)`
  - `Loose` → `events.lose()`
  - `Win` → `events.win()`
- Remove the `handles.forEach { … }` wrappers around those (broadcast now lives in the
  production `GameEvents` impl).
- Timer: delete `startTimer`/`stopTimer` bodies using `fixedRateTimer`; in `setGameRunning`
  call `ticker.start { time++; events.setTime(time) }` on start and `ticker.stop()` on stop.
  `resetTimer()` becomes `{ time = 0; events.setTime(0) }`.
- Randomness: `chooseBombPositions()` uses `possibleTilePositions().shuffled(random).take(bombs)`.
- Move the two emissions currently done by `GameService` into `GameState`:
  - Add `fun revealAt(x: Int, y: Int) { val revealed = reveal(x, y); events.revealTiles(revealed) }`
    and keep `reveal` private (it stays recursive, returns the list, and fires
    `events.win()/lose()` internally as today).
  - Add `fun reset(width: Int, height: Int, bombs: Int) { setupVariables(width, height, bombs); events.restart() }`
    and `fun reset() { setupVariables(); events.restart() }`.
- `flagToggle`, `addPlayer`, `removePlayer`, `stopGame` stay public.

**GameService wiring:**
- Add a production `GameEvents` impl:
  ```kotlin
  class BroadcastGameEvents(
      private val handles: List<ConnectionHandle>,
      private val messageService: MessageService,
  ) : GameEvents {
      override fun revealTiles(tiles: List<TileInfo>) =
          broadcast(MinesweeperMessageType.RevealTiles, TilesInfo(tiles))
      override fun setFlag(x: Int, y: Int, flagged: Boolean) =
          broadcast(MinesweeperMessageType.SetFlag, FlagInfo(x, y, flagged))
      override fun setBombsLeft(count: Int) =
          broadcast(MinesweeperMessageType.SetBombsLeft, BombsLeftInfo(count))
      override fun setTime(seconds: Int) =
          broadcast(MinesweeperMessageType.SetTime, TimeInfo(seconds))
      override fun restart() = broadcastUnit(MinesweeperMessageType.Restart)
      override fun win() = broadcastUnit(MinesweeperMessageType.Win)
      override fun lose() = broadcastUnit(MinesweeperMessageType.Loose)
      private fun <T : Any> broadcast(type: MessageType<T>, payload: T) =
          handles.forEach { messageService.send(it, type, payload) }
      private fun broadcastUnit(type: MessageType<Unit>) =
          handles.forEach { messageService.send(it, type) }
  }
  ```
- When constructing a `GameState`, pass `BroadcastGameEvents(gameState.handles, messageService)`
  (the impl reads the same `handles` list the game mutates) and `FixedRateTicker("Timer $gameHandle")`.
- Update callbacks: `InitMap` → `gameState.reset(width, height, bombs)`; `Restart` →
  `gameState.reset()`; `RevealTile` → `gameState.revealAt(x, y)`; drop the now-duplicated
  `messageService.send(...)` calls in those callbacks.

**TDD steps:**
- [ ] **Step 1: Write the failing test** — `GameStateTest.setup_emits_no_events_until_wired`
  and a construction test that exercises the new API:
  ```kotlin
  package space.kiibou.server
  import kotlin.random.Random
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertTrue

  class GameStateTest {
      private fun newGame(w: Int = 9, h: Int = 9, bombs: Int = 10, seed: Long = 1L):
          Pair<GameState, RecordingGameEvents> {
          val events = RecordingGameEvents()
          val game = GameState(mutableListOf(), w, h, bombs, events, ManualTicker(), Random(seed))
          return game to events
      }

      @Test fun reset_emits_restart_and_bombs_left() {
          val (game, events) = newGame()
          game.reset()
          assertTrue(events.restarts >= 1)
          assertEquals(10, events.bombsLeft.last())
      }
  }
  ```
- [ ] **Step 2: Run test, verify it fails to compile** (new constructor/methods absent).
  Run: `gradle :minesweeper:test --tests "space.kiibou.server.GameStateTest"`
  Expected: FAIL (unresolved reference to new signatures).
- [ ] **Step 3: Implement** the four new files and the `GameState`/`GameService` refactor above.
- [ ] **Step 4: Run test, verify GREEN.** Also run full `gradle :minesweeper:test`.
  Expected: PASS.
- [ ] **Step 5: Commit** (stage only the seam files + test):
  ```bash
  git add minesweeper/src/main/kotlin/space/kiibou/server/GameEvents.kt \
          minesweeper/src/main/kotlin/space/kiibou/server/Ticker.kt \
          minesweeper/src/main/kotlin/space/kiibou/server/GameState.kt \
          minesweeper/src/main/kotlin/space/kiibou/server/GameService.kt \
          minesweeper/src/test/kotlin/space/kiibou/server/
  git commit -m "refactor: decouple GameState from transport, timer, and RNG for testing"
  ```

---

## Task 2: B1 — non-square board indexing bug

**Files:** Modify `GameState.kt` (`possibleTilePositions`); Test: `GameStateTest.kt`.

- [ ] **Step 1: Write the failing test** — on a non-square board every generated position
  is unique and in-bounds:
  ```kotlin
  @Test fun non_square_board_positions_are_valid() {
      val (game, events) = newGame(w = 5, h = 3, bombs = 4)
      game.revealAt(0, 0)                 // triggers placement + reveal on the new board
      val revealed = events.reveals.flatten()
      assertTrue(revealed.all { it.x in 0 until 5 && it.y in 0 until 3 })
      assertTrue(revealed.map { it.x to it.y }.toSet().size == revealed.size) // no dupes
  }
  ```
- [ ] **Step 2: Run — expect FAIL** (out-of-bounds / duplicate positions from `it / height`).
- [ ] **Step 3: Fix** — in `possibleTilePositions`, change `y + it / height` to `y + it / width`.
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Commit** — `git add GameState.kt GameStateTest.kt` →
  `git commit -m "fix: correct row index in possibleTilePositions for non-square boards"`.

---

## Task 3: B3 — first-click safety (3×3 exclusion + shrink-to-tile fallback)

**Files:** Modify `GameState.kt` (`setupVariables`, `reveal`, add placement helpers);
Test: `GameStateTest.kt`.

**Design:** add `private var bombsPlaced = false`. `setupVariables` resets state and sets
`bombsPlaced = false` **without** placing bombs or building number tiles. On the first
`reveal(x, y)` where `!bombsPlaced`, place bombs excluding a zone around `(x, y)`, build
number tiles, set `bombsPlaced = true`, then continue the normal reveal.

```kotlin
private fun placeBombsAvoiding(cx: Int, cy: Int) {
    val full = possibleTilePositions(cx - 1, cy - 1, 3, 3).filter { (x, y) -> isValidTile(x, y) }.toSet()
    val exclusion = if (bombs <= width * height - full.size) full else setOf(Vec2(cx, cy))
    bombTiles = possibleTilePositions()
        .filterNot { it in exclusion }
        .shuffled(random).take(bombs)
        .onEach { (x, y) -> setTile(x, y, TileType.BOMB) }
    createNumberTiles()
    bombsPlaced = true
}
```
Call `if (!bombsPlaced) placeBombsAvoiding(x, y)` at the top of `reveal`'s valid-tile branch.

- [ ] **Step 1: Write failing tests** —
  ```kotlin
  @Test fun first_click_is_never_a_bomb_and_opens_a_pocket() {
      val (game, events) = newGame(w = 9, h = 9, bombs = 10, seed = 42L)
      game.revealAt(4, 4)
      val revealed = events.lastReveal()
      assertTrue(revealed.any { it.x == 4 && it.y == 4 })            // clicked tile revealed
      assertTrue(revealed.none { it.type == TileType.RED_BOMB })     // not an instant loss
      assertEquals(0, events.loses)
  }

  @Test fun dense_board_falls_back_to_tile_only_exclusion() {
      val (game, events) = newGame(w = 3, h = 3, bombs = 8, seed = 7L)
      game.revealAt(1, 1)                  // center; full 3x3 == whole board, must shrink
      assertEquals(0, events.loses)        // clicked tile safe
  }
  ```
- [ ] **Step 2: Run — expect FAIL** (bombs currently placed at setup; first click may lose).
- [ ] **Step 3: Implement** the deferral + `placeBombsAvoiding` + remove bomb placement from
  `setupVariables` (keep the `revealed`/`flagged`/`tiles`/`bombsLeft` reset).
- [ ] **Step 4: Run — expect PASS**, plus full `gradle :minesweeper:test`.
- [ ] **Step 5: Commit** — `git commit -m "feat: guarantee a safe first click with 3x3 exclusion and fallback"`.

---

## Task 4: B2 — lose-reveal wrongly-flagged tiles

**Files:** Modify `GameState.kt` (bomb branch of `reveal`); Test: `GameStateTest.kt`.

- [ ] **Step 1: Write the failing test** — a safe tile flagged in the same row/column as the
  detonated bomb is revealed as `NO_BOMB`. Use a seed whose board is known, or drive it by
  flagging a tile then detonating a known bomb:
  ```kotlin
  @Test fun wrongly_flagged_tile_in_same_row_is_shown_on_loss() {
      // Board where (0,0) is safe and some (bx,by) with by==0 is a bomb (seeded).
      val (game, events) = newGame(w = 5, h = 5, bombs = 3, seed = 123L)
      game.revealAt(0, 0)                         // place bombs, open pocket (first-click safe)
      // find a bomb and a same-row non-bomb unrevealed tile via a helper exposing the board
      // (see test helper `firstBombAndSameRowSafe` added below)
      // flag the safe tile, then detonate the bomb:
      // game.flagToggle(safeX, safeY); game.revealAt(bombX, bombY)
      // assert lastReveal contains TileInfo(safeX, safeY, NO_BOMB)
  }
  ```
  Add a minimal test-only accessor if needed (e.g., `internal fun bombPositions() = bombTiles`)
  guarded `internal` and used only from tests, or assert via emitted reveal contents.
- [ ] **Step 2: Run — expect FAIL** (current `x != flagX && y != flagY` guard skips same
  row/column tiles).
- [ ] **Step 3: Fix** — replace the loop guard with
  `if (isFlagged(fx, fy) && !isBomb(fx, fy)) revealed += TileInfo(fx, fy, TileType.NO_BOMB)`
  (drop the position guard entirely; the clicked bomb is excluded by `!isBomb`).
- [ ] **Step 4: Run — expect PASS.**
- [ ] **Step 5: Commit** — `git commit -m "fix: reveal all wrongly-flagged tiles on loss regardless of row or column"`.

---

## Task 5: B4 — GameService lifecycle (extract GameRegistry; fix leak + NPE)

**Files:**
- Create: `minesweeper/src/main/kotlin/space/kiibou/server/GameRegistry.kt`
- Modify: `GameService.kt` (delegate to registry; guard callbacks/disconnect)
- Test: `minesweeper/src/test/kotlin/space/kiibou/server/GameRegistryTest.kt`

**Design:** move the `users`/`gameStates` maps + join/lookup/leave into a transport-free class.

```kotlin
// GameRegistry.kt
package space.kiibou.server
import space.kiibou.common.GameHandle
import space.kiibou.net.common.ConnectionHandle

class GameRegistry(private val createGame: (GameHandle) -> GameState) {
    private val users = mutableMapOf<ConnectionHandle, GameHandle>()
    private val games = mutableMapOf<GameHandle, GameState>()

    fun join(handle: ConnectionHandle, game: GameHandle): GameState {
        users[handle] = game
        return games.getOrPut(game) { createGame(game) }.also { it.addPlayer(handle) }
    }
    fun gameFor(handle: ConnectionHandle): GameState? = users[handle]?.let { games[it] }
    fun leave(handle: ConnectionHandle) {
        val gh = users.remove(handle) ?: return
        val game = games[gh] ?: return
        game.removePlayer(handle)
        if (game.handles.isEmpty()) games.remove(gh)
    }
    fun activeGameCount(): Int = games.size
}
```

`GameService` holds a `GameRegistry`, its message callbacks use `registry.gameFor(handle)?.let { … }`
(no-op + warn when null), `JoinGame` calls `registry.join(...)`, and `onDisconnect` calls
`registry.leave(it)` (no `!!`, no leak).

- [ ] **Step 1: Write failing tests** in `GameRegistryTest`:
  ```kotlin
  package space.kiibou.server
  import space.kiibou.common.GameHandle
  import space.kiibou.net.common.ConnectionHandle
  import kotlin.random.Random
  import kotlin.test.*

  class GameRegistryTest {
      private fun registry() = GameRegistry { GameState(mutableListOf(), 9, 9, 10,
          RecordingGameEvents(), ManualTicker(), Random(1L)) }
      private fun handle(id: Int) = ConnectionHandle(id.toLong())   // adjust to real ctor

      @Test fun last_player_leaving_removes_the_game() {
          val r = registry(); val h = handle(1)
          r.join(h, GameHandle(0)); assertEquals(1, r.activeGameCount())
          r.leave(h); assertEquals(0, r.activeGameCount())
      }
      @Test fun game_survives_while_one_player_remains() {
          val r = registry(); val a = handle(1); val b = handle(2)
          r.join(a, GameHandle(0)); r.join(b, GameHandle(0))
          r.leave(a); assertEquals(1, r.activeGameCount())
      }
      @Test fun leave_or_lookup_for_unknown_handle_is_safe() {
          val r = registry()
          assertNull(r.gameFor(handle(99)))
          r.leave(handle(99))            // must not throw
      }
  }
  ```
  (Verify the real `ConnectionHandle`/`GameHandle` constructors and adjust the helpers.)
- [ ] **Step 2: Run — expect FAIL** (`GameRegistry` does not exist).
- [ ] **Step 3: Implement** `GameRegistry`; refactor `GameService` to delegate and guard.
- [ ] **Step 4: Run — expect PASS**, plus full `gradle :minesweeper:test`.
- [ ] **Step 5: Commit** — `git commit -m "fix: clean up empty games on disconnect and guard against pre-join messages"`.

---

## Self-Review

- **Spec coverage:** Task 1 = seam (Approach A). Task 2 = B1. Task 3 = B3. Task 4 = B2.
  Task 5 = B4 (leak + NPE via `GameRegistry`). All spec items mapped.
- **Placeholder scan:** the only deliberately-deferred detail is verifying the real
  `ConnectionHandle`/`GameHandle` constructors in Task 5 tests — flagged inline, resolve at
  implementation time by reading `server/.../SocketConnection.kt`/`ConnectionHandle` and
  `common/Data.kt`.
- **Type consistency:** `GameEvents` method names are used identically in `RecordingGameEvents`,
  `BroadcastGameEvents`, and every `GameState` emission point. `revealAt`/`reset`/`flagToggle`
  are the public entry points `GameService` calls.

## Execution Handoff

Two options: **subagent-driven-development** (fresh subagent per task + two-stage review) or
**executing-plans** (inline, batched with checkpoints). Given the tasks are tightly coupled to
one file (`GameState`) and run in this session, inline execution with a review after Task 1 and
Task 3 is the pragmatic choice.
