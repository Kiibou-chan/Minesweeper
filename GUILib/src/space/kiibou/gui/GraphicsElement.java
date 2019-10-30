package space.kiibou.gui;

import processing.core.PImage;
import space.kiibou.GApplet;
import space.kiibou.event.MouseEvent;
import space.kiibou.event.MouseEventListener;
import space.kiibou.event.MouseEventOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class GraphicsElement extends Rectangle implements MouseEventListener {
    private static int gID = 0;

    private final GApplet app;

    private boolean hidden;
    private final int scale;
    private final List<GraphicsElement> children;
    private GraphicsElement parent;

    private boolean active;
    private final MouseOptionMap mouseOptionMap;

    private int hierarchyDepth;
    private final int id;

    private boolean preInitialized;
    private boolean initialized;
    private boolean postInitialized;

    public GraphicsElement(GApplet app, int x, int y, int width, int height, int scale) {
        super(x, y, width, height);
        this.app = app;
        this.hidden = false;
        this.scale = scale;
        children = Collections.synchronizedList(new ArrayList<>());
        mouseOptionMap = new MouseOptionMap();
        active = true;
        hierarchyDepth = 0;
        id = nextID();
        preInitialized = false;
        initialized = false;
        postInitialized = false;
    }

    static Function<Rectangle, TileRenderer> tilemapRenderFactory(PImage image) {
        return (rectangle -> tileRenderFactory(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(), rectangle.getY() + rectangle.getHeight(), image));
    }

    private static TileRenderer tileRenderFactory(int u1, int v1, int u2, int v2, PImage tilemap) {
        return ((graphics, rectangle) -> graphics.image(tilemap, rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight(), u1, v1, u2, v2));
    }

    private static int nextID() {
        return gID++;
    }

    @Override
    public void registerCallback(MouseEventOption option, Consumer<MouseEvent> callback) {
        MouseEventListener.super.registerCallback(option, callback);
        app.registerMethod("mouseEvent", this);
    }

    @Override
    public void unregisterCallback(MouseEventOption option) {
        MouseEventListener.super.unregisterCallback(option);
        if (mouseOptionMap.isEmpty()) {
            app.unregisterMethod("mouseEvent", this);
        }
    }

    public final void preInit() {
        preInitImpl();
        preInitialized = true;
        getChildren().forEach(GraphicsElement::preInit);
    }

    /**
     * Called before children are pre initialized
     */
    protected abstract void preInitImpl();

    public final void init() {
        getChildren().forEach(GraphicsElement::init);
        initImpl();
        initialized = true;
    }

    /**
     * Called after children are initialized
     */
    protected abstract void initImpl();

    public final void postInit() {
        getChildren().forEach(GraphicsElement::postInit);
        postInitImpl();
        postInitialized = true;
    }

    /**
     * Called before children are post initialized
     */
    protected abstract void postInitImpl();

    public boolean isPreInitialized() {
        return preInitialized;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPostInitialized() {
        return postInitialized;
    }

    public final void draw() {
        if (!hidden) {
            drawImpl();
            getChildren().forEach(GraphicsElement::draw);
        }
    }

    protected abstract void drawImpl();


    public GraphicsElement moveTo(int x, int y) {
        int dx = x - getX();
        int dy = y - getY();
        move(dx, dy);
        return this;
    }

    public GraphicsElement move(int x, int y) {
        getChildren().forEach(child -> child.move(x, y));
        setX(getX() + x);
        setY(getY() + y);
        return this;
    }

    public GraphicsElement resize(int width, int height) {
        setWidth(width);
        setHeight(height);
        return this;
    }

    public GraphicsElement resizeUnscaled(int width, int height) {
        setWidth(width * scale);
        setHeight(height * scale);
        return this;
    }

    public int getUnscaledWidth() {
        return getWidth() / getScale();
    }

    public int getUnscaledHeight() {
        return getHeight() / getScale();
    }

    public int getScale() {
        return scale;
    }

    public List<GraphicsElement> getChildren() {
        return children;
    }

    public void addChild(GraphicsElement element) {
        addChild(children.size(), element);
    }

    public void addChild(int index, GraphicsElement element) {
        if (element.getParent() == null) {
            element.setParent(this);
            getChildren().add(index, element);
            element.hierarchyDepth = hierarchyDepth + 1;
        } else {
            throw new IllegalArgumentException("The passed GraphicsElement was is already child of another GraphicsElement.");
        }
    }

    public GraphicsElement getChild(int index) {
        return children.get(index);
    }

    public GraphicsElement removeChild(int index) {
        GraphicsElement removed = getChildren().remove(index);
        removed.setParent(null);
        return removed;
    }

    public int getChildIndex(GraphicsElement child) {
        if (isChild(child)) {
            return children.indexOf(child);
        } else {
            return -1;
        }
    }

    public void replace(GraphicsElement old, GraphicsElement newE) {
        int index = getChildIndex(old);

        if (index != -1) addChild(index, newE);
        else addChild(newE);
    }

    public GraphicsElement removeChild(GraphicsElement child) {
        int index = getChildren().indexOf(child);
        if (index >= 0) {
            return removeChild(index);
        } else {
            throw new IllegalArgumentException("The passed GraphicsElement is not a child of this GraphicsElement.");
        }
    }

    public boolean isChild(GraphicsElement child) {
        return (child != null && equals(child.getParent()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GraphicsElement that = (GraphicsElement) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public GraphicsElement getParent() {
        return parent;
    }

    public void setParent(GraphicsElement parent) {
        this.parent = parent;
    }

    public boolean hasParent() {
        return getParent() != null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public MouseOptionMap getMouseOptionMap() {
        return mouseOptionMap;
    }

    @Override
    public void activate() {
        MouseEventListener.super.activate();
        children.forEach(GraphicsElement::activate);
    }

    @Override
    public void deactivate() {
        MouseEventListener.super.deactivate();
        children.forEach(GraphicsElement::deactivate);
    }

    public GApplet getApp() {
        return app;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void hide() {
        hidden = true;
    }

    public void show() {
        hidden = false;
    }

    public int getHierarchyDepth() {
        return hierarchyDepth;
    }

    public int getId() {
        return id;
    }
}
