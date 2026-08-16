package space.kiibou.gui.text

import javafx.beans.property.SimpleIntegerProperty
import javafx.scene.input.KeyCode
import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.event.KeyAction
import space.kiibou.event.KeyEvent
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

    override fun keyEvent(event: KeyEvent) {
        super.keyEvent(event)

        if (!active) return
        if (event.action != KeyAction.RELEASE) lastEdit = clock()

        val current = value.now ?: ""
        val at = cursorProp.value.coerceIn(0, current.length)

        when (event.action) {
            // Processing sends no TYPE event for the editing keys, only a press.
            KeyAction.PRESS -> when (event.keyCode) {
                KeyCode.BACK_SPACE -> if (at > 0) {
                    value set current.removeRange(at - 1, at)
                    cursorProp.value = at - 1
                }

                KeyCode.DELETE -> if (at < current.length) value set current.removeRange(at, at + 1)

                KeyCode.LEFT -> cursorProp.value = (at - 1).coerceAtLeast(0)
                KeyCode.RIGHT -> cursorProp.value = (at + 1).coerceAtMost(current.length)
                KeyCode.HOME -> cursorProp.value = 0
                KeyCode.END -> cursorProp.value = current.length

                KeyCode.ENTER -> onSubmit?.invoke(current)

                else -> {}
            }

            KeyAction.TYPE -> {
                val key = event.key

                if (!key.isISOControl() && key != '￿') {
                    value set current.substring(0, at) + key + current.substring(at)
                    cursorProp.value = at + 1
                }
            }

            else -> {}
        }
    }

    companion object {
        internal const val CARET_SOLID_MS = 500L
        internal const val CARET_BLINK_MS = 500L
    }
}
