package com.kiibou

import processing.core.PConstants
import processing.core.PGraphics
import processing.core.PImage
import space.kiibou.GApplet
import space.kiibou.gui.GraphicsElement
import space.kiibou.gui.Rectangle
import space.kiibou.gui.loadImage
import java.util.*
import java.util.function.Predicate

class SevenSegmentDisplay(app: GApplet, scale: Int, private val digits: Int, var value: Int)
    : GraphicsElement(app, 0, 0, scale * digitWidth * digits, scale * digitHeight, scale) {

    companion object {
        private val segments: PImage = loadImage("pictures/number_segments.png")
        private val digitBuffer: MutableMap<Int, PImage> = HashMap()
        private const val digitWidth = 13
        private const val digitHeight = 23
        private var segmentARenderer: (PGraphics, Int) -> Unit = segmentRenderer(2, 1, 9, 3, 2, 1, 9, 3, 13, conditionBuilder(0))
        private var segmentBRenderer: (PGraphics, Int) -> Unit = segmentRenderer(9, 2, 3, 9, 9, 4, 3, 9, 13, conditionBuilder(1))
        private var segmentCRenderer: (PGraphics, Int) -> Unit = segmentRenderer(9, 12, 3, 9, 9, 16, 3, 9, 13, conditionBuilder(2))
        private var segmentDRenderer: (PGraphics, Int) -> Unit = segmentRenderer(2, 19, 9, 3, 2, 25, 9, 3, 13, conditionBuilder(3))
        private var segmentERenderer: (PGraphics, Int) -> Unit = segmentRenderer(1, 12, 3, 9, 1, 16, 3, 9, 13, conditionBuilder(4))
        private var segmentFRenderer: (PGraphics, Int) -> Unit = segmentRenderer(1, 2, 3, 9, 1, 4, 3, 9, 13, conditionBuilder(5))
        private var segmentGRenderer: (PGraphics, Int) -> Unit = segmentRenderer(2, 10, 9, 3, 2, 13, 9, 3, 13, conditionBuilder(6))

        private fun segmentRenderer(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int, tw: Int, th: Int, offset: Int, condition: Predicate<Int>): (PGraphics, Int) -> Unit {
            return { graphics, segments ->
                graphics.image(Companion.segments, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), if (condition.test(segments)) tx else tx + offset, ty, (if (condition.test(segments)) tx else tx + offset) + tw, ty + th)
            }
        }

        private fun conditionBuilder(segment: Int): Predicate<Int> {
            return Predicate { segments: Int -> 1 shl segment and segments > 0 }
        }
    }

    private val g: PGraphics = app.graphics
    private lateinit var digitRenderer: PGraphics
    private lateinit var digitCoordinates: Array<Rectangle>
    private var lowerLimit: Int
    private var upperLimit: Int
    private var hasLowerLimit: Boolean
    private var hasUpperLimit: Boolean

    override fun preInitImpl() {}
    public override fun initImpl() {
        digitRenderer = app.createGraphics(digitWidth, digitHeight, PConstants.P2D)
        calcDigitCoordinates()
    }

    override fun postInitImpl() {}

    public override fun drawImpl() {
        var number = value

        if (hasUpperLimit && number > upperLimit) {
            number = upperLimit
        }

        if (hasLowerLimit && number < lowerLimit) {
            number = lowerLimit
        }

        digitCoordinates.indices.reversed().forEach {
            val image: PImage = when (number % 10) {
                0 -> renderDigit(63)
                1 -> renderDigit(6)
                2 -> renderDigit(91)
                3 -> renderDigit(79)
                4 -> renderDigit(102)
                5 -> renderDigit(109)
                6 -> renderDigit(125)
                7 -> renderDigit(7)
                8 -> renderDigit(127)
                9 -> renderDigit(111)
                else -> renderDigit(121)
            }

            val coordinate = digitCoordinates[it]
            g.image(
                    image,
                    coordinate.x.toFloat(),
                    coordinate.y.toFloat(),
                    coordinate.width.toFloat(),
                    coordinate.height.toFloat()
            )
            number /= 10
        }
    }

    private fun calcDigitCoordinates() {
        val digitWidth = width / digits
        digitCoordinates = (0 until digits).map {
            Rectangle(this.x + digitWidth * it, y, digitWidth, height)
        }.toTypedArray()
    }

    override fun move(dx: Int, dy: Int): GraphicsElement {
        super.move(dx, dy)
        calcDigitCoordinates()
        return this
    }

    override fun moveTo(nx: Int, ny: Int): GraphicsElement {
        super.moveTo(nx, ny)
        calcDigitCoordinates()
        return this
    }

    private fun renderDigit(segments: Int): PImage {
        return digitBuffer.computeIfAbsent(segments) {
            digitRenderer.beginDraw()
            digitRenderer.background(0)
            // Render the segments A-G
            segmentARenderer(digitRenderer, it)
            segmentBRenderer(digitRenderer, it)
            segmentCRenderer(digitRenderer, it)
            segmentDRenderer(digitRenderer, it)
            segmentERenderer(digitRenderer, it)
            segmentFRenderer(digitRenderer, it)
            segmentGRenderer(digitRenderer, it)
            val res = digitRenderer.get()
            digitRenderer.endDraw()
            res
        }
    }

    fun setLowerLimit(lowerLimit: Int) {
        this.lowerLimit = lowerLimit
        hasLowerLimit = true
    }

    fun setUpperLimit(upperLimit: Int) {
        this.upperLimit = upperLimit
        hasUpperLimit = true
    }

    fun setLimit(lowerLimit: Int, upperLimit: Int) {
        setLowerLimit(lowerLimit)
        setUpperLimit(upperLimit)
    }

    fun removeLowerLimit() {
        hasLowerLimit = false
    }

    fun removeUpperLimit() {
        hasUpperLimit = false
    }

    fun removeLimit() {
        removeLowerLimit()
        removeUpperLimit()
    }

    operator fun inc(): SevenSegmentDisplay {
        value++
        return this
    }

    operator fun dec(): SevenSegmentDisplay {
        value--
        return this
    }

    init {
        lowerLimit = 0
        upperLimit = 0
        hasLowerLimit = false
        hasUpperLimit = false
    }
}