package space.kiibou.gui;

import space.kiibou.GApplet;
import space.kiibou.event.MouseEventAction;
import space.kiibou.event.MouseEventButton;

import java.util.EnumSet;

import static space.kiibou.event.MouseEventListener.options;
import static space.kiibou.gui.BorderStyle.IN;
import static space.kiibou.gui.BorderStyle.OUT;

public class Button extends GraphicsElement {
    private final BorderBox border;

    public Button(GApplet app, int scale) {
        super(app, 0, 0, 0, 0, scale);

        border = new BorderBox(getApp(), getScale());
        addChild(0, border);
    }

    @Override
    protected void preInitImpl() {
    }

    @Override
    protected void initImpl() {
        border.moveTo(getX(), getY());

        if (border.getWidth() > 0 && border.getHeight() > 0) {
            resize(border.getWidth(), border.getHeight());
        } else {
            border.resize(getWidth(), getHeight());
        }

        border.setBorderStyle(OUT);

        registerCallback(
                options(
                        MouseEventButton.LEFT, MouseEventAction.PRESS
                ), event -> border.setBorderStyle(IN)
        );

        registerCallback(
                options(
                        MouseEventButton.LEFT, EnumSet.of(MouseEventAction.DRAG, MouseEventAction.ELEMENT_ENTER)
                ), event -> border.setBorderStyle(IN)
        );

        registerCallback(
                options(
                        MouseEventButton.LEFT, MouseEventAction.RELEASE
                ), event -> border.setBorderStyle(OUT)
        );

        registerCallback(
                options(
                        MouseEventButton.LEFT, EnumSet.of(MouseEventAction.DRAG, MouseEventAction.ELEMENT_EXIT)
                ), event -> border.setBorderStyle(OUT)
        );
    }

    @Override
    protected void postInitImpl() {

    }

    @Override
    protected void drawImpl() {

    }

    public BorderBox getBorder() {
        return border;
    }
}
