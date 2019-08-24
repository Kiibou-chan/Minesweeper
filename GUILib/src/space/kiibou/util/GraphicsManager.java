package space.kiibou.util;

import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;

import java.util.ArrayList;
import java.util.List;

public class GraphicsManager {
    private final List<GraphicsElement> elements;
    private final GApplet app;

    public GraphicsManager(GApplet app) {
        this.app = app;
        app.registerMethod("pre", this);
        app.registerMethod("draw", this);
        elements = new ArrayList<>();
    }

    public void pre() {
        elements.forEach(GraphicsElement::preInit);
        elements.forEach(GraphicsElement::init);
        elements.forEach(GraphicsElement::postInit);
        app.unregisterMethod("pre", this);
    }

    public void draw() {
        elements.forEach(GraphicsElement::draw);
    }

    public void registerGraphicsElement(GraphicsElement element) {
        elements.add(element);
    }
}