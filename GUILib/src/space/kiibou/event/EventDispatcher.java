package space.kiibou.event;

import processing.event.KeyEvent;
import processing.event.TouchEvent;
import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EventDispatcher {
    private final GApplet app;
    private final Map<String, Set<GraphicsElement>> registry;

    private GraphicsElement prevGraphicsElement;

    public EventDispatcher(GApplet app) {
        this.app = app;

        app.registerMethod("keyEvent", this);
        app.registerMethod("mouseEvent", this);
        app.registerMethod("touchEvent", this);

        Map<String, Set<GraphicsElement>> registry = new HashMap<>();
        registry.put("keyEvent", new HashSet<>());
        registry.put("mouseEvent", new HashSet<>());
        registry.put("touchEvent", new HashSet<>());
        this.registry = Collections.unmodifiableMap(registry);
    }

    private Optional<GraphicsElement> topElement(final int x, final int y, final Set<GraphicsElement> elements) {
        final List<GraphicsElement> colliding = elements.stream()
                .filter(element -> element.collides(x, y))
                .collect(Collectors.toList());
        final int highest = colliding.stream()
                .flatMapToInt(el -> IntStream.of(el.getHierarchyDepth()))
                .max().orElse(0);
        return colliding.stream()
                .filter(el -> el.getHierarchyDepth() == highest)
                .findFirst();
    }

    public void keyEvent(KeyEvent event) {
    }

    public void mouseEvent(processing.event.MouseEvent source) {
        MouseEvent event = new MouseEvent(source);
        Optional<GraphicsElement> opt = topElement(event.getX(), event.getY(), registry.get("mouseEvent"));

        opt.ifPresent(element -> {
            boolean sameElement = element.equals(prevGraphicsElement);

            if (!sameElement) {
                if (prevGraphicsElement != null) {
                    prevGraphicsElement.mouseEvent(new MouseEvent(event, MouseEventAction.ELEMENT_EXIT));
                }

                element.mouseEvent(new MouseEvent(event, MouseEventAction.ELEMENT_ENTER));
                prevGraphicsElement = element;
            }

            element.mouseEvent(event);
        });

        if (!opt.isPresent() && prevGraphicsElement != null) {
            prevGraphicsElement.mouseEvent(new MouseEvent(event, MouseEventAction.ELEMENT_EXIT));
            prevGraphicsElement = null;
        }
    }

    public void touchEvent(TouchEvent event) {
    }

    public void registerMethod(String eventType, GraphicsElement element) {
        switch (eventType) {
            case "keyEvent":
                registry.get("keyEvent").add(element);
                break;
            case "mouseEvent":
                registry.get("mouseEvent").add(element);
                break;
            case "touchEvent":
                registry.get("touchEvent").add(element);
                break;
            default:
                app.registerMethod(eventType, (Object) element);
                break;
        }
    }

    public void unregisterMethod(String eventType, GraphicsElement element) {
        switch (eventType) {
            case "keyEvent":
                registry.get("keyEvent").remove(element);
                break;
            case "mouseEvent":
                registry.get("mouseEvent").remove(element);
                break;
            case "touchEvent":
                registry.get("touchEvent").remove(element);
                break;
            default:
                app.registerMethod(eventType, (Object) element);
                break;
        }
    }

}
