package com.kiibou;

import space.kiibou.GApplet;
import space.kiibou.event.MouseEventAction;
import space.kiibou.event.MouseEventButton;
import space.kiibou.event.MouseEventListener;
import space.kiibou.gui.Button;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.Picture;
import space.kiibou.gui.Rectangle;

import java.util.Timer;
import java.util.TimerTask;

public class ControlBar extends GraphicsElement {
    private final Map map;
    private SevenSegmentDisplay timerDisplay;
    private Button restartButton;
    private SevenSegmentDisplay bombsLeft;
    private final Timer timer;
    private TimerTask timerTask;

    private Picture smiley1;
    private Picture smiley2;
    private Picture smiley3;
    private Picture smiley4;

    public ControlBar(GApplet app, int scale, Map map) {
        super(app, 0, 0, 0, 0, scale);
        this.map = map;
        timer = new Timer("MapTimer", true);
    }

    @Override
    protected void preInitImpl() {
        bombsLeft = new SevenSegmentDisplay(getApp(), getScale(), 3, map.getBombs());
        bombsLeft.setLowerLimit(0);
        addChild(bombsLeft);

        Picture smileys = new Picture(getApp(), "pictures/smiley.png", getScale());

        restartButton = new Button(getApp(), getScale());

        smiley1 = smileys.subPicture(0, 0, 20, 20);
        restartButton.getBorder().addChild(smiley1);

        smiley2 = smileys.subPicture(20, 0, 20, 20);
        restartButton.getBorder().addChild(smiley2);

        smiley3 = smileys.subPicture(40, 0, 20, 20);
        restartButton.getBorder().addChild(smiley3);

        smiley4 = smileys.subPicture(60, 0, 20, 20);
        restartButton.getBorder().addChild(smiley4);

        restartButton.registerCallback(
                MouseEventListener.options(
                        MouseEventButton.LEFT, MouseEventAction.RELEASE
                ), event -> {
                    map.restart();
                    stopTimer();
                    setSmiley(SmileyStatus.NORMAL);
                }
        );

        addChild(restartButton);

        setSmiley(SmileyStatus.NORMAL);

        timerDisplay = new SevenSegmentDisplay(getApp(), getScale(), 3, 0);
        addChild(timerDisplay);
    }

    @Override
    protected void initImpl() {
        int height = getChildren().stream().mapToInt(Rectangle::getHeight).max().orElse(0);
        setHeight(height);
    }

    @Override
    protected void postInitImpl() {
    }

    @Override
    protected void drawImpl() {

    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);

        int timerX = getX() + width - timerDisplay.getWidth();
        timerDisplay.moveTo(timerX, timerDisplay.getY());

        int buttonX = getX() + width / 2 - restartButton.getWidth() / 2;
        restartButton.moveTo(buttonX, restartButton.getY());
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);

        int buttonY = getY() + height - restartButton.getHeight();
        restartButton.moveTo(restartButton.getX(), buttonY);
    }

    public void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                timerDisplay.inc();
            }
        };

        timer.schedule(timerTask, 0, 1000);
    }

    public void stopTimer() {
        if (timerTask != null)
            timerTask.cancel();
    }

    public void resetTimer() {
        timerDisplay.setValue(0);
    }

    public void resetBombsLeft() {
        bombsLeft.setValue(map.getBombs());
    }

    public SevenSegmentDisplay getTimerDisplay() {
        return timerDisplay;
    }

    public SevenSegmentDisplay getBombsLeft() {
        return bombsLeft;
    }

    public void setSmiley(SmileyStatus status) {
        switch (status) {
            case NORMAL:
                smiley1.show();
                smiley2.hide();
                smiley3.hide();
                smiley4.hide();
                break;
            case GLASSES:
                smiley1.hide();
                smiley2.hide();
                smiley3.hide();
                smiley4.show();
                break;
            case DEAD:
                smiley1.hide();
                smiley2.hide();
                smiley3.show();
                smiley4.hide();
                break;
            case SURPRISED:
                smiley1.hide();
                smiley2.show();
                smiley3.hide();
                smiley4.hide();
                break;
        }
    }
}
