package space.kiibou.gui.text

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.input.KeyCode
import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.event.EventModifier.CTRL
import space.kiibou.event.KeyAction
import space.kiibou.event.KeyEventOption
import space.kiibou.event.anyModifiers
import space.kiibou.event.options
import space.kiibou.gui.BorderBox
import space.kiibou.gui.BorderStyle.IN
import space.kiibou.gui.GraphicsElement
import space.kiibou.reactive.now
import space.kiibou.reactive.reactives.Var
import space.kiibou.reactive.reactives.set

/**
 * A single-line text input driven by the key-event system. Click to focus (see
 * [space.kiibou.event.EventDispatcher]), type to edit; Enter fires [onSubmit]. The
 * current text is exposed as the REKotlin [value] source, which the internal
 * [TextElement] renders through the `Signal.toFX` bridge. The visual child is created in
 * [initImpl], so the editing logic works without a running sketch (fonts require one).
 */
class TextInput(
    app: GApplet,
    initial: String = "",
    private val fontSize: Int = 15,
    private val fontName: String = "Times New Roman",
    private val fontColor: Color = BLACK,
) : GraphicsElement(app) {

    val value: Var<String> = Var(initial)

    private val cursorProp = SimpleIntegerProperty(initial.length)

    /** Insertion point, between 0 and the value's length. */
    val cursor: Int get() = cursorProp.value

    internal var clock: () -> Long = System::currentTimeMillis

    private var lastEdit = 0L

    /** Solid for [CARET_SOLID_MS] after a keystroke, then blinking. */
    val caretVisible: Boolean
        get() {
            val idle = clock() - lastEdit

            if (idle < CARET_SOLID_MS) return true

            return ((idle - CARET_SOLID_MS) / CARET_BLINK_MS) % 2 == 0L
        }

    var onSubmit: ((String) -> Unit)? = null

    private var text: TextElement? = null

    init {
        focusable = true
        app.registerMethod("keyEvent", this)
        registerEditingKeys()
    }

    override fun initImpl() {
        val label = TextElement(app, value).also {
            it.fontSizeProperty.unbind()
            it.fontSizeProperty.bind(scaleProperty.multiply(fontSize))
            it.fontNameProperty.value = fontName
            it.fontColorProperty.value = fontColor
        }
        text = label

        BorderBox(app).also {
            it.style = IN
            it.xProp.bind(xProp)
            it.yProp.bind(yProp)

            addChild(it)
            it.addChild(label)

            // A name-sized box stays visible and clickable while the value is empty.
            it.innerWidthProp.unbind()
            it.innerWidthProp.bind(javafx.beans.binding.Bindings.max(label.widthProp, scaleProperty.multiply(100)))

            widthProp.bind(it.widthProp)
            heightProp.bind(it.heightProp)
        }

        label.addChild(Caret(app).also {
            it.xProp.bind(
                javafx.beans.binding.Bindings.createIntegerBinding(
                    { label.x - scale + label.widthOf(label.textProperty.valueSafe.take(cursorProp.value)) },
                    label.xProp, label.textProperty, label.fontSizeProperty, cursorProp,
                ),
            )
            it.widthProp.bind(it.scaleProperty)
            it.heightProp.bind(label.heightProp)
            it.yProp.bind(label.yProp.add(label.heightProp.subtract(it.heightProp).divide(2)))
        })
    }

    /** A child of the text, so the box's bevel is drawn before it rather than over it. */
    private inner class Caret(app: GApplet) : GraphicsElement(app) {
        override fun drawImpl() {
            if (app.eventDispatcher.focused !== this@TextInput) return
            if (!caretVisible) return

            val left = x.toFloat()
            val top = y.toFloat()
            val thickness = width.toFloat()
            val extent = height.toFloat()

            with(app.gg) {
                fill(fontColor)
                rect(left, top, thickness, extent)
            }
        }
    }

    /**
     * Processing sends no TYPE event for the editing keys, so they are handled on PRESS.
     * Characters cannot be registered per key code: a TYPE event carries no code, and the
     * modifier held for a capital would file it under a different option, hence
     * [anyModifiers] on the one TYPE entry.
     */
    private fun registerEditingKeys() {
        onKey(anyModifiers(KeyCode.BACK_SPACE, KeyAction.PRESS)) { at, current ->
            if (at > 0) {
                value set current.removeRange(at - 1, at)
                cursorProp.value = at - 1
            }
        }

        onKey(anyModifiers(KeyCode.DELETE, KeyAction.PRESS)) { at, current ->
            if (at < current.length) value set current.removeRange(at, at + 1)
        }

        onKey(anyModifiers(KeyCode.LEFT, KeyAction.PRESS)) { at, _ -> cursorProp.value = (at - 1).coerceAtLeast(0) }
        onKey(anyModifiers(KeyCode.RIGHT, KeyAction.PRESS)) { at, current ->
            cursorProp.value = (at + 1).coerceAtMost(current.length)
        }

        onKey(anyModifiers(KeyCode.HOME, KeyAction.PRESS)) { _, _ -> cursorProp.value = 0 }
        onKey(anyModifiers(KeyCode.END, KeyAction.PRESS)) { _, current -> cursorProp.value = current.length }

        onKey(anyModifiers(KeyCode.ENTER, KeyAction.PRESS)) { _, current -> onSubmit?.invoke(current) }

        // Exact registrations, so control takes precedence over the per-character ones above.
        onKey(options(KeyCode.LEFT, KeyAction.PRESS, CTRL)) { at, current ->
            cursorProp.value = wordStartBefore(current, at)
        }

        onKey(options(KeyCode.RIGHT, KeyAction.PRESS, CTRL)) { at, current ->
            cursorProp.value = wordEndAfter(current, at)
        }

        onKey(options(KeyCode.BACK_SPACE, KeyAction.PRESS, CTRL)) { at, current ->
            val from = wordStartBefore(current, at)

            if (from < at) {
                value set current.removeRange(from, at)
                cursorProp.value = from
            }
        }

        onKey(options(KeyCode.DELETE, KeyAction.PRESS, CTRL)) { at, current ->
            val to = wordEndAfter(current, at)

            if (to > at) value set current.removeRange(at, to)
        }

        registerCallback(anyModifiers(KeyCode.UNDEFINED, KeyAction.TYPE)) { event ->
            val key = event.key

            if (!key.isISOControl() && key != '￿') {
                val current = value.now ?: ""
                val at = cursorProp.value.coerceIn(0, current.length)

                lastEdit = clock()
                value set current.substring(0, at) + key + current.substring(at)
                cursorProp.value = at + 1
            }
        }
    }

    /** Start of the word left of [at], skipping any whitespace between the two. */
    private fun wordStartBefore(text: String, at: Int): Int {
        var index = at

        while (index > 0 && text[index - 1].isWhitespace()) index--
        while (index > 0 && !text[index - 1].isWhitespace()) index--

        return index
    }

    /** End of the word right of [at], skipping any whitespace between the two. */
    private fun wordEndAfter(text: String, at: Int): Int {
        var index = at

        while (index < text.length && text[index].isWhitespace()) index++
        while (index < text.length && !text[index].isWhitespace()) index++

        return index
    }

    /** Supplies the callback with a clamped cursor and the current value, and unblinks. */
    private fun onKey(option: KeyEventOption, edit: (at: Int, current: String) -> Unit) =
        registerCallback(option) {
            val current = value.now ?: ""

            lastEdit = clock()
            edit(cursorProp.value.coerceIn(0, current.length), current)
        }

    companion object {
        internal const val CARET_SOLID_MS = 500L
        internal const val CARET_BLINK_MS = 500L
    }
}
