package space.kiibou.gui

import processing.core.PConstants.*
import processing.core.PImage
import space.kiibou.GApplet
import space.kiibou.event.MouseEventConsumer
import space.kiibou.event.MouseEventListener
import space.kiibou.event.MouseEventOption
import space.kiibou.event.MouseOptionMap
import java.util.*

val outline = System.getenv("minesweeper.outline")?.toBoolean() ?: false

abstract class GraphicsElement(val app: GApplet, x: Int, y: Int, width: Int, height: Int, val scale: Int) : Rectangle(x, y, width, height), MouseEventListener {
    val children: MutableList<GraphicsElement> = Collections.synchronizedList(ArrayList())
    final override val mouseOptionMap: MouseOptionMap = MouseOptionMap()
    private val id: Int = nextID()
    private var hidden = false
    private var parent: GraphicsElement? = null
    override var active: Boolean = true
    var hierarchyDepth: Int = 0
        private set
    private var preInitialized: Boolean = false
    private var initialized: Boolean = false
    private var postInitialized: Boolean = false
    private var insideDraw: Boolean = false
    private val deferredActions = Collections.synchronizedList(ArrayList<() -> Unit>())

    var clip: Boolean = false

    override fun registerCallback(option: MouseEventOption, callback: MouseEventConsumer) {
        super.registerCallback(option, callback)
        app.registerMethod("mouseEvent", this)
    }

    override fun unregisterCallback(option: MouseEventOption) {
        super.unregisterCallback(option)
        if (mouseOptionMap.isEmpty()) app.unregisterMethod("mouseEvent", this)
    }

    fun preInit() {
        preInitImpl()
        preInitialized = true
        children.forEach(GraphicsElement::preInit)
    }

    /**
     * Called before children are pre initialized
     */
    protected abstract fun preInitImpl()

    fun init() {
        children.forEach(GraphicsElement::init)
        initImpl()
        this.initialized = true
    }

    /**
     * Called after children are initialized
     */
    protected abstract fun initImpl()

    fun postInit() {
        children.forEach(GraphicsElement::postInit)
        postInitImpl()
        postInitialized = true
    }

    /**
     * Called before children are post initialized
     */
    protected abstract fun postInitImpl()

    fun draw() {
        if (!hidden) {
            insideDraw = true
            val g = app.graphics
            /*if (clip) {
                g.clip(getX() * scale, getY() * scale, getWidth() * scale, getHeight() * scale);
            }*/
            g.pushMatrix()
            g.pushStyle()
            drawImpl()
            children.forEach(GraphicsElement::draw)
            if (outline) {
                with(g) {
                    stroke(255f, 50f, 50f)
                    noFill()
                    rect(x.toFloat(), y.toFloat(), this@GraphicsElement.width.toFloat(), this@GraphicsElement.height.toFloat())
                    textSize(2.5f * scale)
                    textAlign(CENTER, TOP)
                    fill(50f, 155f, 50f)
                    text(unscaledWidth, x + this@GraphicsElement.width / 2f, y.toFloat())
                    textAlign(LEFT, CENTER)
                    text(unscaledHeight, x.toFloat(), y + this@GraphicsElement.height / 2f)
                }
            }
            g.popStyle()
            g.popMatrix()
            /*if (clip) {
                g.noClip()
            }*/
            insideDraw = false

            deferredActions.forEach { it() }
            deferredActions.clear()
        }
    }

    protected abstract fun drawImpl()

    fun deferAfterDraw(action: () -> Unit) {
        if (insideDraw) deferredActions.add(action)
        else action()
    }

    open fun moveTo(nx: Int, ny: Int): GraphicsElement {
        val dx = nx - x
        val dy = ny - y
        move(dx, dy)
        return this
    }

    open fun move(dx: Int, dy: Int): GraphicsElement {
        children.forEach { it.move(dx, dy) }
        x += dx
        y += dy
        return this
    }

    fun resize(width: Int, height: Int): GraphicsElement {
        this.width = width
        this.height = height
        return this
    }

    fun resizeUnscaled(width: Int, height: Int): GraphicsElement {
        this.width = width * scale
        this.height = height * scale
        return this
    }

    val unscaledWidth: Int
        get() = width / scale

    val unscaledHeight: Int
        get() = height / scale

    operator fun plusAssign(element: GraphicsElement) {
        addChild(element)
    }

    operator fun plusAssign(p: Pair<Int, GraphicsElement>) {
        addChild(p.first, p.second)
    }

    open fun addChild(element: GraphicsElement) {
        addChild(children.size, element)
    }

    fun addChild(index: Int, element: GraphicsElement) {
        if (insideDraw)
            throw IllegalStateException("Can not modify children during draw")

        if (element.parent == null) {
            element.parent = this
            children.add(index, element)
            element.hierarchyDepth = hierarchyDepth + 1
        } else {
            throw IllegalArgumentException("The passed GraphicsElement is already child of another GraphicsElement.")
        }
    }

    open operator fun get(index: Int): GraphicsElement? {
        return children[index]
    }

    fun removeChild(index: Int): GraphicsElement {
        if (insideDraw)
            throw IllegalStateException("Can not modify children during draw")

        val removed: GraphicsElement = children.removeAt(index)
        removed.parent = null
        return removed
    }

    fun getChildIndex(child: GraphicsElement?): Int = when {
        isChild(child) -> children.indexOf(child)
        else -> -1
    }

    fun replace(old: GraphicsElement?, newE: GraphicsElement) {
        if (insideDraw)
            throw IllegalStateException("Can not modify children during draw")

        val index = getChildIndex(old)
        if (index != -1) addChild(index, newE) else addChild(newE)
    }

    fun removeChild(child: GraphicsElement?): GraphicsElement {
        if (insideDraw)
            throw IllegalStateException("Can not modify children during draw")

        val index = children.indexOf(child)
        return if (index >= 0) {
            removeChild(index)
        } else {
            throw IllegalArgumentException("The passed GraphicsElement is not a child of this GraphicsElement.")
        }
    }

    fun isChild(child: GraphicsElement?): Boolean {
        return child != null && equals(child.parent)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as GraphicsElement
        return id == that.id
    }

    override fun hashCode(): Int {
        return id
    }

    fun hasParent(): Boolean {
        return parent != null
    }

    override fun activate() {
        super.activate()
        children.forEach(GraphicsElement::activate)
    }

    override fun deactivate() {
        super.deactivate()
        children.forEach(GraphicsElement::deactivate)
    }

    fun hide() {
        hidden = true
    }

    fun show() {
        hidden = false
    }
}

private var gID = 0

fun tilemapRenderFactory(image: PImage): (Int, Int, Int, Int) -> TileRenderer {
    return { x, y, w, h -> tileRenderFactory(x, y, x + w, y + h, image) }
}

private fun tileRenderFactory(u1: Int, v1: Int, u2: Int, v2: Int, tilemap: PImage): TileRenderer {
    return { g, x, y, w, h -> g.image(tilemap, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), u1, v1, u2, v2) }
}

private fun nextID(): Int {
    return gID++
}