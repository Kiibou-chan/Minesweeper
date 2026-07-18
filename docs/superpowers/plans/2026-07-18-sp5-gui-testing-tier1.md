# SP-5 · GUI Testing Framework, Tier 1 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Headless GUI interaction tests through production dispatch: tag-addressed
widgets, a robot that clicks/types at real widget positions, a fake network, and five
lobby-flow acceptance tests. Includes the approved `TextInput` minimum-width fix.

**Architecture:** per the approved spec
`docs/superpowers/specs/2026-07-18-sp5-gui-testing-tier1-design.md`.

**Tech Stack:** Kotlin 2.2.21, Gradle `java-test-fixtures`, kotlin.test + JUnit4.

## Global Constraints

- No AI co-author trailer in commits.
- Test commands: `gradle :graphics-library:test`, `gradle :minesweeper:test`, final
  `gradle build` (add `-Porg.gradle.java.installations.paths=/opt/jdk24` in this
  environment).
- Production behavior unchanged when `textMetrics == null` except the approved
  `TextInput` min-width.
- All existing suites stay green after every task.

---

### Task 1: Framework in graphics-library

**Files:**
- Modify: `graphics-library/build.gradle.kts` (add `java-test-fixtures` plugin)
- Modify: `gui/GraphicsElement.kt` (`testTag`)
- Modify: `util/GraphicsManager.kt` (roots accessor)
- Modify: `GApplet.kt` (`textMetrics`, `elementRoots`)
- Create: `gui/text/TextMetrics.kt` (interface + `EstimatingTextMetrics`)
- Modify: `gui/text/TextElement.kt` (metrics branch in both size bindings)
- Modify: `gui/text/TextInput.kt` (min clickable width `max(text, 60·scale)`)
- Create: `graphics-library/src/testFixtures/kotlin/space/kiibou/test/GuiRobot.kt`
  (robot + `walk()` + `findByTag`)
- Test: `graphics-library/src/test/kotlin/space/kiibou/GuiFrameworkTest.kt`

**Interfaces produced (used by Task 2):**

```kotlin
interface TextMetrics {
    fun width(fontName: String, sizePx: Int, text: String): Int
    fun height(fontName: String, sizePx: Int, text: String): Int
}
object EstimatingTextMetrics : TextMetrics   // width = (0.6·size·length), height = size

// GApplet
var textMetrics: TextMetrics?                // null = production path
val elementRoots: List<GraphicsElement>

// space.kiibou.test (testFixtures)
class GuiRobot(private val app: GApplet) {
    fun pump()
    fun findByTag(tag: String): GraphicsElement          // error if absent
    fun clickOn(tag: String); fun clickOn(element: GraphicsElement)
    fun rightClickOn(tag: String)
    fun type(text: String); fun pressEnter()
}
fun GraphicsElement.walk(): Sequence<GraphicsElement>
```

Robot click = synthesize `processing.event.MouseEvent(PRESS)` then `(RELEASE)` at
`(x + width/2, y + height/2)` via `eventDispatcher.mouseEvent`, then `pre()`. Typing =
`processing.event.KeyEvent(TYPE, key)` via `eventDispatcher.keyEvent`, then `pre()`.

**TextElement binding change (pattern for both width and height):**

```kotlin
widthProp.bind(IntegerBinding(textProperty, fontSizeProperty, fontProperty) {
    val metrics = app.textMetrics
    if (metrics != null) metrics.width(fontNameProperty.value, fontSizeProperty.value, textProperty.valueSafe)
    else app.gg.textWidth(fontProperty.value, fontSizeProperty.value, textProperty.valueSafe)
})
```
(`fontProperty` stays a listed dependency but is only *read* on the production branch,
so no font is created headless.)

**TextInput min width:** in `initImpl`, replace `widthProp.bind(it.widthProp)` with
`widthProp.bind(Bindings.max(it.widthProp, scaleProperty.multiply(60)))`.

**Steps:**
- [ ] **1. Failing tests** in `GuiFrameworkTest` (a `TestApp : GApplet` exposing
  `graphicsManager`; `app.textMetrics = EstimatingTextMetrics`):
  - `text_element_lays_out_headless`: `TextElement(app, "Hello").width > 0`.
  - `find_by_tag_resolves_nested_elements`: tag a `Button` inside a `VerticalList`.
  - `robot_click_fires_button`: `Button(app, TextElement(app, "Click"))`, tag it,
    register + `manager.pre()`, `robot.clickOn("btn")` → `clicked` observed once.
  - `robot_click_then_type_edits_text_input`: `TextInput(app, "x")` tagged, init,
    `clickOn` → `type("ab")` → value `"xab"`; `pressEnter` fires `onSubmit`.
  - `empty_text_input_is_clickable`: `TextInput(app)` after init has `width >= 60`.
- [ ] **2. Run** `gradle :graphics-library:test --tests "space.kiibou.GuiFrameworkTest"`
  — expect FAIL (unresolved references).
