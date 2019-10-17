package space.kiibou;

import processing.core.PApplet;
import space.kiibou.event.EventDispatcher;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.util.GraphicsManager;

public class GApplet extends PApplet {
    private final GraphicsManager graphicsManager;
    private final EventDispatcher eventDispatcher;

    public GApplet() {
        graphicsManager = new GraphicsManager(this);
        eventDispatcher = new EventDispatcher(this);
    }

    public void registerMethod(String methodName, GraphicsElement target) {
        eventDispatcher.registerMethod(methodName, target);
    }

    public void unregisterMethod(String methodName, GraphicsElement target) {
        eventDispatcher.unregisterMethod(methodName, target);
    }

    public void registerGraphicsElement(GraphicsElement element) {
        graphicsManager.registerGraphicsElement(element);
    }

    public GraphicsManager getGraphicsManager() {
        return graphicsManager;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
}
