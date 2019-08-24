package com.kiibou;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import space.kiibou.GApplet;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.ImageBuffer;
import space.kiibou.gui.Rectangle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SevenSegmentDisplay extends GraphicsElement {
    private static final PImage segments;
    private static final Map<Integer, PImage> digitBuffer;
    private static final int digitWidth;
    private static final int digitHeight;

    private static final BiConsumer<PGraphics, Integer> segmentARenderer;
    private static final BiConsumer<PGraphics, Integer> segmentBRenderer;
    private static final BiConsumer<PGraphics, Integer> segmentCRenderer;
    private static final BiConsumer<PGraphics, Integer> segmentDRenderer;
    private static final BiConsumer<PGraphics, Integer> segmentERenderer;
    private static final BiConsumer<PGraphics, Integer> segmentFRenderer;
    private static final BiConsumer<PGraphics, Integer> segmentGRenderer;

    static {
        segments = ImageBuffer.loadImage("pictures/number_segments.png");
        digitBuffer = new HashMap<>();
        digitWidth = 13;
        digitHeight = 23;

        segmentARenderer = segmentRenderer(2, 1, 9, 3, 2, 1, 9, 3, 13, conditionBuilder(0));
        segmentBRenderer = segmentRenderer(9, 2, 3, 9, 9, 4, 3, 9, 13, conditionBuilder(1));
        segmentCRenderer = segmentRenderer(9, 12, 3, 9, 9, 16, 3, 9, 13, conditionBuilder(2));
        segmentDRenderer = segmentRenderer(2, 19, 9, 3, 2, 25, 9, 3, 13, conditionBuilder(3));
        segmentERenderer = segmentRenderer(1, 12, 3, 9, 1, 16, 3, 9, 13, conditionBuilder(4));
        segmentFRenderer = segmentRenderer(1, 2, 3, 9, 1, 4, 3, 9, 13, conditionBuilder(5));
        segmentGRenderer = segmentRenderer(2, 10, 9, 3, 2, 13, 9, 3, 13, conditionBuilder(6));
    }

    private final AtomicInteger value = new AtomicInteger();
    private int digits;

    private PGraphics g;

    private PGraphics digitRenderer;
    private Rectangle[] digitCoordinates;

    private int lowerLimit;
    private int upperLimit;
    private boolean hasLowerLimit;
    private boolean hasUpperLimit;

    public SevenSegmentDisplay(GApplet app, int scale, int digits, int value) {
        super(app, 0, 0, scale * digitWidth * digits, scale * digitHeight, scale);
        this.digits = digits;
        this.value.set(value);
        this.g = app.getGraphics();

        lowerLimit = 0;
        upperLimit = 0;
        hasLowerLimit = false;
        hasUpperLimit = false;
    }

    private static BiConsumer<PGraphics, Integer> segmentRenderer(int x, int y, int w, int h, int tx, int ty, int tw, int th, int offset, Predicate<Integer> condition) {
        return (graphics, segments) -> graphics.image(SevenSegmentDisplay.segments, x, y, w, h, condition.test(segments) ? tx : tx + offset, ty, (condition.test(segments) ? tx : tx + offset) + tw, ty + th);
    }

    private static Predicate<Integer> conditionBuilder(int segment) {
        return segments -> ((1 << segment) & segments) > 0;
    }

    @Override
    protected void preInitImpl() {

    }

    @Override
    public void initImpl() {
        digitRenderer = getApp().createGraphics(digitWidth, digitHeight, PConstants.P2D);

        calcDigitCoordinates();
    }

    @Override
    protected void postInitImpl() {

    }

    @Override
    public void drawImpl() {
        int number = value.get();

        if (hasUpperLimit && number > upperLimit) {
            number = upperLimit;
        }

        if (hasLowerLimit && number < lowerLimit) {
            number = lowerLimit;
        }

        for (int i = digitCoordinates.length - 1; i >= 0; i--) {
            int digit = number % 10;
            PImage image;

            switch (digit) {
                case 0:
                    image = renderDigit(0b0111111);
                    break;
                case 1:
                    image = renderDigit(0b0000110);
                    break;
                case 2:
                    image = renderDigit(0b1011011);
                    break;
                case 3:
                    image = renderDigit(0b1001111);
                    break;
                case 4:
                    image = renderDigit(0b1100110);
                    break;
                case 5:
                    image = renderDigit(0b1101101);
                    break;
                case 6:
                    image = renderDigit(0b1111101);
                    break;
                case 7:
                    image = renderDigit(0b0000111);
                    break;
                case 8:
                    image = renderDigit(0b1111111);
                    break;
                case 9:
                    image = renderDigit(0b1101111);
                    break;
                default:
                    image = renderDigit(0b1111001);
                    break;
            }

            Rectangle coordinate = digitCoordinates[i];

            g.image(image, coordinate.getX(), coordinate.getY(), coordinate.getWidth(), coordinate.getHeight());
            number /= 10;
        }
    }

    private void calcDigitCoordinates() {
        int digitWidth = getWidth() / digits;
        digitCoordinates = new Rectangle[digits];
        for (int i = 0; i < digits; i++) {
            int x = getX() + digitWidth * i;
            digitCoordinates[i] = new Rectangle(x, getY(), digitWidth, getHeight());
        }
    }

    @Override
    public GraphicsElement move(int x, int y) {
        super.move(x, y);
        calcDigitCoordinates();
        return this;
    }

    @Override
    public GraphicsElement moveTo(int x, int y) {
        super.moveTo(x, y);
        calcDigitCoordinates();
        return this;
    }

    private PImage renderDigit(int segments) {
        return digitBuffer.computeIfAbsent(segments, s -> {
            digitRenderer.beginDraw();
            digitRenderer.background(0);

            // Render the segments A-G
            segmentARenderer.accept(digitRenderer, segments);
            segmentBRenderer.accept(digitRenderer, segments);
            segmentCRenderer.accept(digitRenderer, segments);
            segmentDRenderer.accept(digitRenderer, segments);
            segmentERenderer.accept(digitRenderer, segments);
            segmentFRenderer.accept(digitRenderer, segments);
            segmentGRenderer.accept(digitRenderer, segments);

            PImage res = digitRenderer.get();
            digitRenderer.endDraw();
            return res;
        });
    }

    public void inc() {
        value.getAndIncrement();
    }

    public void dec() {
        value.getAndDecrement();
    }

    public void add(int value) {
        this.value.addAndGet(value);
    }

    public void sub(int value) {
        this.value.addAndGet(-value);
    }

    public void mul(int i) {
        this.value.updateAndGet(v -> v * i);
    }

    public void div(int i) {
        this.value.updateAndGet(v -> v / i);
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        this.value.set(value);
    }

    public void setLowerLimit(int lowerLimit) {
        this.lowerLimit = lowerLimit;
        hasLowerLimit = true;
    }

    public void setUpperLimit(int upperLimit) {
        this.upperLimit = upperLimit;
        hasUpperLimit = true;
    }

    public void setLimit(int lowerLimit, int upperLimit) {
        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);
    }

    public void removeLowerLimit() {
        hasLowerLimit = false;
    }

    public void removeUpperLimit() {
        hasUpperLimit = false;
    }

    public void removeLimit() {
        removeLowerLimit();
        removeUpperLimit();
    }
}
