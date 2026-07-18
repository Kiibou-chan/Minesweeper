# SP-2 · Event Migration Completion — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Working keyboard input in `graphics-library` — correct click-to-focus, a
`TextInput` widget proving the path, internal-API and warning cleanups, and the layout
freeze documented.

**Architecture:** The dispatcher's key path (queue → per-frame drain → focused element)
already exists; this stage fixes its focus rule (press-only, currently hover), replaces
the `com.sun.javafx` KeyCodeMap dependency with public API, and adds the first consumer:
`TextInput`, which overrides `keyEvent` to handle TYPE-action characters and exposes its
value as a REKotlin `Var<String>` (bridged to the JavaFX-bound `TextElement` via the
existing `toFX` seam).

**Tech Stack:** Kotlin 2.2.21, JavaFX KeyCode (public API only), REKotlin 0.1.1,
`kotlin.test` + JUnit4.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-07-18-sp2-event-migration-completion-design.md`.
- No AI co-author trailer in commits.
- Test commands: `gradle :graphics-library:test` and full `gradle build` at the end
  (`-Porg.gradle.java.installations.paths=/opt/jdk24` in this environment).
- Unit tests construct `GApplet()` directly (headless-safe, as `GridTest` shows) and
  synthetic `processing.event.MouseEvent`/`KeyEvent` objects; no OpenGL, no xvfb needed
  except for the final regression boot.

## Discovered-state corrections to the spec

- The dispatcher key path exists; "wire end-to-end" reduces to fixing focus semantics and
  adding a consumer.
- Focus bug: `EventDispatcher.dispatchEvents` reassigns `focusedElement` on **every**
  mouse event over a focusable element (hover steals focus); the doc comment on the field
  says click. Fix: reassign only when the event's actions contain `MouseAction.PRESS`.
- `KeyEvent` eagerly calls `com.sun.javafx.scene.input.KeyCodeMap` — internal API; under
  the module system this needs `--add-exports` at any use site. Replace with a map built
  from the public `KeyCode.getCode()`.

---

### Task 1: Public-API key-code mapping + data-class warning cleanup

**Files:** Modify `event/KeyEvent.kt`, `event/MouseEvent.kt`;
Test: `graphics-library/src/test/kotlin/space/kiibou/KeyCodeMappingTest.kt`.

- Replace `KeyCodeMap.valueOf(source.keyCode)` with lookup in
  `KeyCode.entries.associateBy { it.code }` (fallback `KeyCode.UNDEFINED`); drop the
  `com.sun.javafx` import.
- Annotate `KeyEventOption` and `MouseEventOption` with `@ConsistentCopyVisibility`
  (removes the Kotlin 2.x copy-visibility warnings).
- [ ] Failing test: `KeyCodeMappingTest` builds `KeyEvent(processing.event.KeyEvent(null,
  0L, PRESS, 0, 'a', 65))` and asserts `keyCode == KeyCode.A`; keyCode 8 → `BACK_SPACE`;
  keyCode 0 → `UNDEFINED`. RED today only under module-system runtime (IllegalAccess); on
  classpath runs it passes pre-fix — treat as characterization, verify no `com.sun`
  import remains via the compile.
- [ ] Implement → GREEN → commit `refactor: map key codes via public JavaFX API`.

### Task 2: Press-only focus + focused accessor

**Files:** Modify `event/EventDispatcher.kt`;
Test: `graphics-library/src/test/kotlin/space/kiibou/FocusTest.kt`.

- In the mouse loop, wrap the focus reassignment in
  `if (MouseAction.PRESS in event.actions) { … }`; a press over no focusable element
  leaves focus unchanged.
- Expose `val focused: GraphicsElement?` (public getter for the private field) so
  widgets can render focus state.
- [ ] Failing tests: two focusable elements with disjoint bounds registered for key
  events; synthetic MOVE over element B does **not** move focus (fails pre-fix), PRESS
  over B does; PRESS over empty space keeps focus.
- [ ] Implement → GREEN → commit `fix: move keyboard focus on click instead of hover`.

### Task 3: TextInput widget

**Files:** Create `gui/text/TextInput.kt`;
Test: `graphics-library/src/test/kotlin/space/kiibou/TextInputTest.kt`.

```kotlin
class TextInput(app: GApplet, initial: String = "", fontSize: Int = 15,
                fontName: String = "Times New Roman") : GraphicsElement(app) {
    val value: Var<String>                  // REKotlin source of truth
    var onSubmit: ((String) -> Unit)?       // Enter callback
    // focusable = true; registers itself for keyEvent; child TextElement bound to
    // value via the Signal-based TextElement constructor; caret suffix when focused.
    override fun keyEvent(event: KeyEvent)  // TYPE: printable append, '\b' delete, '\n'/'\r' submit
}
```

- [ ] Failing tests: typing appends (`"ab"`), backspace deletes, backspace on empty is a
  no-op, Enter fires `onSubmit` with the current value, events are ignored while
  `active == false`, and `value` is observable (REKotlin `observe` sees changes).
- [ ] Implement → GREEN → commit `feat: add TextInput widget driven by key events`.

### Task 4: Demo, docs, regression

**Files:** Modify `graphics-library/src/test/kotlin/space/kiibou/TestMain.kt`,
`CLAUDE.md`, `docs/superpowers/WORKFLOW.md`.

- [ ] `TestMain` gains a `TextInput` whose submits append a `TextElement` to the list
  (manual harness; run locally to try typing).
- [ ] CLAUDE.md: add a "Reactive boundaries" paragraph — events + derived state on
  REKotlin; layout properties stay JavaFX (eager REKotlin propagation without batching is
  too slow per-frame); `Signal.toFX` is the sanctioned bridge.
- [ ] Full `gradle build` green; minesweeper app xvfb boot + click regression unchanged.
- [ ] Commit `docs: document reactive boundaries and add TextInput demo` and mark SP-2
  complete in WORKFLOW.md.

## Self-Review

Spec scope → tasks: key path end-to-end = T2+T3; TextInput = T3; cleanup = T1; freeze doc
= T4. No placeholders; `TextInput.value`/`onSubmit` names used consistently. Deviations
from spec recorded under "Discovered-state corrections".

## Execution Handoff

Inline execution, single checkpoint at the end (stage is small); matches prior stages.
