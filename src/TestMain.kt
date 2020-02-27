import processing.core.PApplet
import processing.core.PImage
import space.kiibou.GApplet
import space.kiibou.gui.BorderBox
import space.kiibou.gui.GGraphics
import space.kiibou.gui.Picture
import space.kiibou.gui.VerticalList

class TestMain : GApplet() {

    override fun settings() {
        size(800, 800, GGraphics::class.java.canonicalName)
    }

    override fun setup() {
        frameRate(30f)
        /* TODO: 03/11/2019 \
         * Primitives:
         * arc (x, y, w, h, angle: start [radians], angle: stop [radians], mode { PIE, OPEN, CHORD })
         * circle (x, y, extent)
         * ellipse (x, y, w, h)
         * point (x, y)
         * quad (x1, y1, x2, y2, x3, y3, x4, y4)
         * square (x, y, extent)
         * triangle (x1, y1, x2, y2, x3, y3)
         *
         * Curves:
         * bezier (x1, y1, x2, y2, x3, y3, x4, y4)
         * curve (x1, y1, x2, y2, x3, y3, x4, y4)
         *  curveTightness (tightness: [-5..5])
         *
         * Modes:
         * ellipseMode (mode { RADIUS, CENTER, CORNER, CORNERS })
         * rectMode (mode { RADIUS, CENTER, CORNER, CORNERS })
         *
         * Vertex:
         * contour -> beginContour, endContour
         * bezierVertex (cx2, cy2, cx3, cy3, x4, y4)
         * curveVertex (x, y)
         * quadraticVertex (cx, cy, x, y)
         *
         * Transforms:
         * translate
         * rotate
         * scale
         * apply matrix?
         *
         * // TODO: 03/11/2019 : Text
         *
         * Add Events and Animations?
         *
         * Add Binding/Referential Properties
         */

        val list = VerticalList(this, 20, 20, 2)
        val p1 = Picture(this, PImage(100, 50), 2)
        val p2 = Picture(this, PImage(100, 40), 2)
        val p3 = Picture(this, PImage(100, 30), 2)

        p1.heightProp.bind(graphicsManager.mouseX)
        p2.heightProp.bind(graphicsManager.mouseY)

        list += BorderBox(this, 2).also {
            it.addChild(p1)
            it.bindProps(p1)
        }

        list += BorderBox(this, 2).also {
            it.addChild(p2)
            it.bindProps(p2)
        }

        list += BorderBox(this, 2).also {
            it.addChild(p3)
            it.bindProps(p3)
        }

        registerGraphicsElement(list)

    }

    override fun draw() {
        background(0xAA)
    }
}

fun main() {
    PApplet.main(TestMain::class.java)
}