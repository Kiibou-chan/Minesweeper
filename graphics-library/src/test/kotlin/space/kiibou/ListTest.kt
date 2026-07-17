package space.kiibou

import processing.core.PApplet
import space.kiibou.gui.Button
import space.kiibou.gui.VerticalList
import space.kiibou.gui.text.TextElement
import space.kiibou.reactive.now
import space.kiibou.reactive.observe
import space.kiibou.reactive.reactives.Events

class ListTest : GApplet() {
    override fun settings() {
        setScale(2)
        size(1200, 800, G2D)
    }

    lateinit var list: VerticalList
    override fun setup() {
        frameRate(30f)

        list = VerticalList(this, 3)

        val bigger = Button(this, TextElement(this, "Bigger"))
        val smaller = Button(this, TextElement(this, "Smaller"))

        val textSignal = Events.foldAll("-----") {
            bigger.clicked act { cur, _ -> "$cur-" }
            smaller.clicked act { cur, _ -> cur.substring(0, (cur.length - 1).coerceAtLeast(5)) }
        }

        val text = TextElement(this, textSignal)

        val add = Button(this, TextElement(this, "Add"))
        val remove = Button(this, TextElement(this, "Remove"))

        remove.deactivate()

        val extraText = TextElement(this, "abc")

        add.clicked.observe {
            list.addChild(extraText)
            add.deactivate()
            remove.activate()
        }

        remove.clicked.observe {
            list.removeChild(extraText)
            add.activate()
            remove.deactivate()
        }

        list += bigger
        list += smaller
        list += text
        list += add
        list += remove

        list.x = 10
        list.y = 10

        graphicsManager.scale = 4

        registerGraphicsElement(list)
    }

    override fun draw() {
        background(0xCC)

        fill(255)
        rect(list.x.toFloat(), list.y.toFloat(), list.width.toFloat(), list.height.toFloat())

        fill(0)
        text("${list.unscaledWidth} - ${list.unscaledHeight}", width - 150f, 20f)
    }
}

fun main() {
    PApplet.main(ListTest::class.java)
}
