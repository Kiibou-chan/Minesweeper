package com.kiibou;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;
import space.kiibou.GApplet;

public class Minesweeper extends GApplet {
    private Map map;

    public static void main(String[] args) {
        GApplet.main(Minesweeper.class.getName());
    }

    @Override
    public void settings() {
        size(800, 800, P2D);
//        fullScreen(P2D);
        PJOGL.setIcon("pictures/icon.png");
    }

    @Override
    public void setup() {
        surface.setTitle("Minesweeper");

        ((PGraphicsOpenGL) g).textureSampling(2);
        frameRate(60);

        map = new Map(this, 0, 0, 9, 9, 2, 10);
        registerGraphicsElement(map);
    }

    @Override
    public void draw() {
        if (width != map.getWidth() || height != map.getHeight()) {
            surface.setSize(map.getWidth(), map.getHeight());
        }

        background(204);
    }
}
