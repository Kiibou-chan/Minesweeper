package space.kiibou.gui.text

import space.kiibou.GApplet
import space.kiibou.data.BLACK
import space.kiibou.data.Color
import space.kiibou.event.KeyAction
import space.kiibou.event.KeyEvent
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
        text = TextElement(app, value).also {
            it.fontSizeProperty.unbind()
            it.fontSizeProperty.bind(scaleProperty.multiply(fontSize))
            it.fontNameProperty.value = fontName
            it.fontColorProperty.value = fontColor

            addChild(it)

            it.xProp.bind(xProp)
            it.yProp.bind(yProp)
            // Minimum width keeps an empty input visible and clickable.
            widthProp.bind(javafx.beans.binding.Bindings.max(it.widthProp, scaleProperty.multiply(60)))
            heightProp.bind(it.heightProp)
        }
    }

    override fun keyEvent(event: KeyEvent) {
        super.keyEvent(event)

        if (!active || event.action != KeyAction.TYPE) return

        val current = value.now ?: ""

        when (val key = event.key) {
            '\b' -> if (current.isNotEmpty()) value set current.dropLast(1)
            '\n', '\r' -> onSubmit?.invoke(current)
            else -> if (!key.isISOControl() && key != '￿') value set (current + key)
        }
    }

    override fun drawImpl() {
        val text = text ?: return

        if (app.eventDispatcher.focused === this) {
            with(app.gg) {
                fill(text.fontColorProperty.value)
                rect(
                    (x + width).toFloat(), y.toFloat(),
                    (2 * scale).toFloat(), height.toFloat(),
                )
            }
        }
    }
}
