package com.kiibou;

import processing.core.PGraphics;
import space.kiibou.GApplet;
import space.kiibou.event.MouseEvent;
import space.kiibou.event.MouseEventAction;
import space.kiibou.event.MouseEventButton;
import space.kiibou.event.MouseEventListener;
import space.kiibou.gui.Button;
import space.kiibou.gui.GraphicsElement;
import space.kiibou.gui.Picture;

import java.util.EnumSet;

import static space.kiibou.event.MouseEventListener.options;

public class Tile extends GraphicsElement {
    protected static final int tileWidth;
    protected static final int tileHeight;

    static {
        tileWidth = 16;
        tileHeight = 16;
    }

    /**
     * X-Position of the tile on the Map
     */
    private int tileX;

    /**
     * Y-Position of the tile on the Map
     */
    private int tileY;

    private Map map;

    private TileType type;
    private boolean revealed;
    private boolean flagged;

    private Button button;
    private Picture flag;
    private Picture tilePicture;

    public Tile(GApplet app, Map map, int scale, int tileX, int tileY) {
        super(app, 0, 0, scale * tileWidth, scale * tileHeight, scale);
        this.tileX = tileX;
        this.tileY = tileY;

        this.map = map;
        PGraphics g = app.getGraphics();
        this.type = TileType.EMPTY;

        revealed = false;
        flagged = false;

        flag = new Picture(app, "tiles/flag_tile.png", scale);
        flag.hide();

        tilePicture = new Picture(app, type.getPath(), scale);
        // tilePicture.hide();
    }

    @Override
    protected void preInitImpl() {
        addChild(tilePicture);

        button = new Button(getApp(), getScale());
        button.resize(getWidth(), getHeight());
        button.getBorder().addChild(flag);
        addChild(button);
    }

    @Override
    protected void initImpl() {
        button.moveTo(getX(), getY());
        tilePicture.resize(getWidth(), getHeight());
        tilePicture.moveTo(getX(), getY());

        button.registerCallback(
                MouseEventListener.options(
                        MouseEventButton.LEFT, MouseEventAction.RELEASE
                ), event -> {
                    map.getControlBar().setSmiley(SmileyStatus.NORMAL);
                    onLeftClick(event);
                }
        );

        button.registerCallback(
                MouseEventListener.options(
                        MouseEventButton.RIGHT, MouseEventAction.RELEASE
                ), this::onRightClick
        );

        button.registerCallback(
                options(
                        MouseEventButton.LEFT, MouseEventAction.PRESS
                ), event -> map.getControlBar().setSmiley(SmileyStatus.SURPRISED)
        );

        button.registerCallback(
                options(
                        MouseEventButton.LEFT, EnumSet.of(MouseEventAction.DRAG, MouseEventAction.ELEMENT_ENTER)
                ), event -> map.getControlBar().setSmiley(SmileyStatus.SURPRISED)
        );

        button.registerCallback(
                options(
                        MouseEventButton.LEFT, EnumSet.of(MouseEventAction.DRAG, MouseEventAction.ELEMENT_EXIT)
                ), event -> map.getControlBar().setSmiley(SmileyStatus.NORMAL)
        );

        flag.moveTo(button.getX() + button.getBorder().getBorderWidth(), button.getY() + button.getBorder().getBorderHeight());
        flag.resize(button.getBorder().getInnerWidth(), button.getBorder().getInnerHeight());
    }

    @Override
    protected void postInitImpl() {
    }

    @Override
    public void drawImpl() {
    }

    private void onLeftClick(MouseEvent event) {
        reveal();
    }

    private void onRightClick(MouseEvent event) {
        if (!revealed) {
            setFlagged(!flagged);
        }
    }

    private void reveal() {
        if (type == TileType.BOMB) {
            map.loose();
            setType(TileType.RED_BOMB);
        } else {
            map.reveal(tileX, tileY);
        }

        tilePicture.show();
    }

    public void reset() {
        setType(TileType.EMPTY);
        setRevealed(false);
    }

    @Override
    public void activate() {
        button.activate();
    }

    @Override
    public void deactivate() {
        button.deactivate();
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;

        int index;
        if (tilePicture != null) {
            index = getChildIndex(tilePicture);
            removeChild(tilePicture);
        } else index = getChildren().size();

        tilePicture = new Picture(getApp(), type.getPath(), getScale());
        addChild(index, tilePicture);
        tilePicture.moveTo(getX(), getY());
        tilePicture.resize(getWidth(), getHeight());
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;

        if (revealed) {
            button.hide();
            setFlagged(false);
            deactivate();
        } else {
            button.show();
            setFlagged(false);
            activate();
        }
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        if (this.flagged != flagged) {
            map.tileFlag(flagged);
        }

        if (flagged) flag.show();
        else flag.hide();

        this.flagged = flagged;
    }
}
