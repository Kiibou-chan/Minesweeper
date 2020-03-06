package space.kiibou.gui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.FXCollections.observableArrayList
import javafx.collections.FXCollections.synchronizedObservableList
import processing.core.PConstants.*
import processing.core.PImage
import space.kiibou.GApplet
import space.kiibou.data.Rectangle
import space.kiibou.event.MouseEventConsumer
import space.kiibou.event.MouseEventListener
import space.kiibou.event.MouseEventOption
import space.kiibou.event.MouseOptionMap
import java.util.*
import kotlin.collections.ArrayList

val outline = System.getenv("outline")?.toBoolean() ?: false

abstract class GraphicsElement(val app: GApplet, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0)
    : Rectangle(x, y, width, height), MouseEventListener {
    val scaleProp = SimpleIntegerProperty(1)
    val scale: Int
        get() = scaleProp.value
    val childrenProperty: SimpleListProperty<GraphicsElement> = SimpleListProperty(synchronizedObservableList(observableArrayList<GraphicsElement>()))
    val children: MutableList<GraphicsElement>
        get() = childrenProperty.value
    final override val mouseOptionMap: MouseOptionMap = MouseOptionMap()
    val id: Int = nextID()
    var hidden = false
        protected set
    private var parent: GraphicsElement? = null
    override var active: Boolean = true
    var hierarchyDepth: Int = 0
        private set
    private var preInitialized: Boolean = false
    private var initialized: Boolean = false
    private var postInitialized: Boolean = false
    private var insideDraw: Boolean = false
    private val deferredActions = Collections.synchronizedList(ArrayList<() -> Unit>())

    var clip: Boolean = true

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
            if (clip) {
                g.clip((x).toFloat(), (y).toFloat(), (width).toFloat(), (height).toFloat())
            }
            g.pushMatrix()
            g.pushStyle()
            drawImpl()
            children.forEach(GraphicsElement::draw)
            g.popStyle()
            g.popMatrix()
            if (clip) {
                g.noClip()
            }
            if (outline) {
                with(g) {
                    stroke(255f, 50f, 50f)
                    noFill()
                    rect(x.toFloat(), y.toFloat(), this@GraphicsElement.width.toFloat(), this@GraphicsElement.height.toFloat())
                    textSize(5f * scale)
                    textAlign(CENTER, TOP)
                    fill(50f, 155f, 50f)
                    text(unscaledWidth, x + this@GraphicsElement.width / 2f, y.toFloat())
                    textAlign(LEFT, CENTER)
                    text(unscaledHeight, x.toFloat(), y + this@GraphicsElement.height / 2f)
                }
            }
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
        x = x.plus(dx)
        y = y.plus(dy)
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
            element.scaleProp.bind(scaleProp)
            element.hierarchyDepth = hierarchyDepth + 1
        } else {
            throw IllegalArgumentException("The passed GraphicsElement is already child of another GraphicsElement.")
        }
    }

    open operator fun get(index: Int): GraphicsElement? {
        return children[index]
    }

    open fun removeChild(index: Int): GraphicsElement {
        if (insideDraw)
            throw IllegalStateException("Can not modify children during draw")

        val removed: GraphicsElement = children.removeAt(index)
        removed.parent = null
        removed.scaleProp.unbind()
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
        if (index != -1) {
            removeChild(index)
            addChild(index, newE)
        } else addChild(newE)
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

    final override fun toString(): String {
        return "${this.javaClass.simpleName}@${hashCode()}"
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