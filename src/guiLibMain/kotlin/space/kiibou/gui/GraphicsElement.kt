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

val outline = System.getenv("outline")?.toBoolean() ?: false

abstract class GraphicsElement(open val app: GApplet, x: Int = 0, y: Int = 0, width: Int = 0, height: Int = 0) :
    Rectangle(x, y, width, height), MouseEventListener {
    val scaleProp = SimpleIntegerProperty(1)
    val scale: Int get() = scaleProp.value

    val childrenProperty: SimpleListProperty<GraphicsElement> =
        SimpleListProperty(synchronizedObservableList(observableArrayList()))
    val children: MutableList<GraphicsElement> get() = childrenProperty.value

    final override val mouseOptionMap: MouseOptionMap = MouseOptionMap()

    val id: Int = nextID()

    var hidden = false
        protected set

    private var parent: GraphicsElement? = null

    override var active: Boolean = true

    var hierarchyDepth: Int = 0
        private set

    private var insideDraw: Boolean = false

    private val deferredActions = Collections.synchronizedList(ArrayList<() -> Unit>())

    var clip: Boolean = false

    val unscaledWidth: Int
        get() = width / scale

    val unscaledHeight: Int
        get() = height / scale

    override fun registerCallback(option: MouseEventOption, callback: MouseEventConsumer) {
        super.registerCallback(option, callback)
        app.registerMethod("mouseEvent", this)
    }

    override fun unregisterCallback(option: MouseEventOption) {
        super.unregisterCallback(option)
        if (mouseOptionMap.isEmpty()) app.unregisterMethod("mouseEvent", this)
    }

    fun init() {
        children.forEach(GraphicsElement::init)
        initImpl()
    }

    /**
     * Called after children are initialized
     */
    protected open fun initImpl() {}

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
                    rect(
                        x.toFloat(),
                        y.toFloat(),
                        this@GraphicsElement.width.toFloat(),
                        this@GraphicsElement.height.toFloat()
                    )
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

    protected open fun drawImpl() {}

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

    operator fun plusAssign(element: GraphicsElement) {
        addChild(element)
    }

    operator fun get(index: Int): GraphicsElement {
        return children[index]
    }

    operator fun set(index: Int, element: GraphicsElement) {
        addChild(index, element)
    }

    open fun addChild(element: GraphicsElement) {
        addChild(children.size, element)
    }

    open fun addChild(index: Int, element: GraphicsElement) {
        checkCanModifyChildren()
        if (element.parent != null) throw IllegalArgumentException("The passed GraphicsElement is already child of another GraphicsElement.")

        children.add(index, element)

        element.let {
            it.parent = this
            it.scaleProp.bind(scaleProp)
            it.hierarchyDepth = hierarchyDepth + 1
        }
    }

    open fun removeChild(index: Int): GraphicsElement {
        checkCanModifyChildren()

        return children.removeAt(index).apply {
            parent = null
            scaleProp.unbind()
        }
    }

    fun removeChild(child: GraphicsElement): GraphicsElement {
        checkCanModifyChildren()

        val index = children.indexOf(child)

        if (index == -1) throw IllegalArgumentException("The passed GraphicsElement is not a child of this GraphicsElement.")

        return removeChild(index)
    }

    fun getChildIndex(child: GraphicsElement): Int = when {
        isChild(child) -> children.indexOf(child)
        else -> -1
    }

    fun replace(oldElement: GraphicsElement, newElement: GraphicsElement) {
        checkCanModifyChildren()

        val index = getChildIndex(oldElement)

        if (index == -1) throw IllegalArgumentException("The passed GraphicsElement can not be replaced because it is not a child of this GraphicsElement")

        removeChild(index)
        addChild(index, newElement)
    }

    fun removeAllChildren() {
        checkCanModifyChildren()

        while (children.isNotEmpty())
            removeChild(0)
    }

    fun isChild(child: GraphicsElement): Boolean {
        return equals(child.parent)
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

    private fun checkCanModifyChildren() {
        if (insideDraw) throw IllegalStateException("Can not modify children during draw")
    }

    override fun equals(other: Any?): Boolean {
        return (other as? GraphicsElement)?.id == id
    }

    final override fun toString(): String {
        return "${this.javaClass.simpleName}@${hashCode()}"
    }

    override fun hashCode(): Int {
        return id
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