# SP-2 · Finish the REKotlin Event Migration — Design

**Status:** draft for review
**Date:** 2026-07-18
**Sequencing:** second of three stages (after SP-3 server, before SP-3 client). The SP-3
client's screen system builds on the event semantics this stage finalizes.

## Goal

Finish the `graphics-library` event-system migration onto REKotlin, prove the new
key/focus path with a real widget, and formally freeze the layout system on JavaFX
properties so the wholesale-migration question stays settled.

## Non-goals

- Migrating layout properties (`xProp`/`widthProp`/`scaleProperty`, `BorderBox`/`Grid`/
  `VerticalList` binding math) off JavaFX. REKotlin is eager/push with no batch API;
  per-frame layout stays on lazy JavaFX bindings. Revisiting requires a batch/transaction
  API in REKotlin first (its own project, out of scope).
- Any server or game-logic change.
- The lobby screens themselves (SP-3 client).

## Scope

1. **Key events end-to-end.** `KeyEvent`/`KeyEventListener`/`KeyOptionMap` exist but
   nothing consumes them. Wire the full path: Processing `keyEvent` → `EventDispatcher`
   queue → per-frame drain → focused element. Define focus: `GApplet` tracks one focused
   `GraphicsElement`; click-to-focus for `focusable` elements; key events go to the
   focused element only (no bubbling for now — YAGNI).
2. **`TextInput` widget** (`graphics-library`): focusable, renders its text + caret via
   the existing text elements, handles printable chars, backspace, and an Enter/submit
   callback. Exposes its value as a REKotlin `Var<String>`. This is the proof the key
   system works and the building block SP-3b needs for typed room codes. Demonstrated in
   `TestMain`.
3. **Mouse-event cleanup.** Remove remaining dead/duplicated pre-migration code paths
   (`data class copy()` visibility warnings in `MouseEvent.kt` included).
4. **Layout freeze documentation.** CLAUDE.md gets a short "reactive boundaries" section:
   events + derived state on REKotlin, layout on JavaFX, `Signal.toFX` as the sanctioned
   bridge, and why (eager propagation, no batching → too slow per-frame).

## Testing strategy

- Unit tests for the pure parts without OpenGL: key-option matching, focus bookkeeping,
  `TextInput` editing logic (feed synthetic `KeyEvent`s, assert the `Var<String>`).
  Follows `GridTest`/`ListTest` style in `graphics-library`.
- Manual/e2e: `TestMain` gains a `TextInput` demo; under xvfb, verify boot + no event
  regressions in the Minesweeper app itself.

## Success criteria

- Typing into the `TestMain` demo works (focus by click, edit, submit).
- No remaining references to the pre-migration event paths; graphics-library compiles
  without the migration-era warnings it can control.
- CLAUDE.md states the layout freeze and the bridge rule.
- All existing tests stay green.

## Risks / open points

- Focus interactions with `EventDispatcher`'s once-per-frame drain (ordering of a click
  that both moves focus and types is frame-quantized — acceptable).
- Processing `keyEvent` registration mirrors the existing `mouseEvent` register/
  unregister-on-empty pattern in `GraphicsElement`; keep the two symmetrical.
