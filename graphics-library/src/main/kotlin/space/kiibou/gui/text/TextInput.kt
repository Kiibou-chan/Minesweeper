package space.kiibou.gui.text

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
            it.xProp.bind(label.xProp.add(label.widthProp))
            it.yProp.bind(label.yProp)
            it.widthProp.bind(it.scaleProperty.multiply(2))
            it.heightProp.bind(label.heightProp)
        })
    }

    /** A child of the text, so the box's bevel is drawn before it rather than over it. */
    private inner class Caret(app: GApplet) : GraphicsElement(app) {
        override fun drawImpl() {
            if (app.eventDispatcher.focused !== this@TextInput) return

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

        val current = value.now ?: ""

        when (event.action) {
            // Processing sends no TYPE event for backspace or enter, only a press.
            KeyAction.PRESS -> when (event.keyCode) {
                KeyCode.BACK_SPACE -> if (current.isNotEmpty()) value set current.dropLast(1)
                KeyCode.ENTER -> onSubmit?.invoke(current)
                else -> {}
            }

            KeyAction.TYPE -> {
                val key = event.key
                if (!key.isISOControl() && key != '￿') value set (current + key)
            }

            else -> {}
        }
    }

}