- [ ] **3. Implement** the files above.
- [ ] **4. Run** — expect PASS; then the full `:graphics-library:test` suite.
- [ ] **5. Commit** `feat: add headless GUI testing framework (tags, scene query, robot)`.

### Task 2: Seam, tags, and flow tests in minesweeper

**Files:**
- Create: `minesweeper/src/main/kotlin/space/kiibou/net/GameConnection.kt`
  (interface + `ClientGameConnection(client: Client)`)
- Modify: `Minesweeper.kt` — `client: GameConnection`; extract `initUi()` from
  `setup()`; add `internal fun initHeadless(connection: GameConnection)` (estimating
  metrics + connection + `initUi()` + `graphicsManager.pre()`)
- Modify: `lobby/MainMenuScreen.kt`, `lobby/RoomListScreen.kt`,
  `lobby/RoomLobbyScreen.kt` — add tags: `screen.menu`, `menu.nameInput`,
  `menu.singleplayer`, `menu.multiplayer`; `screen.rooms`, `rooms.back`,
  `rooms.create`, `rooms.refresh`, `rooms.joinInput`, `rooms.row.<gameId>`;
  `screen.lobby`, `lobby.title`, `lobby.ready`, `lobby.start`, `lobby.leave`
- Create: `minesweeper/src/test/kotlin/space/kiibou/lobby/FakeGameConnection.kt`
- Test: `minesweeper/src/test/kotlin/space/kiibou/lobby/LobbyFlowTest.kt`
- Modify: `minesweeper/build.gradle.kts` —
  `testImplementation(testFixtures(project(":graphics-library")))`

**Interfaces:**

```kotlin
interface GameConnection {
    fun <T : Any> send(messageType: MessageType<T>, payload: T)
    fun send(messageType: MessageType<Unit>)
}

class FakeGameConnection : GameConnection {
    val sent = mutableListOf<Message<*>>()   // both sends record here
}
```

Test harness in `LobbyFlowTest`: construct `Minesweeper()`, `initHeadless(fake)`,
`GuiRobot(app)`; `receive(type, payload)` = `eventDispatcher.messageEvent(Message(...))`
+ `pump()`.

**Steps:**
- [ ] **1. Failing tests** (five acceptance flows from the spec):
  - `singleplayer_click_creates_and_readies_a_room` — clickOn `menu.singleplayer` →
    `sent` types contain `CreateRoom`, `SetReady`; receive `YourId(0)` + owner
    `RoomState` → `screen.lobby` not `effectivelyHidden`, `lobby.title` text `"Room 0"`,
    `lobby.start` visible.
  - `non_owners_do_not_see_owner_controls` — receive `RoomState(owner = 99, ...)` →
    `lobby.start` `effectivelyHidden`.
  - `multiplayer_lists_rooms_and_join_by_click` — clickOn `menu.multiplayer` →
    `ListRooms` sent; receive `RoomList(room 5, 2 players)` → clickOn `rooms.row.5` →
    `JoinRoom(GameHandle(5))` sent.
  - `typing_a_room_number_joins_it` — on rooms screen: clickOn `rooms.joinInput`,
    `type("7")`, `pressEnter` → `JoinRoom(GameHandle(7))` sent.
  - `name_typed_on_the_menu_is_sent_before_entering_multiplayer` — clickOn
    `menu.nameInput`, `type("Svenja")`, clickOn `menu.multiplayer` → `sent` contains
    `SetName(NameInfo("Svenja"))` before `ListRooms`.
- [ ] **2. Run** — expect FAIL (seam/tags/fixtures missing).
- [ ] **3. Implement.** `setup()` keeps identical runtime order (client assigned before
  `connect`, per the SP-3 fix) via `ClientGameConnection`.
- [ ] **4. Run** flow tests, then full `:minesweeper:test`.
- [ ] **5. Commit** `feat: adopt GUI test framework for menu and lobby flows`.

### Task 3: Verification + docs

- [ ] Full `gradle build` green.
- [ ] xvfb boot regression (app still reaches the menu; no exceptions) — guards the
  `setup()`/seam refactor in the real app.
- [ ] WORKFLOW.md: mark SP-5 Tier 1 complete; Tier 2 (in-process xvfb harness, pixel
  probes) listed as future work.
- [ ] Commit `docs: mark SP-5 GUI-testing tier 1 complete`.

## Self-Review

- **Spec coverage:** tags/query/metrics/robot → T1; min-width → T1; seam/headless
  boot/tags/five flows → T2; suites green + boot regression → T3. Non-goals untouched.
- **Placeholder scan:** none; every step names files and expected outcomes; code shown
  for the two non-obvious edits (metrics branch, min width).
- **Type consistency:** `GameConnection` signatures match the `Client.send` overloads
  the UI already calls; `EstimatingTextMetrics`/`GuiRobot` names used identically in
  T1 and T2.

## Execution Handoff

Two options: **subagent-driven** (fresh subagent per task, two-stage review) or
**inline** (this session, checkpoint after Task 1 and at the end). Tasks are tightly
coupled through the new framework API; inline is recommended, as in prior stages.
